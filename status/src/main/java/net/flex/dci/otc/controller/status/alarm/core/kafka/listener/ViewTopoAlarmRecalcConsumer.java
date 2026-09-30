package net.flex.dci.otc.controller.status.alarm.core.kafka.listener;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.aspect.Log;
import net.flex.dci.otc.common.model.view.ViewTopoAlarmRecalcMsg;
import net.flex.dci.otc.controller.status.alarm.core.kafka.handler.ViewTopoAlarmRecalcHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.AcknowledgingMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * 2026/2/22
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class ViewTopoAlarmRecalcConsumer implements
        AcknowledgingMessageListener<String, ViewTopoAlarmRecalcMsg> {

    private final Map<Integer, BlockingQueue<ViewTopoAlarmRecalcTask>> partitionQueues = new ConcurrentHashMap<>();

    private final ExecutorService executor = new ThreadPoolExecutor(
            5,
            20,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private final ViewTopoAlarmRecalcHandler viewTopoAlarmRecalcHandler;

    @Log
    @Override
    public void onMessage(ConsumerRecord<String, ViewTopoAlarmRecalcMsg> consumerRecord,
            Acknowledgment acknowledgment) {
        log.info("the view Topo alarm recalcMsg is:{} the partition is:{}", consumerRecord,
                consumerRecord.partition());
        int partition = consumerRecord.partition();
        partitionQueues.computeIfAbsent(partition, id -> {
            BlockingQueue<ViewTopoAlarmRecalcTask> partitionQueue = new LinkedBlockingQueue<ViewTopoAlarmRecalcTask>(
                    1000);
            executor.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        ViewTopoAlarmRecalcTask viewTopoAlarmRecalcTask = partitionQueue.take();
                        log.info("start to process view topo alarm recalcTask:{}",
                                viewTopoAlarmRecalcTask);
                        viewTopoAlarmRecalcHandler.recalculateViewTopo(
                                viewTopoAlarmRecalcTask.viewTopoAlarmRecalcMsg);
                        viewTopoAlarmRecalcTask.acknowledgment.acknowledge();
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                    } catch (Exception ex) {
                        log.error("process the view topo alarm recalcTask failed the reason is:{}",
                                ex.getMessage(),
                                ex);
                    }
                }
            });
            return partitionQueue;
        }).offer(new ViewTopoAlarmRecalcTask(consumerRecord.value(), acknowledgment));

    }

    @AllArgsConstructor
    @Data
    private static class ViewTopoAlarmRecalcTask {

        private ViewTopoAlarmRecalcMsg viewTopoAlarmRecalcMsg;

        private Acknowledgment acknowledgment;

    }
}
