package dev.normlanguage.windows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Semaphore;

public final class AtomicTextStore {
    private final Path root;
    private final int maximumBytes;
    private final ConcurrentHashMap<String, Semaphore> locks = new ConcurrentHashMap<>();
    public AtomicTextStore(String directory, int maximumBytes) throws IOException {
        if (maximumBytes < 1) throw new IllegalArgumentException("Maximum bytes must be positive");
        Path requested = Path.of(directory).toAbsolutePath().normalize();
        Files.createDirectories(requested);
        if (Files.isSymbolicLink(requested)) throw new IOException("Store root must not be a symbolic link");
        root = requested.toRealPath();
        this.maximumBytes = maximumBytes;
    }
    private Path file(String key) throws IOException {
        if (key == null || !key.matches("[a-zA-Z0-9_-]{1,128}")) throw new IOException("Invalid store key");
        Path target = root.resolve(key + ".json");
        if (Files.isSymbolicLink(target)) throw new IOException("Symbolic links are not allowed");
        return target;
    }
    public String read(String key) throws IOException {
        Path target = file(key);
        if (!Files.exists(target, LinkOption.NOFOLLOW_LINKS)) return null;
        try (var input = Files.newInputStream(target, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes(maximumBytes + 1);
            if (bytes.length > maximumBytes) throw new IOException("Stored value exceeds limit");
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
    public void write(String key, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maximumBytes) throw new IOException("Stored value exceeds limit");
        Path target = file(key);
        Path pending = Files.createTempFile(root, ".pending-", ".tmp");
        try {
            try (FileChannel writer = FileChannel.open(pending, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) writer.write(buffer);
                writer.force(true);
            }
            try { Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { throw new IOException("Atomic replacement is required", e); }
        } finally { Files.deleteIfExists(pending); }
    }
    public StoreLease acquire(String key, int timeoutMillis) throws IOException, InterruptedException {
        file(key);
        if (timeoutMillis < 1) throw new IllegalArgumentException("Lock timeout must be positive");
        if (!locks.containsKey(key) && locks.size() >= 4096) throw new IOException("Store lock capacity exceeded");
        Semaphore lock = locks.computeIfAbsent(key, ignored -> new Semaphore(1, true));
        if (!lock.tryAcquire(timeoutMillis, TimeUnit.MILLISECONDS)) return null;
        Path lockPath = root.resolve(key + ".lock");
        FileChannel channel = null;
        boolean leased = false;
        try {
            channel = FileChannel.open(lockPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
            long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
            do {
                FileLock fileLock = null;
                try { fileLock = channel.tryLock(); } catch (OverlappingFileLockException ignored) { }
                if (fileLock != null) { leased = true; return new StoreLease(lock, channel, fileLock); }
                Thread.sleep(10);
            } while (System.nanoTime() < deadline);
            return null;
        } finally {
            if (!leased) { try { if (channel != null) channel.close(); } finally { lock.release(); } }
        }
    }
    public java.util.List<String> keys(String prefix, int limit) throws IOException {
        if (prefix == null || !prefix.matches("[a-zA-Z0-9_-]{1,80}") || limit < 1 || limit > 1000) throw new IOException("Invalid key listing request");
        record Entry(String key, long modified) { }
        java.util.List<Entry> entries = new java.util.ArrayList<>();
        try (var stream = Files.newDirectoryStream(root, prefix + "*.json")) {
            for (Path path : stream) {
                if (entries.size() >= 50000) throw new IOException("Store inventory exceeds scan limit");
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) continue;
                String name = path.getFileName().toString();
                entries.add(new Entry(name.substring(0, name.length() - 5), Files.getLastModifiedTime(path, LinkOption.NOFOLLOW_LINKS).toMillis()));
            }
        }
        entries.sort(java.util.Comparator.comparingLong(Entry::modified).reversed().thenComparing(Entry::key));
        return entries.stream().limit(limit).map(Entry::key).toList();
    }
    public static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
