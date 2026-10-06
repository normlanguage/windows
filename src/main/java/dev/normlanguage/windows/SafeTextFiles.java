package dev.normlanguage.windows;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public final class SafeTextFiles {
    private SafeTextFiles() { }
    private static Path validate(String source) throws IOException {
        Path path = Path.of(source);
        if (!path.isAbsolute() || source.startsWith("\\\\") || source.startsWith("//"))
            throw new IOException("Use an absolute local path, not a network share");
        path = path.normalize();
        Path current = path.getRoot();
        for (Path part : path) {
            current = current.resolve(part);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
                if (Files.isSymbolicLink(current) || !current.toRealPath(LinkOption.NOFOLLOW_LINKS).equals(current.toRealPath()))
                    throw new IOException("Linked paths and directory junctions are not supported");
            }
        }
        return path;
    }
    public static String canonical(String source) throws IOException {
        Path path = validate(source);
        if (Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return path.toRealPath(LinkOption.NOFOLLOW_LINKS).toString();
        return path.getParent().toRealPath().resolve(path.getFileName()).toString();
    }
    public static String name(String source) throws IOException { return validate(source).getFileName().toString(); }
    public static String parent(String source) throws IOException { return validate(source).getParent().toString(); }
    public static String child(String directory, String name) throws IOException {
        if (name.isEmpty() || name.equals(".") || name.equals("..") || name.contains("/") || name.contains("\\") || name.contains(":"))
            throw new IOException("Expected a single path component");
        return validate(validate(directory).resolve(name).toString()).toString();
    }
    public static boolean directory(String source) throws IOException { return Files.isDirectory(validate(source), LinkOption.NOFOLLOW_LINKS); }
    public static String read(String source, int maximumBytes) throws IOException {
        if (maximumBytes < 1) throw new IOException("A positive read limit is required");
        Path path = validate(source);
        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) return null;
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) throw new IOException("Expected a regular file");
        try (var input = Files.newInputStream(path, LinkOption.NOFOLLOW_LINKS)) {
            byte[] bytes = input.readNBytes(maximumBytes + 1);
            if (bytes.length > maximumBytes) throw new IOException("Text file exceeds the configured limit");
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        }
    }
    public static List<String> children(String directory, int maximumCount) throws IOException {
        if (maximumCount < 1) throw new IOException("A positive child limit is required");
        Path root = validate(directory);
        if (!Files.exists(root)) return List.of();
        List<String> result = new ArrayList<>();
        try (var children = Files.newDirectoryStream(root)) {
            for (Path child : children) {
                if (result.size() >= maximumCount) throw new IOException("Directory contains too many items");
                result.add(child.toString());
            }
        }
        result.sort(String::compareToIgnoreCase);
        return List.copyOf(result);
    }
    public static synchronized boolean compareAndWrite(String source, String expectedDigest, String content, int maximumBytes) throws IOException {
        Path path = validate(source);
        if (!Files.isDirectory(path.getParent(), LinkOption.NOFOLLOW_LINKS)) throw new IOException("The parent directory must already exist");
        String current = read(source, maximumBytes);
        String digest = current == null ? "absent" : AtomicTextStore.digest(current);
        if (!digest.equals(expectedDigest)) return false;
        if (content == null) {
            if (current != null) Files.delete(path);
            return true;
        }
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > maximumBytes) throw new IOException("Text exceeds the configured limit");
        Path temporary = Files.createTempFile(path.getParent(), ".windows-pending-", ".tmp");
        try {
            try (FileChannel stream = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) stream.write(buffer);
                stream.force(true);
            }
            validate(source);
            String check = read(source, maximumBytes);
            if (!(check == null ? "absent" : AtomicTextStore.digest(check)).equals(expectedDigest)) return false;
            Files.move(temporary, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } finally { Files.deleteIfExists(temporary); }
    }
}
