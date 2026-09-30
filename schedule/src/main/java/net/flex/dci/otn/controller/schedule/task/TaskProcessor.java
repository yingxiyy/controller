package net.flex.dci.otn.controller.schedule.task;

import net.flex.dci.otc.mongo.mdoel.schedule.MoSchedule;
import net.flex.dci.otn.controller.schedule.enums.TaskType;

public interface TaskProcessor {

    void process(MoSchedule moSchedule);

    TaskType getTaskType();
}
