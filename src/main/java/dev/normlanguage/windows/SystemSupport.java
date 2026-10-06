package dev.normlanguage.windows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class SystemSupport {
    private SystemSupport() { }
    public static String validateProxy(String value) {
        if (value == null || value.isBlank()) return "";
        URI uri = URI.create(value.trim());
        if ((!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))
            || uri.getHost() == null || uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null
            || (uri.getPath() != null && !uri.getPath().isEmpty() && !uri.getPath().equals("/"))
            || uri.getPort() == 0 || uri.getPort() < -1 || uri.getPort() > 65535)
            throw new IllegalArgumentException("Use an HTTP/HTTPS proxy address without credentials, paths or queries");
        return uri.toASCIIString();
    }
    public static String validateEndpoint(String source) {
        URI uri = URI.create(source);
        boolean loopback = "127.0.0.1".equals(uri.getHost()) || "localhost".equals(uri.getHost()) || "[::1]".equals(uri.getHost());
        if (source.length() > 2048 || uri.getHost() == null || uri.getUserInfo() != null || uri.getFragment() != null || uri.getQuery() != null
            || (!"https".equalsIgnoreCase(uri.getScheme()) && !(loopback && "http".equalsIgnoreCase(uri.getScheme()))))
            throw new IllegalArgumentException("Destination must use HTTPS or loopback HTTP, without embedded credentials, query or fragment");
        return uri.toASCIIString();
    }
    public static long probeProxy(String source, int timeoutMillis) throws IOException {
        URI uri = URI.create(validateProxy(source));
        if (uri.getHost() == null || timeoutMillis < 1 || timeoutMillis > 15000) throw new IllegalArgumentException("A proxy address and bounded timeout are required");
        int port = uri.getPort() < 0 ? (uri.getScheme().equalsIgnoreCase("https") ? 443 : 80) : uri.getPort();
        long start = System.nanoTime();
        try (Socket socket = new Socket()) { socket.connect(new InetSocketAddress(uri.getHost(), port), timeoutMillis); }
        return (System.nanoTime() - start) / 1000000;
    }
    public static String formatTime(String epochMillis) {
        if (epochMillis == null || epochMillis.isEmpty()) return "—";
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(Long.parseLong(epochMillis)));
    }
    public static int utf8Size(String text) { return text.getBytes(StandardCharsets.UTF_8).length; }
}
