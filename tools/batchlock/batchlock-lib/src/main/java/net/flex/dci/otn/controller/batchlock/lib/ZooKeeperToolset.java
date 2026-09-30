/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.batchlock.lib;

import org.apache.curator.framework.CuratorFramework;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.batchlock.core.ITransactionBuilder;
import net.flex.dci.otn.controller.batchlock.core.LockProvider;
import net.flex.dci.otn.controller.batchlock.core.TransactionBuilder;
import net.flex.dci.otn.controller.batchlock.provider.zookeeper.curator.ZookeeperCuratorLockProvider;

@Slf4j
public class ZooKeeperToolset extends TransactionBuilder
        implements ITransactionBuilder {

    private static ZooKeeperToolset _instance_ = null;

    private LockProvider lockProvider;
    private CuratorFramework client;
    private static final String idNode = "/id";

    protected ZooKeeperToolset() {
        super();
        init();
    }

    public static ZooKeeperToolset instance() {
        if (_instance_ == null) {
            _instance_ = new ZooKeeperToolset();
        }
        return _instance_;
    }

    public void setNameSpace(String nameSpace) {
        if (lockProvider != null) {
            lockProvider.setNameSpace(nameSpace);
        }
    }

    private void init() {
        lockProvider = null;

        String nameSpace = ZooKeeperConfig.instance().getNameSpace();
        client = ZkClient.getInstance().getClient();
        if (client != null) {
            lockProvider = new ZookeeperCuratorLockProvider(client, nameSpace);
        }

        initLockProvider(lockProvider);
        log.info("ZooKeeperTransactionBuilder init ok.");
    }

    public int genID() {
        return lockProvider.getId();
    }
    
    public CuratorFramework getClient() {
    	return client;
    }
}
