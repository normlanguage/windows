package dev.normlanguage.windows;

import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.*;

public final class VerifiedArchive {
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private volatile int progress;
    private volatile InputStream active;

    public int progress() { return progress; }
    public void cancel() {
        cancelled.set(true);
        InputStream stream = active;
        if (stream != null) try { stream.close(); } catch (IOException ignored) { }
    }
    public static String hash(String path) throws IOException {
        try (var input = Files.newInputStream(Path.of(path))) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[65536];
            for (int size; (size = input.read(buffer)) != -1;) digest.update(buffer, 0, size);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
    public synchronized String install(String source, String sha256, String directory, String proxy, String catalog) throws IOException {
        check();
        if (!sha256.matches("[a-fA-F0-9]{64}")) throw new IllegalArgumentException("Invalid archive digest");
        Map<String, String> files = new LinkedHashMap<>();
        for (String line : catalog.split("\\R")) {
            if (line.isBlank()) continue;
            String[] parts = line.split("\\t", -1);
            if (parts.length != 2 || !parts[1].matches("[a-fA-F0-9]{64}")) throw new IOException("Invalid file catalog");
            safe(Path.of(directory).toAbsolutePath().normalize(), parts[0]);
            if (files.putIfAbsent(parts[0], parts[1].toLowerCase(Locale.ROOT)) != null) throw new IOException("Duplicate catalog path");
        }
        if (files.isEmpty() || files.size() > 100000) throw new IOException("Invalid file catalog size");
        Path parent = Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(parent);
        Path root = parent.resolve(sha256.toLowerCase(Locale.ROOT));
        try (var channel = FileChannel.open(parent.resolve(".install.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.tryLock()) {
            if (lock == null) throw new IOException("Another installation is active");
            progress = 0;
            if (!files.isEmpty() && Files.isDirectory(root)) {
                boolean valid = true;
                int count = 0;
                for (var item : files.entrySet()) {
                    check();
                    Path file = safe(root, item.getKey());
                    if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS) || !hash(file.toString()).equals(item.getValue())) { valid = false; break; }
                    progress = ++count * 20 / files.size();
                }
                if (valid) { progress = 100; return root.toString(); }
            }
            Path archive = Files.createTempFile(parent, ".download-", ".zip");
            Path staging = Files.createTempDirectory(parent, ".extract-");
            Path previous = parent.resolve(".replaced-" + UUID.randomUUID());
            try {
                try (InputStream input = open(source, proxy); OutputStream output = Files.newOutputStream(archive)) {
                    active = input;
                    byte[] buffer = new byte[65536];
                    long bytes = 0;
                    for (int size; (size = input.read(buffer)) != -1;) {
                        check();
                        bytes += size;
                        if (bytes > 4L * 1024 * 1024 * 1024) throw new IOException("Archive exceeds size limit");
                        output.write(buffer, 0, size);
                    }
                } finally { active = null; }
                check();
                if (!hash(archive.toString()).equalsIgnoreCase(sha256)) throw new IOException("Archive checksum mismatch");
                progress = 25;
                Map<String, String> extracted = new LinkedHashMap<>();
                Set<String> names = new HashSet<>();
                long totalBytes = 0;
                try (var zip = new ZipFile(archive.toFile())) {
                    var entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        check();
                        ZipEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (entry.isDirectory()) {
                            safe(staging, name.replaceFirst("/$", ""));
                            continue;
                        }
                        Path target = safe(staging, name);
                        if (!names.add(name.toLowerCase(Locale.ROOT)) || names.size() > 100000 || name.equals(".files")) throw new IOException("Invalid archive entries");
                        if (!files.isEmpty() && !files.containsKey(name)) throw new IOException("Archive has unlisted file");
                        Files.createDirectories(target.getParent());
                        try (InputStream input = zip.getInputStream(entry); OutputStream output = Files.newOutputStream(target, StandardOpenOption.CREATE_NEW)) {
                            active = input;
                            byte[] buffer = new byte[65536];
                            for (int size; (size = input.read(buffer)) != -1;) {
                                check();
                                totalBytes += size;
                                if (totalBytes > 8L * 1024 * 1024 * 1024) throw new IOException("Expanded archive exceeds limit");
                                output.write(buffer, 0, size);
                            }
                        } finally { active = null; }
                        String actual = hash(target.toString());
                        if (!files.isEmpty() && !actual.equals(files.get(name))) throw new IOException("File checksum mismatch");
                        extracted.put(name, actual);
                        progress = 25 + extracted.size() * 70 / Math.max(1, zip.size());
                    }
                }
                if (extracted.isEmpty() || (!files.isEmpty() && !extracted.keySet().equals(files.keySet()))) throw new IOException("Archive is incomplete");
                StringBuilder receipt = new StringBuilder();
                extracted.forEach((name, hash) -> receipt.append(name).append('\t').append(hash).append('\n'));
                Files.writeString(staging.resolve(".files"), receipt);
                check();
                if (Files.exists(root)) Files.move(root, previous, StandardCopyOption.ATOMIC_MOVE);
                try { Files.move(staging, root, StandardCopyOption.ATOMIC_MOVE); }
                catch (IOException error) {
                    if (Files.exists(previous)) Files.move(previous, root, StandardCopyOption.ATOMIC_MOVE);
                    throw error;
                }
                progress = 100;
                return root.toString();
            } finally {
                Files.deleteIfExists(archive);
                removeTree(staging);
                removeTree(previous);
            }
        } catch (java.nio.channels.OverlappingFileLockException error) { throw new IOException("Another installation is active", error); }
    }
    private InputStream open(String source, String proxy) throws IOException {
        if (!source.startsWith("https://")) return Files.newInputStream(Path.of(source));
        Proxy connectionProxy = Proxy.NO_PROXY;
        if (!proxy.isBlank()) {
            URI uri = URI.create(proxy);
            if (!"http".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null || uri.getUserInfo() != null) throw new IOException("Download proxy must be HTTP");
            connectionProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(uri.getHost(), uri.getPort() < 0 ? 80 : uri.getPort()));
        }
        URLConnection connection = URI.create(source).toURL().openConnection(connectionProxy);
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);
        return connection.getInputStream();
    }
    private void check() throws IOException {
        if (cancelled.get() || Thread.currentThread().isInterrupted()) throw new IOException("Installation cancelled");
    }
    private static Path safe(Path root, String name) throws IOException {
        if (name.isBlank() || name.contains("\\") || name.contains(":") || name.startsWith("/") || name.contains("\t") || name.contains("\n")) throw new IOException("Invalid archive path");
        for (String part : name.split("/", -1)) if (part.isEmpty() || part.equals(".") || part.equals("..") || part.endsWith(".") || part.endsWith(" ")) throw new IOException("Invalid archive path");
        Path result = root.resolve(name).normalize();
        if (!result.startsWith(root)) throw new IOException("Archive path escapes destination");
        return result;
    }
    private static void removeTree(Path directory) throws IOException {
        if (!Files.exists(directory)) return;
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }
}
