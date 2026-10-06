package dev.normlanguage.windows;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;

public final class ApplicationFiles {
    private ApplicationFiles() { }
    public static String executableOnPath(String filename) throws IOException {
        for (String directory : System.getenv().getOrDefault("PATH", "").split(java.io.File.pathSeparator)) {
            if (directory.isBlank()) continue;
            String found = findExecutable(directory, filename, 1);
            if (found != null) return found;
        }
        return null;
    }
    public static String materialize(String resource, String directory) throws IOException {
        if (!resource.matches("[A-Za-z0-9_./-]+") || resource.startsWith("/") || resource.contains(".."))
            throw new IOException("Invalid application resource path");
        byte[] content;
        try (var stream = ApplicationFiles.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) throw new IOException("Missing application resource: " + resource);
            content = stream.readNBytes(16777217);
            if (content.length > 16777216) throw new IOException("Application resource exceeds limit");
        }
        Path target = Path.of(directory).toAbsolutePath().normalize().resolve(resource);
        Files.createDirectories(target.getParent());
        if (Files.exists(target) && Arrays.equals(content, Files.readAllBytes(target))) return target.toString();
        Path pending = Files.createTempFile(target.getParent(), ".resource-", ".tmp");
        try {
            Files.write(pending, content);
            Files.move(pending, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(pending); }
        return target.toString();
    }
    public static String findExecutable(String directory, String filename, int depth) throws IOException {
        if (depth < 1 || depth > 8 || !filename.matches("[A-Za-z0-9_.-]+")) throw new IOException("Invalid executable search");
        Path root = Path.of(directory).toAbsolutePath();
        if (!Files.isDirectory(root)) return null;
        try (var paths = Files.find(root, depth, (path, attributes) -> attributes.isRegularFile() && path.getFileName().toString().equalsIgnoreCase(filename))) {
            return paths.sorted().map(Path::toString).findFirst().orElse(null);
        }
    }
}
