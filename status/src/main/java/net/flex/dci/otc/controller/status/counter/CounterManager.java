package net.flex.dci.otc.controller.status.counter;

import static net.flex.dci.otc.controller.status.util.Constants.ALARM_HASH_CNT_KEY;
import static net.flex.dci.otc.controller.status.util.Constants.ALARM_KEY_PATTERN;

import cn.hutool.core.collection.ConcurrentHashSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.cache.redis.core.RedisCacheOperation;
import net.flex.dci.otc.controller.status.core.enums.AlarmSeverityCode;
import net.flex.dci.otc.controller.status.util.Constants;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import net.flex.dci.otn.db.jpa.service.dao.AlarmDaoService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 2026/7/28
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class CounterManager {

    private final RedisCacheOperation redisOp;

    private final AlarmDaoService alarmDaoService;

    private final Set<String> pendingInit = new ConcurrentHashSet<>();

    private final Set<String> pendingNeInit = new ConcurrentHashSet<>();

    private final Set<String> changedNe = new ConcurrentHashSet<>();


    @Scheduled(fixedDelay = 200)
    public void batchInit() {
        if (pendingInit.isEmpty()) {
            return;
        }
        List<String> nmlKeys = new ArrayList<>(pendingInit);
        pendingInit.clear();
        Map<String, List<AlarmSeverity>> alarmSeverityMap = alarmDaoService.getAlarmSeverityByNmlKeysWithMap(
                nmlKeys);
        for (String nmlKey : nmlKeys) {
            List<AlarmSeverity> alarmSeverities = alarmSeverityMap.get(nmlKey);
            Map<String, Long> countMap = new HashMap<>();
            for (AlarmSeverity alarmSeverity : alarmSeverities) {
                countMap.merge(alarmSeverity.name(), 1L, Long::sum);
            }
            String key = generateCntKey(nmlKey);
            redisOp.hsetAll(key, new HashMap<>(countMap));
        }
    }

    @Scheduled(fixedDelay = 200)
    public void batchInitNe() {
        initNeCounters();
    }

    public boolean updateAndCheckChanged(String nmlKey, String neId, AlarmSeverity alarmSeverity,
            boolean isClear) {
        String cacheKey = generateCntKey(nmlKey);
        if (!redisOp.hasKey(cacheKey)) {
            if (isClear) {
                log.debug("[{}] counter not exist,drop clear", nmlKey);
                return false;
            }
            redisOp.hincrBy(cacheKey, alarmSeverity.name(), 1);
            pendingInit.add(nmlKey);
            updateNeCountersAndCheckChanged(neId, alarmSeverity, isClear);
            changedNe.add(neId);
            return true;
        }
        AlarmSeverity old = getCurrentAlarmSeverity(cacheKey);
        if (isClear) {
            long cur = toLong(redisOp.getValue(cacheKey, alarmSeverity.name()));
            if (cur <= 0) {
                log.debug("[{}] {} counter already 0,skip clear", neId, alarmSeverity.name());
                return false;
            }
            redisOp.hincrBy(cacheKey, alarmSeverity.name(), -1);
        } else {
            redisOp.hincrBy(cacheKey, alarmSeverity.name(), 1);
        }

        boolean neChanged = updateNeCountersAndCheckChanged(neId, alarmSeverity, isClear);
        if (neChanged) {
            changedNe.add(neId);
        }
        AlarmSeverity current = getCurrentAlarmSeverity(cacheKey);

        if (isClear && current.ordinal() > old.ordinal()) {
            redisOp.hincrBy(cacheKey, alarmSeverity.name(), 1);
            return false;
        }

        return current != old;

    }

    public boolean updateNeCountersAndCheckChanged(String neId, AlarmSeverity alarmSeverity,
            boolean isClear) {
        String key = generateNeCntKey(neId);
        if (!redisOp.hasKey(key)) {
            if (isClear) {
                log.debug("[{}] counter not exist,drop clear", neId);
                return false;
            }
            redisOp.hincrBy(key, alarmSeverity.name(), 1);
            pendingNeInit.add(neId);
            return true;
        }
        AlarmSeverity old = getCurrentAlarmSeverity(key);
        if (isClear) {
            long cur = toLong(redisOp.getValue(key, alarmSeverity.name()));
            if (cur <= 0) {
                log.debug("[{}] {} counter already 0,skip clear", neId, alarmSeverity.name());
                return false;
            }
            redisOp.hincrBy(key, alarmSeverity.name(), -1);
        } else {
            redisOp.hincrBy(key, alarmSeverity.name(), 1);
        }
        AlarmSeverity current = getCurrentAlarmSeverity(key);
        if (isClear && current.ordinal() > old.ordinal()) {
            redisOp.hincrBy(key, alarmSeverity.name(), 1);
            return false;
        }
        return old != current;
    }


    public AlarmSeverity getNeSeverity(String neId) {
        String cacheKey = generateNeCntKey(neId);
        if (!redisOp.hasKey(cacheKey)) {
            pendingNeInit.add(neId);
            return AlarmSeverity.Cleared;
        }
        return getCurrentAlarmSeverity(cacheKey);
    }


    private AlarmSeverity getCurrentAlarmSeverity(String key) {
        Map<String, String> counts = redisOp.getAll(key);

        if (counts == null || counts.isEmpty()) {
            return AlarmSeverity.Cleared;
        }
        int code = 0;
        for (Map.Entry<String, String> entry : counts.entrySet()) {
            if (toLong(entry.getValue()) > 0) {
                code |= AlarmSeverityCode.getSeverityCode(AlarmSeverity.valueOf(entry.getKey()));
            }
        }
        return AlarmSeverityCode.getSeverity(code);
    }

    
    public boolean isNeSeverityChanged(String neId) {
        return changedNe.remove(neId);
    }


    public void discardNeChange(String neId) {
        changedNe.remove(neId);
    }

    /**
     * @param neId
     */
    public void refreshNeCounters(String neId) {
        log.info("refresh ne relative counters:{}", neId);
        String neCounterKey = generateNeCntKey(neId);
        String neCntKeyPattern = generateNeCntKeyPattern(neId);
        redisOp.removeKeyAsync(neCounterKey);
        //remove all the alarm count for the
        redisOp.deleteKeyPattern(neCntKeyPattern);
        List<AlarmRecord> alarms = alarmDaoService.getCurrentAlarmsByNeIds(
                Collections.singletonList(neId));
        for (AlarmRecord alarm : alarms) {
            if (alarm.getNmlKey() != null) {
                redisOp.hincrBy(generateCntKey(alarm.getNmlKey()), alarm.getSeverity().name(), 1);
            }
            redisOp.hincrBy(neCounterKey, alarm.getSeverity().name(), 1);
        }

    }

    private String generateNeCntKeyPattern(String neId) {
        return ALARM_HASH_CNT_KEY + neId + "*";
    }

    private void initNeCounters() {
        if (pendingNeInit.isEmpty()) {
            return;
        }
        List<String> neIds = new ArrayList<>(pendingNeInit);
        pendingNeInit.clear();
        List<AlarmRecord> currentAlarms = alarmDaoService.getCurrentAlarmsByNeIds(neIds);
        Map<String, Map<String, Long>> neCounts = new HashMap<>();
        for (AlarmRecord alarmRecord : currentAlarms) {
            neCounts.computeIfAbsent(alarmRecord.getNeId(), k -> new HashMap<>())
                    .merge(alarmRecord.getSeverity().name(), 1L, Long::sum);
        }
        neCounts.forEach(
                (neId, cnts) -> redisOp.hsetAll(generateNeCntKey(neId), new HashMap<>(cnts)));
    }

    private String generateNeCntKey(String neId) {
        return Constants.ALARM_HASH_NE_CNT_KEY + neId;
    }

    private String generateCntKey(String nmlKey) {
        return ALARM_HASH_CNT_KEY + nmlKey;
    }

    private long toLong(Object v) {
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        if (v instanceof String) {
            return Long.parseLong((String) v);
        }
        return 0L;
    }

    public void clearAlarmCache() {
        log.info("refresh  current alarm key alarm");
        redisOp.deleteKeyPattern(ALARM_KEY_PATTERN);
    }
}
