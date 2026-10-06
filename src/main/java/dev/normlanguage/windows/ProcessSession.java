package dev.normlanguage.windows;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class ProcessSession implements AutoCloseable {
    private final ProcessBuilder builder;
    private final BlockingQueue<String> lines = new ArrayBlockingQueue<>(64);
    private final AtomicBoolean closed = new AtomicBoolean();
    private final int maximumLineChars;
    private volatile Process process;
    private volatile IOException readFailure;
    private volatile boolean outputEnded;
    private volatile boolean errorsEnded;
    private OutputStream input;
    private Thread reader;
    private Thread errors;
    private int logCapacity = 128;
    private boolean logOnly;
    private final ArrayDeque<String> logLines = new ArrayDeque<>();
    private final List<String> secrets = new ArrayList<>();
    private WindowsProcessJob job;
    private boolean requireOwnership;
    private String ownership = "process-tree";
    private final Set<ProcessHandle> owned = ConcurrentHashMap.newKeySet();
    private Thread tracker;

    public synchronized ProcessSession ownJob() {
        if (process != null) throw new IllegalStateException("Process already started");
        requireOwnership = true;
        return this;
    }

    public synchronized ProcessSession logMode(int maximumLines) {
        if (process != null || maximumLines < 1 || maximumLines > 2048) throw new IllegalArgumentException("Invalid log configuration");
        logCapacity = maximumLines;
        logOnly = true;
        return this;
    }
    public synchronized ProcessSession captureErrors(int maximumLines) {
        if (process != null || maximumLines < 1 || maximumLines > 2048) throw new IllegalArgumentException("Invalid log configuration");
        logCapacity = maximumLines;
        logOnly = false;
        return this;
    }
    public synchronized ProcessSession redact(String secret) {
        if (process != null) throw new IllegalStateException("Process already started");
        if (secret != null && !secret.isEmpty()) secrets.add(secret);
        return this;
    }
    public synchronized List<String> logs() { return List.copyOf(logLines); }
    public String ownership() { return ownership; }
    public int exitCode() { return process == null || process.isAlive() ? -1 : process.exitValue(); }

    public ProcessSession(String executable, String directory, int maximumLineChars) {
        if (executable == null || executable.isBlank() || maximumLineChars < 1) throw new IllegalArgumentException("Invalid process configuration");
        this.builder = new ProcessBuilder(executable).directory(new File(directory));
        this.maximumLineChars = maximumLineChars;
    }
    public ProcessSession argument(String value) {
        if (process != null) throw new IllegalStateException("Process already started");
        builder.command().add(Objects.requireNonNull(value));
        return this;
    }
    public ProcessSession environment(String key, String value) {
        if (process != null) throw new IllegalStateException("Process already started");
        builder.environment().put(key, value);
        return this;
    }
    public ProcessSession removeEnvironment(String key) {
        if (process != null) throw new IllegalStateException("Process already started");
        builder.environment().remove(key);
        return this;
    }
    public synchronized void start() throws IOException {
        if (process != null || closed.get()) throw new IllegalStateException("Invalid process lifecycle");
        process = builder.start();
        if (requireOwnership) {
            try { job = WindowsProcessJob.attach(process.pid()); ownership = job == null ? "process-tree" : "windows-job"; }
            catch (RuntimeException error) {
                ownership = "process-tree";
                if (logCapacity != 0) appendLog("ownership", "Windows Job Object unavailable; owned process-tree cleanup active; crash cleanup is not guaranteed");
            }
            owned.addAll(process.descendants().toList());
            tracker = Thread.ofVirtual().name("norm-process-ownership").start(() -> {
                try {
                    while (!closed.get() && process.isAlive()) {
                        owned.removeIf(handle -> !handle.isAlive());
                        owned.addAll(process.descendants().toList());
                        Thread.sleep(25);
                    }
                } catch (InterruptedException error) { Thread.currentThread().interrupt(); }
            });
        }
        input = process.getOutputStream();
        reader = Thread.ofVirtual().name("norm-process-stdout").start(() -> {
            try (Reader source = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder line = new StringBuilder();
                int value;
                while (!closed.get() && (value = source.read()) != -1) {
                    if (value == '\n') {
                        if (!line.isEmpty() && line.charAt(line.length() - 1) == '\r') line.setLength(line.length() - 1);
                        if (logOnly) appendLog("stdout", line.toString()); else lines.put(line.toString());
                        line.setLength(0);
                    } else {
                        if (line.length() >= maximumLineChars && !logOnly) throw new IOException("Process output line exceeded limit");
                        if (line.length() < maximumLineChars) line.append((char) value);
                    }
                }
                if (!line.isEmpty() && !closed.get()) { if (logOnly) appendLog("stdout", line.toString()); else lines.put(line.toString()); }
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
              catch (IOException e) { if (!closed.get()) readFailure = e; }
            finally { outputEnded = true; }
        });
        errors = Thread.ofVirtual().name("norm-process-stderr").start(() -> {
            try (Reader source = new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8)) {
                StringBuilder line = new StringBuilder();
                int value;
                while (!closed.get() && (value = source.read()) != -1) {
                    if (value == '\n') { appendLog("stderr", line.toString()); line.setLength(0); }
                    else if (line.length() < maximumLineChars) line.append((char) value);
                }
                if (!line.isEmpty()) appendLog("stderr", line.toString());
            } catch (IOException ignored) { }
            finally { errorsEnded = true; }
        });
    }
    private synchronized void appendLog(String source, String value) {
        for (String secret : secrets) value = value.replace(secret, "[redacted]");
        value = value.replaceAll("(?i)(authorization[\\s:=]+)(?:bearer|basic)[ \\t]+[^\\s,;]+", "$1[redacted]");
        value = value.replaceAll("(?i)(authorization|api[_-]?key|access[_-]?token|token|password|secret)([\\s:=]+)[^\\s,;]+", "$1$2[redacted]");
        value = value.replaceAll("(?i)(https?://)[^/@\\s]+:[^/@\\s]+@", "$1[redacted]@");
        while (logLines.size() >= logCapacity) logLines.removeFirst();
        logLines.addLast(source + ": " + value);
    }
    public synchronized void sendLine(String line) throws IOException {
        if (input == null || closed.get()) throw new IOException("Process is not running");
        if (line.indexOf('\n') >= 0 || line.indexOf('\r') >= 0 || line.length() > maximumLineChars) throw new IOException("Invalid outbound process line");
        input.write(line.getBytes(StandardCharsets.UTF_8));
        input.write('\n');
        input.flush();
    }
    public String receiveLine(int timeoutMillis) throws IOException, InterruptedException {
        if (timeoutMillis < 1) throw new IllegalArgumentException("Timeout must be positive");
        String value = lines.poll(timeoutMillis, TimeUnit.MILLISECONDS);
        if (value == null && readFailure != null) throw readFailure;
        return value;
    }
    public boolean alive() { return process != null && process.isAlive(); }
    public boolean outputEnded() { return outputEnded && errorsEnded && lines.isEmpty(); }
    public long pid() { return process == null ? -1 : process.pid(); }
    @Override public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        if (input != null) try { input.close(); } catch (IOException ignored) { }
        if (process != null) {
            owned.addAll(process.descendants().toList());
            List<ProcessHandle> children = List.copyOf(owned);
            children.forEach(ProcessHandle::destroy);
            process.destroy();
            try { if (!process.waitFor(750, TimeUnit.MILLISECONDS)) process.destroyForcibly(); }
            catch (InterruptedException e) { process.destroyForcibly(); Thread.currentThread().interrupt(); }
            children.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
            if (job != null) job.close();
            try { process.getInputStream().close(); } catch (IOException ignored) { }
            try { process.getErrorStream().close(); } catch (IOException ignored) { }
        }
        if (reader != null) reader.interrupt();
        if (errors != null) errors.interrupt();
        if (tracker != null) tracker.interrupt();
        lines.clear();
    }
}
