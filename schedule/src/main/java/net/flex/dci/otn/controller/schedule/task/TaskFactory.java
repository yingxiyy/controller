package net.flex.dci.otn.controller.schedule.task;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.schedule.enums.TaskType;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TaskFactory {


    private final Map<TaskType, TaskProcessor> processorMap = new HashMap<>();


    public TaskFactory(List<TaskProcessor> processors) {
        log.debug("start to init the task processor factory");
        processors.forEach(taskProcessor -> {
            processorMap.put(taskProcessor.getTaskType(), taskProcessor);
        });
    }


    public TaskProcessor getImplement(TaskType type) {
        return processorMap.getOrDefault(type, processorMap.get(TaskType.unknown));
    }
}
