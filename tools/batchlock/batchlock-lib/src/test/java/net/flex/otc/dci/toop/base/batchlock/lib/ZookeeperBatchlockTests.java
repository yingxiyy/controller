/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */package net.flex.otc.dci.toop.base.batchlock.lib;

import static org.assertj.core.api.Assertions.assertThat;

import net.flex.dci.otn.controller.batchlock.core.BatchLockTransaction;
import net.flex.dci.otn.controller.batchlock.lib.ZooKeeperToolset;
import net.flex.dci.otn.controller.batchlock.support.LockException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class ZookeeperBatchlockTests {

//    @Test
//    public void shouldHaveZKConfig() {
//        try {
//            Transaction t = ZooKeeperTransactionBuilder.instance().newTransaction();
//            assertThat(t).isNotNull();
//        } catch (LockException e) {
//            log.error("exception caught", e);
//            assertThat(e.getMessage()).contains("No lockProvider found");
//        }
//    }

    @Test
    public void shouldLock() {
        try {
            BatchLockTransaction t = ZooKeeperToolset.instance().newTransaction();
            log.info("transaction ok");
            t.lock("Site-1608689248991#Ne-1608689970857#LINECARD-1-1#PORT-1-1-L1");
            t.require();
            assertThat(t).isNotNull();
            try {
                Thread.sleep(300000);
            } catch (Exception e) {
                log.error("can't sleep");
            }
            t.dismiss();
        } catch (LockException e) {
            log.error("exception caught", e);
        }
    }

}
