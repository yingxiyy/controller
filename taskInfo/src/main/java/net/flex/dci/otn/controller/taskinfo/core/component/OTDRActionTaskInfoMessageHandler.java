package net.flex.dci.otn.controller.taskinfo.core.component;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.taskinfo.enums.TaskActionType;
import net.flex.dci.otn.controller.taskinfo.message.TaskInfoMessager;
import net.flex.dci.otn.controller.taskinfo.utils.ConvertorUtils;
import net.flex.dci.otn.db.jpa.entity.TaskInfo;
import net.flex.dci.otn.db.jpa.service.dao.TaskInfoDaoService;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2025/8/1
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class OTDRActionTaskInfoMessageHandler extends AbstractActionTaskInfoMessageHandler {

    public OTDRActionTaskInfoMessageHandler(
            TaskInfoDaoService taskInfoDaoService,
            TaskInfoMessager taskInfoMessager) {
        super(taskInfoDaoService, taskInfoMessager);
    }

    @Override
    public void handlerTaskMessage(TaskInfoMessage taskInfoMessage) {
        long t0 = System.nanoTime();
        log.debug("handle the otdr task,task info is message:{}", taskInfoMessage);
        String objectId = taskInfoMessage.getObjectId();
        String resultId = taskInfoMessage.getScanResultId();
        String otdrTaskName = taskInfoMessage.getResourceName();
        log.debug("current otdr task info ref objectId:{} and resultId:{} taskName:{}", objectId,
                resultId, otdrTaskName);
        List<TaskInfo> otdrTasks = taskInfoDaoService.findOtdrTaskInfoByObjectIdAndResultIdNotFinished(
                objectId, resultId);
        long queryCostUs = (System.nanoTime() - t0) / 1_000;
        taskInfoMessage.setResourceId(objectId);
        boolean successfully = taskInfoMessage.getSuccessfully();
        if (successfully) {
            handleOtdrTaskFinished(taskInfoMessage, otdrTasks);
        } else {
            handleOtdrTaskCreate(taskInfoMessage, otdrTasks);
        }

        long totalCostUs = (System.nanoTime() - t0) / 1_000;
        if (totalCostUs > 2000) {
            log.warn("otdr handler cost {}μs (query={}μs), objectId={}", totalCostUs, queryCostUs, objectId);
        }
    }

    private void handleOtdrTaskCreate(TaskInfoMessage taskInfoMessage, List<TaskInfo> otdrTasks) {
        String objectId = taskInfoMessage.getObjectId();
        String resultId = taskInfoMessage.getScanResultId();
        String otdrTaskName = taskInfoMessage.getResourceName();
        log.debug("current otdr task info ref objectId:{} and resultId:{} taskName:{}", objectId,
                resultId, otdrTaskName);
        if (CollectionUtils.isEmpty(otdrTasks)) {
            //create a new OTDR task info
            createOtdrTaskInfo(taskInfoMessage);
        } else {
            createSubOtdrTask(taskInfoMessage, otdrTasks);
        }
    }

    private void createOtdrTaskInfo(TaskInfoMessage taskInfoMessage) {
        log.debug("create a new otdr task info:{}", taskInfoMessage);
        long otdrTaskInfo = createTaskInfo(taskInfoMessage);
        if (taskInfoMessage.getDetail() != null) {
            taskInfoDaoService.saveDetail(otdrTaskInfo, taskInfoMessage.getDetail());
        }
    }

    private void createSubOtdrTask(TaskInfoMessage taskInfoMessage, List<TaskInfo> otdrTasks) {
        log.debug("add to as sub otdr task work");
        TaskInfo rootTask = otdrTasks.stream().filter(taskInfo -> taskInfo.getRoot() == true)
                .findAny().orElse(null);
        if (rootTask == null) {
            log.error("there no root task for the otdr task,discard it");
        }
        //update rootTask
        Long groupId = rootTask.getActionTime();
        rootTask.setGroupId(groupId);
        taskInfoDaoService.save(rootTask);
        //record sub task
        TaskInfo subOtdrTask = ConvertorUtils.convertTaskInfoMsg2Entity(taskInfoMessage);
        subOtdrTask.setRoot(false);
        subOtdrTask.setGroupId(groupId);
        TaskInfo dbTaskInfo = taskInfoDaoService.save(subOtdrTask);
        taskInfoMessager.notifyTaskInfoCreate(
                ConvertorUtils.convertTaskInfoEntity2Dto(dbTaskInfo, taskInfoMessage));
    }

    private void handleOtdrTaskFinished(TaskInfoMessage taskInfoMessage, List<TaskInfo> otdrTasks) {
        String objectId = taskInfoMessage.getObjectId();
        String resultId = taskInfoMessage.getScanResultId();
        String otdrTaskName = taskInfoMessage.getResourceName();
        log.debug("current otdr task finished ref objectId:{} and resultId:{} taskName:{}",
                objectId,
                resultId, otdrTaskName);
        if (CollectionUtils.isEmpty(otdrTasks)) {
            log.warn("the current otdr task is none sense,discard it");
            return;
        }
        for (TaskInfo otdrTask : otdrTasks) {
            long otdrTaskDBId = attachDetail2TaskInfo(otdrTask, taskInfoMessage);
            if (taskInfoMessage.getDetail() != null) {
                taskInfoDaoService.saveDetail(otdrTaskDBId, taskInfoMessage.getDetail());
            }
        }
    }


    @Override
    public TaskActionType taskActionType() {
        return TaskActionType.OTDR_TASK;
    }
}
