package dev.normlanguage.windows;

import java.awt.Desktop;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;

public final class DesktopActions {
    private DesktopActions() { }
    public static void browse(String address) throws IOException {
        URI uri = URI.create(address);
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) throw new IOException("Only HTTP and HTTPS links are allowed");
        if (!Desktop.isDesktopSupported()) throw new IOException("Desktop integration is unavailable");
        Desktop.getDesktop().browse(uri);
    }
    public static void openDirectory(String directory) throws IOException {
        Path path = Path.of(directory).toRealPath();
        if (!Files.isDirectory(path)) throw new IOException("Expected a directory");
        if (!Desktop.isDesktopSupported()) throw new IOException("Desktop integration is unavailable");
        Desktop.getDesktop().open(path.toFile());
    }
    public static void copyText(String text) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
    }
}
