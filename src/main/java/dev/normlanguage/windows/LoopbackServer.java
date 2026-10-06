package dev.normlanguage.windows;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class LoopbackServer implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    private final Semaphore permits;
    private final int maximumBodyBytes;
    private final int deadlineSeconds;
    public LoopbackServer(int port, int maximumConcurrent, int maximumBodyBytes, int deadlineSeconds) throws IOException {
        if (port < 0 || port > 65535 || maximumConcurrent < 1 || maximumBodyBytes < 1 || deadlineSeconds < 1) throw new IllegalArgumentException("Invalid server configuration");
        this.server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), port), 32);
        this.permits = new Semaphore(maximumConcurrent);
        this.maximumBodyBytes = maximumBodyBytes;
        this.deadlineSeconds = deadlineSeconds;
    }
    public int port() { return server.getAddress().getPort(); }
    public void start(Consumer<HttpExchangeHandle> handler) {
        server.createContext("/", raw -> {
            HttpExchangeHandle exchange = new HttpExchangeHandle(raw, maximumBodyBytes);
            if (!permits.tryAcquire()) {
                try { exchange.respond(429, "application/json", "{\"error\":{\"message\":\"Too many concurrent requests\",\"type\":\"rate_limit_error\",\"code\":\"concurrency_limit\"}}"); }
                finally { exchange.close(); }
                return;
            }
            ScheduledFuture<?> deadline = timer.schedule(exchange::close, deadlineSeconds, TimeUnit.SECONDS);
            try { handler.accept(exchange); }
            catch (Throwable error) {
                if (!exchange.started()) try { exchange.respond(500, "application/json", "{\"error\":{\"message\":\"Internal server error\",\"type\":\"server_error\"}}"); } catch (IOException ignored) { }
            } finally { deadline.cancel(false); exchange.close(); permits.release(); }
        });
        server.setExecutor(executor);
        server.start();
    }
    public static long nowMillis() { return System.currentTimeMillis(); }
    public static String identifier() { return UUID.randomUUID().toString().replace("-", ""); }
    public static String randomToken() { byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes); return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    public static boolean secureEquals(String left, String right) { return left != null && right != null && MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8)); }
    @Override public void close() { server.stop(0); timer.shutdownNow(); executor.shutdownNow(); }
}
