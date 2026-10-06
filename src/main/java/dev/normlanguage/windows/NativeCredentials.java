package dev.normlanguage.windows;


import java.nio.charset.StandardCharsets;
import java.util.Base64;


public final class NativeCredentials {
    private NativeCredentials() { }
    public static String protect(String value) throws Exception { return transform(value, true); }
    public static String unprotect(String value) throws Exception { return transform(value, false); }
    public static synchronized String loadOrCreateToken(String filename, String purpose) throws Exception {
        var path = java.nio.file.Path.of(filename).toAbsolutePath();
        java.nio.file.Files.createDirectories(path.getParent());
        byte[] entropy = purpose.getBytes(StandardCharsets.UTF_8);
        try (var channel = java.nio.channels.FileChannel.open(java.nio.file.Path.of(filename + ".lock"), java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            if (!java.nio.file.Files.exists(path)) {
                byte[] secret = new byte[32];
                new java.security.SecureRandom().nextBytes(secret);
                byte[] protectedBytes;
                try { protectedBytes = com.sun.jna.platform.win32.Crypt32Util.cryptProtectData(secret, entropy, 0, "", null); }
                finally { java.util.Arrays.fill(secret, (byte) 0); }
                var pending = java.nio.file.Files.createTempFile(path.getParent(), ".credential-", ".tmp");
                try {
                    java.nio.file.Files.write(pending, protectedBytes);
                    java.nio.file.Files.move(pending, path, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                } finally { java.nio.file.Files.deleteIfExists(pending); }
            }
            byte[] plain = com.sun.jna.platform.win32.Crypt32Util.cryptUnprotectData(java.nio.file.Files.readAllBytes(path), entropy, 0, null);
            try { return Base64.getEncoder().encodeToString(plain); }
            finally { java.util.Arrays.fill(plain, (byte) 0); }
        }
    }
    private static String transform(String value, boolean protect) {
        if (value.isEmpty()) return "";
        if (value.length() > 16384) throw new IllegalArgumentException("Credential exceeds storage limit");
        if (protect) return Base64.getEncoder().encodeToString(com.sun.jna.platform.win32.Crypt32Util.cryptProtectData(value.getBytes(StandardCharsets.UTF_8)));
        return new String(com.sun.jna.platform.win32.Crypt32Util.cryptUnprotectData(Base64.getDecoder().decode(value)), StandardCharsets.UTF_8);
    }}
