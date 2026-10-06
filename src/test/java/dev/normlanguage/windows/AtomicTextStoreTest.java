package dev.normlanguage.windows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;

public final class AtomicTextStoreTest {
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
    @org.junit.jupiter.api.Test
    void verifiesPlatformContract() throws Exception {
        Path root = Files.createTempDirectory("norm-store-test-");
        var store = new AtomicTextStore(root.toString(), 128);
        int passed = 0;
        try {
            store.write("entry", "中文\nfixture");
            require("中文\nfixture".equals(store.read("entry")), "UTF-8 roundtrip"); passed++;
            boolean bounded = false;
            try { store.write("oversize", "x".repeat(129)); } catch (java.io.IOException expected) { bounded = true; }
            require(bounded && store.read("oversize") == null, "bounded atomic write"); passed++;
            var held = store.acquire("scope", 1000);
            require(held != null, "acquire");
            require(store.acquire("scope", 30) == null, "same-store mutual exclusion"); passed++;
            try (var executor = Executors.newSingleThreadExecutor()) {
                executor.submit(() -> { held.close(); return null; }).get();
            }
            try (var renewed = store.acquire("scope", 1000)) { require(renewed != null, "lease release must be cross-thread safe"); }
            passed++;
            var other = new AtomicTextStore(root.toString(), 128);
            try (var heldAgain = store.acquire("shared", 1000)) {
                require(heldAgain != null && other.acquire("shared", 30) == null, "independent-store file locking");
            }
            try (var renewed = other.acquire("shared", 1000)) { require(renewed != null, "file lock released"); }
            passed++;
            require(store.keys("ent", 5).equals(java.util.List.of("entry")), "bounded key discovery"); passed++;
            boolean rejected = false;
            try { store.read("../outside"); } catch (java.io.IOException expected) { rejected = true; }
            require(rejected, "path traversal rejected"); passed++;
            require(SystemSupport.validateProxy("http://127.0.0.1:7890").equals("http://127.0.0.1:7890"), "valid proxy");
            require(SystemSupport.validateProxy("").isEmpty(), "explicit direct connection"); passed++;
            for (String bad : java.util.List.of("file:///C:/test", "https://user:password@example.test", "http://example.test/secret?token=x")) {
                boolean invalid = false;
                try { SystemSupport.validateProxy(bad); } catch (IllegalArgumentException expected) { invalid = true; }
                require(invalid, "invalid proxy rejected");
            }
            passed++;
            System.out.println("STORE_TESTS=" + passed + "/9 passed");
        } finally {
            try (var paths = Files.walk(root)) { for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path); }
        }
    }
}
