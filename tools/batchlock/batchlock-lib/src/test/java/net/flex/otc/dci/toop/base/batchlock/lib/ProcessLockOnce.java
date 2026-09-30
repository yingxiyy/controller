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
public class ProcessLockOnce {

    public static void main(String[] args) {
        try {
            ZooKeeperToolset.instance().setNameSpace("AAAAA");
            BatchLockTransaction t = ZooKeeperToolset.instance().newTransaction();
            log.info("transaction ok");
            t.lock("Site-1608689248991#Ne-1608689970857#LINECARD-1-1#PORT-1-1-L2");
            if (t.require_once()) {
                log.info("transaction {} locked", t);
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    log.error("can't sleep");
                }
                t.dismiss();
                log.info("transaction {} unlocked", t);
            } else {
                log.info("transaction {} can't be locked, exit", t);
            }

            log.info("lock again");
            t.lock("Site-1608689248991#Ne-1608689970857#LINECARD-1-1#PORT-1-1-L2");
            if (t.require_once()) {
                log.info("transaction {} locked", t);
                try {
                    Thread.sleep(1000);
                } catch (Exception e) {
                    log.error("can't sleep");
                }
                t.dismiss();
                log.info("transaction {} unlocked again", t);
            } else {
                log.info("transaction {} can't be locked, exit", t);
            }

        } catch (LockException e) {
            log.error("exception caught", e);
        }
    }
}
