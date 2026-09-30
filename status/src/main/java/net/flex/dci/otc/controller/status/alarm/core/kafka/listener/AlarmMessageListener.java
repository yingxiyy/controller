/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.alarm.core.kafka.listener;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.alarm.Alarm;
import net.flex.dci.otc.controller.status.alarm.core.kafka.processor.AlarmNotificationProcessor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.BatchAcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;


/**
 * @version 1.0
 * @date 2021/10/22 15:53
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmMessageListener implements
        BatchAcknowledgingMessageListener<String, List<Alarm>> {

    private static final int QUEUE_CAPACITY = 10000;

    private static final int WORKER_SHARDS = 32;

    private static final int ALARM_BATCH_SIZE = 500;

    private final ExecutorService executor = new ThreadPoolExecutor(
            WORKER_SHARDS, WORKER_SHARDS, 0L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(),
            new ThreadFactoryBuilder().setNameFormat("alarm-shard-%d").build()
    );

    private final AlarmNotificationProcessor alarmNotificationProcessor;

    private final Map<Integer, BlockingQueue<AlarmTask>> shardQueues = new ConcurrentHashMap<>();


    @SneakyThrows
    @Override
    public void onMessage(List<ConsumerRecord<String, List<Alarm>>> consumerRecords,
            Acknowledgment acknowledgment) {
        if (consumerRecords == null || consumerRecords.isEmpty()) {
            acknowledgment.acknowledge();
            return;
        }
        Map<Integer, List<ConsumerRecord<String, List<Alarm>>>> groupByPartition = consumerRecords.stream()
                .collect(Collectors.groupingBy(ConsumerRecord::partition));
        List<Map.Entry<Integer, List<Alarm>>> chunks = new ArrayList<>();
        for (Map.Entry<Integer, List<ConsumerRecord<String, List<Alarm>>>> entry : groupByPartition.entrySet()) {
            List<Alarm> partitionAlarms = entry.getValue().stream()
                    .flatMap(record -> record.value().stream())
                    .collect(Collectors.toList());
            for (int i = 0; i < partitionAlarms.size(); i += ALARM_BATCH_SIZE) {
                int end = Math.min(i + ALARM_BATCH_SIZE, partitionAlarms.size());
                chunks.add(new AbstractMap.SimpleEntry<>(entry.getKey(),
                        new ArrayList<>(partitionAlarms.subList(i, end))));
            }
        }
        if (chunks.isEmpty()) {
            acknowledgment.acknowledge();
            return;
        }

        ChunkedAck chunkedAck = new ChunkedAck(acknowledgment, chunks.size());

        for (Map.Entry<Integer, List<Alarm>> chunk : chunks) {
            int partition = chunk.getKey();
            BlockingQueue<AlarmTask> queue = shardQueueOf(partition);
            AlarmTask task = new AlarmTask(chunk.getValue(), chunkedAck);
            if (!queue.offer(task)) {
                log.warn("shard queue full, partition={}, blocking put: {} alarms",
                        partition, chunk.getValue().size());
                queue.put(task);
            }
        }
    }

    private BlockingQueue<AlarmTask> shardQueueOf(int partition) {
        int shard = Math.floorMod(partition, WORKER_SHARDS);
        return shardQueues.computeIfAbsent(shard, k -> {
            BlockingQueue<AlarmTask> q = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
            executor.execute(() -> consumeShardQueue(k, q));
            return q;
        });
    }

    private void consumeShardQueue(int shard, BlockingQueue<AlarmTask> queue) {
        log.info("alarm shard worker started: shard={}", shard);
        while (!Thread.currentThread().isInterrupted()) {
            AlarmTask alarmTask = null;
            try {
                alarmTask = queue.take();
                long start = System.currentTimeMillis();
                log.debug("processor the alarm :{}", alarmTask.alarms);
                alarmNotificationProcessor.process(alarmTask.alarms);
                long cost = System.currentTimeMillis() - start;

                if (cost > 1000) {
                    log.warn("Slow batch: shard={}, {} alarms, cost={}ms",
                            shard, alarmTask.alarms.size(), cost);
                } else {
                    log.debug("Batch processed shard={}, {} alarms, cost={}ms",
                            shard, alarmTask.alarms.size(), cost);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                log.error("failed to handle the alarm, shard={}", shard, ex);
            } finally {
                if (alarmTask != null) {
                    try {
                        alarmTask.acknowledgment.acknowledge();
                    } catch (Exception ackEx) {
                        log.warn("ack failed, shard={} (partition may be revoked)", shard, ackEx);
                    }
                }
            }
        }
        log.info("alarm shard worker stopped: shard={}", shard);
    }

    private static class ChunkedAck implements Acknowledgment {

        private final Acknowledgment delegate;

        private final AtomicInteger remaining;

        ChunkedAck(Acknowledgment delegate, int chunks) {
            this.delegate = delegate;
            this.remaining = new AtomicInteger(chunks);
        }

        @Override
        public void acknowledge() {
            if (remaining.decrementAndGet() == 0) {
                delegate.acknowledge();
            }
        }
    }

    @Data
    @AllArgsConstructor
    private static class AlarmTask {

        private List<Alarm> alarms;
        private Acknowledgment acknowledgment;
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdownNow();
    }
}
