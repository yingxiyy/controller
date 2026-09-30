package net.flex.dci.otn.controller.taskinfo.core.listener;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.aspect.Log;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.taskinfo.core.handler.TaskInfoHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TaskInfoKafkaListener implements MessageListener<String, String> {

    private static final int QUEUE_CAPACITY = 50000;
    private static final int SHARD_COUNT = 8;
    private static final int BATCH_SIZE = 200;
    private static final long MAX_WAIT_MS = 200;

    @Autowired
    private TaskInfoHandler taskInfoHandler;

    private final List<BlockingQueue<TaskInfoMessage>> shardQueues = new ArrayList<>(SHARD_COUNT);

    private final ThreadPoolExecutor executor;

    private volatile boolean running = true;
    private final Gson gson = new Gson();

    public TaskInfoKafkaListener() {
        this.executor = new ThreadPoolExecutor(
                SHARD_COUNT, SHARD_COUNT,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new TaskWorkerThreadFactory());
    }

    @PostConstruct
    public void init() {
        for (int i = 0; i < SHARD_COUNT; i++) {
            shardQueues.add(new LinkedBlockingQueue<>(QUEUE_CAPACITY / SHARD_COUNT));
        }
        for (int i = 0; i < SHARD_COUNT; i++) {
            final int shardIndex = i;
            executor.submit(() -> consumeShard(shardIndex));
        }
        log.info("TaskInfoListener initialized, concurrency: {}", SHARD_COUNT);
    }

    private void consumeShard(int shardIndex) {
        BlockingQueue<TaskInfoMessage> queue = shardQueues.get(shardIndex);

        while (running && !Thread.currentThread().isInterrupted()) {
            try {

                TaskInfoMessage message = queue.take();
                long t0 = System.nanoTime();
                taskInfoHandler.handlerTaskMessage(message);
                long costUs = (System.nanoTime() - t0) / 1_000;
                if (costUs > 1000) {
                    log.warn("shard-{} handle msg cost {}μs, resourceId={}, actionType={}",
                            shardIndex, costUs, message.getResourceId(), message.getActionType());
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.error("shard-{} consumer thread interrupted", shardIndex);
                break;
            } catch (Exception ex) {
                log.error("shard-{} failed to handle message: {}", shardIndex, ex.getMessage(), ex);
            }
        }
        log.info("shard-{} stopped", shardIndex);
    }

    @Log
    @Override
    public void onMessage(ConsumerRecord<String, String> record) {
        try {
            String taskMessage = record.value();
            TaskInfoMessage message = gson.fromJson(taskMessage, TaskInfoMessage.class);

            int shardIndex = hashToShard(message);
            BlockingQueue<TaskInfoMessage> queue = shardQueues.get(shardIndex);

            queue.put(message);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("message offer in queue failed", e);
        } catch (Exception e) {
            log.error("Kafka message handler failed，offset:{}", record.offset(), e);
        }

    }

    private int hashToShard(TaskInfoMessage message) {
        String key = message.getResourceId();
        return key != null ? Math.abs(key.hashCode() % SHARD_COUNT) : 0;
    }

    @PreDestroy
    public void shutdown() {
        log.info("TaskInfoListener shutting down...");
        running = false;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("TaskInfoListener shutdown finished");
    }

    private static class TaskWorkerThreadFactory implements ThreadFactory {

        private final AtomicInteger threadNumber = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "task-shard-" + threadNumber.getAndIncrement());
            t.setDaemon(false);
            return t;
        }
    }
}