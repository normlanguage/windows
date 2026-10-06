package dev.normlanguage.windows;

import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.*;

public final class VerifiedArchiveTest {
    @org.junit.jupiter.api.Test
    void verifiesPlatformContract() throws Exception {
        Path root = Files.createTempDirectory("verified-archive-test-");
        Path zip = root.resolve("source.zip");
        byte[] content = "runtime".getBytes(StandardCharsets.UTF_8);
        try (var out = new ZipOutputStream(Files.newOutputStream(zip))) {
            out.putNextEntry(new ZipEntry("bin/tool.txt"));
            out.write(content);
            out.closeEntry();
        }
        String hash = digest(Files.readAllBytes(zip));
        String catalog = "bin/tool.txt\t" + digest(content) + "\n";
        var installer = new VerifiedArchive();
        Path installed = Path.of(installer.install(zip.toString(), hash, root.resolve("cache").toString(), "", catalog));
        require(Files.readString(installed.resolve("bin/tool.txt")).equals("runtime"));
        require(installer.progress() == 100);
        Files.writeString(installed.resolve("bin/tool.txt"), "broken");
        require(installer.install(zip.toString(), hash, root.resolve("cache").toString(), "", catalog).equals(installed.toString()));
        require(Files.readString(installed.resolve("bin/tool.txt")).equals("runtime"));
        expectFailure(() -> installer.install(zip.toString(), "0".repeat(64), root.resolve("invalid").toString(), "", catalog));
        expectFailure(() -> installer.install(zip.toString(), hash, root.resolve("traversal").toString(), "", "../outside\t" + digest(content)));
        expectFailure(() -> installer.install(zip.toString(), hash, root.resolve("duplicate").toString(), "", catalog + catalog));
        expectFailure(() -> installer.install(zip.toString(), hash, root.resolve("missing").toString(), "", "absent\t" + digest(content)));
        var cancelled = new VerifiedArchive();
        cancelled.cancel();
        expectFailure(() -> cancelled.install(zip.toString(), hash, root.resolve("cancelled").toString(), "", catalog));
        require(!Files.exists(root.resolve("outside")));
        System.out.println("VerifiedArchive: 8 cases passed");
    }
    private static String digest(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static void require(boolean condition) { if (!condition) throw new AssertionError(); }
    private static void expectFailure(Checked action) throws Exception {
        try { action.run(); } catch (java.io.IOException | IllegalArgumentException error) { return; }
        throw new AssertionError("Expected failure");
    }
    private interface Checked { void run() throws Exception; }
}
