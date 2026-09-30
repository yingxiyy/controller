package net.flex.dci.otn.controller.resource.statistic.utils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.dto.query.CircuitQueryDTO;
import net.flex.dci.otn.controller.resource.statistic.dto.query.CustomQueryConditionDTO;
import net.flex.dci.otn.controller.resource.statistic.dto.query.MaterialQueryDTO;
import net.flex.dci.otn.controller.resource.statistic.dto.query.PerformanceQueryDTO;
import net.flex.dci.otn.controller.resource.statistic.dto.query.QueryResultInfo;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;
import net.flex.dci.otn.controller.resource.statistic.enums.MaterialCategory;
import net.flex.dci.otn.controller.resource.statistic.export.CardCsv;
import net.flex.dci.otn.controller.resource.statistic.export.LLDPCsv;
import net.flex.dci.otn.controller.resource.statistic.export.TransceiverCsv;
import net.flex.dci.otn.controller.resource.statistic.export.TunnelCsv;
import net.flex.dci.otn.controller.resource.statistic.utils.Constants.Sheet;
import org.springframework.util.DigestUtils;

/**
 * 2026/7/22
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class UnifiedQueryParamResolver {

    public static QueryResultInfo resolveQueryResult(UnifiedQueryParam param) {
        if (param.getQueryCondition() == null) {
            return null;
        }
        CustomQueryConditionDTO queryCondition = param.getQueryCondition();
        switch (param.getResourceQueryType()) {
            case Material:
                return resolveMaterialQuery(queryCondition.getMaterialQuery());

            case Performance:
                return resolvePerformanceQuery(queryCondition.getPerformanceQuery());

            case Tunnel:
                return resolveCircuitQuery(queryCondition.getTunnelQuery());
        }
        return null;
    }

    private static QueryResultInfo resolveCircuitQuery(CircuitQueryDTO tunnelQuery) {
        log.debug("resolve circuit query the:{}", tunnelQuery);
        boolean isLLDP = tunnelQuery.isIncludeLLdp();
        String sheetName = isLLDP ? Sheet.LLDP : Sheet.TUNNEL;
        Class<?> clazz = isLLDP ? LLDPCsv.class : TunnelCsv.class;
        return QueryResultInfo.builder().sheetName(sheetName).clazz(clazz).build();
    }

    private static QueryResultInfo resolvePerformanceQuery(PerformanceQueryDTO queryCondition) {
        log.debug("resolve performance query:{}", queryCondition);
        return QueryResultInfo.builder().build();
    }

    private static QueryResultInfo resolveMaterialQuery(MaterialQueryDTO queryCondition) {
        log.debug("resolve material query the condition:{}", queryCondition);
        MaterialCategory materialCategory = queryCondition.getMaterialCategory();
        String sheetName = Sheet.CARD;
        Class<?> clazz = CardCsv.class;
        if (materialCategory == MaterialCategory.TRANSCEIVER) {
            sheetName = Sheet.TRANSCEIVER;
            clazz = TransceiverCsv.class;
        }
        return QueryResultInfo.builder().clazz(clazz).sheetName(sheetName).build();
    }

    public static String generateQueryFileName(UnifiedQueryParam param) {
        String type = param.getResourceQueryType().name().toLowerCase();
        String subnets = param.getSubnet() != null
                ? String.join(",", param.getSubnet()) : "";
        String hash = DigestUtils.md5DigestAsHex(subnets.getBytes(StandardCharsets.UTF_8))
                .substring(0, 8);
        String detail = resolveDetailSuffix(param);

        return String.format("%s%s_%s_%s", type, detail, hash,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")));
    }

    private static String resolveDetailSuffix(UnifiedQueryParam param) {
        if (param.getQueryCondition() == null) {
            return "";
        }
        CustomQueryConditionDTO c = param.getQueryCondition();
        if (c.getMaterialQuery() != null) {
            return "_" + c.getMaterialQuery().getMaterialCategory().name().toLowerCase();
        }
        if (c.getTunnelQuery() != null) {
            return c.getTunnelQuery().isIncludeLLdp() ? "_lldp" : "_tunnel";
        }
        if (c.getPerformanceQuery() != null) {
            PerformanceQueryDTO p = c.getPerformanceQuery();
            return p.getPerformanceLevel() != null ? "_" + p.getPerformanceLevel().name()
                    .toLowerCase() : "_pm";
        }
        return "";
    }

}
