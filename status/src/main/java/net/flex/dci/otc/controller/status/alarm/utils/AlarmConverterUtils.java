package net.flex.dci.otc.controller.status.alarm.utils;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.status.dto.alarm.AlarmDto;
import net.flex.dci.otc.i18n.core.DciI18nMessage;
import net.flex.dci.otn.db.jpa.entity.AlarmHistoryRecord;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.BasicCache;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.ArchiveType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GetAlarmDetailOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.GetAlarmDetailOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.current.alarms.output.Alarm;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;

/**
 * @version 1.0
 * @date 2022/1/10 14:22
 */
@Slf4j
public class AlarmConverterUtils {

    private static DciI18nMessage message;

    private static DciTopologyCacheManager topologyCacheManager;


    private static final Cache<String, String> NML_KEY_NAME_CACHE = Caffeine.newBuilder()
            .maximumSize(100000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public static void setDciI18Message(DciI18nMessage dciI18nMessage) {
        message = dciI18nMessage;
    }

    public static void setDciTopologyCacheManager(DciTopologyCacheManager dciTopologyCacheManager) {
        topologyCacheManager = dciTopologyCacheManager;
    }

    public static List<Alarm> convert2AlarmList(
            List<AlarmRecord> alarmList) throws Exception {
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.current.alarms.output.Alarm> retList =
                new ArrayList<>();

        if (alarmList != null) {
            List<String> nmlKeys = getCurrentAlarmNmlKeys(alarmList);
            Map<String, String> basicCacheMap = getNmlKeyNamesWithLocalCache(
                    nmlKeys);
            for (AlarmRecord alarm : alarmList) {
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.current.alarms.output.AlarmBuilder builder =
                        new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.current.alarms.output.AlarmBuilder();
                builder.setAlarmId(alarm.getAlarmId());
                builder.setServerity(alarm.getSeverity() != null ? AlarmSeverity
                        .forValue(alarm.getSeverity().getIntValue()) : null);
                builder.setResourceRef(alarm.getResourceRef());
//                String alarmText = getAlarmMessage(alarm);
                builder.setAlarmText(alarm.getAlarmText());
                builder.setAlarmGroup(alarm.getAlarmGroup());
                builder.setAlarmTypeId(alarm.getAlarmTypeId());
                builder.setCreationTime(toMillis(BigInteger.valueOf(
                        alarm.getCreationTime() == null ? 0L : alarm.getCreationTime())));
                builder.setCreationReceivedTime(
                        toMillis(BigInteger.valueOf(alarm.getNmlReceivedTime() == null ? 0L
                                : alarm.getNmlReceivedTime())));
//				builder.setClearTime()
//				builder.setClearReceivedTime()
//				builder.setArchiveTime(value)
//				builder.setArchiveType(value)
                builder.setNeId(alarm.getNeId());
                builder.setNmlKey(alarm.getNmlKey());
                String nmlKeyName = Optional.ofNullable(basicCacheMap.get(alarm.getNmlKey()))
                        .orElse(alarm.getNmlKey());

                builder.setNmlKeyName(nmlKeyName);
                builder.setSa(alarm.getSa());
                builder.setEquipmentRef(alarm.getComponentRef());
                builder.setAlarmIndex(alarm.getIndex());
                retList.add(builder.build());
            }
        }
        return retList;
    }


    public static List<AlarmDto> convert2AlarmDtoList(List<AlarmRecord> alarmList) {
        List<AlarmDto> retList = new ArrayList<>();
        if (alarmList != null && !alarmList.isEmpty()) {
            List<String> nmlKeys = getCurrentAlarmNmlKeys(alarmList);
            Map<String, String> basicCacheMap = getNmlKeyNamesWithLocalCache(
                    nmlKeys);
            for (AlarmRecord alarm : alarmList) {
                retList.add(AlarmDto.builder()
                        .alarmId(alarm.getAlarmId())
                        .resourceRef(alarm.getResourceRef())
                        .alarmTypeId(alarm.getAlarmTypeId())
                        .nmlKeyName(Optional.ofNullable(basicCacheMap.get(alarm.getNmlKey()))
                                .orElse(alarm.getNmlKey()))
                        .equipmentRef(alarm.getComponentRef())
                        .alarmGroup(alarm.getAlarmGroup())
                        .creationReceivedTime(toLongMillis(alarm.getNmlReceivedTime() == null ? 0L
                                : alarm.getNmlReceivedTime()))
                        .sa(alarm.getSa())
                        .alarmText(alarm.getAlarmText())
                        .creationTime(toLongMillis(alarm.getCreationTime() == null ? 0L
                                : alarm.getCreationTime()))
                        .serverity(alarm.getSeverity() != null
                                ? alarm.getSeverity().name().toLowerCase(Locale.ROOT) : null)
                        .nmlKey(alarm.getNmlKey())
                        .neId(alarm.getNeId())
                        .alarmIndex(alarm.getIndex())
                        .build());
            }
        }
        return retList;
    }


    public static List<AlarmDto> convert2HisAlarmDtoList(List<AlarmHistoryRecord> alarmList) {
        List<AlarmDto> retList = new ArrayList<>();
        if (alarmList != null && !alarmList.isEmpty()) {
            List<String> nmlKeys = getHistoryAlarmNmlKeys(alarmList);
            Map<String, String> basicCacheMap = getNmlKeyNamesWithLocalCache(
                    nmlKeys);
            for (AlarmHistoryRecord alarm : alarmList) {
                retList.add(AlarmDto.builder()
                        .alarmId(alarm.getAlarmId())
                        .resourceRef(alarm.getResourceRef())
                        .alarmTypeId(alarm.getAlarmTypeId())
                        .nmlKeyName(Optional.ofNullable(basicCacheMap.get(alarm.getNmlKey()))
                                .orElse(alarm.getNmlKey()))
                        .equipmentRef(alarm.getComponentRef())
                        .alarmGroup(alarm.getAlarmGroup())
                        .creationReceivedTime(toLongMillis(alarm.getNmlReceivedTime() == null ? 0L
                                : alarm.getNmlReceivedTime()))
                        .sa(alarm.getSa())
                        .alarmText(alarm.getAlarmText())
                        .creationTime(toLongMillis(alarm.getCreationTime() == null ? 0L
                                : alarm.getCreationTime()))
                        .serverity(alarm.getSeverity() != null
                                ? alarm.getSeverity().name().toLowerCase(Locale.ROOT) : null)
                        .nmlKey(alarm.getNmlKey())
                        .neId(alarm.getNeId())
                        .alarmIndex(alarm.getIndex())
                        .clearTime(alarm.getClearedTime() == null ? null
                                : toLongMillis(alarm.getClearedTime()))
                        .archiveTime(alarm.getArchivedTime() == null ? null
                                : toLongMillis(alarm.getArchivedTime()))
                        .archiveType(alarm.getActionType() != null
                                ? ArchiveType.forValue(alarm.getActionType().getIntValue()).name()
                                .toLowerCase(
                                        Locale.ROOT)
                                : null)
                        .build());
            }
        }
        return retList;
    }


    private static Map<String, String> getNmlKeyNamesWithLocalCache(List<String> nmlKeys) {
        Map<String, String> result = new HashMap<>();
        List<String> missKeys = new ArrayList<>();
        for (String key : nmlKeys) {
            if (key == null) {
                continue;
            }
            String name = NML_KEY_NAME_CACHE.getIfPresent(key);
            if (name != null) {
                result.put(key, name);
            } else {
                missKeys.add(key);
            }
        }
        if (!missKeys.isEmpty()) {
            Map<String, BasicCache> cacheMap = topologyCacheManager.batchGetValuesAutoType(
                    missKeys);
            for (String key : missKeys) {
                String name = Optional.ofNullable(cacheMap.get(key))
                        .map(BasicCache::getFriendlyName).orElse(key);
                NML_KEY_NAME_CACHE.put(key, name);
                result.put(key, name);
            }
        }
        return result;
    }

    private static List<String> getCurrentAlarmNmlKeys(List<AlarmRecord> alarmList) {
        Set<String> nmlKey = new HashSet<>();
        alarmList.forEach(alarmRecord -> {
            nmlKey.add(alarmRecord.getNmlKey());
        });
        return new ArrayList<>(nmlKey);
    }

    private static List<String> getHistoryAlarmNmlKeys(List<AlarmHistoryRecord> alarmList) {
        Set<String> nmlKeys = new HashSet<>();
        alarmList.forEach(alarmRecord -> {
            nmlKeys.add(alarmRecord.getNmlKey());
        });
        return new ArrayList<>(nmlKeys);
    }


    public static List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.history.alarms.output.Alarm> converter2HistoryAlarmList(
            List<AlarmHistoryRecord> alarmList) throws Exception {
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.history.alarms.output.Alarm> retList = new ArrayList<>();

        if (alarmList != null) {
            List<String> nmlKeys = getHistoryAlarmNmlKeys(alarmList);
            Map<String, String> cacheMap = getNmlKeyNamesWithLocalCache(nmlKeys);
            for (AlarmHistoryRecord alarm : alarmList) {
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.history.alarms.output.AlarmBuilder builder =
                        new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.history.alarms.output.AlarmBuilder();
                builder.setAlarmId(alarm.getAlarmId());
                builder.setServerity(alarm.getSeverity() != null ? AlarmSeverity
                        .forValue(alarm.getSeverity().getIntValue()) : null);
                builder.setResourceRef(alarm.getResourceRef());
//                String alarmText = getAlarmMessage(alarm);
                builder.setAlarmText(alarm.getAlarmText());
                builder.setAlarmGroup(alarm.getAlarmGroup());
                builder.setAlarmTypeId(alarm.getAlarmTypeId());
                builder.setCreationTime(toMillis(BigInteger.valueOf(
                        alarm.getCreationTime() == null ? 0 : alarm.getCreationTime())));
                builder.setCreationReceivedTime(
                        alarm.getNmlReceivedTime() == null ? null
                                : toMillis(BigInteger.valueOf(alarm.getNmlReceivedTime())));
                builder.setClearTime(toMillis(BigInteger.valueOf(alarm.getClearedTime())));
                builder.setArchiveTime(toMillis(BigInteger.valueOf(alarm.getArchivedTime())));
                builder.setArchiveType(alarm.getActionType() != null ? ArchiveType
                        .forValue(alarm.getActionType().getIntValue()) : null);
                builder.setNeId(alarm.getNeId());
                builder.setNmlKey(alarm.getNmlKey());
                String nmlKeyName = Optional.ofNullable(cacheMap.get(alarm.getNmlKey()))
                        .orElse(alarm.getNmlKey());
                builder.setNmlKeyName(
                        nmlKeyName);
                builder.setSa(alarm.getSa());
                builder.setEquipmentRef(alarm.getComponentRef());
                builder.setAlarmIndex(alarm.getIndex());
                retList.add(builder.build());
            }
        }
        return retList;
    }


    public static List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.alarms.output.Alarm> convert2HistoryAlarmListForAll(
            List<AlarmHistoryRecord> alarmList) throws Exception {
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.alarms.output.Alarm> retList = new ArrayList<>();
        if (alarmList != null) {
            List<String> nmlKeys = getHistoryAlarmNmlKeys(alarmList);
            Map<String, String> cacheMap = getNmlKeyNamesWithLocalCache(nmlKeys);
            for (AlarmHistoryRecord alarm : alarmList) {
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.alarms.output.AlarmBuilder builder =
                        new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.alarms.output.AlarmBuilder();
                builder.setAlarmId(alarm.getAlarmId());
                builder.setServerity(alarm.getSeverity() != null ? AlarmSeverity
                        .forValue(alarm.getSeverity().getIntValue()) : null);
                builder.setResourceRef(alarm.getResourceRef());
//                String alarmText = getAlarmMessage(alarm);
                builder.setAlarmText(alarm.getAlarmText());
                builder.setAlarmGroup(alarm.getAlarmGroup());
                builder.setAlarmTypeId(alarm.getAlarmTypeId());
                builder.setCreationTime(toMillis(BigInteger.valueOf(alarm.getCreationTime())));
                builder.setCreationReceivedTime(
                        toMillis(BigInteger.valueOf(alarm.getNmlReceivedTime())));
                builder.setClearTime(alarm.getClearedTime() != null ? toMillis(
                        BigInteger.valueOf(alarm.getClearedTime())) : null);
                builder.setArchiveTime(alarm.getArchivedTime() == null ? null
                        : toMillis(BigInteger.valueOf(alarm.getArchivedTime())));
                builder.setArchiveType(alarm.getActionType() != null ? ArchiveType
                        .forValue(alarm.getActionType().getIntValue()) : null);
                builder.setNeId(alarm.getNeId());
                if (alarm.getClearedTime() != null || alarm.getArchivedTime() != null) {
                    builder.setSearchType("history");
                } else {
                    builder.setSearchType("current");
                }
                builder.setNmlKey(alarm.getNmlKey());

                String nmlKeyName = Optional.ofNullable(cacheMap.get(alarm.getNmlKey()))
                        .orElse(alarm.getNmlKey());
                builder.setNmlKeyName(
                        nmlKeyName);
                builder.setSa(alarm.getSa());
                builder.setEquipmentRef(alarm.getComponentRef());
                builder.setAlarmIndex(alarm.getIndex());
                retList.add(builder.build());
            }
        }
        return retList;
    }


    public static GetAlarmDetailOutput convert2AlarmDetails(Object alarmObject) {
        GetAlarmDetailOutputBuilder builder = new GetAlarmDetailOutputBuilder();

        if (alarmObject instanceof AlarmRecord) {
            AlarmRecord alarm = (AlarmRecord) alarmObject;
            builder.setAlarmId(alarm.getAlarmId());
            builder.setServerity(alarm.getSeverity() != null ? AlarmSeverity
                    .forValue(alarm.getSeverity().getIntValue()) : null);
            builder.setResourceRef(alarm.getResourceRef());
//            builder.setAlarmText(alarm.getAlarmText());
            String alarmText = getAlarmMessage(alarm);
            builder.setAlarmText(alarmText);
            builder.setAlarmGroup(alarm.getAlarmGroup());
            builder.setAlarmTypeId(alarm.getAlarmTypeId());
            builder.setCreationTime(toMillis(BigInteger.valueOf(alarm.getCreationTime())));
            builder.setCreationReceivedTime(
                    toMillis(BigInteger.valueOf(alarm.getNmlReceivedTime())));
            builder.setNeId(alarm.getNeId());
            builder.setNmlKey(alarm.getNmlKey());
            builder.setSa(alarm.getSa());
            builder.setEquipmentRef(alarm.getComponentRef());
            String nmlKeyName = getAlarmNmlKeyName(alarm.getNmlKey());
            builder.setNmlKeyName(
                    nmlKeyName);

        } else if (alarmObject instanceof AlarmHistoryRecord) {
            AlarmHistoryRecord alarm = (AlarmHistoryRecord) alarmObject;
            builder.setAlarmId(alarm.getAlarmId());
            builder.setServerity(alarm.getSeverity() != null ? AlarmSeverity
                    .forValue(alarm.getSeverity().getIntValue()) : null);
            builder.setResourceRef(alarm.getResourceRef());
            String alarmText = getAlarmMessage(alarm);
            builder.setAlarmText(alarmText);
            builder.setAlarmGroup(alarm.getAlarmGroup());
            builder.setAlarmTypeId(alarm.getAlarmTypeId());
            builder.setCreationTime(toMillis(BigInteger.valueOf(alarm.getCreationTime())));
            builder.setCreationReceivedTime(
                    alarm.getNmlReceivedTime() == null ? null
                            : toMillis(BigInteger.valueOf(alarm.getNmlReceivedTime())));
            builder.setClearTime(toMillis(BigInteger.valueOf(alarm.getClearedTime())));
            builder.setClearReceivedTime(
                    toMillis(BigInteger.valueOf(alarm.getClearedTime())));
            builder.setArchiveTime(toMillis(BigInteger.valueOf(alarm.getArchivedTime())));
            builder.setArchiveType(alarm.getActionType() != null ? ArchiveType
                    .forValue(alarm.getActionType().getIntValue()) : null);
            builder.setNeId(alarm.getNeId());
            builder.setNmlKey(alarm.getNmlKey());
            builder.setSa(alarm.getSa());
            builder.setEquipmentRef(alarm.getComponentRef());
            builder.setAlarmIndex(alarm.getIndex());
            String nmlKeyName = getAlarmNmlKeyName(alarm.getNmlKey());
            builder.setNmlKeyName(
                    nmlKeyName);
        }

        return builder.build();
    }

    public static String getAlarmMessage(AlarmRecord alarm) {
        return message.getAlarmText(alarm.getAlarmGroup(),
                alarm.getAlarmText());
    }

    private static String getAlarmMessage(AlarmHistoryRecord alarm) {
        return message.getAlarmText(alarm.getAlarmGroup(),
                alarm.getAlarmText());
    }

    public static String getAlarmNmlKeyName(String nmlKey) {
        BasicCache cache = topologyCacheManager.getValue(nmlKey);
        return cache == null ? nmlKey : cache.getFriendlyName();
    }

    /**
     * 将时间戳统一转换为毫秒级
     *
     * @param timestamp 原始时间戳（毫秒或纳秒）
     * @return 毫秒级时间戳
     */
    public static BigInteger toMillis(long timestamp) {
        // 判断是否是纳秒级（大于 10^14 基本可以认为是纳秒）
        if (timestamp > 10000000000000L) {
            return BigInteger.valueOf(timestamp / 1_000_000);
        }
        return BigInteger.valueOf(timestamp);
    }

    public static BigInteger toMillis(BigInteger timestamp) {
        if (timestamp.compareTo(BigInteger.valueOf(10000000000000L)) > 0) {
            return timestamp.divide(BigInteger.valueOf(1_000_000));
        }
        return timestamp;
    }

    private static Long toLongMillis(long timestamp) {
        if (timestamp > 10000000000000L) {
            return timestamp / 1_000_000L;
        }
        return timestamp;
    }

    public static String getAlarmNeId(String nmlKey) {
        if (PhysicalTpIdNamingRule.isTpId(nmlKey)) {
            return PhysicalTpIdNamingRule.getNodeId(nmlKey);
        } else if (PhysicalEqpIdNamingRule.isEquipId(nmlKey)
                || PhysicalEqpIdNamingRule.isTransceiverId(nmlKey)) {
            return PhysicalEqpIdNamingRule.getNodeId(nmlKey);
        } else if (PhysicalNodeIdNamingRule.isPhyNodeId(nmlKey)) {
            return nmlKey;
        }
        return null;
    }
}
