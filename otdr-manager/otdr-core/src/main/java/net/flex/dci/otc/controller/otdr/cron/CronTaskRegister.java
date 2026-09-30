/*
 * Copyright (c) 2019 Network flex Any comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.otdr.cron;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;


@Data
@Slf4j
@Component
public class CronTaskRegister implements DisposableBean {

    private final Map<String, CronTask> scheduledTasks = new ConcurrentHashMap<>();

    @Autowired
    private TaskScheduler taskScheduler;

    public void addCronTask(String serialNum, Runnable scheduleTask, String cronExpression) {
        log.debug("add cron task for issue serial number is {}", serialNum);
        addCronTask(
                new org.springframework.scheduling.config.CronTask(scheduleTask, cronExpression),
                serialNum);
    }

    private void addCronTask(org.springframework.scheduling.config.CronTask cronTask,
            String serialNum) {
        if (cronTask != null) {

            if (this.scheduledTasks.containsKey(serialNum)) {
                //remove it
                removeCronTask(serialNum);
            }
            this.scheduledTasks.put(serialNum, scheduleCronTask(cronTask));
        }
    }

    public CronTask scheduleCronTask(org.springframework.scheduling.config.CronTask cronTask) {
        CronTask maintainScheduledTask = new CronTask();
        maintainScheduledTask.future = this.taskScheduler
                .schedule(cronTask.getRunnable(), cronTask.getTrigger());
        return maintainScheduledTask;
    }

    public void removeCronTask(String serialNum) {
        log.debug("remove cron task for issue serial number is {}", serialNum);
        CronTask scheduledTask = this.scheduledTasks.remove(serialNum);
        if (scheduledTask != null) {
            scheduledTask.cancel();
        }
        log.debug("finish to remove task for issue serial number is {}", serialNum);
    }

    @Override
    public void destroy() {
//        for(MaintainScheduledTask task:this.scheduledTasks.values()) {
//            task.cancel();
//        }
        this.scheduledTasks.values().forEach(task -> {
            task.cancel();
        });
        this.scheduledTasks.clear();
    }
}

