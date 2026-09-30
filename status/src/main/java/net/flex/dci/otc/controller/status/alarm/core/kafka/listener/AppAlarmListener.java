package net.flex.dci.otc.controller.status.alarm.core.kafka.listener;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.aspect.Log;
import net.flex.dci.otc.common.model.alarm.AppAlarm;
import net.flex.dci.otc.controller.status.alarm.core.kafka.handler.AppAlarmHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * start to handle the app alarm
 *
 * @version 1.0
 * @date 2022/6/7 13:45
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppAlarmListener implements AcknowledgingMessageListener<String, AppAlarm> {

    private final Map<Integer, BlockingQueue<AppAlarmTask>> partitionQueues = new ConcurrentHashMap<>();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final AppAlarmHandler appAlarmHandler;

    @Override
    @Log
    public void onMessage(ConsumerRecord<String, AppAlarm> consumerRecord,
            Acknowledgment acknowledgment) {
        log.info("the alarm notification text  is {} partition:{}", consumerRecord,
                consumerRecord.partition());
        AppAlarm appAlarm = consumerRecord.value();
        int partition = consumerRecord.partition();
        partitionQueues.computeIfAbsent(partition, id -> {
            BlockingQueue<AppAlarmTask> queue = new LinkedBlockingQueue<>(2000); // 减小从 20000 -> 2000
            executor.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        AppAlarmTask appAlarmTask = queue.take();
                        log.info("start to process the app alarm :{}", appAlarmTask);
                        appAlarmHandler.handle(appAlarmTask.appAlarm);
                        appAlarmTask.acknowledgment.acknowledge();
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    } catch (Exception ex) {
                        log.error("process the app alarm failed the reason is:{}", ex.getMessage(),
                                ex);
                    }
                }
            });
            return queue;
        }).offer(new AppAlarmTask(appAlarm, acknowledgment));
    }

    @Data
    @AllArgsConstructor
    private static class AppAlarmTask {

        private AppAlarm appAlarm;
        private Acknowledgment acknowledgment;
    }
}
