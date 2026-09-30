package net.flex.dci.otn.controller.taskinfo.core.handler.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otn.controller.taskinfo.core.component.ActionTaskInfoMessageHandler;
import net.flex.dci.otn.controller.taskinfo.core.handler.TaskInfoHandler;
import net.flex.dci.otn.controller.taskinfo.enums.TaskActionType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/5/2 9:39
 */
@Component
@Slf4j
public class TaskInfoHandlerImpl implements TaskInfoHandler {


    private final Map<TaskActionType, ActionTaskInfoMessageHandler> taskActionTypeHandlerMap;

    public TaskInfoHandlerImpl(
            List<ActionTaskInfoMessageHandler> actionTaskInfoMessageHandler) {
        this.taskActionTypeHandlerMap = actionTaskInfoMessageHandler.stream()
                .collect(Collectors.toMap(
                        ActionTaskInfoMessageHandler::taskActionType,
                        Function.identity(),
                        (h1, h2) -> {
                            throw new IllegalStateException(
                                    "Duplicate handler for action type " + h1.taskActionType());
                        }
                ));
    }

    @Override
    public void handlerTaskMessage(TaskInfoMessage taskInfoMessage) {
        log.debug("start to handle the task info message,the message is :{}", taskInfoMessage);
        ActionType actionType = taskInfoMessage.getActionType();
        String typeStr = null;
        if (actionType != null) {
            typeStr = actionType.name();
        }
        this.taskActionTypeHandlerMap.getOrDefault(TaskActionType.fromActionName(typeStr),
                        taskActionTypeHandlerMap.get(TaskActionType.DEFAULT))
                .handlerTaskMessage(taskInfoMessage);

    }


}
