package dev.normlanguage.windows;


import java.nio.charset.StandardCharsets;
import java.util.Base64;


public final class NativeCredentials {
    private NativeCredentials() { }
    public static String protect(String value) throws Exception { return transform(value, true); }
    public static String unprotect(String value) throws Exception { return transform(value, false); }
    private static String transform(String value, boolean protect) {
        if (value.isEmpty()) return "";
        if (value.length() > 16384) throw new IllegalArgumentException("Credential exceeds storage limit");
        if (protect) return Base64.getEncoder().encodeToString(com.sun.jna.platform.win32.Crypt32Util.cryptProtectData(value.getBytes(StandardCharsets.UTF_8)));
        return new String(com.sun.jna.platform.win32.Crypt32Util.cryptUnprotectData(Base64.getDecoder().decode(value)), StandardCharsets.UTF_8);
    }}
