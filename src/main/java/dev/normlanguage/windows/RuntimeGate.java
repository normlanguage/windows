package dev.normlanguage.windows;

import java.util.concurrent.Semaphore;

public final class RuntimeGate {
    private final Semaphore lock = new Semaphore(1, true);
    public void enter() throws InterruptedException { lock.acquire(); }
    public void exit() { lock.release(); }
}
