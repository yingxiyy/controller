/// *
// *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
// *
// *  This program and the accompanying materials are made available under the
// *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
// *  and is available at http://www.eclipse.org/legal/epl-v10.html
// */
//
//package net.flex.dci.otc.controller.ne.manager.core;
//
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//
//@Component
//@Slf4j
//public class NeSyncDaemon {
//
//    @Autowired
//    private NeManagerService neManagerService;
//
//    @Scheduled(initialDelay = 2000, fixedRate = 60000)
//    public void scheduleNeSyncTask() {
//        try {
//            neManagerService.manageNe();
//        } catch (Exception e) {
//            log.error("Failed to schedule ne sync task.", e);
//        }
//
//    }
//}
