/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.otc.dci.toop.base.batchlock.lib;

import net.flex.dci.otn.controller.batchlock.core.BatchLockTransaction;
import net.flex.dci.otn.controller.batchlock.lib.ZooKeeperToolset;
import net.flex.dci.otn.controller.batchlock.support.LockException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProcessLock {

    public static void main(String[] args) {
        try {
            ZooKeeperToolset.instance().setNameSpace("AAAAA");
            BatchLockTransaction t = ZooKeeperToolset.instance().newTransaction();
            log.info("transaction ok");
            t.lock("test4");
            t.require();
            log.info("transaction {} locked", t);
            try {
                Thread.sleep(60000);
            } catch (Exception e) {
                log.error("can't sleep");
            }
            t.dismiss();
            log.info("transaction {} unlocked", t);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("can't sleep");
            }

            log.info("lock again");
//            t = ZooKeeperToolset.instance().newTransaction();
            t.lock("test4");
            t.require();
            log.info("transaction {} locked again", t);
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("can't sleep");
            }
            t.dismiss();
            log.info("transaction {} unlocked again", t);

            log.info("end of process");
        } catch (LockException e) {
            log.error("exception caught", e);
        }
    }
}
