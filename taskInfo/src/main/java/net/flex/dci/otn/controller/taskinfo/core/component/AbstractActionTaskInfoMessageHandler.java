package net.flex.dci.otn.controller.taskinfo.core.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.taskinfo.message.TaskInfoMessager;
import net.flex.dci.otn.controller.taskinfo.utils.ConvertorUtils;
import net.flex.dci.otn.db.jpa.entity.TaskInfo;
import net.flex.dci.otn.db.jpa.service.dao.TaskInfoDaoService;

/**
 * 2025/8/2
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractActionTaskInfoMessageHandler implements ActionTaskInfoMessageHandler {

    protected final TaskInfoDaoService taskInfoDaoService;

    protected final TaskInfoMessager taskInfoMessager;


    /**
     * create a new task
     *
     * @param taskInfoMessage
     */
    protected Long createTaskInfo(TaskInfoMessage taskInfoMessage) {
        log.info("create a new task {}, resource id is :{}", taskInfoMessage.getActionType(),
                taskInfoMessage.getResourceId());
        try {
            TaskInfo taskInfo = ConvertorUtils.convertTaskInfoMsg2Entity(taskInfoMessage);
            TaskInfo dbTaskInfo = taskInfoDaoService.save(taskInfo);
            taskInfoMessager.notifyTaskInfoCreate(
                    ConvertorUtils.convertTaskInfoEntity2Dto(dbTaskInfo, taskInfoMessage));
            return dbTaskInfo.getId();
        } catch (Exception ex) {
            log.error("create a new task info failed");
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to create a new task", ex);
        }

    }

    /**
     * attach task info
     *
     * @param taskInfo
     * @param taskInfoMessage
     */
    protected Long attachDetail2TaskInfo(TaskInfo taskInfo,
            TaskInfoMessage taskInfoMessage) {
        log.info("attach to taskInfo :{} ,resource id is :{}", taskInfo.getId(),
                taskInfoMessage.getResourceId());
        try {
            TaskInfo newTaskInfo = ConvertorUtils.updateTaskInfoFromMessage(taskInfo,
                    taskInfoMessage);
            log.info("update taskInfo is:{}", newTaskInfo);
            newTaskInfo.setId(taskInfo.getId());
            TaskInfo dbTaskInfo = taskInfoDaoService.save(newTaskInfo);
            taskInfoMessager.notifyTaskInfoUpdate(
                    ConvertorUtils.convertTaskInfoEntity2Dto(dbTaskInfo, taskInfoMessage));
            return dbTaskInfo.getId();
        } catch (Exception ex) {
            log.error("create a new task info failed，reason is:{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to create a new task", ex);
        }

    }
}
