package net.flex.dci.otc.controller.status.core.worker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * 2026/9/21
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class KeyAffinityExecutor {

    private final ExecutorService[] workers;

    public KeyAffinityExecutor(int n, String namePrefix) {
        this.workers = new ExecutorService[n];
        for (int i = 0; i < n; i++) {
            final int idx = i;
            ThreadFactory tf = r -> {
                Thread t = new Thread(r, namePrefix + "-" + idx);
                t.setDaemon(true);
                return t;
            };
            workers[i] = Executors.newSingleThreadScheduledExecutor(tf);
        }
    }

    public void execute(String key, Runnable runnable) {
        int idx = Math.floorMod(key.hashCode(), workers.length);
        workers[idx].submit(runnable);
    }

    public void shutDown() {
        for (ExecutorService w : workers) {
            w.shutdown();
        }
    }
}
