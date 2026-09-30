package net.flex.dci.otn.controller.implement.common.lifecycle;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.model.TaskInfoMessage.ResourceType;
import net.flex.dci.otn.controller.implement.common.recorder.StepRecord;
import net.flex.dci.otn.controller.implement.common.recorder.TaskRecord;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
public class LifeCycleSevice {
    private static final AtomicLong TASK_GROUP_ID_GENERATOR = new AtomicLong(System.currentTimeMillis());

    //  private TaskInfoKafkaService kafkaService;
    private TaskInfoMessage taskInfoMessage;
    private TaskRecord taskRecord;
    private Gson gson;

    public LifeCycleSevice() {
        taskInfoMessage = null;
        taskRecord = null;

        gson = new Gson();
//    kafkaService = SpringBeanFinder.getBean(TaskInfoKafkaService.class);
    }

    public void setGroupId(long groupId) {
        taskInfoMessage.setGroupId(groupId);
        taskInfoMessage.setRoot(false);
    }

    public TaskInfoMessage getTaskInfoMessage() {
        return taskInfoMessage;
    }

    public Long getGroupId() {
        return taskInfoMessage.getGroupId();
    }

    public void buildLifeService(String linkId, ResourceType linkType, String friendlyName,
                                 ActionType actionType, String who) {
        log.info("create {} lifecycle record {} {} {}", linkType.name(), linkId, friendlyName,
                actionType);

        taskInfoMessage = new TaskInfoMessage(
                who,
                linkType,
                actionType,
                null);

        taskInfoMessage.setResourceId(linkId);
        taskInfoMessage.setResourceName(friendlyName);
        taskInfoMessage.setSuccessfully(false);
        taskInfoMessage.setGroupId(TASK_GROUP_ID_GENERATOR.incrementAndGet());
        taskInfoMessage.setRoot(true);

        taskRecord = new TaskRecord(linkId, friendlyName);
    }

    public void logStartLinkImpl(String linkId, ResourceType linkType, String friendlyName,
            ActionType actionType, String who, Long groupId) {

        buildLifeService(linkId, linkType, friendlyName, actionType, who);
        if (groupId != null) {
            taskInfoMessage.setGroupId(groupId);
            taskInfoMessage.setRoot(false);
        }
        sendMsg();
    }

    public void updateTaskInfo(String linkId, ResourceType linkType, String friendlyName) {
        log.info("update taskInfo {} {} {}", linkType.name(), linkId, friendlyName);

        taskInfoMessage.setResourceId(linkId);
        taskInfoMessage.setResourceName(friendlyName);
        taskInfoMessage.setSuccessfully(false);

        taskRecord = new TaskRecord(linkId, friendlyName);

    }

    public void logStatusChanged(StepRecord stepRecord) {
        if (taskInfoMessage == null || taskRecord == null) {
            log.error("the taskInfoMessage is null, hasn't been initialized");
            return;
        }
        if (stepRecord == null) {
            try {
                throw new RuntimeException("the stepRecord == null");
            } catch (Exception e) {
                log.error("err:", e);
                return;
            }
        }
        taskRecord.setStepRecord(stepRecord);
        taskInfoMessage.setDetail(gson.toJson(taskRecord));
//    log.debug("update taskInfo detail {}", taskInfoMessage.getDetail());
        sendMsg();
    }

    public void logEndLinkImpl(String errorMsg) {
        log.info("finish {} lifecycle record {} {} {} with status {}",
                taskInfoMessage.getResourceType(),
                taskInfoMessage.getResourceId(),
                taskInfoMessage.getResourceName(),
                taskInfoMessage.getActionType(),
                errorMsg == null ? "OK" : errorMsg);

        taskRecord.setResult("done");
        taskInfoMessage.setDetail(gson.toJson(taskRecord));
        taskInfoMessage.setEndTime(System.currentTimeMillis());

        if (errorMsg != null) {
            taskInfoMessage.setSuccessfully(false);
            taskInfoMessage.setErrorReason(errorMsg);
        } else {
            taskInfoMessage.setSuccessfully(true);
        }
//    log.debug("end task info: {}", gson.toJson(taskInfoMessage));
        sendMsg();

        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title(taskInfoMessage.getActionType() + " "
                                + taskInfoMessage.getResourceType())
                        .message(String.format("%s %s %s result %s",
                                taskInfoMessage.getActionType(),
                                taskInfoMessage.getResourceType(),
                                taskInfoMessage.getResourceName(),
                                errorMsg == null ? "success"
                                        : String.format("fail. (%s)", errorMsg)))
                        .error(errorMsg != null)
                        .build());
    }

    /**
     * publish message to life cycle module to save date in mysql
     *
     * @param
     */
    public void sendMsg() {
//        log.info("send msg");
        TaskInfoMessager.sendMessage(taskInfoMessage);
    }

    public String getWhoDoesThis() {
        return taskInfoMessage.getWho();
    }
}