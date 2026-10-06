package dev.normlanguage.windows;

import java.io.IOException;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;

public final class EndpointProbe {
    public static boolean tcpOpen(int port, int timeoutMillis) {
        if (port < 1 || port > 65535 || timeoutMillis < 1) throw new IllegalArgumentException("Invalid TCP probe");
        try (var socket = new Socket()) {
            socket.connect(new InetSocketAddress(InetAddress.getByAddress(new byte[] {127, 0, 0, 1}), port), timeoutMillis);
            return true;
        } catch (IOException error) { return false; }
    }
    public static URI loopbackUri(String text) {
        URI uri = URI.create(text.trim());
        String host = uri.getHost();
        if (!uri.getScheme().equals("http") || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null || uri.getPort() < 1 ||
                !("127.0.0.1".equals(host) || "[::1]".equals(host) || "::1".equals(host))) throw new IllegalArgumentException("Expected numeric loopback HTTP address");
        return uri;
    }
    public static int readinessFile(String path, int timeoutMillis) {
        try {
            Path file = Path.of(path);
            if (!Files.exists(file)) return -1;
            byte[] content;
            try (var source = Files.newInputStream(file)) { content = source.readNBytes(4097); }
            if (content.length > 4096) return 0;
            URI base = loopbackUri(new String(content, java.nio.charset.StandardCharsets.UTF_8));
            if (!(base.getPath().equals("/") || base.getPath().equals("/healthz") || base.getPath().equals("/readyz") || base.getPath().isEmpty())) return 0;
            try (var client = HttpClient.newBuilder().proxy(new ProxySelector() {
                public java.util.List<Proxy> select(URI uri) { return java.util.List.of(Proxy.NO_PROXY); }
                public void connectFailed(URI uri, SocketAddress address, IOException error) { }
            }).followRedirects(HttpClient.Redirect.NEVER).connectTimeout(Duration.ofMillis(timeoutMillis)).build()) {
                var request = HttpRequest.newBuilder(base.resolve("/readyz")).timeout(Duration.ofMillis(timeoutMillis)).GET().build();
                var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
                try (var body = response.body()) {
                    int status = response.statusCode();
                    return status == 404 || status == 405 || status == 501 ? -1 : status == 200 ? 1 : 0;
                }
            }
        } catch (InterruptedException error) { Thread.currentThread().interrupt(); return 0; }
          catch (IOException | IllegalArgumentException error) { return 0; }
    }
}
