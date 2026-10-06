package dev.normlanguage.windows;

import java.io.*;
import java.net.http.HttpHeaders;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public final class HttpResult implements AutoCloseable {
    private final int status;
    private final HttpHeaders headers;
    private final InputStream source;
    private final boolean sse;
    private final int maximumBytes;
    private final ExecutorService workers;
    private final Runnable released;
    private int total;
    private volatile boolean closed;
    HttpResult(int status, HttpHeaders headers, InputStream source, int maximumBytes, ExecutorService workers, Runnable released) {
        this.status = status; this.headers = headers; this.source = source; this.maximumBytes = maximumBytes; this.workers = workers; this.released = released;
        sse = headers.firstValue("Content-Type").orElse("").toLowerCase(java.util.Locale.ROOT).startsWith("text/event-stream");
    }
    public int status() { return status; }
    public String header(String name) { return headers.firstValue(name).orElse(""); }
    public String event(int timeoutMillis) throws Exception {
        if (timeoutMillis < 1 || timeoutMillis > 20000) throw new IllegalArgumentException("Invalid event timeout");
        if (closed) return null;
        Future<String> pending = workers.submit(() -> {
            var frame = new ByteArrayOutputStream();
            var data = new StringBuilder();
            int value;
            while (!closed && (value = source.read()) != -1) {
                if (++total > maximumBytes) throw new IOException("Response exceeded bound");
                if (!sse || value != '\n') { frame.write(value); continue; }
                String line = frame.toString(StandardCharsets.UTF_8);
                frame.reset();
                if (line.endsWith("\r")) line = line.substring(0, line.length() - 1);
                if (line.isEmpty() && !data.isEmpty()) return data.toString();
                if (line.startsWith("data:")) data.append(line.substring(5).replaceFirst("^ ", "")).append('\n');
            }
            if (sse) return data.isEmpty() ? null : data.toString();
            return frame.size() == 0 ? null : frame.toString(StandardCharsets.UTF_8);
        });
        try { return pending.get(timeoutMillis, TimeUnit.MILLISECONDS); }
        catch (ExecutionException error) { close(); if (error.getCause() instanceof Exception cause) throw cause; throw error; }
        catch (InterruptedException | TimeoutException error) { close(); throw error; }
        finally { if (!pending.isDone()) pending.cancel(true); }
    }
    public synchronized void close() {
        if (closed) return;
        closed = true;
        try { source.close(); } catch (IOException ignored) { }
        released.run();
    }
}
