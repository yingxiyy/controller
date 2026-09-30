package net.flex.dci.otc.controller.status.alarm.core.kafka.processor.impl;

import static net.flex.dci.otc.controller.status.alarm.enums.AlarmElementType.NE;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.alarm.Alarm;
import net.flex.dci.otc.common.model.alarm.CtrlAlarm;
import net.flex.dci.otc.controller.status.alarm.core.kafka.processor.AlarmNotificationProcessor;
import net.flex.dci.otc.controller.status.alarm.dto.AlarmsDetail;
import net.flex.dci.otc.controller.status.alarm.notification.AlarmNotifier;
import net.flex.dci.otc.controller.status.core.processor.AlarmStateChangeProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/4/1 16:39
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AlarmProcessorImpl implements AlarmNotificationProcessor {

    private final AlarmStateChangeProcessor alarmStateChangeProcessor;

    private final AlarmNotifier alarmNotifier;

    @Autowired
    @Qualifier("alarmStateExecutor")
    private ExecutorService stateExecutor;

    @Autowired
    @Qualifier("alarmMessageExecutor")
    private Executor alarmMessageExecutor;

    @Value("${alarm.expire.seconds:30}")
    private long alarmExpireSeconds;

    @Override
    public void process(List<Alarm> alarms) {
        log.debug("start to handle the alarm, count:{}", alarms.size());
        long now = System.currentTimeMillis();
        AlarmsDetail detail = classifyAlarms(alarms, now);

        if (detail.getExpiredCount() > 0) {
            log.warn("Found {} expired alarms, skip state calculation", detail.getExpiredCount());
        }
        CtrlAlarm notifyCtrlAlarm = CtrlAlarm.builder()
                .newAlarms(detail.getAllNewAlarms())
                .clearAlarms(detail.getAllClearAlarms())
                .eventTime(now)
                .build();
        asyncSendAlarmMessage(notifyCtrlAlarm);

        if (!CollectionUtils.isEmpty(detail.getValidClearAlarms()) || !CollectionUtils.isEmpty(
                detail.getValidNewAlarms())) {
            CtrlAlarm stateCtrlAlarm = CtrlAlarm.builder()
                    .newAlarms(detail.getValidNewAlarms())
                    .clearAlarms(detail.getValidClearAlarms())
                    .eventTime(now)
                    .build();
            try {
//                stateExecutor.submit(() -> {
                try {
                    alarmStateChangeProcessor.process(stateCtrlAlarm);
                } catch (Exception e) {
                    log.error("Process alarm state failed", e);
                }
//                });
            } catch (Exception e) {
                log.error("Submit alarm task failed, executor may be full", e);
            }
        }
    }

    private AlarmsDetail classifyAlarms(List<Alarm> alarms, long now) {
        Map<String, Alarm> creates = new HashMap<>();
        Map<String, Alarm> clears = new HashMap<>();
//        Map<String, Alarm> latestByAlarmId = new HashMap<>();
        for (Alarm alarm : alarms) {
            if (alarm == null || alarm.getId() == null) {
                continue;
            }
            alarm.setAlarmTypeId(NE.name());
//            Alarm existing = latestByAlarmId.get(alarm.getId());
//            if (existing == null || alarm.getTimeCreated() > existing.getTimeCreated()) {
//                latestByAlarmId.put(alarm.getId(), alarm);
//            }
            ((Boolean.TRUE.equals(alarm.getIsClear())) ? clears : creates).put(alarm.getId(),
                    alarm);
        }

        List<Alarm> allNewAlarms = new ArrayList<>(creates.values());
        List<Alarm> validNewAlarms = new ArrayList<>();
        List<Alarm> validClearAlarms = new ArrayList<>();
        List<Alarm> allClearAlarms = new ArrayList<>(clears.values());

        Set<String> createOnly = new HashSet<>(creates.keySet());
        createOnly.removeAll(clears.keySet());
        Set<String> clearOnly = new HashSet<>(clears.keySet());
        clearOnly.removeAll(creates.keySet());

        int expiredCount = 0;
        for (String id : createOnly) {
            Alarm alarm = creates.get(id);
            if ((now - alarm.getTimeCreated()) / 1000 > alarmExpireSeconds) {
                expiredCount++;
            } else {
                validNewAlarms.add(alarm);
            }
        }
        for (String id : clearOnly) {
            Alarm alarm = clears.get(id);
            validClearAlarms.add(alarm);
        }

        return AlarmsDetail.builder()
                .allNewAlarms(allNewAlarms)
                .validNewAlarms(validNewAlarms)
                .allClearAlarms(allClearAlarms)
                .validClearAlarms(validClearAlarms)
                .expiredCount(expiredCount)
                .build();
    }

    private Alarm latest(List<Alarm> alarms) {
        return alarms.stream()
                .max(Comparator.comparingLong(Alarm::getTimeCreated))
                .orElse(null);
    }

    private long calculateDelaySeconds(Alarm alarm, long now) {
        return (now - alarm.getTimeCreated()) / 1000;
    }

    private boolean isAlarmExpired(Alarm alarm, long now) {
        boolean isClear = Boolean.TRUE.equals(alarm.getIsClear());
        return !isClear && calculateDelaySeconds(alarm, now) > alarmExpireSeconds;
    }


    private void asyncSendAlarmMessage(CtrlAlarm ctrlAlarm) {
        alarmMessageExecutor.execute(() -> {
            try {
                alarmNotifier.sendMessage(ctrlAlarm);
                log.info("Alarm message sent successfully for clear alarm: {} and new alarm:{}",
                        ctrlAlarm.getClearAlarms().size(), ctrlAlarm.getNewAlarms().size());
            } catch (Exception e) {
                log.error("Failed to send alarm message for alarm: {}", ctrlAlarm, e);
            }
        });

    }

//    private CtrlAlarm buildCtrlAlarm(List<Alarm> alarms) {
//        // 同 alarmId 只保留 timeCreated 最晚的一条，消除窗口内冗余的中间状态
//        List<Alarm> latestAlarms = deduplicateByLatestTimestamp(alarms);
//
//        List<Alarm> clearAlarms = new ArrayList<>();
//        List<Alarm> newAlarms = new ArrayList<>();
//
//        for (Alarm alarm : latestAlarms) {
//            boolean isClear = Boolean.TRUE.equals(alarm.getIsClear());
//            alarm.setAlarmTypeId(NE.name());
//            if (isClear) {
//                clearAlarms.add(alarm);
//            } else {
//                newAlarms.add(alarm);
//            }
//        }
//
//        CtrlAlarm ctrlAlarm = CtrlAlarm.builder()
//                .newAlarms(newAlarms)
//                .clearAlarms(clearAlarms)
//                .eventTime(new Date().getTime())
//                .build();
//        return ctrlAlarm;
//    }


    private List<Alarm> deduplicateByLatestTimestamp(List<Alarm> alarms) {
        Map<String, Alarm> latestByAlarmId = new HashMap<>();
        for (Alarm alarm : alarms) {
            if (alarm.getId() == null) {
                continue;
            }
            Alarm existing = latestByAlarmId.get(alarm.getId());
            if (existing == null || alarm.getTimeCreated() > existing.getTimeCreated()) {
                latestByAlarmId.put(alarm.getId(), alarm);
            }
        }

        int originalSize = alarms.size();
        int dedupedSize = latestByAlarmId.size();
        if (dedupedSize < originalSize) {
            log.info(
                    "Deduplicated by alarmId: {} -> {} (removed {} redundant entries, kept latest per id)",
                    originalSize, dedupedSize, originalSize - dedupedSize);
        }

        return new ArrayList<>(latestByAlarmId.values());
    }


}