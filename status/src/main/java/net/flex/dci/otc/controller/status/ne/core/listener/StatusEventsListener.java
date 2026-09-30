package net.flex.dci.otc.controller.status.ne.core.listener;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.aspect.Log;
import net.flex.dci.otc.common.model.ne.StatusChangeEvent;
import net.flex.dci.otc.controller.status.core.processor.StatusEventsChangeProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.BatchAcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/26/2025 1:14 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StatusEventsListener implements
        BatchAcknowledgingMessageListener<String, StatusChangeEvent> {

    private final StatusEventsChangeProcessor statusEventsChangeProcessor;


    private final BlockingQueue<StatusChangeEvent> queue = new LinkedBlockingQueue<>(
            5000); // 减小从 50000 -> 5000

    private final ExecutorService processExecutor = new ThreadPoolExecutor(
            16,
            16,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000), // 减小从 10000 -> 1000
            new ThreadFactoryBuilder().setNameFormat("status-event-consumer-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private final ExecutorService eventExecutor = new ThreadPoolExecutor(
            16,
            16,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(10000),
            new ThreadFactoryBuilder().setNameFormat("status-event-process-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );


    @PostConstruct
    public void init() {
        for (int i = 0; i < 16; i++) {
            processExecutor.submit(this::consumeQueue);
        }
        log.info("StatusEventsListener started with 16 consumer threads");
    }

//    @Log
//    @Override
//    public void onMessage(ConsumerRecord<String, StatusChangeEvent> data) {
//        log.info("handle the status change event, partition is:{}", data.partition());
//
//        StatusChangeEvent statusChangeEvent = data.value();
//        if (statusChangeEvent == null) {
//            log.warn("the status change event is null, discard it");
//            return;
//        }
//
//        if (!queue.offer(statusChangeEvent)) {
//            log.error("Queue full, processing directly: {}", statusChangeEvent);
//            eventExecutor.submit(() -> processSingle(statusChangeEvent));
//        }
//    }

    private void consumeQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<StatusChangeEvent> batch = new ArrayList<>();

                queue.drainTo(batch, 200);

                if (batch.isEmpty()) {
                    StatusChangeEvent event = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (event != null) {
                        batch.add(event);
                    }
                }

                if (!batch.isEmpty()) {
                    processBatchOptimized(batch);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Failed to consume queue", e);
            }
        }
    }

    private void processBatchOptimized(List<StatusChangeEvent> events) {
        long start = System.currentTimeMillis();

        Map<String, StatusChangeEvent> latestEvents = events.stream()
                .collect(Collectors.toMap(
                        StatusChangeEvent::getObjectId,
                        e -> e,
                        (existing, replacement) -> replacement
                ));

        Map<String, List<StatusChangeEvent>> groupedByNeId = new HashMap<>();
        for (StatusChangeEvent event : latestEvents.values()) {
            String neId = extractNeId(event.getObjectId());
            groupedByNeId.computeIfAbsent(neId, k -> new ArrayList<>()).add(event);
        }

        log.debug("Batch processing: {} events -> {} unique -> {} ne groups",
                events.size(), latestEvents.size(), groupedByNeId.size());

        for (Map.Entry<String, List<StatusChangeEvent>> entry : groupedByNeId.entrySet()) {
            List<StatusChangeEvent> neEvents = entry.getValue();
            CompletableFuture.runAsync(() -> {
                try {
                    for (StatusChangeEvent event : neEvents) {
                        //                            if (!eventLimit.tryAcquire(500, TimeUnit.MILLISECONDS)) {
//                                log.warn("Event processing rejected for neId: {}, queue full",
//                                        entry.getKey());
//                                continue;
//                            }
//                            try {
                        statusEventsChangeProcessor.processStatusEvent(event);
//                            } finally {
//                                eventLimit.release();
//                            }
                    }
                } catch (Exception e) {
                    log.error("Failed to process events for neId: {}", entry.getKey(), e);
                }
            }, eventExecutor);
        }

        long cost = System.currentTimeMillis() - start;
        if (cost > 1000) {
            log.warn(
                    "Slow batch process: {}ms for {} events ({} unique, {} ne groups)",
                    cost, events.size(), latestEvents.size(), groupedByNeId.size());
        } else {
            log.debug("Batch process: {}ms for {} events ({} unique, {} ne groups)",
                    cost, events.size(), latestEvents.size(), groupedByNeId.size());
        }
    }

    /**
     * 从 objectId 中提取 neId objectId 格式: Site-xxx#Ne-xxx#...
     */
    private String extractNeId(String objectId) {
        if (objectId == null) {
            return "UNKNOWN";
        }
        String[] parts = objectId.split("#");
        if (parts.length >= 2 && parts[1].startsWith("Ne-")) {
            return parts[1];
        }
        return "UNKNOWN";
    }

    private void processSingle(StatusChangeEvent event) {
        try {
            statusEventsChangeProcessor.processStatusEvent(event);
        } catch (Exception e) {
            log.error("Failed to process single event: {}", event, e);
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down StatusEventsListener...");
        processExecutor.shutdown();
        eventExecutor.shutdown();
        try {
            if (!processExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                processExecutor.shutdownNow();
            }
            if (!eventExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                eventExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            processExecutor.shutdownNow();
            eventExecutor.shutdownNow();
        }
    }

    @Log
    @Override
    public void onMessage(List<ConsumerRecord<String, StatusChangeEvent>> consumerRecords,
            Acknowledgment acknowledgment) {
        if (consumerRecords == null || consumerRecords.isEmpty()) {
            acknowledgment.acknowledge();
            return;
        }
        long now = System.currentTimeMillis();
        final long EXPIRE_TIME = 10 * 1000;
        log.info("start to handle the status change event count:{}", consumerRecords.size());
        List<StatusChangeEvent> eventList = consumerRecords.stream()
                .map(ConsumerRecord::value)
                .filter(Objects::nonNull)
                .filter(event -> {
                    if (event.getTimestamp() == null) {
                        log.warn("empty timestamp skip it");
                        return false;
                    }
                    long diff = now - event.getTimestamp();
                    if (diff > EXPIRE_TIME) {
                        log.debug("message expired {}ms，discard it，objectId:{}", diff,
                                event.getObjectId());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
        if (eventList.isEmpty()) {
            acknowledgment.acknowledge();
            return;
        }
        int totalDiscardOld = 0;
        for (StatusChangeEvent event : eventList) {
            if (!queue.offer(event)) {
                queue.poll();
                queue.offer(event);
                totalDiscardOld++;
            }
        }
        if (totalDiscardOld > 0) {
            log.warn("Queue full summary: total discard {} old status events", totalDiscardOld);
        }
        acknowledgment.acknowledge();
    }
}
