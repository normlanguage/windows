package dev.normlanguage.windows;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public final class LocalHttpSession implements AutoCloseable {
    private final URI endpoint;
    private final int maximumBytes;
    private final HttpClient client;
    private final ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor();
    private final Set<HttpResult> responses = ConcurrentHashMap.newKeySet();
    private volatile boolean closed;
    public LocalHttpSession(String endpoint, int maximumBytes) {
        this.endpoint = EndpointProbe.loopbackUri(endpoint);
        if (maximumBytes < 1 || maximumBytes > 2097152) throw new IllegalArgumentException("Invalid response bound");
        this.maximumBytes = maximumBytes;
        client = HttpClient.newBuilder().proxy(new ProxySelector() {
            public List<Proxy> select(URI uri) { return List.of(Proxy.NO_PROXY); }
            public void connectFailed(URI uri, SocketAddress address, IOException error) { }
        }).followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofSeconds(2)).build();
    }
    public HttpResult request(String method, String body, Map<String, String> headers, int timeoutMillis) throws Exception {
        if (closed || timeoutMillis < 1 || timeoutMillis > 20000) throw new IllegalStateException("Invalid HTTP session state");
        var request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofMillis(timeoutMillis));
        headers.forEach(request::header);
        request.method(method, body.isEmpty() ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
        var response = client.send(request.build(), HttpResponse.BodyHandlers.ofInputStream());
        final HttpResult[] reference = new HttpResult[1];
        var result = new HttpResult(response.statusCode(), response.headers(), response.body(), maximumBytes, workers, () -> responses.remove(reference[0]));
        reference[0] = result;
        responses.add(result);
        if (closed) { result.close(); throw new IOException("HTTP session closed"); }
        return result;
    }
    public void close() {
        closed = true;
        responses.forEach(HttpResult::close);
        workers.shutdownNow();
        client.shutdownNow();
    }
}
