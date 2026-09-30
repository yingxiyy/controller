package net.flex.dci.otn.controller.taskinfo.core.component;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.taskinfo.enums.TaskActionType;
import net.flex.dci.otn.controller.taskinfo.message.TaskInfoMessager;
import net.flex.dci.otn.controller.taskinfo.utils.ConvertorUtils;
import net.flex.dci.otn.db.jpa.entity.TaskInfo;
import net.flex.dci.otn.db.jpa.service.dao.TaskInfoDaoService;
import org.springframework.stereotype.Component;

/**
 * 2025/8/1
 *
 * @author musa
 * @version 1.0
 **/

@Component
@Slf4j
public class DefaultActionTaskInfoMessageHandler extends AbstractActionTaskInfoMessageHandler {

    private final TaskInfoBatchBuffer batchBuffer;

    public DefaultActionTaskInfoMessageHandler(
            TaskInfoDaoService taskInfoDaoService,
            TaskInfoMessager taskInfoMessager, TaskInfoBatchBuffer batchBuffer) {
        super(taskInfoDaoService, taskInfoMessager);
        this.batchBuffer = batchBuffer;
    }

    @Override
    public void handlerTaskMessage(TaskInfoMessage taskInfoMessage) {
        log.debug("start to handle the default action type task info message,the message is :{}",
                taskInfoMessage);
//        TaskInfo entity = ConvertorUtils.convertTaskInfoMsg2Entity(taskInfoMessage);
//        batchBuffer.submit(entity, taskInfoMessage);
        TaskInfo entity;
        if (taskInfoMessage.getId() != null) {
            TaskInfo existing = taskInfoDaoService.findById(taskInfoMessage.getId());
            if (existing != null) {
                entity = ConvertorUtils.updateTaskInfoFromMessage(existing, taskInfoMessage);
            } else {
                entity = ConvertorUtils.convertTaskInfoMsg2Entity(taskInfoMessage);
            }
        } else {
            entity = ConvertorUtils.convertTaskInfoMsg2Entity(taskInfoMessage);
        }
        batchBuffer.submit(entity, taskInfoMessage);
    }


    @Override
    public TaskActionType taskActionType() {
        return TaskActionType.DEFAULT;
    }
}
