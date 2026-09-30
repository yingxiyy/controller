package net.flex.dci.otn.controller.implement.physical.component.ne.attribute;

import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.FINISH;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.START;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.FAILED;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.PHYSICAL_ELEMENT.BATCH_UPDATE_NODE_PHYSICAL;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.PHYSICAL_ELEMENT.UPDATE_NODE_FRIENDLY_NAME;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.PHYSICAL_ELEMENT.UPDATE_NODE_LOGIN_INFO;

import com.alibaba.fastjson.JSON;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otn.controller.implement.common.utils.CommonUtils;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.update.node.input.Nodes;
import org.springframework.stereotype.Component;

/**
 * 2026/4/14
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class NodePhysicalUpdateTaskMessageHandler {


    public TaskInfoMessage generateBatchUpdateTaskInfo(List<Nodes> nodesList,
            TaskInfoMessage parentTaskInfoMessage) {
        log.debug("generate the batch update node task info ");
        int size = nodesList.size();
        String batchUpdateNeTopic = generateBatchOperationName(size);

        TaskInfoMessage batchTaskInfoMessage = new TaskInfoMessage(
                parentTaskInfoMessage.getWho(),
                parentTaskInfoMessage.getResourceType(),
                ActionType.updateDevice,
                parentTaskInfoMessage.getDetail()
        );
        batchTaskInfoMessage.setResourceName(batchUpdateNeTopic);
        batchTaskInfoMessage.setResourceId(batchUpdateNeTopic);
        batchTaskInfoMessage.setRoot(true);
        batchTaskInfoMessage.setGroupId(System.currentTimeMillis());
        batchTaskInfoMessage.setSuccessfully(false);

        return batchTaskInfoMessage;
    }


    private String generateBatchOperationName(int tunnelCount) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
        String timestamp = dateFormat.format(new Date());
        return String.format(BATCH_UPDATE_NODE_PHYSICAL, tunnelCount, timestamp);
    }


    public String generateUpdateFriendlyNameOperationName(String oldFriendlyName,
            String friendlyName) {
        return String.format(UPDATE_NODE_FRIENDLY_NAME, oldFriendlyName, friendlyName);
    }

    public TaskInfoMessage generateUpdateTaskInfo(Nodes nodes,
            TaskInfoMessage taskInfoMessage, ActionType actionType) {
        log.debug("generate update friendly name task info");
        Long groupId = taskInfoMessage.getGroupId();
        NodeInfo nodeInfo = buildNodeInfo(nodes);
        String jsonDetail = JSON.toJSONString(nodeInfo);
        TaskInfoMessage newTaskInfoMessage = new TaskInfoMessage(taskInfoMessage.getWho(),
                taskInfoMessage.getResourceType(), actionType, taskInfoMessage.getDetail());
        newTaskInfoMessage.setGroupId(taskInfoMessage.getGroupId());
        newTaskInfoMessage.setRoot(groupId == null);
        newTaskInfoMessage.setSuccessfully(false);
        newTaskInfoMessage.setResourceId(nodes.getNodeId().getValue());
        newTaskInfoMessage.setDetail(jsonDetail);
        return newTaskInfoMessage;
    }

    private NodeInfo buildNodeInfo(Nodes nodes) {
        Physical physical = nodes.getPhysical();
        return NodeInfo.builder().neId(nodes.getNodeId().getValue()).ip(physical.getIp())
                .port(physical.getPort() == null ? null : physical.getPort().getValue())
                .friendlyName(physical.getFriendlyName())
                .loginName(physical.getLoginName()).loginPwd(physical.getLoginPasswd()).build();
    }

    public String generateUpdateNeLoginInfoOperationName(String friendlyName) {
        return String.format(UPDATE_NODE_LOGIN_INFO, friendlyName);
    }

    public void startLogBatchUpdate(TaskInfoMessage taskInfoMessage) {
        log.debug("log batch aps switch result the TaskInfoMessage:{}", taskInfoMessage);
        TaskInfoMessager.sendMessage(taskInfoMessage);
        String msg = String.format(taskInfoMessage.getResourceName() + START);
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().title(BroadCastConstant.BATCH_UPDATE_NODE_PHYSICAL)
                        .message(msg).error(false).build());
    }


    public void endLogBatchUpdate(String resultMessage, TaskInfoMessage taskInfoMessage,
            int failedSize) {
        log.debug("end to batch update result the taskInfo message is finished");
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        taskInfoMessage.setErrorReason(resultMessage);
        taskInfoMessage.setSuccessfully(failedSize == 0);
        TaskInfoMessager.sendMessage(taskInfoMessage);
        String msg = taskInfoMessage.getResourceName() + FINISH + " " + resultMessage;
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().title(BroadCastConstant.BATCH_UPDATE_NODE_PHYSICAL)
                        .message(msg).error(failedSize > 0).build());

    }

    public void endLogFailedBatchUpdate(String errorResult, TaskInfoMessage taskInfoMessage) {
        log.debug("end to batch update result the taskInfo message is failed");
        String requestBody = taskInfoMessage.getDetail();
        String detailBody = CommonUtils.buildRequestDetail(requestBody, errorResult);
        taskInfoMessage.setDetail(detailBody);
        taskInfoMessage.setEndTime(System.currentTimeMillis());
        taskInfoMessage.setErrorReason(FAILED);
        taskInfoMessage.setSuccessfully(false);
        TaskInfoMessager.sendMessage(taskInfoMessage);
        String msg = taskInfoMessage.getResourceName() + FINISH + " " + errorResult;
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder().title(BroadCastConstant.BATCH_UPDATE_NODE_PHYSICAL)
                        .message(msg).error(true).build());
    }
}
