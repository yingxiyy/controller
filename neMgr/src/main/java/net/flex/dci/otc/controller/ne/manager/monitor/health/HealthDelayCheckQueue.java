package net.flex.dci.otc.controller.ne.manager.monitor.health;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 12/11/2025 2:29 PM
 */
@Component
@Slf4j
public class HealthDelayCheckQueue {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    private final Map<String, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    private final Map<String, String> keyTasks = new ConcurrentHashMap<>();

    public String submit(String key, Runnable task, long delay, TimeUnit unit) {
        String taskId = UUID.randomUUID().toString();
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                task.run();
            } finally {
                tasks.remove(taskId);
            }
        }, delay, unit);
        tasks.put(taskId, future);
        keyTasks.put(key, taskId);
        return taskId;
    }

    public void cancel(String taskId) {
        ScheduledFuture<?> future = tasks.remove(taskId);
        if (future != null) {
            future.cancel(false);
        }
    }

    public String getPendingTaskId(String key) {
        return keyTasks.get(key);
    }

}
