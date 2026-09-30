/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.app.monitor.monitor;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.app.monitor.monitor.task.CleanDeadNodeTask;
import net.flex.dci.otn.controller.app.monitor.monitor.task.WatchServerTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(value = 2)
@Slf4j
public class Monitor implements ApplicationRunner {


    @Autowired
    WatchServerTask watchServerTask;
    @Autowired
    CleanDeadNodeTask cleanDeadNodeTask;


    /**
     * everything is ready ,so start monitor
     *
     * @param args
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info("------------APP-Monitor start--------------");
        ScheduledExecutorService timer = Executors.newScheduledThreadPool(2);
        timer.scheduleAtFixedRate(watchServerTask, 60, 30, TimeUnit.SECONDS);
        timer.scheduleAtFixedRate(cleanDeadNodeTask, 10, 60, TimeUnit.MINUTES);
    }


}