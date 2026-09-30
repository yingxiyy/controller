package net.flex.dci.otn.controller.resource.statistic.utils;

import static net.flex.dci.otn.controller.resource.statistic.utils.Constants.QUERY_TASK_PREFIX;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.dto.csv.UnifiedExportRequest;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedResourceQueryParam;

/**
 * 2026/6/22
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class CommonUtils {

    /**
     * export csv file name
     *
     * @return
     */
    public static String exportCsvFileName(UnifiedExportRequest exportRequest) {
        log.debug("export csv file name");
        String type = exportRequest.getUnifiedType().name().toLowerCase();
        String scope = exportRequest.getScope() == null ? "default"
                : exportRequest.getScope().name().toLowerCase();

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        return String.format("%s_inventory_%s_%s", type, scope, timestamp);
    }


    public static String buildNameWithFirst(String base, String type, List<String> names) {
        String first = names.get(0);
        int total = names.size();
        String safeFirst = sanitize(first);
        if (total == 1) {
            return base + "_" + type + "_" + safeFirst;
        } else {
            return base + "_" + type + "_" + safeFirst + "_plus_" + total;
        }
    }

    private static String sanitize(String name) {
        if (name == null) {
            return "unknown";
        }
        return name.replaceAll("[^\\w\\u4e00-\\u9fa5_-]", "_");
    }

    public static String exportQueryFileName(UnifiedResourceQueryParam unifiedResourceQueryParam) {
        String type = unifiedResourceQueryParam.getUnifiedType().name().toLowerCase();

        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        return String.format("query_%s_%s", type, timestamp);
    }


    public static String generateQueryTaskName(UnifiedQueryParam unifiedQueryParam) {
        return QUERY_TASK_PREFIX + unifiedQueryParam.getResourceQueryType() + "_"
                + System.currentTimeMillis();
    }
}
