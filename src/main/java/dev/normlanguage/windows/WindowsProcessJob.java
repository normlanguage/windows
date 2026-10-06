package dev.normlanguage.windows;

import com.sun.jna.*;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.win32.StdCallLibrary;
import com.sun.jna.win32.W32APIOptions;

public final class WindowsProcessJob implements AutoCloseable {
    interface Kernel extends StdCallLibrary {
        Kernel INSTANCE = Native.load("kernel32", Kernel.class, W32APIOptions.UNICODE_OPTIONS);
        HANDLE CreateJobObjectW(Pointer security, WString name);
        boolean SetInformationJobObject(HANDLE job, int informationClass, Pointer information, int length);
        boolean AssignProcessToJobObject(HANDLE job, HANDLE process);
        HANDLE OpenProcess(int access, boolean inherit, int processId);
        boolean CloseHandle(HANDLE handle);
    }
    private HANDLE handle;
    private WindowsProcessJob(HANDLE handle) { this.handle = handle; }
    public static WindowsProcessJob attach(long pid) {
        if (!System.getProperty("os.name").startsWith("Windows")) return null;
        if (Native.POINTER_SIZE != 8) throw new IllegalStateException("Windows process ownership requires x64");
        Kernel kernel = Kernel.INSTANCE;
        HANDLE job = kernel.CreateJobObjectW(null, null);
        if (job == null) throw new IllegalStateException("Unable to create process ownership job");
        try {
            Memory information = new Memory(144);
            information.clear();
            information.setInt(16, 0x2000);
            if (!kernel.SetInformationJobObject(job, 9, information, 144)) throw new IllegalStateException("Unable to configure process ownership job");
            HANDLE process = kernel.OpenProcess(0x0001 | 0x0100, false, Math.toIntExact(pid));
            if (process == null) throw new IllegalStateException("Unable to acquire owned process");
            try { if (!kernel.AssignProcessToJobObject(job, process)) throw new IllegalStateException("Unable to assign owned process job: " + Native.getLastError()); }
            finally { kernel.CloseHandle(process); }
            return new WindowsProcessJob(job);
        } catch (RuntimeException error) { kernel.CloseHandle(job); throw error; }
    }
    public synchronized void close() {
        if (handle != null) { Kernel.INSTANCE.CloseHandle(handle); handle = null; }
    }
}
