package net.flex.dci.otn.controller.implement.physical.component.aps;

import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.AUTO_SWITCH_TEMPLATE;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.BATCH_APS_SWITCH_RESULT;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.BATCH_APS_SWITCH_START;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.BATCH_APS_SWITCH_TEMPLATE;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.CONFIG_APS_TOPIC_TEMPLATE;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.FINISH;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.NONE_AUTO_SWITCH_TEMPLATE;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.RESTORE_PATH_TEMPLATE;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.TUNNEL_AUTO_SWITCH_TEMPLATE;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.TUNNEL_NONE_AUTO_SWITCH_TEMPLATE;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.FAILED;

import com.alibaba.fastjson.JSON;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.AuthConstant;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.model.TaskInfoMessage.ResourceType;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.implement.common.dto.ApsSwitchConfig;
import net.flex.dci.otn.controller.implement.common.dto.ApsSwitchConfigTaskDetail;
import net.flex.dci.otn.controller.implement.common.dto.ApsSwitchControlSubTaskDetail;
import net.flex.dci.otn.controller.implement.common.dto.RestoreResult;
import net.flex.dci.otn.controller.implement.common.dto.SwitchResult;
import net.flex.dci.otn.controller.implement.common.enums.ApsMember;
import net.flex.dci.otn.controller.implement.common.enums.ApsSwitchMode;
import net.flex.dci.otn.controller.implement.common.enums.CustomApsPath;
import net.flex.dci.otn.controller.implement.common.enums.SetResultCode;
import net.flex.dci.otn.controller.implement.common.utils.Constants;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.batch.aps._switch.input.ApsSwitch;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;

/**
 * 2025/8/24
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class ApsTaskMessageHandler {

    private final PhyNodeDao phyNodeDao;


    public TaskInfoMessage generateApsSwitchControlTask(HttpServletRequest request,
            String apsSwitchControlInput) {
        log.debug("generate aps switch controlTask apsSwitch control input is:{}",
                apsSwitchControlInput);
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                ResourceType.device,
                ActionType.apsSwitch, //will change based on param
                apsSwitchControlInput);
        return taskInfoMessage;
    }

    public TaskInfoMessage generateBatchApsTaskInfo(HttpServletRequest request,
            String batchApsSwitchInput, List<String> tunnelIds) {
        log.debug("generate batch aps taskInfo input is:{}", batchApsSwitchInput);
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                ResourceType.tunnel,
                ActionType.batchApsSwitch, //will change based on param
                batchApsSwitchInput);
        String batchApsTaskTopic = generateBatchOperationName(
                tunnelIds.size());
        taskInfoMessage.setResourceName(batchApsTaskTopic);
        taskInfoMessage.setResourceId(batchApsTaskTopic);
        taskInfoMessage.setRoot(true);
        taskInfoMessage.setGroupId(System.currentTimeMillis());
        taskInfoMessage.setSuccessfully(false);
        return taskInfoMessage;
    }

    public TaskInfoMessage generateRestoreApsPathTask(HttpServletRequest request, String input) {
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                ResourceType.device,
                ActionType.updateDevice, //will change based on param
                input);
        return taskInfoMessage;
    }

    private String generateBatchOperationName(int tunnelCount) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
        String timestamp = dateFormat.format(new Date());
        // 生成类似 "BATCH_SWITCH_25_TUNNELS_20251015_1420"
        return String.format(BATCH_APS_SWITCH_TEMPLATE, tunnelCount, timestamp);
    }

    public TaskInfoMessage enrichApsCommandInfo(TaskInfoMessage taskInfo, String neId,
            String apsName, CustomApsPath apsPath,
            ApsSwitchMode switchMode) {
        if (taskInfo.getGroupId() != null) {
            log.debug("sub task aps switch,do nothing");
            return taskInfo;
        }
        log.debug("enrich aps command info the aps mode is:{}", switchMode);
        String neName = phyNodeDao.getFriendlyName(neId);
        String taskInfoTopic = generateTaskInfoTopic(neName, apsName, apsPath, switchMode);
        taskInfo.setResourceName(taskInfoTopic);
        taskInfo.setResourceId(neId);
        taskInfo.setSuccessfully(false);
        return taskInfo;
    }

    public TaskInfoMessage enrichRestoreCommandInfo(TaskInfoMessage restoreTaskInfo, String neId,
            String apsName, ApsMember apsMember) {
        log.debug("enrich restore aps command info the aps:{} member:{}", apsName, apsMember);
        String neName = phyNodeDao.getFriendlyName(neId);
        String taskInfoTopic = generateRestoreTaskInfoTopic(neName, apsName, apsMember);
        restoreTaskInfo.setResourceName(taskInfoTopic);
        restoreTaskInfo.setResourceId(neId);
        restoreTaskInfo.setSuccessfully(false);
        return restoreTaskInfo;
    }

    /**
     * generate task info topic
     *
     * @param neName
     * @param apsName
     * @param apsPath
     * @return
     */
    private String generateTaskInfoTopic(String neName, String apsName, CustomApsPath apsPath,
            ApsSwitchMode switchMode) {
        log.debug(
                "generate task info resource name the switch mode is:{} and neName :{} aps Name :{}",
                switchMode, neName, apsName);
        String taskInfoTopic = "";
        if (switchMode == ApsSwitchMode.AUTO) {
            taskInfoTopic = String.format(AUTO_SWITCH_TEMPLATE, switchMode.getName(), neName,
                    apsName);
        } else {
            taskInfoTopic = String.format(NONE_AUTO_SWITCH_TEMPLATE, switchMode,
                    apsPath.actualApsPath().getBriefName(), neName, apsName);
        }
        return taskInfoTopic;
    }

    public void logApsSwitchResult(TaskInfoMessage taskInfoMessage, SwitchResult switchResult) {
        log.debug("log aps switch result taskInfoMessage:{}", taskInfoMessage);
        SetResultCode executeCode = switchResult.getCode();
        boolean isError = true;
        String msg = String.format("%s %s %s", BroadCastConstant.APS_SWITCH_COMMAND,
                taskInfoMessage.getResourceName(), FINISH);
        StringBuilder msgBuilder = new StringBuilder(msg);
        if (executeCode.equals(SetResultCode.SUCCESS)) {
            isError = false;
            msgBuilder.append(Constants.SUCCESSFULLY);
            taskInfoMessage.setSuccessfully(true);
        } else if (executeCode.equals(SetResultCode.FAILED)) {
            isError = true;
            msgBuilder.append(FAILED)
                    .append(Constants.REASON_IS)
                    .append(switchResult.getMessage());
            taskInfoMessage.setSuccessfully(false);
            ApsSwitchConfigTaskDetail apsSwitchConfigTaskDetail = ApsSwitchConfigTaskDetail.builder()
                    .commandDetail(taskInfoMessage.getDetail())
                    .detailResult(switchResult.getMessage())
                    .build();
            String switchDetial = JSON.toJSONString(apsSwitchConfigTaskDetail);
            taskInfoMessage.setDetail(switchDetial);
            taskInfoMessage.setErrorReason(FAILED);
        }
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        TaskInfoMessager.sendMessage(taskInfoMessage);
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title(BroadCastConstant.APS_SWITCH_COMMAND)
                        .message(msgBuilder.toString())
                        .error(isError)
                        .build());
    }

    public void logApsRestoreResult(TaskInfoMessage restoreTaskInfo, RestoreResult restoreResult) {
        log.debug("log aps restore path taskInfoMessage:{}", restoreTaskInfo);
        SetResultCode executeCode = restoreResult.getCode();
        boolean isError = true;
        String msg = String.format("%s %s %s", BroadCastConstant.RESTORE_PATH,
                restoreTaskInfo.getResourceName(), FINISH);
        StringBuilder msgBuilder = new StringBuilder(msg);
        if (executeCode.equals(SetResultCode.SUCCESS)) {
            isError = false;
            msgBuilder.append(Constants.SUCCESSFULLY);
            restoreTaskInfo.setSuccessfully(true);
        } else if (executeCode.equals(SetResultCode.FAILED)) {
            isError = true;
            msgBuilder.append(FAILED)
                    .append(Constants.REASON_IS)
                    .append(restoreResult.getMessage());
            restoreTaskInfo.setSuccessfully(false);
            restoreTaskInfo.setErrorReason(restoreResult.getMessage());
        }
        restoreTaskInfo.setEndTime(System.currentTimeMillis());
        TaskInfoMessager.sendMessage(restoreTaskInfo);
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title(BroadCastConstant.RESTORE_PATH)
                        .message(msgBuilder.toString())
                        .error(isError)
                        .build());
    }


    public void logStartBatchApsSwitch(TaskInfoMessage rootTaskInfo) {
        log.debug("log batch aps switch result the TaskInfoMessage:{}", rootTaskInfo);
        TaskInfoMessager.sendMessage(rootTaskInfo);
        String msg = String.format(BATCH_APS_SWITCH_START, rootTaskInfo.getResourceName());
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().title(BroadCastConstant.BATCH_APS_SWITCH_COMMAND)
                        .message(msg).error(false).build());
    }

    public void logFishedBatchApsSwitch(TaskInfoMessage rootTaskInfo, List<SwitchResult> results) {
        log.debug("calculate the total batch aps switch result:{}", rootTaskInfo.getResourceName());
        long successCount = results.stream()
                .filter(result -> result.getCode() == SetResultCode.SUCCESS)
                .count();
        int failureCount = results.size() - (int) successCount;
        int totalCount = results.size();
        double successRate = calculateSuccessRate((int) successCount, totalCount);
        String resultsMessage = String.format(BATCH_APS_SWITCH_RESULT, successCount, failureCount,
                totalCount,
                successRate);
        rootTaskInfo.setErrorReason(resultsMessage);
        rootTaskInfo.setEndTime(System.currentTimeMillis());
        boolean hasError = failureCount > 0;
        rootTaskInfo.setSuccessfully(failureCount == 0);
        TaskInfoMessager.sendMessage(rootTaskInfo);
        String detailedMessage = buildDetailedMessage(rootTaskInfo.getResourceName(), results,
                (int) successCount,
                failureCount, successRate);
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().title(BroadCastConstant.BATCH_APS_SWITCH_COMMAND)
                        .message(detailedMessage).error(hasError).build()
        );

        if (hasError) {
            log.warn("Batch switch operation completed with errors: {}", resultsMessage);
        } else {
            log.info("Batch switch operation completed successfully: {}", resultsMessage);
        }
    }

    private String buildDetailedMessage(String operationId, List<SwitchResult> results,
            int successCount, int failureCount, double successRate) {
        String message = "Operation: " + operationId + "\n"
                + "Status: " + (failureCount == 0 ? "Complete success" :
                successCount > 0 ? "Partial success" : "Complete failure")
                + "\n"
                + String.format("Results: Success(%d) Failed(%d) Total(%d) Success rate(%.1f%%)",
                successCount, failureCount, successCount + failureCount, successRate)
                + "\n";
        return message;
    }


    public double calculateSuccessRate(int successCount, int totalCount) {
        if (totalCount <= 0) {
            return 0.0;
        }
        return (double) successCount / totalCount * 100;
    }

    private String generateApsSwitchControlSubTaskTopic(String tunnelName, String neName,
            String apsName, ApsPath targetPath) {
        CustomApsPath customApsPath = CustomApsPath.fromApsPath(targetPath);
        ApsSwitchMode switchMode = customApsPath.getApsSwitchMode();
        log.debug(
                "generate aps switch sub task info resource name the switch mode is:{} and neName :{} aps Name :{} ref tunnel :{}",
                switchMode, neName, apsName, tunnelName);
        String taskInfoTopic = "";
        if (switchMode == ApsSwitchMode.AUTO) {
            taskInfoTopic = String.format(TUNNEL_AUTO_SWITCH_TEMPLATE, switchMode.getName(), neName,
                    apsName, tunnelName);
        } else {
            taskInfoTopic = String.format(TUNNEL_NONE_AUTO_SWITCH_TEMPLATE, switchMode,
                    customApsPath.actualApsPath().getBriefName(), neName, apsName, tunnelName);
        }
        return taskInfoTopic;
    }


    private String generateApsSwitchConfigSubTaskTopic(String tunnelName, String neName,
            String apsName) {
        String apsSwitchConfigSubTaskTopic = String.format(CONFIG_APS_TOPIC_TEMPLATE, neName,
                apsName, tunnelName);
        return apsSwitchConfigSubTaskTopic;
    }


    /**
     * log aps switch control total finish subTask
     *
     * @param tunnel
     * @param ochLinkId
     * @param apsCrossConnection
     * @param username
     * @param groupId
     * @param targetPath
     * @param switchResultCode
     * @param message
     */
    public void logApsSwitchControlSubTask(Tunnel tunnel, String ochLinkId,
            CrossConnections apsCrossConnection, String username, Long groupId,
            ApsPath targetPath,
            SetResultCode switchResultCode, String message) {
        String tunnelId = tunnel.getTunnelId().getValue();
        log.debug("aps switch control sub task for the tunnel Id:{} ref och link id:{}", tunnelId,
                ochLinkId);
        String neId = apsCrossConnection.getNodeRef().getValue();
        String apsCrossConnectionId = apsCrossConnection.getCrossConnectionId().getValue();
        String apsName = apsCrossConnection.getAps().getName();
        String tunnelName = tunnel.getFriendlyName();
        String neName = phyNodeDao.getFriendlyName(neId);
        ApsSwitchControlSubTaskDetail apsSwitchControlSubTaskDetail = getApsSwitchControlSubTaskDetail(
                targetPath, tunnelId, tunnelName, neId, neName, apsName, apsCrossConnectionId);
        String detail = JSON.toJSONString(apsSwitchControlSubTaskDetail);
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(username, ResourceType.device,
                ActionType.apsSwitch, detail);
        taskInfoMessage.setGroupId(groupId);
        taskInfoMessage.setSuccessfully(true);
        taskInfoMessage.setRoot(false);
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        String topic = generateApsSwitchControlSubTaskTopic(tunnelName, neName, apsName,
                targetPath);
        taskInfoMessage.setResourceName(topic);
        taskInfoMessage.setResourceId(neId);
        String detailMessage = buildApsSwitchSetPopupMessage(tunnelName, neName, apsName,
                targetPath,
                switchResultCode, message);
        TaskInfoMessager.sendMessage(taskInfoMessage);
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().title(BroadCastConstant.BATCH_APS_SWITCH_COMMAND)
                        .message(detailMessage).error(false).build()
        );
    }

    /**
     * aps switch set sub task start
     *
     * @param tunnel
     * @param ochLinkId
     * @param apsCrossConnection
     * @param username
     * @param groupId
     * @param targetPath
     * @return
     */
    public TaskInfoMessage logStartApsSwitchControlSubTask(Tunnel tunnel, String ochLinkId,
            CrossConnections apsCrossConnection, String username, Long groupId,
            ApsPath targetPath) {
        String tunnelId = tunnel.getTunnelId().getValue();
        log.debug("log aps switch control sub task start,the tunnel id:{} ref och link id:{}",
                tunnelId, ochLinkId);
        String neId = apsCrossConnection.getNodeRef().getValue();
        String apsCrossConnectionId = apsCrossConnection.getCrossConnectionId().getValue();
        String apsName = apsCrossConnection.getAps().getName();
        String tunnelName = tunnel.getFriendlyName();
        String neName = phyNodeDao.getFriendlyName(neId);
        ApsSwitchControlSubTaskDetail apsSwitchControlSubTaskDetail = getApsSwitchControlSubTaskDetail(
                targetPath, tunnelId, tunnelName, neId, neName, apsName, apsCrossConnectionId);

        String detail = JSON.toJSONString(apsSwitchControlSubTaskDetail);
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(username, ResourceType.device,
                ActionType.apsSwitch, detail);
        taskInfoMessage.setGroupId(groupId);
        taskInfoMessage.setRoot(false);
        taskInfoMessage.setSuccessfully(false);

        String topic = generateApsSwitchControlSubTaskTopic(tunnelName, neName, apsName,
                targetPath);
        taskInfoMessage.setResourceName(topic);
        taskInfoMessage.setResourceId(neId);
        TaskInfoMessager.sendMessage(taskInfoMessage);
        String detailMessage = buildApsSwitchStartPopupMessage(tunnelName, neName, apsName,
                targetPath);
        TaskInfoMessager.sendMessage(taskInfoMessage);
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().title(BroadCastConstant.BATCH_APS_SWITCH_COMMAND)
                        .message(detailMessage).error(false).build()
        );
        return taskInfoMessage;
    }

    private ApsSwitchControlSubTaskDetail getApsSwitchControlSubTaskDetail(
            ApsPath targetPath, String tunnelId, String tunnelName, String neId, String neName,
            String apsName, String apsCrossConnectionId) {
        ApsSwitchControlSubTaskDetail apsSwitchControlSubTaskDetail = ApsSwitchControlSubTaskDetail.builder()
                .tunnelId(tunnelId)
                .tunnelName(tunnelName)
                .neId(neId)
                .neName(neName)
                .apsName(apsName)
                .crossConnectionId(apsCrossConnectionId)
                .apsPath(targetPath)
                .build();
        return apsSwitchControlSubTaskDetail;
    }


    public void logFinishApsSwitchControlSubTask(TaskInfoMessage subTaskInfo, SetResultCode code,
            String message) {
        log.debug("log finish aps switch control sub task:{} result :{} result message :{}",
                subTaskInfo.getResourceName(), code, message);
        String detailJson = subTaskInfo.getDetail();
        log.debug("send aps switch control subTask finished notification");
        ApsSwitchControlSubTaskDetail apsSwitchControlSubTaskDetail = JSON.parseObject(detailJson,
                ApsSwitchControlSubTaskDetail.class);
        apsSwitchControlSubTaskDetail.setDetailResult(message);
        boolean isSuccessFully = code.equals(SetResultCode.SUCCESS);
        boolean isError = !code.equals(SetResultCode.SUCCESS);
        subTaskInfo.setSuccessfully(isSuccessFully);
        subTaskInfo.setErrorReason(isSuccessFully ? null : FAILED);
        subTaskInfo.setEndTime(System.currentTimeMillis());
        subTaskInfo.setDetail(JSON.toJSONString(apsSwitchControlSubTaskDetail));
        TaskInfoMessager.sendMessage(subTaskInfo);
//        String detailJson = subTaskInfo.getDetail();
//        log.debug("send aps switch control subTask finished notification");
//        ApsSwitchControlSubTaskDetail apsSwitchControlSubTaskDetail = JSON.parseObject(detailJson,
//                ApsSwitchControlSubTaskDetail.class);
        String finishDetailMessage = buildApsSwitchSetPopupMessage(
                apsSwitchControlSubTaskDetail.getTunnelName(),
                apsSwitchControlSubTaskDetail.getNeName(),
                apsSwitchControlSubTaskDetail.getApsName(),
                apsSwitchControlSubTaskDetail.getApsPath(), code, message);
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().title(BroadCastConstant.BATCH_APS_SWITCH_COMMAND)
                        .message(finishDetailMessage).error(isError).build()
        );
    }

    public TaskInfoMessage logStartApsConfigSubTask(Tunnel tunnel, String ochLinkId,
            String username, String apsCrossConnectionId, String apsName, String neId,
            Long groupId, ApsSwitch apsSwitch) {
        String tunnelId = tunnel.getTunnelId().getValue();
        log.debug("log aps config subtask start for the tunnel :{} ref och link id:{}", tunnelId,
                ochLinkId);
        String tunnelName = tunnel.getFriendlyName();
        String neName = phyNodeDao.getFriendlyName(neId);
        ApsSwitchConfig apsSwitchConfig = ApsSwitchConfig.parseApsSwitch(apsSwitch);
        ApsSwitchConfigTaskDetail apsSwitchConfigTaskDetail = constructApsSwitchConfigDetails(
                tunnelId, tunnelName, neId, neName, apsName, apsCrossConnectionId, apsSwitchConfig);
        String detail = JSON.toJSONString(apsSwitchConfigTaskDetail);
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(username, ResourceType.device,
                ActionType.updateDevice, detail);
        taskInfoMessage.setGroupId(groupId);
        taskInfoMessage.setRoot(false);
        String topic = generateApsSwitchConfigSubTaskTopic(tunnelName, neName, apsName);
        taskInfoMessage.setResourceName(topic);
        taskInfoMessage.setResourceId(neId);
        String detailMessage = buildApsConfigStartPopupMessage(tunnelName, neName, apsName);
        TaskInfoMessager.sendMessage(taskInfoMessage);
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().title(BroadCastConstant.BATCH_APS_SWITCH_COMMAND)
                        .message(detailMessage).error(false).build()
        );
        return taskInfoMessage;
    }

    public void logFinishApsConfigSubTask(TaskInfoMessage subTask, SetResultCode code,
            String message) {
        log.debug("log finish aps config for the subTask:{} result code:{} and message is:{}",
                subTask.getResourceName(), code, message);
        boolean isSuccessFully = code.equals(SetResultCode.SUCCESS);
        boolean isError = !code.equals(SetResultCode.SUCCESS);
        subTask.setSuccessfully(isSuccessFully);
        subTask.setEndTime(System.currentTimeMillis());
        String detailJson = subTask.getDetail();
        log.debug("send config aps switch control subTask finished notification");
        ApsSwitchConfigTaskDetail apsSwitchConfigTaskDetail = JSON.parseObject(detailJson,
                ApsSwitchConfigTaskDetail.class);
        apsSwitchConfigTaskDetail.setDetailResult(message);
        subTask.setDetail(JSON.toJSONString(apsSwitchConfigTaskDetail));
        TaskInfoMessager.sendMessage(subTask);
        String finishDetailMessage = buildApsSwitchConfigPopupMessage(
                apsSwitchConfigTaskDetail.getTunnelName(),
                apsSwitchConfigTaskDetail.getNeName(),
                apsSwitchConfigTaskDetail.getApsName(),
                code, message);
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().title(BroadCastConstant.BATCH_APS_SWITCH_COMMAND)
                        .message(finishDetailMessage).error(isError).build()
        );
    }

    public void logApsConfigSubTask(Tunnel tunnel, String ochLinkId, String username,
            String apsCrossConnectionId, String neId, Long groupId,
            String apsName, ApsSwitch apsSwitch, SetResultCode switchResultCode,
            String message) {
        String tunnelId = tunnel.getTunnelId().getValue();
        log.debug("log aps config subTask for the tunnel Id:{} ref och link id:{}", tunnelId,
                ochLinkId);
        String tunnelName = tunnel.getFriendlyName();
        String neName = phyNodeDao.getFriendlyName(neId);
        ApsSwitchConfig apsSwitchConfig = ApsSwitchConfig.parseApsSwitch(apsSwitch);
        ApsSwitchConfigTaskDetail apsSwitchConfigTaskDetail = constructApsSwitchConfigDetails(
                tunnelId, tunnelName, neId, neName, apsName, apsCrossConnectionId, apsSwitchConfig);
        String detail = JSON.toJSONString(apsSwitchConfigTaskDetail);
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(username, ResourceType.device,
                ActionType.updateDevice, detail);
        taskInfoMessage.setGroupId(groupId);
        taskInfoMessage.setSuccessfully(true);
        taskInfoMessage.setRoot(false);
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        String topic = generateApsSwitchConfigSubTaskTopic(tunnelName, neName, apsName);
        taskInfoMessage.setResourceName(topic);
        taskInfoMessage.setResourceId(neId);
        String detailMessage = buildApsSwitchConfigPopupMessage(tunnelName, neName, apsName,
                switchResultCode, message);
        TaskInfoMessager.sendMessage(taskInfoMessage);
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().title(BroadCastConstant.BATCH_APS_SWITCH_COMMAND)
                        .message(detailMessage).error(false).build()
        );

    }

    private ApsSwitchConfigTaskDetail constructApsSwitchConfigDetails(String tunnelId,
            String tunnelName, String neId, String neName, String apsName,
            String apsCrossConnectionId, ApsSwitchConfig apsSwitchConfig) {
        ApsSwitchConfigTaskDetail apsSwitchConfigTaskDetail = ApsSwitchConfigTaskDetail.builder()
                .tunnelId(tunnelId)
                .tunnelName(tunnelName)
                .neId(neId)
                .apsName(apsName)
                .apsXcId(apsCrossConnectionId)
                .neName(neName)
                .apsSwitch(apsSwitchConfig)
                .build();
        return apsSwitchConfigTaskDetail;
    }

    private String buildApsSwitchConfigPopupMessage(String tunnelName, String neName,
            String apsName, SetResultCode resultCode, String message) {
        boolean success = resultCode == SetResultCode.SUCCESS;
        String resultText = success ? "successful" : "failed";

        String base = String.format(
                "APS config %s: Tunnel %s, NE %s, APS %s",
                resultText, tunnelName, neName, apsName
        );

        if (!success && message != null && !message.isEmpty()) {
            base += String.format(". Reason: %s", message);
        }

        return base;
    }

    private String buildApsSwitchSetPopupMessage(
            String shortTunnelName,
            String neName,
            String apsName,
            ApsPath targetPath,
            SetResultCode resultCode,
            String reason) {

        String resultText = resultCode == SetResultCode.SUCCESS ? "successful" : "failed";

        if (resultCode == SetResultCode.SUCCESS) {
            return String.format(
                    "APS switch %s: Tunnel %s, NE %s, APS %s,set %s ",
                    resultText, shortTunnelName, neName, apsName, targetPath
            );
        } else {
            return String.format(
                    "APS switch %s: Tunnel %s, NE %s, APS %s failed to set %s. Reason: %s.",
                    resultText, shortTunnelName, neName, apsName, targetPath, reason
            );
        }
    }

    private String buildApsSwitchStartPopupMessage(String tunnelName, String neName, String apsName,
            ApsPath targetPath) {
        return String.format(
                "APS switch : Tunnel %s, NE %s, APS %s,set %s start",
                tunnelName, neName, apsName, targetPath
        );
    }

    private String buildApsConfigStartPopupMessage(String tunnelName, String neName,
            String apsName) {
        return String.format(
                "APS configuration : Tunnel %s, NE %s, APS %s config start",
                tunnelName, neName, apsName
        );
    }


    private String generateRestoreTaskInfoTopic(String neName, String apsName,
            ApsMember apsMember) {
        log.debug("generate restore task info topic neName:{} apsName:{} target member:{}", neName,
                apsName, apsMember);
        String taskInfoTopic = String.format(RESTORE_PATH_TEMPLATE, neName, apsName,
                apsMember.name());
        return taskInfoTopic;
    }


}