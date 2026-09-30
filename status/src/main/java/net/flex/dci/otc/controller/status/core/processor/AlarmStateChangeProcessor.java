package net.flex.dci.otc.controller.status.core.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.alarm.Alarm;
import net.flex.dci.otc.common.model.alarm.CtrlAlarm;
import net.flex.dci.otc.controller.status.core.IStateProcessor;
import net.flex.dci.otc.controller.status.core.processor.alarm.AlarmClearStateProcessor;
import net.flex.dci.otc.controller.status.core.processor.alarm.AlarmCreateStateProcessor;
import net.flex.dci.otc.controller.status.core.processor.alarm.AlarmStateUpdateProcessor;
import net.flex.dci.otc.controller.status.core.worker.KeyAffinityExecutor;
import net.flex.dci.otc.controller.status.dto.nmlkey.NmlKeyDto;
import net.flex.dci.otc.controller.status.util.NmlKeyHelper;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/2 16:21
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlarmStateChangeProcessor implements IStateProcessor {

    private final AlarmClearStateProcessor alarmClearStateProcessor;

    private final AlarmCreateStateProcessor alarmCreateStateProcessor;

    private final AlarmStateUpdateProcessor alarmStateUpdateProcessor;

    private final DciTopologyCacheManager dciTopologyCacheManager;

    private final KeyAffinityExecutor keyAffinityExecutor = new KeyAffinityExecutor(16, "alarm-ne");

    @Autowired
    @Qualifier("alarmProcessExecutor")
    private ExecutorService executor;


    @Override
    public void process(String neId) {
        log.info("start to handle alarm state change for everything based on ne :{}", neId);
        alarmStateUpdateProcessor.process(neId);
    }

    @Override
    public void process(CtrlAlarm ctrlAlarm) {
        log.info("start to handle the alarm state change");
        List<Alarm> newAlarms = ctrlAlarm.getNewAlarms();
        List<Alarm> removeAlarms = ctrlAlarm.getClearAlarms();

        Map<String, List<Alarm>> createAlarmByIdMap = buildAlarmMap(newAlarms);
        Map<String, List<Alarm>> clearAlarmByIdMap = buildAlarmMap(removeAlarms);
        Set<String> neIds = new HashSet<>();
        neIds.addAll(createAlarmByIdMap.keySet());
        neIds.addAll(clearAlarmByIdMap.keySet());
        Map<String, PhyNodeCache> phyNodeCaches =
                dciTopologyCacheManager.batchGetValues(new ArrayList<>(neIds), PhyNodeCache.class);
        for (String neId : neIds) {
            final List<Alarm> create = createAlarmByIdMap.getOrDefault(neId, new ArrayList<>());
            final List<Alarm> clear = clearAlarmByIdMap.getOrDefault(neId, new ArrayList<>());
            final PhyNodeCache phyNodeCache = phyNodeCaches.get(neId);
            //thread executor
            keyAffinityExecutor.execute(neId, () -> {
                try {
                    if (!create.isEmpty()) {
                        alarmCreateStateProcessor.process(neId, create, phyNodeCache);
                    }
                    if (!clear.isEmpty()) {
                        alarmClearStateProcessor.process(neId, clear, phyNodeCache);
                    }
                } catch (Exception e) {
                    log.error("alarm state task failed neId={}", neId, e);
                }
            });
        }
//        if (!createAlarmByIdMap.isEmpty()) {
//            log.info("start to handle the create alarm,the createAlarmSize:{}",
//                    createAlarmByIdMap.size());
////            futures.add(handleCreateAlarmParallel(createAlarmByIdMap));
//            handleCreateAlarmParallel(createAlarmByIdMap);
//        }
//
//        if (!clearAlarmByIdMap.isEmpty()) {
//            log.info("start to handle the clear alarm,the clearAlarmSize:{}",
//                    clearAlarmByIdMap.size());
////            futures.add(handleClearAlarmParallel(clearAlarmByIdMap));
//            handleClearAlarmParallel(clearAlarmByIdMap);
//        }
//
////        if (!futures.isEmpty()) {
////            try {
////                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
////            } catch (Exception e) {
////                log.error("Failed to process alarm state change", e);
////            }
////        }
//
//        long cost = System.currentTimeMillis() - start;
//        if (cost > 5000) {
//            log.warn("Slow alarm state change: {}ms", cost);
//        } else {
//            log.debug("Alarm state change: {}ms", cost);
//        }
    }

//    /**
//     * handle clear alarm - 真正并行化
//     */
//    private CompletableFuture<Void> handleClearAlarmParallel(
//            Map<String, List<Alarm>> clearAlarmByIdMap) {
//        List<CompletableFuture<Void>> futures = new ArrayList<>();
//        log.debug("Start to handle clear alarms, count: {}", clearAlarmByIdMap.size());
//
//        for (Map.Entry<String, List<Alarm>> entry : clearAlarmByIdMap.entrySet()) {
//            final String neId = entry.getKey();
//            final List<Alarm> alarms = entry.getValue();
//            futures.add(CompletableFuture.runAsync(() -> {
//                try {
//
//                    alarmClearStateProcessor.process(neId, alarms);
//
//                } catch (Exception e) {
//                    log.error("Failed to process clear alarm for neId: {}", neId, e);
//                }
//            }, executor));
//        }
//
//        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
//    }

//    /**
//     * handle create alarm - 真正并行化
//     */
//    private CompletableFuture<Void> handleCreateAlarmParallel(
//            Map<String, List<Alarm>> createAlarmByIdMap) {
//        List<CompletableFuture<Void>> futures = new ArrayList<>();
//        log.debug("Start to handle create alarms, count: {}", createAlarmByIdMap.size());
//
//        for (Map.Entry<String, List<Alarm>> entry : createAlarmByIdMap.entrySet()) {
//            final String neId = entry.getKey();
//            final List<Alarm> alarms = entry.getValue();
//            futures.add(CompletableFuture.runAsync(() -> {
//                try {
////                    if (!alarmLimit.tryAcquire(500, java.util.concurrent.TimeUnit.MILLISECONDS)) {
////                        log.warn("Alarm limit busy, skipping create for neId: {}", neId);
////                        return;
////                    }
////                    try {
//                    alarmCreateStateProcessor.process(neId, alarms);
////                    } finally {
////                        alarmLimit.release();
////                    }
//                } catch (Exception e) {
//                    log.error("Failed to process create alarm for neId: {}", neId, e);
//                }
//            }, executor));
//        }
//
//        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
//    }

    private Map<String, List<Alarm>> buildAlarmMap(List<Alarm> alarms) {
        Map<String, List<Alarm>> map = new HashMap<>();
        if (alarms == null || alarms.isEmpty()) {
            return map;
        }
        for (Alarm alarm : alarms) {
            String nmlKey = alarm.getToopKey();
            NmlKeyDto nmlKeyDto = NmlKeyHelper.getDetailInfoFromNmlKey(nmlKey);
            if (nmlKeyDto == null || nmlKeyDto.getPhyNodeId() == null) {
                log.warn("Cannot extract neId from toopKey: {}, skip this alarm", nmlKey);
                continue;
            }
            String neId = nmlKeyDto.getPhyNodeId();
            map.computeIfAbsent(neId, k -> new ArrayList<>()).add(alarm);
        }
        return map;
    }
}
