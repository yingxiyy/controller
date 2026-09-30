/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.flex.dci.otn.controller.batchlock.provider.zookeeper.curator;

import static java.util.Objects.requireNonNull;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.PathUtils;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.data.Stat;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.batchlock.core.AbstractSimpleLock;
import net.flex.dci.otn.controller.batchlock.core.LockConf;
import net.flex.dci.otn.controller.batchlock.core.LockProvider;
import net.flex.dci.otn.controller.batchlock.core.SimpleLock;
import net.flex.dci.otn.controller.batchlock.support.LockException;
import net.flex.dci.otn.controller.batchlock.support.annotation.NonNull;

@Slf4j
public class ZookeeperCuratorLockProvider implements LockProvider {

    public static final String DEFAULT_PATH = "/STATE/batchLock";
    private final String path;
    private final CuratorFramework client;
    private String nameSpace;
    private String realPath;

    public static final String GID_NODE = "/STATE/global_id";

    public ZookeeperCuratorLockProvider(@NonNull CuratorFramework client, String nameSpace) {
        this(client, DEFAULT_PATH, nameSpace);
    }

    public ZookeeperCuratorLockProvider(@NonNull CuratorFramework client, @NonNull String path,
            String nameSpace) {
        this.client = requireNonNull(client);
        this.path = PathUtils.validatePath(path);
        this.nameSpace = nameSpace;
        if (!StringUtils.isEmpty(this.nameSpace)) {
            this.realPath = this.path + "/" + this.nameSpace;
        }
    }

    @Override
    @NonNull
    public Optional<SimpleLock> lock(@NonNull LockConf lockConfiguration) {
        try {
            ReentrantLock lock = new ReentrantLock(client, realPath, lockConfiguration);
            boolean ret = lock.acquire(60000, TimeUnit.MILLISECONDS);
            if (ret) {
                log.debug("lock {} of transaction {} acquired", lockConfiguration.getName(),
                        lockConfiguration.getTid());
                return Optional.of(new CuratorLock(lock, lockConfiguration));
            }
            log.debug("lock {} of transaction {} cant' be acquired", lockConfiguration.getName(),
                    lockConfiguration.getTid());
            return Optional.empty();
        } catch (Exception e) {
            log.debug("Exception occured locking {}", lockConfiguration.getName(), e);
            return Optional.empty();
        }
    }

    private static final class CuratorLock extends AbstractSimpleLock {

        private ReentrantLock lock;

        private CuratorLock(ReentrantLock lock, LockConf lockConfiguration) {
            super(lockConfiguration);
            this.lock = lock;
        }

        @Override
        public void doUnlock() throws LockException {
            try {
                lock.release();
            } catch (Exception e) {
                log.debug("Can not unlock {}", lockConfiguration.getName(), e);
                throw new LockException("Can not unlock " + lockConfiguration.getName(), e);
            }
        }
    }

    @Override
    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
        if (!StringUtils.isEmpty(this.nameSpace)) {
            realPath = path + "/" + this.nameSpace;
        }
    }

    @Override
    public int getId() throws LockException {
        if (client == null) {
            throw new LockException("no zookeeper connection");
        }
        try {
            if (null == client.checkExists().forPath(GID_NODE)) {
                client.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT)
                        .forPath(GID_NODE);
            }
            Stat stat = client.setData().withVersion(-1).forPath(GID_NODE);
            return stat.getVersion();
        } catch (Exception e) {
            throw new LockException("zookeeper operation error", e);
        }
    }
}
