package net.flex.dci.otn.controller.resource.statistic.core.utils;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;

/**
 * 2026/4/11
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class ResourceStatisticUtil {

    private static final String DEFAULT_PATTERN = "yyyy-MM-dd HH:mm:ss Z";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_PATTERN);
    private static final ZoneId SYSTEM_ZONE = ZoneId.systemDefault();

    public static String getSimpleTpName(String tpId) {
        if (tpId == null || tpId.isEmpty()) {
            return "";
        }
        int lastHashIndex = tpId.lastIndexOf('#');
        if (lastHashIndex == -1) {
            return tpId;
        }
        return tpId.substring(lastHashIndex + 1);
    }

    public static String formatTimestampWithZone(Long timestamp) {
        if (timestamp == null) {
            return "-";
        }
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), SYSTEM_ZONE)
                .format(FORMATTER);
    }

}
