package net.flex.dci.otn.controller.apsswitchlog.listener;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
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
import net.flex.dci.otn.controller.apsswitchlog.processor.ApsSwitchLogMessageProcessor;
import net.flex.dci.otn.db.jpa.entity.ApsSwitchLog;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

/**
 * 2026/5/17
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class ApsSwitchLogMessageConsumer implements MessageListener<String, ApsSwitchLog> {

    private final BlockingQueue<ApsSwitchLog> queue = new LinkedBlockingQueue<>(5000);


    private final ExecutorService apsSwitchLogExecutors = new ThreadPoolExecutor(
            8,
            16,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(1000),
            new ThreadFactoryBuilder().setNameFormat("ne-aps-switch-log-consumer-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );


    private final ApsSwitchLogMessageProcessor processor;


    @PostConstruct
    public void init() {
        for (int i = 0; i < 8; i++) {
            apsSwitchLogExecutors.submit(this::consumeQueue);
        }
        log.info("ApsSwitchLog listener started with 8 consumer threads");
    }

    @Override
    @Log
    public void onMessage(ConsumerRecord<String, ApsSwitchLog> consumerRecord) {
        log.debug("handle the aps switch log partition is:{}", consumerRecord.partition());
        ApsSwitchLog apsSwitchLog = consumerRecord.value();

        if (apsSwitchLog == null) {
            log.debug("Meaningless messages discard it");
            return;
        }
        try {
            queue.put(apsSwitchLog);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while putting message to queue", e);
        }
    }

    private void consumeQueue() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                List<ApsSwitchLog> batch = new ArrayList<>();
                queue.drainTo(batch, 200);

                if (batch.isEmpty()) {
                    ApsSwitchLog msg = queue.poll(100, TimeUnit.MILLISECONDS);
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

    private void processBatchOptimized(List<ApsSwitchLog> batch) {
        log.debug("Batch processing: {} messages", batch.size());
        if (!batch.isEmpty()) {
            try {
                processor.processBatch(batch);
            } catch (Exception e) {
                List<String> neIds = batch.stream().map(ApsSwitchLog::getNeId).collect(Collectors.toList());
                log.error("Failed to process batch neIds: {}", neIds, e);
            }
        }
    }


    @PreDestroy
    public void shutdown() {
        apsSwitchLogExecutors.shutdown();
        try {
            if (!apsSwitchLogExecutors.awaitTermination(60, TimeUnit.SECONDS)) {
                apsSwitchLogExecutors.shutdownNow();
            }
        } catch (InterruptedException e) {
            apsSwitchLogExecutors.shutdownNow();
        }
    }
}
