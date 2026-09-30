package net.flex.dci.otn.controller.schedule.task;

import static net.flex.dci.otn.controller.schedule.utils.Constants.CONTROLLER_DB_BACKUP_CRON;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.ScheduleDao;
import net.flex.dci.otc.mongo.mdoel.schedule.MoSchedule;
import net.flex.dci.otn.controller.schedule.component.DynamicScheduler;
import net.flex.dci.otn.controller.schedule.enums.ExecuteResult;
import net.flex.dci.otn.controller.schedule.enums.TaskStatus;
import net.flex.dci.otn.controller.schedule.enums.TaskType;
import org.springframework.stereotype.Component;

/**
 * 2026/6/2
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class DefaultTaskInitializer {

    private final ScheduleDao scheduleDao;

    private final DynamicScheduler schedulerConfig;

    public void initialSystemTask() {
        log.info("initial system task");
        if (scheduleDao.getById("0") != null) {
            log.info("Default backup task already exists");
            return;
        }
        log.info("Successfully created default controller DB backup task");

        MoSchedule moSchedule = MoSchedule.builder()
                .id("0")
                .cronExpression(CONTROLLER_DB_BACKUP_CRON)
                .name("Controller DB Backup")
                .operator("system")
                .domainName("default")
                .description("Daily Controller DB Backup")
                .taskType(TaskType.controllerDataBackup.name())
                .status(TaskStatus.Activated.name())
                .operationName("CtrlDbBackup")
                .result(ExecuteResult.Unknown.name())
                .build();
        scheduleDao.save(moSchedule);
        schedulerConfig.refreshAllTasks();
        log.info("Scheduler refreshed, default task is now active");
    }

}
