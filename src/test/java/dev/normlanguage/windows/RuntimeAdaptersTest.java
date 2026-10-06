package dev.normlanguage.windows;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

public final class RuntimeAdaptersTest {
    @org.junit.jupiter.api.Test
    void verifiesPlatformContract() throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/readyz", exchange -> { exchange.sendResponseHeaders(200, -1); exchange.close(); });
        server.createContext("/rpc", exchange -> {
            byte[] body = "data: {\"jsonrpc\":\"2.0\",\"id\":1,\"result\":{}}\n\n".getBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().add("Mcp-Session-Id", "own-session");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.createContext("/open", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().write("data: {\"result\":{}}\n\n".getBytes());
            exchange.getResponseBody().flush();
            try { Thread.sleep(3000); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            exchange.close();
        });
        server.createContext("/stalled", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().write("data: ".getBytes());
            exchange.getResponseBody().flush();
            try { Thread.sleep(3000); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            exchange.close();
        });
        server.setExecutor(java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        var health = Files.createTempFile("health", ".txt");
        try {
            int port = server.getAddress().getPort();
            if (!EndpointProbe.tcpOpen(port, 300)) throw new AssertionError("TCP probe");
            Files.writeString(health, "http://127.0.0.1:" + port + "/healthz");
            if (EndpointProbe.readinessFile(health.toString(), 500) != 1) throw new AssertionError("ready health");
            Files.writeString(health, "http://example.com/readyz");
            if (EndpointProbe.readinessFile(health.toString(), 500) != 0) throw new AssertionError("remote URL rejected");
            if (EndpointProbe.readinessFile(health + "missing", 500) != -1) throw new AssertionError("unknown health");
            try (var transport = new LocalHttpSession("http://127.0.0.1:" + port + "/rpc", 4096)) {
                var response = transport.request("POST", "{}", java.util.Map.of("X-Protocol", "fixture"), 500);
                try (response) {
                    if (response.status() != 200 || !response.header("Mcp-Session-Id").equals("own-session") || !response.event(500).contains("result")) throw new AssertionError("HTTP/SSE transport");
                }
            }
            try (var transport = new LocalHttpSession("http://127.0.0.1:" + port + "/open", 4096); var response = transport.request("POST", "{}", java.util.Map.of(), 500)) {
                long began = System.nanoTime();
                if (!response.event(500).contains("result") || System.nanoTime() - began > TimeUnit.SECONDS.toNanos(1)) throw new AssertionError("SSE awaited EOF");
            }
            try (var transport = new LocalHttpSession("http://127.0.0.1:" + port + "/stalled", 4096); var response = transport.request("POST", "{}", java.util.Map.of(), 500)) {
                boolean timedOut = false;
                try { response.event(100); } catch (java.util.concurrent.TimeoutException expected) { timedOut = true; }
                if (!timedOut || response.event(100) != null) throw new AssertionError("body timeout closes stream");
            }
            var java = System.getProperty("java.home") + "/bin/java";
            try (var process = new ProcessSession(java, System.getProperty("user.dir"), 4096)) {
                process.argument("-cp").argument(System.getProperty("windows.test.classpath")).argument("dev.normlanguage.windows.RuntimeAdaptersTest$Child");
                process.logMode(128).redact("top-secret");
                process.start();
                long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
                while ((process.alive() || !process.outputEnded()) && System.nanoTime() < until) Thread.sleep(10);
                if (process.exitCode() != 0) throw new AssertionError("child exit");
                if (!process.outputEnded()) throw new AssertionError("both output streams drained");
                var logs = process.logs();
                if (logs.size() > 128 || logs.size() < 100 || logs.stream().anyMatch(line -> line.contains("top-secret") || line.contains("undisclosed"))) throw new AssertionError("bounded redaction");
                if (logs.stream().noneMatch(line -> line.contains("stderr"))) throw new AssertionError("stderr captured: " + logs);
            }
            try (var process = new ProcessSession(java, System.getProperty("user.dir"), 4096)) {
                process.argument("-cp").argument(System.getProperty("windows.test.classpath")).argument("dev.normlanguage.windows.RuntimeAdaptersTest$Child");
                process.captureErrors(32).redact("top-secret");
                process.start();
                if (!"top-secret stdout 0".equals(process.receiveLine(5000))) throw new AssertionError("protocol stdout remains intact");
                Thread.sleep(1500);
                if (process.logs().stream().noneMatch(line -> line.contains("stderr")) || process.logs().stream().anyMatch(line -> line.contains("top-secret") || line.contains("undisclosed"))) throw new AssertionError("protocol mode captures redacted stderr");
            }
            long descendant;
            try (var process = new ProcessSession(java, System.getProperty("user.dir"), 4096)) {
                process.argument("-cp").argument(System.getProperty("windows.test.classpath")).argument("dev.normlanguage.windows.RuntimeAdaptersTest$Child").argument("parent");
                process.ownJob();
                process.start();
                String childPid = process.receiveLine(10000);
                if (childPid == null) throw new AssertionError("Child PID unavailable; alive=" + process.alive() + "; exit=" + process.exitCode());
                descendant = Long.parseLong(childPid);
                Thread.sleep(150);
                if (!(process.ownership().equals("windows-job") || process.ownership().equals("process-tree"))) throw new AssertionError("explicit ownership capability");
            }
            long until = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            while (ProcessHandle.of(descendant).map(ProcessHandle::isAlive).orElse(false) && System.nanoTime() < until) Thread.sleep(10);
            if (ProcessHandle.of(descendant).map(ProcessHandle::isAlive).orElse(false)) throw new AssertionError("owned descendant survived close");
            var gate = new RuntimeGate();
            gate.enter();
            var release = Thread.ofVirtual().start(gate::exit);
            release.join();
            gate.enter();
            gate.exit();
        } finally { server.stop(0); Files.deleteIfExists(health); }
        System.out.println("Runtime adapter tests passed");
    }
    public static final class Child {
        public static void main(String[] args) throws Exception {
            if (args.length > 0) {
                if (args[0].equals("parent")) {
                    var child = new ProcessBuilder(System.getProperty("java.home") + "/bin/java", "-cp", System.getProperty("java.class.path"), Child.class.getName(), "hold").start();
                    System.out.println(child.pid());
                }
                Thread.sleep(30000);
                return;
            }
            Thread.sleep(200);
            for (int i = 0; i < 100; i++) System.out.println("top-secret stdout " + i);
            Thread.sleep(100);
            System.err.println("top-secret stderr");
            System.err.println("Authorization: Bearer undisclosed");
        }
    }
}
