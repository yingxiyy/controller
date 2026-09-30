package net.flex.dci.otn.controller.schedule.component;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.ScheduleDao;
import net.flex.dci.otc.mongo.mdoel.schedule.MoSchedule;
import net.flex.dci.otn.controller.schedule.enums.TaskStatus;
import net.flex.dci.otn.controller.schedule.enums.TaskType;
import net.flex.dci.otn.controller.schedule.monitor.LeaderElector;
import net.flex.dci.otn.controller.schedule.task.TaskFactory;
import net.flex.dci.otn.controller.schedule.task.TaskProcessor;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

/**
 * 2026/6/2
 *
 * @author musa
 * @version 1.0
 **/
@EnableScheduling
@Component
@RequiredArgsConstructor
@Slf4j
public class DynamicScheduler implements SchedulingConfigurer {

    private final ScheduleDao scheduleDao;

    private final TaskFactory taskFactory;

    private final Set<ScheduledFuture<?>> scheduledTasks = new CopyOnWriteArraySet<>();
    private final TaskScheduler taskScheduler;

    private final LeaderElector leaderElector;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(taskScheduler);
        refreshAllTasks();
    }

    public void refreshAllTasks() {
        log.info("refresh all the current tasks");
        int canceledTaskCount = 0;
        for (ScheduledFuture<?> futures : scheduledTasks) {
            futures.cancel(false);
            canceledTaskCount++;
        }
        scheduledTasks.clear();

        log.info("clear scheduled task count:{}", canceledTaskCount);
        if (!leaderElector.isLeaderShip()) {
            log.info("not leader, no scheduled task registered");
            return;
        }
        List<MoSchedule> activeTasks = scheduleDao.findByStatus(TaskStatus.Activated.name());
        log.info("loadFrom the database {} activated jobs", activeTasks.size());

        for (MoSchedule task : activeTasks) {
            registerTask(task);
        }

        log.info("load from the database registered job  {}", scheduledTasks.size());
    }

    private void registerTask(MoSchedule moSchedule) {
        try {
            TaskProcessor processor = taskFactory.getImplement(
                    TaskType.valueOf(moSchedule.getTaskType()));
            Runnable taskRunnable = () -> executeTask(moSchedule, processor);
            ScheduledFuture<?> future;
            if (moSchedule.getNextExecuteTime() != null) {
                Instant executeTime = Instant.ofEpochSecond(
                        moSchedule.getNextExecuteTime().longValue());
                if (executeTime.isBefore(Instant.now())) {
                    moSchedule.setStatus(TaskStatus.Expired.name());
                    log.warn("One-time task {} execution time has passed, marked as expired",
                            moSchedule.getName());
                    scheduleDao.save(moSchedule);
                    return;
                }
                future = taskScheduler.schedule(taskRunnable, executeTime);
                log.info("Successfully registered one-time task: {}, execution time: {}",
                        moSchedule.getName(), executeTime);
            } else if (moSchedule.getCronExpression() != null) {
                future = taskScheduler.schedule(taskRunnable,
                        new CronTrigger(moSchedule.getCronExpression()));
                log.info("Successfully registered recurring task: {}, cron expression: {}",
                        moSchedule.getName(), moSchedule.getCronExpression());
            } else {
                log.error("Task {} has no scheduling rule specified, skipping registration",
                        moSchedule.getName());
                return;
            }
            scheduledTasks.add(future);
        } catch (Exception e) {
            log.error(" Failed to register task: {}", moSchedule.getName(), e);
        }
    }

    private void executeTask(MoSchedule moSchedule, TaskProcessor processor) {
        log.info("start to execute the job:{}", moSchedule.getName());
        long startTime = System.currentTimeMillis();
        try {
            processor.process(moSchedule);
            moSchedule.setResult("Success");
            moSchedule.setResultInfo(
                    "execute success，cost: " + (System.currentTimeMillis() - startTime) + "ms");
            log.info("tiger job execute finished : {}，cost: {}ms", moSchedule.getName(),
                    System.currentTimeMillis() - startTime);
        } catch (Exception e) {
            log.error("job execute failed: {}", moSchedule.getName(), e);
            moSchedule.setResult("Failure");
            moSchedule.setResultInfo("job execute failed: " + e.getMessage());
        } finally {
            if (moSchedule.getNextExecuteTime() != null) {
                moSchedule.setStatus("Expired");
                log.info("One-time task {} execution time has executed, marked as expired",
                        moSchedule.getName());
            }
            scheduleDao.update(moSchedule);

        }
    }


}
