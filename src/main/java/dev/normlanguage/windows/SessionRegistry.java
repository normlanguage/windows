package dev.normlanguage.windows;

import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SessionRegistry implements AutoCloseable {
    private static final class State {
        final Runnable disconnect;
        boolean authenticated;
        boolean busy;
        Thread work;
        ScheduledFuture<?> expiry;
        State(Runnable disconnect) { this.disconnect = disconnect; }
    }
    private final ConcurrentHashMap<String, State> sessions = new ConcurrentHashMap<>();
    private final Set<Thread> active = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    private final Semaphore capacity;
    private final int maximumSessions;
    private final int authenticationSeconds;
    private final int taskSeconds;
    private final AtomicBoolean closed = new AtomicBoolean();

    public SessionRegistry(int maximumSessions, int maximumTasks, int authenticationSeconds, int taskSeconds) {
        if (maximumSessions < 1 || maximumTasks < 1 || authenticationSeconds < 1 || taskSeconds < 1)
            throw new IllegalArgumentException("Positive limits are required");
        this.maximumSessions = maximumSessions;
        this.authenticationSeconds = authenticationSeconds;
        this.taskSeconds = taskSeconds;
        capacity = new Semaphore(maximumTasks);
    }
    public synchronized boolean register(String id, Runnable disconnect) {
        if (closed.get() || sessions.size() >= maximumSessions || sessions.containsKey(id)) return false;
        State state = new State(disconnect);
        sessions.put(id, state);
        state.expiry = timer.schedule(() -> {
            synchronized (state) { if (state.authenticated) return; }
            remove(id);
            disconnect.run();
        }, authenticationSeconds, TimeUnit.SECONDS);
        return true;
    }
    public boolean authenticate(String id) {
        State state = sessions.get(id);
        if (state == null) return false;
        synchronized (state) {
            if (sessions.get(id) != state) return false;
            state.authenticated = true;
            if (state.expiry != null) state.expiry.cancel(false);
            return true;
        }
    }
    public boolean authenticated(String id) {
        State state = sessions.get(id);
        if (state == null) return false;
        synchronized (state) { return state.authenticated; }
    }
    public boolean submit(String id, Runnable action) {
        State state = sessions.get(id);
        if (state == null || closed.get()) return false;
        synchronized (state) {
            if (!state.authenticated || state.busy || sessions.get(id) != state || !capacity.tryAcquire()) return false;
            state.busy = true;
            state.work = Thread.ofVirtual().unstarted(() -> {
                Thread executing = Thread.currentThread();
                ScheduledFuture<?> deadline = null;
                try {
                    deadline = timer.schedule(() -> { executing.interrupt(); state.disconnect.run(); }, taskSeconds, TimeUnit.SECONDS);
                    if (!executing.isInterrupted()) action.run();
                } finally {
                    if (deadline != null) deadline.cancel(false);
                    synchronized (state) { state.busy = false; state.work = null; }
                    active.remove(executing);
                    capacity.release();
                }
            });
            active.add(state.work);
            try { state.work.start(); }
            catch (RuntimeException error) { active.remove(state.work); state.work = null; state.busy = false; capacity.release(); throw error; }
            return true;
        }
    }
    public void remove(String id) {
        State state = sessions.remove(id);
        if (state != null) synchronized (state) {
            if (state.expiry != null) state.expiry.cancel(false);
            if (state.work != null) state.work.interrupt();
        }
    }
    @Override public void close() {
        if (closed.compareAndSet(false, true)) {
            sessions.forEach((id, state) -> { remove(id); try { state.disconnect.run(); } catch (RuntimeException ignored) { } });
            active.forEach(Thread::interrupt);
            timer.shutdownNow();
        }
    }
}
