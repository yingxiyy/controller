package net.flex.dci.otn.controller.taskinfo.enums;

import lombok.Getter;

/**
 * 2025/8/2
 *
 * @author musa
 * @version 1.0
 **/
@Getter
public enum TaskActionType {

    OTDR_TASK("otdr"),
    DEFAULT("default");


    String taskActionName;

    TaskActionType(String taskActionName) {
        this.taskActionName = taskActionName;
    }

    public static TaskActionType fromActionName(String taskActionName) {
        for (TaskActionType actionType : TaskActionType.values()) {
            if (actionType.getTaskActionName().equals(taskActionName)) {
                return actionType;
            }
        }
        return null;
    }
}
