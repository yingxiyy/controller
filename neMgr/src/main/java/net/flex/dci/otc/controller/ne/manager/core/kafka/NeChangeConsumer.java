package net.flex.dci.otc.controller.ne.manager.core.kafka;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.core.kafka.model.ElementChange;
import net.flex.dci.otc.controller.ne.manager.core.kafka.service.NeChangeRegisteredHandlerService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/27 16:07
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NeChangeConsumer implements MessageListener<String, ElementChange> {

    private final NeChangeRegisteredHandlerService neChangeRegisteredHandlerService;

//    private final Map<Integer, BlockingQueue<ElementChange>> partitionQueues = new ConcurrentHashMap<>();

    private final BlockingQueue<ElementChange> globalMessageQueue = new LinkedBlockingQueue<>(
            2000);

    private static final int CONSUMER_THREAD_COUNT = 4;

    private static final int BATCH_SIZE = 50;


    private final ThreadPoolExecutor businessPool = new ThreadPoolExecutor(
            8, 16,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(2000),
            new ThreadFactoryBuilder().setNameFormat("ne-change-biz-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @PostConstruct
    public void startConsumers() {
        log.info("Starting {} Kafka consumer threads...", CONSUMER_THREAD_COUNT);
        for (int i = 0; i < 4; i++) {
            new Thread(this::consumerLoop, "consumer-" + i).start();
        }
        log.info("All {} Kafka consumer threads started successfully", CONSUMER_THREAD_COUNT);
    }


    @Override
    public void onMessage(ConsumerRecord<String, ElementChange> consumerRecord) {
        log.info("start to handle the consumer record:{}", consumerRecord);
        ElementChange elementChange = consumerRecord.value();
        if (elementChange == null) {
            log.debug("Discard null message");
            return;
        }

        if (!globalMessageQueue.offer(elementChange)) {
            log.error("NeChange queue is full, process directly: {}", elementChange.getMsgType());
            processDirectly(elementChange);
        }
    }

    private void processDirectly(ElementChange elementChange) {
        CompletableFuture.runAsync(() -> {
            neChangeRegisteredHandlerService.processElementChange(elementChange);
        }, businessPool);
    }


    private void consumerLoop() {
        String threadName = Thread.currentThread().getName();
        log.info("Consumer thread {} started", threadName);

        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<ElementChange> batch = new ArrayList<>(BATCH_SIZE);
                ElementChange firstMessage = globalMessageQueue.take();
                batch.add(firstMessage);
                globalMessageQueue.drainTo(batch, BATCH_SIZE - 1);
                int batchSize = batch.size();
                log.debug("{} processing message for neElement size: {}", threadName, batchSize);
                List<ElementChange> processingBatch = new ArrayList<>(batch);
                CompletableFuture.runAsync(() -> {
                    try {
                        neChangeRegisteredHandlerService.batchMessage(processingBatch);
                    } catch (Exception e) {
                        log.error("Async process neChange batch error", e);
                    }
                }, businessPool);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.info("Consumer thread {} interrupted, exiting", threadName);
                break;
            }
        }

        log.info("Consumer thread {} stopped", threadName);
    }


    @PreDestroy
    public void shutdown() {
        log.info("Shutting down NeChangeConsumer...");
        log.info("Remaining messages in queue: {}", globalMessageQueue.size());
        businessPool.shutdown();
        try {
            if (!businessPool.awaitTermination(60, TimeUnit.SECONDS)) {
                log.warn("Consumer threads did not terminate in 60 seconds, forcing shutdown");
                businessPool.shutdownNow();
            }
        } catch (InterruptedException e) {
            log.error("Interrupted while waiting for consumer threads to terminate", e);
            businessPool.shutdownNow();
        }

        log.info("NeChangeConsumer shutdown complete. Remaining unprocessed messages: {}",
                globalMessageQueue.size());
    }
}
