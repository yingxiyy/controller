package net.flex.dci.otn.controller.subnet.manager.component.taskinfo;

import static net.flex.dci.otn.controller.subnet.manager.utils.Constants.SUCCESS;

import com.alibaba.fastjson.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.subnet.manager.dto.task.SubNetTaskOperationDetail;
import net.flex.dci.otn.controller.subnet.manager.utils.Constants;
import net.flex.dci.otn.controller.subnet.manager.utils.SubnetUtils;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.springframework.stereotype.Component;

/**
 * 2026/2/28
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class SubnetTaskNotificationImpl implements SubnetTaskNotification {

    @Override
    public void sendStartNotification(String broadcastKey, TaskInfoMessage taskInfo) {
        try {
            logMessage(broadcastKey, taskInfo.getResourceName(), "STARTED", true, taskInfo);
            log.info("Send start notification: {} - {}", broadcastKey, taskInfo.getResourceName());
        } catch (Exception e) {
            log.error("Send start notification failed", e);
        }
    }

    @Override
    public void sendSuccessNotification(String broadcastKey, TaskInfoMessage taskInfoMessage) {
        try {
            logMessage(broadcastKey, taskInfoMessage.getResourceName(), SUCCESS, true,
                    taskInfoMessage);
        } catch (Exception e) {
            log.error("Send success notification failed", e);
        }
    }


    @Override
    public void sendFailedNotification(String broadcastKey, String errorMessage,
            TaskInfoMessage taskInfoMessage) {
        try {
            logMessage(broadcastKey, taskInfoMessage.getResourceName(), errorMessage, false,
                    taskInfoMessage);
        } catch (Exception e) {
            log.error("Send failed notification failed", e);
        }
    }

    /**
     * send failed notification
     *
     * @param broadcastTitle
     * @param resourceName
     * @param responseMessage
     * @param isSuccess
     * @param taskInfo
     */

    private void logMessage(String broadcastTitle, String resourceName, String responseMessage,
            boolean isSuccess,
            TaskInfoMessage taskInfo) {
        try {
            String title = broadcastTitle;
            String msg = String.format("%s %s ", title, resourceName);
            StringBuilder msgBuilder = new StringBuilder(msg);
            String detail = taskInfo.getDetail();
            SubNetTaskOperationDetail subNetTaskOperationDetail = JSON.parseObject(detail,
                    SubNetTaskOperationDetail.class);
            String requestBody = JSON.toJSONString(subNetTaskOperationDetail.getRequest());
            taskInfo.setSuccessfully(isSuccess);
            if (isSuccess) {
                msgBuilder.append(Constants.SUCCESSFULLY);
            }
            String detailBody = SubnetUtils.buildRequestDetail(requestBody,
                    responseMessage);
            taskInfo.setDetail(detailBody);
            TaskInfoMessager.sendMessage(taskInfo);
//            BroadcastMessager.publishKafkaMessage(
//                    BroadcastMessage.builder()
//                            .title(title)
//                            .message(msgBuilder.toString())
//                            .error(!isSuccess)
//                            .build());

            log.info("Send subnet notification: {}", msgBuilder);
        } catch (Exception e) {
            log.error("Send subnet notification failed exception:{}", e.getMessage(), e);
        }
    }
}
