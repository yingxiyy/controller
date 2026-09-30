package net.flex.dci.otc.controller.otdr.cron;

import java.util.concurrent.ScheduledFuture;

/**
 *
 */
public final class CronTask {

    volatile ScheduledFuture<?> future;


    /**
     * Trigger cancellation of this scheduled task.
     */
    public void cancel() {
        ScheduledFuture<?> future = this.future;
        if (future != null) {
            future.cancel(true);
        }
    }
}
