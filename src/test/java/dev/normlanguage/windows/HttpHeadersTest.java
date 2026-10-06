package dev.normlanguage.windows;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class HttpHeadersTest {
    @Test
    void transportsCallerHeadersWithoutProtocolPolicy() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            var headers = exchange.getRequestHeaders();
            boolean valid = "value".equals(headers.getFirst("X-Caller"))
                && headers.getFirst("Mcp-Session-Id") == null
                && headers.getFirst("MCP-Protocol-Version") == null
                && "text/plain".equals(headers.getFirst("Content-Type"));
            exchange.sendResponseHeaders(valid ? 204 : 400, -1);
            exchange.close();
        });
        server.start();
        try (var session = new LocalHttpSession("http://127.0.0.1:" + server.getAddress().getPort() + "/", 4096)) {
            try (var response = session.request("POST", "payload", Map.of("X-Caller", "value", "Content-Type", "text/plain"), 1000)) {
                assertEquals(204, response.status());
            }
        } finally { server.stop(0); }
    }
}
