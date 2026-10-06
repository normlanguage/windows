package dev.normlanguage.windows;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SessionRegistryTest {
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    @org.junit.jupiter.api.Test
    void verifiesPlatformContract() throws Exception {
        int passed = 0;
        try (var registry = new SessionRegistry(1, 1, 1, 3)) {
            require(registry.register("first", () -> {}), "register");
            require(!registry.register("second", () -> {}), "session bound");
            passed++;
            require(!registry.submit("first", () -> {}), "unauthenticated task");
            require(registry.authenticate("first"), "authenticate");
            passed++;
            var begun = new CountDownLatch(1);
            var stopped = new CountDownLatch(1);
            require(registry.submit("first", () -> {
                begun.countDown();
                try { Thread.sleep(10000); }
                catch (InterruptedException expected) { stopped.countDown(); }
            }), "submit");
            require(begun.await(2, TimeUnit.SECONDS), "task began");
            require(!registry.submit("first", () -> {}), "concurrent task bound");
            passed++;
            registry.remove("first");
            require(stopped.await(2, TimeUnit.SECONDS), "disconnect interrupts work");
            passed++;
            var completed = new CountDownLatch(1);
            require(registry.register("next", () -> {}), "register after disconnect");
            require(registry.authenticate("next"), "authenticate next");
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            boolean accepted = false;
            while (!accepted && System.nanoTime() < until) {
                accepted = registry.submit("next", completed::countDown);
                if (!accepted) Thread.sleep(10);
            }
            require(accepted && completed.await(2, TimeUnit.SECONDS), "capacity released after cancellation");
            passed++;
        }
        try (var registry = new SessionRegistry(1, 1, 1, 1)) {
            var expired = new CountDownLatch(1);
            require(registry.register("idle", expired::countDown), "idle register");
            require(expired.await(3, TimeUnit.SECONDS), "authentication deadline");
            require(!registry.authenticate("idle"), "expired session stays closed");
            passed++;
            var timedOut = new CountDownLatch(1);
            require(registry.register("long", timedOut::countDown), "long register");
            require(registry.authenticate("long"), "long authenticate");
            var interrupted = new AtomicBoolean();
            require(registry.submit("long", () -> { try { Thread.sleep(10000); } catch (InterruptedException expected) { interrupted.set(true); } }), "long submit");
            require(timedOut.await(3, TimeUnit.SECONDS), "execution deadline");
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
            while (!interrupted.get() && System.nanoTime() < until) Thread.sleep(10);
            require(interrupted.get(), "execution deadline interrupts worker");
            passed++;
        }
        System.out.println("PLATFORM_TESTS=" + passed + "/7 passed");
    }
}
