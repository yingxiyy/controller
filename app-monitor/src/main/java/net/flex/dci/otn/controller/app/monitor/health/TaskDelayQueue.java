package net.flex.dci.otn.controller.app.monitor.health;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 *
 * @version 1.0
 * @date 12/11/2025 4:51 PM
 */
@Component
public class TaskDelayQueue {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public String submit(Runnable runnable, long delay, TimeUnit timeUnit, String taskId) {
        ScheduledFuture<?> future = scheduler.schedule(runnable, delay, timeUnit);
        tasks.put(taskId, future);
        return taskId;
    }

    public void cancel(String taskId) {
        ScheduledFuture<?> future = tasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
    }

}
