package dev.normlanguage.windows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class ApplicationFilesTest {
    @TempDir Path root;
    @Test void materializesExactBundledBytesAndRepairsDamage() throws Exception {
        String path = ApplicationFiles.materialize("windows-fixture.txt", root.toString());
        assertEquals("bundled-content", Files.readString(Path.of(path)).strip());
        Files.writeString(Path.of(path), "damaged");
        assertEquals(path, ApplicationFiles.materialize("windows-fixture.txt", root.toString()));
        assertEquals("bundled-content", Files.readString(Path.of(path)).strip());
        assertThrows(Exception.class, () -> ApplicationFiles.materialize("../outside", root.toString()));
    }
    @Test void preservesProtectedTokensAcrossLaunches() throws Exception {
        String path = root.resolve("api-key.dpapi").toString();
        String token = NativeCredentials.loadOrCreateToken(path, "test-purpose");
        assertEquals(44, token.length());
        assertEquals(token, NativeCredentials.loadOrCreateToken(path, "test-purpose"));
        assertFalse(java.util.Arrays.equals(Files.readAllBytes(Path.of(path)), java.util.Base64.getDecoder().decode(token)));
        assertThrows(Exception.class, () -> NativeCredentials.loadOrCreateToken(path, "wrong-purpose"));
    }
    @Test void searchesOnlyTheRequestedDirectoryDepth() throws Exception {
        Path binary = root.resolve("version/bin/tool.exe");
        Files.createDirectories(binary.getParent());
        Files.write(binary, new byte[] {77, 90});
        assertNull(ApplicationFiles.findExecutable(root.toString(), "tool.exe", 1));
        assertEquals(binary.toString(), ApplicationFiles.findExecutable(root.toString(), "tool.exe", 3));
        assertThrows(Exception.class, () -> ApplicationFiles.findExecutable(root.toString(), "../tool.exe", 3));
    }
}
