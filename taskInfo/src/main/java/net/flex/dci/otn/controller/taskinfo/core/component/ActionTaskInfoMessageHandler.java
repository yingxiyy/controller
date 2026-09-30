package net.flex.dci.otn.controller.taskinfo.core.component;

import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.taskinfo.enums.TaskActionType;

/**
 * 2025/8/1
 *
 * @author musa
 * @version 1.0
 **/
public interface ActionTaskInfoMessageHandler {

    void handlerTaskMessage(TaskInfoMessage taskInfoMessage);

    TaskActionType taskActionType();
}
