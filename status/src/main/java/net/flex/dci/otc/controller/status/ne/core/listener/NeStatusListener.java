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
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.aspect.Log;
import net.flex.dci.otc.common.model.ne.NeStatus;
import net.flex.dci.otc.common.model.ne.NeStatusMessage;
import net.flex.dci.otc.common.model.type.NeStatusType;
import net.flex.dci.otc.controller.status.core.processor.NeStateChangeProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.BatchAcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/2 15:38
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NeStatusListener implements
        BatchAcknowledgingMessageListener<String, NeStatusMessage> {

    private final NeStateChangeProcessor neStateChangeProcessor;


    private final BlockingQueue<NeStatusMessage> queue = new LinkedBlockingQueue<>(
            5000); // 减小从 50000 -> 5000


    private final ExecutorService processExecutor = new ThreadPoolExecutor(
            8,
            16,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactoryBuilder().setNameFormat("ne-status-consumer-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private final ExecutorService eventExecutor = new ThreadPoolExecutor(
            8,
            16,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactoryBuilder().setNameFormat("ne-status-process-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @PostConstruct
    public void init() {

        for (int i = 0; i < 8; i++) {
            processExecutor.submit(this::consumeQueue);
        }
        log.info("NeStatusListener started with 8 consumer threads");
    }

//    @Override
//    @Log
//    public void onMessage(ConsumerRecord<String, NeStatusMessage> consumerRecord) {
//        log.info("handle the ne status change , partition is:{}", consumerRecord.partition());
//        NeStatusMessage neStatusMessage = consumerRecord.value();
//
//        if (neStatusMessage == null) {
//            log.debug("Meaningless messages discard it");
//            return;
//        }
//        if (!queue.offer(neStatusMessage)) {
//            log.error("Queue full, processing directly: {}", neStatusMessage);
//            processNeStatusMessageAsync(neStatusMessage);
//        }
//    }

    private void consumeQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<NeStatusMessage> batch = new ArrayList<>();
                queue.drainTo(batch, 200);

                if (batch.isEmpty()) {
                    NeStatusMessage msg = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (msg != null) {
                        batch.add(msg);
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

    private void processBatchOptimized(List<NeStatusMessage> messages) {
        long start = System.currentTimeMillis();

        Map<String, NeStatusType> neIdToStatus = new HashMap<>();
        for (NeStatusMessage msg : messages) {
            if (msg.getNeStatuses() != null) {
                for (NeStatus status : msg.getNeStatuses()) {
                    neIdToStatus.put(status.getNeId(), status.getNeStatus());
                }
            }
        }

        log.debug("Batch processing: {} messages -> {} unique neIds",
                messages.size(), neIdToStatus.size());

        // 2. 分类处理，并行化
        List<String> processNeIds = new ArrayList<>();
        List<String> removeNeIds = new ArrayList<>();

        for (Map.Entry<String, NeStatusType> entry : neIdToStatus.entrySet()) {
            String neId = entry.getKey();
            NeStatusType statusType = entry.getValue();
            if (statusType.equals(NeStatusType.REMOVED)) {
                removeNeIds.add(neId);
            } else {
                processNeIds.add(neId);
            }
        }

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        if (!processNeIds.isEmpty()) {
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    neStateChangeProcessor.processBatch(processNeIds);
                } catch (Exception e) {
                    log.error("Failed to process batch neIds: {}", processNeIds, e);
                }
            }, eventExecutor));
        }
        if (!removeNeIds.isEmpty()) {
            futures.add(CompletableFuture.runAsync(() -> {
                neStateChangeProcessor.processRemoveBatch(removeNeIds);
            }, eventExecutor));
        }
//        for (String neId : removeNeIds) {
//            futures.add(CompletableFuture.runAsync(() -> {
//                try {
//                    neStateChangeProcessor.processRemove(neId);
//                } catch (Exception e) {
//                    log.error("Failed to process remove neId: {}", neId, e);
//                }
//            }, eventExecutor));
//        }

        long tWait = System.currentTimeMillis();
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(5, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.warn("eventExecutor tasks not completed within 5s, release consumer thread");
        } catch (Exception e) {
            log.error("Failed to wait for eventExecutor tasks", e);
        }
        long waitCost = System.currentTimeMillis() - tWait;
        if (waitCost > 2000) {
            log.warn("[MONITOR] processBatchOptimized allOf.join waited {}ms", waitCost);
        }

        long cost = System.currentTimeMillis() - start;
        if (cost > 1000) {
            log.warn("[MONITOR] slow ne status batch: {}ms for {} messages ({} unique)",
                    cost, messages.size(), neIdToStatus.size());
        } else {
            log.debug("Ne status batch process: {}ms for {} messages ({} unique)",
                    cost, messages.size(), neIdToStatus.size());
        }
    }

    private void processNeStatusMessageAsync(NeStatusMessage neMsg) {
        CompletableFuture.runAsync(() -> {
            processNeStatusMessage(neMsg);
        }, eventExecutor);
    }

    private void processNeStatusMessage(NeStatusMessage neMsg) {
        log.info("process ne status message change:{}", neMsg);
        if (neMsg.getNeStatuses() != null && !neMsg.getNeStatuses().isEmpty()) {
            neMsg.getNeStatuses().forEach(neStatus -> {
                String neId = neStatus.getNeId();
                NeStatusType statusType = neStatus.getNeStatus();
                if (statusType.equals(NeStatusType.LOSS_TRACK)) {
                    neStateChangeProcessor.process(neId);
                } else if (statusType.equals(NeStatusType.SYNCED) || statusType.equals(
                        NeStatusType.CONFIG)) {
                    neStateChangeProcessor.process(neId);
                } else if (statusType.equals(NeStatusType.REMOVED)) {
                    neStateChangeProcessor.processRemove(neId);
                }
            });
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down NeStatusListener...");
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

    @Override
    @Log
    public void onMessage(List<ConsumerRecord<String, NeStatusMessage>> records,
            Acknowledgment acknowledgment) {
        if (records == null || records.isEmpty()) {
            acknowledgment.acknowledge();
            return;
        }
        log.info("Received ne status batch: {} messages from partitions: {}",
                records.size(), getPartitions(records));
        List<NeStatusMessage> validMessages = records.stream().map(ConsumerRecord::value)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (validMessages.isEmpty()) {
            acknowledgment.acknowledge();
            return;
        }
        int totalDiscardOld = 0;
//        synchronized (queue) {
//            for (NeStatusMessage message : validMessages) {
//                while (!queue.offer(message)) {
//                    NeStatusMessage oldMessage = queue.poll();
//                    if (oldMessage != null) {
//                        totalDiscardOld++;
//                        log.warn("Queue full, discard old ne status message");
//                    }
//                }
//            }
//        }
        for (NeStatusMessage message : validMessages) {
            if (!queue.offer(message)) {
                queue.poll();
                queue.offer(message);
                totalDiscardOld++;
            }
        }

        if (totalDiscardOld > 0) {
            log.warn("Queue full summary: discarded {} old ne status messages", totalDiscardOld);
        }

        acknowledgment.acknowledge();
        log.debug("Batch offset committed successfully: {} messages", validMessages.size());
    }


    private String getPartitions(List<ConsumerRecord<String, NeStatusMessage>> records) {
        return records.stream()
                .map(ConsumerRecord::partition)
                .distinct()
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElse("none");
    }
}
