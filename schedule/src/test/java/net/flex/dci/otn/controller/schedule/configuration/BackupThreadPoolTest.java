package net.flex.dci.otn.controller.schedule.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

class BackupThreadPoolTest {
    @Test
    void idleBackupWorkersCanRetireAndBothProvidersCanRun() throws Exception {
        ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) new BackupThreadPool().backupTaskExecutor();
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        try {
            assertTrue(executor.getThreadPoolExecutor().allowsCoreThreadTimeOut());
            for (int i = 0; i < 2; i++) {
                executor.execute(() -> {
                    started.countDown();
                    try { release.await(5, TimeUnit.SECONDS); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                });
            }
            assertTrue(started.await(5, TimeUnit.SECONDS));
        } finally {
            release.countDown();
            executor.shutdown();
        }
    }

    @Test
    void cancelledFutureIsRemovedImmediately() {
        ThreadPoolTaskScheduler scheduler = (ThreadPoolTaskScheduler) new BackupThreadPool().taskScheduler();
        try {
            ScheduledFuture<?> future = scheduler.schedule(() -> {}, Instant.now().plusSeconds(86400));
            assertEquals(1, scheduler.getScheduledThreadPoolExecutor().getQueue().size());
            future.cancel(false);
            assertEquals(0, scheduler.getScheduledThreadPoolExecutor().getQueue().size());
        } finally {
            scheduler.shutdown();
        }
    }
}
