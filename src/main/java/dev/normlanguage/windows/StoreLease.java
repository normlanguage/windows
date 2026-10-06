package dev.normlanguage.windows;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.concurrent.Semaphore;

public final class StoreLease implements AutoCloseable {
    private Semaphore lock;
    private final FileChannel channel;
    private final FileLock fileLock;
    StoreLease(Semaphore lock, FileChannel channel, FileLock fileLock) {
        this.lock = lock;
        this.channel = channel;
        this.fileLock = fileLock;
    }
    @Override public void close() throws IOException {
        if (lock != null) {
            try { try { fileLock.release(); } finally { channel.close(); } }
            finally { lock.release(); lock = null; }
        }
    }
}