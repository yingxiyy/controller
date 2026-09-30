/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.batchlock.provider.zookeeper.curator;

import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.curator.framework.recipes.locks.LockInternalsDriver;
import org.apache.curator.framework.recipes.locks.Revocable;
import org.apache.curator.framework.recipes.locks.RevocationListener;
import org.apache.curator.framework.recipes.locks.StandardLockInternalsDriver;
import org.apache.curator.shaded.com.google.common.collect.Maps;
import org.apache.curator.utils.PathUtils;
import org.apache.curator.utils.ZKPaths;

import com.google.common.util.concurrent.MoreExecutors;
import net.flex.dci.otn.controller.batchlock.core.LockConf;

public class ReentrantLock implements InterProcessLock, Revocable<ReentrantLock> {

    private final LockInternals internals;
    private final String basePath;
    private LockConf lockConf;

    private final ConcurrentMap<String, LockData> transactionData = Maps.newConcurrentMap();

    private static class LockData {

        final String owningTransaction;
        final String lockPath;
//        final AtomicInteger lockCount = new AtomicInteger(1);

        private LockData(String owningTid, String lockPath) {
            this.owningTransaction = owningTid;
            this.lockPath = lockPath;
        }
    }

    private static final String LOCK_NAME = "lock";

    /**
     * @param client client
     * @param path   the opName to lock
     */
    public ReentrantLock(CuratorFramework client, String path, LockConf lockConf) {
        this(client, ZKPaths.makePath(path, lockConf.getName()), new StandardLockInternalsDriver());
        this.lockConf = lockConf;
    }

    /**
     * @param client client
     * @param path   the opName to lock
     * @param driver lock driver
     */
    ReentrantLock(CuratorFramework client, String path, LockInternalsDriver driver) {
        this(client, path, LOCK_NAME, 1, driver);
    }

    ReentrantLock(CuratorFramework client, String path, String lockName, int maxLeases,
            LockInternalsDriver driver) {
        basePath = PathUtils.validatePath(path);
        internals = new LockInternals(client, driver, path, lockName, maxLeases);
    }

    /**
     * Acquire the mutex - blocking until it's available. Note: the same thread can call acquire
     * re-entrantly. Each call to acquire must be balanced by a call to {@link #release()}
     *
     * @throws Exception ZK errors, connection interruptions
     */
    @Override
    public void acquire() throws Exception {
        if (!internalLock(-1, null)) {
            throw new IOException("Lost connection while trying to acquire lock: " + basePath);
        }
    }

    /**
     * Acquire the mutex - blocks until it's available or the given time expires. Note: the same
     * thread can call acquire re-entrantly. Each call to acquire that returns true must be balanced
     * by a call to {@link #release()}
     *
     * @param time time to wait
     * @param unit time unit
     * @return true if the mutex was acquired, false if not
     * @throws Exception ZK errors, connection interruptions
     */
    @Override
    public boolean acquire(long time, TimeUnit unit) throws Exception {
        return internalLock(time, unit);
    }

    /**
     * Returns true if the mutex is acquired by a thread in this JVM
     *
     * @return true/false
     */
    @Override
    public boolean isAcquiredInThisProcess() {
        return (transactionData.size() > 0);
    }

    /**
     * Perform one release of the mutex if the calling thread is the same thread that acquired it.
     * If the thread had made multiple calls to acquire, the mutex will still be held when this
     * method returns.
     *
     * @throws Exception ZK errors, interruptions, current thread does not own the lock
     */
    @Override
    public void release() throws Exception {
        /*
         * Note on concurrency: a given lockData instance can be only acted on by a single thread so
         * locking isn't necessary
         */

        String tid = lockConf.getTid();
        LockData lockData = transactionData.get(tid);
        if (lockData == null) {
            throw new IllegalMonitorStateException("You do not own the lock: " + basePath);
        }

//        int newLockCount = lockData.lockCount.decrementAndGet();
//        if (newLockCount > 0) {
//            return;
//        }
//        if (newLockCount < 0) {
//            throw new IllegalMonitorStateException(
//                    "Lock count has gone negative for lock: " + basePath);
//        }
        try {
            internals.releaseLock(lockData.lockPath);
        } finally {
            transactionData.remove(tid);
        }
    }

    /**
     * Return a sorted list of all current nodes participating in the lock
     *
     * @return list of nodes
     * @throws Exception ZK errors, interruptions, etc.
     */
    public Collection<String> getParticipantNodes() throws Exception {
        return LockInternals.getParticipantNodes(internals.getClient(), basePath,
                internals.getLockName(), internals.getDriver());
    }

    @Override
    public void makeRevocable(RevocationListener<ReentrantLock> listener) {
        makeRevocable(listener, MoreExecutors.directExecutor());
    }

    @Override
    public void makeRevocable(final RevocationListener<ReentrantLock> listener,
            Executor executor) {
        internals.makeRevocable(new RevocationSpec(executor, new Runnable() {

            @Override
            public void run() {
                listener.revocationRequested(ReentrantLock.this);
            }
        }));
    }

    /**
     * Returns true if the mutex is acquired by the calling thread
     * 
     * @return true/false
     */
    public boolean isOwnedByTransaction(String tid) {
        LockData lockData = transactionData.get(tid);
//        return (lockData != null) && (lockData.lockCount.get() > 0);
        return (lockData != null);
    }

    protected byte[] getLockNodeBytes() {
        return null;
    }

    private boolean internalLock(long time, TimeUnit unit) throws Exception {
        /*
         * Note on concurrency: a given lockData instance can be only acted on by a single thread so
         * locking isn't necessary
         */

        String tid = lockConf.getTid();

        LockData lockData = transactionData.get(tid);
        if (lockData != null) {
            // re-entering
//            lockData.lockCount.incrementAndGet();
            return true;
        }

        String lockPath = internals.attemptLock(time, unit, getLockNodeBytes());
        if (lockPath != null) {
            LockData newLockData = new LockData(tid, lockPath);
            transactionData.put(tid, newLockData);
            return true;
        }

        return false;
    }
}
