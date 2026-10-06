package dev.normlanguage.windows;
import java.net.URI;
public final class StructuredText {
    private StructuredText() { }
    public static boolean matches(String value, String pattern) { return value.matches(pattern); }
    public static java.util.List<String> pathSegments(String value, String scheme, String host) {
        URI uri = URI.create(value);
        if (!scheme.equalsIgnoreCase(uri.getScheme()) || !host.equalsIgnoreCase(uri.getHost()) || (uri.getPort() != -1 && uri.getPort() != 443) || uri.getUserInfo() != null) throw new IllegalArgumentException("Invalid URL origin");
        return java.util.List.of(uri.getRawPath().split("/"));
    }
}
