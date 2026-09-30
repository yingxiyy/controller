package net.flex.dci.otn.controller.apsswitchlog.component;

import static net.flex.dci.otn.controller.utils.Constants.DEFAULT_SAMPLE_INTERVAL;
import static net.flex.dci.otn.controller.utils.Constants.DOT;
import static net.flex.dci.otn.controller.utils.Constants.PRIMARY;

import com.alibaba.druid.util.StringUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.apsswitchlog.dto.ApsSwitchLogDetail;
import net.flex.dci.otn.controller.apsswitchlog.dto.ApsSwitchLogDto;
import net.flex.dci.otn.controller.apsswitchlog.dto.PageApsSwitchLogDto;
import net.flex.dci.otn.controller.apsswitchlog.dto.SamplePoint;
import net.flex.dci.otn.db.jpa.entity.ApsSwitchLog;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.PhyNodeCache;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/16 13:04
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApsSwitchLogConvertor {

    private static final Pattern PATTERN_A = Pattern.compile("A:([0-9A-F]+)(?=B:|C:|S:|$)");
    private static final Pattern PATTERN_B = Pattern.compile("B:([0-9A-F]+)(?=C:|S:|$)");
    private static final Pattern PATTERN_C = Pattern.compile("C:([0-9A-F]+)(?=S:|$)");
    private static final Pattern PATTERN_S = Pattern.compile("S:([0-9A-F]+)");

    private final DciTopologyCacheManager dciTopologyCacheManager;


    public PageApsSwitchLogDto convertApsSwitchLogPaged(
            Page<ApsSwitchLog> apsSwitchLogfoPage) {
        log.debug("convert aps switch log paged to output");
        List<ApsSwitchLogDto> apsSwitchLogDtos = convertApsSwitchLog2RestOutput(
                apsSwitchLogfoPage.getContent());
        PageApsSwitchLogDto pageIdcData = PageApsSwitchLogDto.builder()
                .apsSwitchLogs(apsSwitchLogDtos)
                .currentPage((long) apsSwitchLogfoPage.getPageable().getPageNumber() + 1L)
                .totalPages(apsSwitchLogfoPage.getTotalPages())
                .pageSize((long) apsSwitchLogfoPage.getSize())
                .totalElements(apsSwitchLogfoPage.getTotalElements()).build();

        return pageIdcData;
    }

    public List<ApsSwitchLogDto> convertApsSwitchLog2RestOutput(
            List<ApsSwitchLog> content) {
        List<String> neIds = content.stream().map(ApsSwitchLog::getNeId)
                .collect(Collectors.toList());
        Map<String, PhyNodeCache> phyNodeCacheMap = dciTopologyCacheManager.batchGetValues(neIds,
                PhyNodeCache.class);
        List<ApsSwitchLogDto> apsSwitchLogDtos = content.stream()
                .map(apsSwitchLog -> convert2ApsSwitchLog(apsSwitchLog, phyNodeCacheMap)).collect(
                        Collectors.toList());
        return apsSwitchLogDtos;
    }

    public ApsSwitchLogDto convert2ApsSwitchLog(ApsSwitchLog apsSwitchLog,
            Map<String, PhyNodeCache> phyNodeCacheMap) {
        String neId = apsSwitchLog.getNeId();
        PhyNodeCache neCache = phyNodeCacheMap.get(neId);
        String neName = neCache == null ? neId : neCache.getFriendlyName();
        long startTime = TimeUnit.NANOSECONDS.toMillis(apsSwitchLog.getStartTime());
        long endTime = TimeUnit.NANOSECONDS.toMillis(apsSwitchLog.getEndTime());
        long duration = TimeUnit.NANOSECONDS.toMillis(apsSwitchLog.getDuration());
        String realActivePath = StringUtils.isEmpty(apsSwitchLog.getActivePath()) ? PRIMARY
                : apsSwitchLog.getActivePath();
        String activePath = realActivePath + DOT + apsSwitchLog.getActiveIndex();
        List<ApsSwitchLogDetail> apsSwitchLogDetails = new ArrayList<>();
        try {

            apsSwitchLogDetails = convertLogText2ApsSwitchLogDetail(
                    apsSwitchLog.getText());
        } catch (Exception e) {
            log.error("failed to convert aps switch log detials :{}", e.getMessage(), e);
            // Do nothing now need parse the log text case by case
        }
        return ApsSwitchLogDto.builder()
                .id(apsSwitchLog.getId())
                .logId(apsSwitchLog.getLogId())
                .neId(neId)
                .neName(neName)
                .apsMode(apsSwitchLog.getApsMode())
                .apsModuleName(apsSwitchLog.getApsModuleName())
                .duration(duration)
                .startTime(startTime)
                .createTimestamp(apsSwitchLog.getCreateTimestamp())
                .endTime(endTime)
                .triggerType(apsSwitchLog.getTriggerType())
                .activePath(activePath)
                .logDetails(apsSwitchLogDetails)
                .build();
    }

    public ApsSwitchLogDto convert2ApsSwitchLog(ApsSwitchLog apsSwitchLog) {
        String neId = apsSwitchLog.getNeId();
        PhyNodeCache neCache = dciTopologyCacheManager.getValue(neId, PhyNodeCache.class);
        String neName = neCache == null ? neId : neCache.getFriendlyName();
        long startTime = TimeUnit.NANOSECONDS.toMillis(apsSwitchLog.getStartTime());
        long endTime = TimeUnit.NANOSECONDS.toMillis(apsSwitchLog.getEndTime());
        long duration = TimeUnit.NANOSECONDS.toMillis(apsSwitchLog.getDuration());
        String realActivePath = StringUtils.isEmpty(apsSwitchLog.getActivePath()) ? PRIMARY
                : apsSwitchLog.getActivePath();
        String activePath = realActivePath + DOT + apsSwitchLog.getActiveIndex();
        List<ApsSwitchLogDetail> apsSwitchLogDetails = new ArrayList<>();
        try {

            apsSwitchLogDetails = convertLogText2ApsSwitchLogDetail(
                    apsSwitchLog.getText());
        } catch (Exception e) {
            log.error("failed to convert aps switch log detials :{}", e.getMessage(), e);
            // Do nothing now need parse the log text case by case
        }
        return ApsSwitchLogDto.builder()
                .id(apsSwitchLog.getId())
                .logId(apsSwitchLog.getLogId())
                .neId(neId)
                .neName(neName)
                .apsMode(apsSwitchLog.getApsMode())
                .apsModuleName(apsSwitchLog.getApsModuleName())
                .duration(duration)
                .startTime(startTime)
                .createTimestamp(apsSwitchLog.getCreateTimestamp())
                .endTime(endTime)
                .triggerType(apsSwitchLog.getTriggerType())
                .activePath(activePath)
                .logDetails(apsSwitchLogDetails)
                .build();
    }

    private List<ApsSwitchLogDetail> convertLogText2ApsSwitchLogDetail(String logText) {
        log.trace("convert log text to aps switch log detail.log txt:{}", logText);
        List<ApsSwitchLogDetail> apsSwitchLogDetails = new ArrayList<>();
        if (logText == null || logText.isEmpty()) {

            return apsSwitchLogDetails;
        }
        if (logText.startsWith("A-in:")) {
            log.debug("Processing A-in format log text");
            String[] splitText = logText.split("\\|");
            log.trace("Split text length: {}", splitText.length);
            int indexA = splitText[0].indexOf(" ");
            int indexB = splitText[1].indexOf(" ");
            int indexC = splitText[2].indexOf(" ");
            int indexS = splitText[3].indexOf(" ");

            Map<String, String> channelHex = new HashMap<>();
            channelHex.put("A", splitText[0].substring(indexA + 1));
            channelHex.put("B", splitText[1].substring(indexB + 1));
            channelHex.put("C", splitText[2].substring(indexC + 1));
            channelHex.put("S", splitText[3].substring(indexS + 1));
            for (Map.Entry<String, String> channelHexEntry : channelHex.entrySet()) {
                String channel = channelHexEntry.getKey();
                String samplePointRaw = channelHexEntry.getValue();
                List<SamplePoint> samplePoints = getAccelinkSamplePoints(samplePointRaw,
                        DEFAULT_SAMPLE_INTERVAL);
                ApsSwitchLogDetail channelApsSwitchLogDetails = ApsSwitchLogDetail.builder()
                        .channel(channel).samplePoints(samplePoints).build();
                apsSwitchLogDetails.add(channelApsSwitchLogDetails);
            }
        } else {
            Map<String, String> channelHex = new HashMap<>();
//            channelHex.put("A", logText.replaceAll(".*A:([0-9A-F]+)B:.*", "$1"));
//            channelHex.put("B", logText.replaceAll(".*B:([0-9A-F]+)(?=C:|S:|$).*", "$1"));
//            channelHex.put("C", logText.replaceAll(".*C:([0-9A-F]+)S:.*", "$1"));
//            channelHex.put("S", logText.replaceAll(".*S:([0-9A-F]+)", "$1"));
            channelHex.put("A", extract(logText, PATTERN_A));
            channelHex.put("B", extract(logText, PATTERN_B));
            channelHex.put("C", extract(logText, PATTERN_C));
            channelHex.put("S", extract(logText, PATTERN_S));

            for (Map.Entry<String, String> channelHexEntry : channelHex.entrySet()) {
                String channel = channelHexEntry.getKey();
                String samplePointRaw = channelHexEntry.getValue();
                if (samplePointRaw == null) {
                    continue;
                }
                List<SamplePoint> samplePoints = getSamplePoints(samplePointRaw,
                        DEFAULT_SAMPLE_INTERVAL);
                ApsSwitchLogDetail channelApsSwitchLogDetails = ApsSwitchLogDetail.builder()
                        .channel(channel).samplePoints(samplePoints).build();
                apsSwitchLogDetails.add(channelApsSwitchLogDetails);
            }
        }

        return apsSwitchLogDetails;
    }

    private String extract(String logText, Pattern pattern) {
        Matcher m = pattern.matcher(logText);
        return m.find() ? m.group(1) : null;
    }

    private List<SamplePoint> getSamplePoints(String samplePointRaw, long intervalMs) {
        log.trace("get sample points by the sample point raw:{}", samplePointRaw);
        List<SamplePoint> points = new ArrayList<>();
        String hexStr = samplePointRaw.replaceAll("\\s+", "");
        for (int i = 0; i < hexStr.length(); i += 4) {
            int index = (i / 4) + 1;
            int b1 = Integer.parseInt(hexStr.substring(i, i + 2), 16);
            int b2 = Integer.parseInt(hexStr.substring(i + 2, i + 4), 16);
            int sign = (b1 & 0x80) == 0 ? 1 : -1;
            int integerPart = b1 & 0x7F;
            double fractionPart = b2 / 256.0;
            double value = sign * (integerPart + fractionPart);
            points.add(SamplePoint.builder().index(index).value(value)
                    .timeOffsetMs((index - 1) * intervalMs).build());
        }
        return points;
    }

    private List<SamplePoint> getAccelinkSamplePoints(String samplePointRaw, long intervalMs) {
        List<SamplePoint> points = new ArrayList<>();
        String[] samplePointRaws = samplePointRaw.split(" ");
        if (samplePointRaws == null || samplePointRaws.length == 0) {
            return points;
        }

        for (int i = 0; i < samplePointRaws.length; i++) {
            String value = samplePointRaws[i];
            if (i == 40) {
                value = value.substring(1, value.length() - 1);
            }
            points.add(SamplePoint.builder().index(i + 1).value(Double.parseDouble(value))
                    .timeOffsetMs((i) * intervalMs).build());
        }
        return points;
    }


}
