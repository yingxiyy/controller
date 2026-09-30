package net.flex.dci.otn.controller.schedule.task.detail;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.schedule.enums.TaskType;
import net.flex.dci.otn.controller.schedule.task.AbstractTaskImpl;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultTaskImpl extends AbstractTaskImpl {

    @Override
    public TaskType getTaskType() {
        return null;
    }

//    @Override
//    public TaskType getTaskType() {
//        return MoSchedule.TaskType.unknown;
//    }
}
