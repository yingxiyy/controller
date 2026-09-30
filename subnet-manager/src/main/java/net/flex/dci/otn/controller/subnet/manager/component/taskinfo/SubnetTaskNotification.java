package net.flex.dci.otn.controller.subnet.manager.component.taskinfo;

import net.flex.dci.otc.common.model.TaskInfoMessage;

/**
 * 2026/2/28
 *
 * @author musa
 * @version 1.0
 **/
public interface SubnetTaskNotification {

    void sendStartNotification(String broadcastKey, TaskInfoMessage taskInfoMessage);

    void sendSuccessNotification(String broadcastKey, TaskInfoMessage taskInfoMessage);

    void sendFailedNotification(String broadcastKey, String errorMessage,
            TaskInfoMessage taskInfoMessage);
}
