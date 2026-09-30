package net.flex.dci.otc.controller.otdr.utils;

import static net.flex.dci.otc.common.constants.Constants.UNDER_LINE;
import static net.flex.dci.otc.controller.otdr.utils.OtdrConstants.SCAN_RESULT_FAILED;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.controller.otdr.domain.OtdrScanTaskDetails;
import net.flex.dci.otc.controller.otdr.model.OtdrTaskInfoResult;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.MonitorDirection;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrParameter.ScanMode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;

/**
 * @version 1.0
 * @date 7/29/2025 3:27 PM
 */
@Slf4j
public class OTDRLogger {


    public static void logOTDRScan(TaskInfoMessage taskInfoMessage, Link otsLink,
            String monitorTpId, ScanMode scanMode, MonitorDirection direction, String resultId) {
        log.debug("log otdr scan task,otsLink :{} monitorTpId:{} direction:{} resultId:{}",
                otsLink.getLinkId().getValue(), monitorTpId, direction, resultId);
        OtsLinkInfo otsLinkInfo = getPhyLinkInfo(otsLink);
        boolean monitorTpIsSource = isMonitorTpIdSource(otsLink, monitorTpId);
        String monitorDirection = OtdrUtils.getMonitorDirectionStr(monitorTpIsSource, direction);
        String resourceName = generateOtdrResourceName(otsLinkInfo.linkName, monitorDirection,
                scanMode);

        OtdrTaskInfoResult otdrTaskInfoResult = OtdrTaskInfoResult.builder()
                .linkId(otsLink.getLinkId().getValue())
                .monitorPort(monitorTpId)
                .otdrResultIdOnNe(resultId)
                .build();
        taskInfoMessage.setResourceName(resourceName);
        taskInfoMessage.setRoot(true);
        taskInfoMessage.setScanResultId(monitorTpId + UNDER_LINE + resultId);
        taskInfoMessage.setSuccessfully(false);
        taskInfoMessage.setDetail(JSONObject.toJSONString(otdrTaskInfoResult));
        taskInfoMessage.setObjectId(otsLink.getLinkId().getValue());
        taskInfoMessage.setObjectType(otsLinkInfo.getLinkType().name());
        TaskInfoMessager.sendMessage(taskInfoMessage);
    }

    public static void logOTDRFinish(TaskInfoMessage taskInfoMessage, Link otsLink,
            String monitorName, String resultId, Long otdrResultId) {
        log.debug(
                "log otdr scan task finished,otsLink :{} monitorName:{} resultId:{} otdrResultId:{}",
                otsLink.getLinkId().getValue(), monitorName, resultId, otdrResultId);
        OtdrTaskInfoResult otdrTaskInfoResult = OtdrTaskInfoResult.builder()
                .otdrDbResultId(otdrResultId)
                .linkId(otsLink.getLinkId().getValue())
                .monitorPort(monitorName)
                .otdrResultIdOnNe(resultId)
                .build();
        String resultDetail = JSONObject.toJSONString(otdrTaskInfoResult);
        taskInfoMessage.setRoot(true);
        taskInfoMessage.setSuccessfully(true);
        taskInfoMessage.setDetail(resultDetail);
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        TaskInfoMessager.sendMessage(taskInfoMessage);
    }

    private static String generateOtdrResourceName(String otsLinkName, String monitorDirection,
            ScanMode scanMode) {
        return String.format("%s->%s-%s-OTDR-SCAN", otsLinkName, monitorDirection, scanMode.name());
    }

    private static boolean isMonitorTpIdSource(Link otsLink, String monitorTpId) {
        String sourceTpId = otsLink.getSource().getSourceTp().getValue();
        return monitorTpId.equals(sourceTpId);
    }

    private static OtsLinkInfo getPhyLinkInfo(Link otsLink) {
        Physical otsLinkPhysical = otsLink.getAugmentation(
                Link1.class).getPhysical();
        String otsLinkName = otsLinkPhysical.getFriendlyName();
        LinkType linkType = otsLinkPhysical.getLinkType();
        return OtsLinkInfo.builder().linkName(otsLinkName).linkType(linkType).build();
    }

    /**
     * log OTDR scan failure to task info manager
     *
     * @param taskInfoMessage
     * @param ex
     */
    public static void logOTDRScanFailure(TaskInfoMessage taskInfoMessage, Exception ex,
            Link otsLink, String monitorTpId, MonitorDirection direction, ScanMode scanMode) {
        log.debug(
                "record log otdr scan task failed exception is:{}", ex.getMessage());
        OtsLinkInfo otsLinkInfo = getPhyLinkInfo(otsLink);
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        boolean isSourceTp = isMonitorTpIdSource(otsLink, monitorTpId);
        String monitorDirectionStr = OtdrUtils.getMonitorDirectionStr(isSourceTp, direction);
        String resourceName = generateOtdrResourceName(otsLinkInfo.linkName, monitorDirectionStr,
                scanMode);
        taskInfoMessage.setSuccessfully(false);
        taskInfoMessage.setResourceName(resourceName);
        taskInfoMessage.setScanResultId(SCAN_RESULT_FAILED);
        taskInfoMessage.setObjectId(otsLink.getLinkId().getValue());
        taskInfoMessage.setObjectType(otsLinkInfo.getLinkType().name());
        String taskInfoDetails = taskInfoMessage.getDetail();
        OtdrScanTaskDetails otdrScanTaskDetails = OtdrScanTaskDetails.builder()
                .input(JSON.parse(taskInfoDetails)).errorMessage(ex.getMessage()).build();
        taskInfoMessage.setDetail(JSONObject.toJSONString(otdrScanTaskDetails));
        TaskInfoMessager.sendMessage(taskInfoMessage);
    }

    public static void logOTDRScanFailure(TaskInfoMessage taskInfoMessage, String failedMessage,
            Link otsLink, String monitorTpId, MonitorDirection direction, ScanMode scanMode) {
        log.debug(
                "record log otdr scan task failed exception is:{}", failedMessage);
        OtsLinkInfo otsLinkInfo = getPhyLinkInfo(otsLink);
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        boolean isSourceTp = isMonitorTpIdSource(otsLink, monitorTpId);
        String monitorDirectionStr = OtdrUtils.getMonitorDirectionStr(isSourceTp, direction);
        String resourceName = generateOtdrResourceName(otsLinkInfo.linkName, monitorDirectionStr,
                scanMode);
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        taskInfoMessage.setSuccessfully(false);
        taskInfoMessage.setResourceName(resourceName);
        taskInfoMessage.setScanResultId(SCAN_RESULT_FAILED);
        taskInfoMessage.setObjectId(otsLink.getLinkId().getValue());
        taskInfoMessage.setObjectType(otsLinkInfo.getLinkType().name());
        String taskInfoDetails = taskInfoMessage.getDetail();
        OtdrScanTaskDetails otdrScanTaskDetails = OtdrScanTaskDetails.builder()
                .input(JSON.parse(taskInfoDetails)).errorMessage(failedMessage).build();
        taskInfoMessage.setDetail(JSONObject.toJSONString(otdrScanTaskDetails));
        TaskInfoMessager.sendMessage(taskInfoMessage);
    }

    @Data
    @Builder
    private static class OtsLinkInfo {

        private LinkType linkType;
        private String linkName;
    }
}
