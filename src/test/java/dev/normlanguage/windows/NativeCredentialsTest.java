package dev.normlanguage.windows;

public class NativeCredentialsTest {
    @org.junit.jupiter.api.Test
    void verifiesPlatformContract() throws Exception {
        String protectedValue = NativeCredentials.protect("synthetic-native-credential-test");
        if (protectedValue.contains("synthetic") || !NativeCredentials.unprotect(protectedValue).equals("synthetic-native-credential-test")) throw new AssertionError("DPAPI round trip failed");
        if (!StructuredText.matches("plugin_asdk_app_123", "plugin_asdk_app_[A-Za-z0-9_-]+")) throw new AssertionError("Pattern match failed");
        for (String url : new String[] {"https://evil.test/apps/id", "https://user@chatgpt.com/apps/id", "http://chatgpt.com/apps/id", "https://chatgpt.com:444/apps/id"}) {
            try { StructuredText.pathSegments(url, "https", "chatgpt.com"); throw new AssertionError("Invalid origin accepted"); } catch (IllegalArgumentException expected) { }
        }
        System.out.println("Native credential and URL boundary checks passed");
    }
}
