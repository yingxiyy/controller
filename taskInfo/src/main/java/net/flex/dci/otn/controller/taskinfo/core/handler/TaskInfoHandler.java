package net.flex.dci.otn.controller.taskinfo.core.handler;

import net.flex.dci.otc.common.model.TaskInfoMessage;

/**
 * @version 1.0
 * @date 2022/5/2 9:39
 */
public interface TaskInfoHandler {

    void handlerTaskMessage(TaskInfoMessage taskInfoMessage);
}
