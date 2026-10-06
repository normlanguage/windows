package dev.normlanguage.windows;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicBoolean;

public final class DesktopTray implements AutoCloseable {
    private TrayIcon icon;
    private final AtomicBoolean closed = new AtomicBoolean();
    public boolean install(String title, String showLabel, String exitLabel, Runnable show, Runnable exit) throws Exception {
        if (!SystemTray.isSupported()) return false;
        if (icon != null || closed.get()) throw new IllegalStateException("Tray lifecycle is already initialized");
        Runnable install = () -> {
            BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
            Graphics2D graphics = image.createGraphics();
            try {
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setColor(new Color(42, 63, 99)); graphics.fillRoundRect(0, 0, 32, 32, 10, 10);
                graphics.setColor(Color.WHITE); graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 23)); graphics.drawString("G", 7, 25);
            } finally { graphics.dispose(); }
            PopupMenu menu = new PopupMenu();
            MenuItem open = new MenuItem(showLabel); open.addActionListener(event -> { if (!closed.get()) show.run(); });
            MenuItem quit = new MenuItem(exitLabel); quit.addActionListener(event -> { if (!closed.get()) exit.run(); });
            menu.add(open); menu.addSeparator(); menu.add(quit);
            TrayIcon next = new TrayIcon(image, title, menu); next.setImageAutoSize(true);
            next.addActionListener(event -> { if (!closed.get()) show.run(); });
            try { SystemTray.getSystemTray().add(next); icon = next; }
            catch (AWTException failure) { throw new IllegalStateException("Tray installation failed", failure); }
        };
        if (EventQueue.isDispatchThread()) install.run(); else EventQueue.invokeAndWait(install);
        return icon != null;
    }
    public boolean available() { return icon != null && !closed.get(); }
    public void message(String title, String text, boolean error) {
        if (!available()) return;
        EventQueue.invokeLater(() -> { if (available()) icon.displayMessage(title, text, error ? TrayIcon.MessageType.ERROR : TrayIcon.MessageType.INFO); });
    }
    public void tooltip(String text) {
        if (!available()) return;
        EventQueue.invokeLater(() -> { if (available()) icon.setToolTip(text); });
    }
    @Override public void close() {
        if (closed.compareAndSet(false, true)) EventQueue.invokeLater(() -> { if (icon != null) { SystemTray.getSystemTray().remove(icon); icon = null; } });
    }
}
