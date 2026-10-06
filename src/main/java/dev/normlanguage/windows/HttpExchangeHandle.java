package dev.normlanguage.windows;

import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.util.concurrent.atomic.AtomicBoolean;

public final class HttpExchangeHandle implements AutoCloseable {
    private final HttpExchange exchange;
    private final int maximumBodyBytes;
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile boolean started;
    HttpExchangeHandle(HttpExchange exchange, int maximumBodyBytes) {
        this.exchange = exchange;
        this.maximumBodyBytes = maximumBodyBytes;
    }
    public String method() { return exchange.getRequestMethod(); }
    public String path() { return exchange.getRequestURI().getPath(); }
    public String header(String name) { return exchange.getRequestHeaders().getFirst(name); }
    public String body() throws IOException {
        byte[] bytes = exchange.getRequestBody().readNBytes(maximumBodyBytes + 1);
        if (bytes.length > maximumBodyBytes) return null;
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    }
    public boolean started() { return started; }
    public void responseHeader(String name, String value) {
        if (started || closed.get()) throw new IllegalStateException("Response already started or closed");
        exchange.getResponseHeaders().set(name, value);
    }
    public void begin(int status, String contentType) throws IOException {
        if (started || closed.get()) throw new IOException("Response already started or closed");
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.getResponseHeaders().set("Referrer-Policy", "no-referrer");
        exchange.sendResponseHeaders(status, 0);
        started = true;
    }
    public void write(String value) throws IOException {
        if (!started || closed.get()) throw new IOException("Response is not writable");
        OutputStream output = exchange.getResponseBody();
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.flush();
    }
    public void respond(int status, String contentType, String value) throws IOException {
        begin(status, contentType);
        write(value);
    }
    @Override public void close() { if (closed.compareAndSet(false, true)) exchange.close(); }
}
