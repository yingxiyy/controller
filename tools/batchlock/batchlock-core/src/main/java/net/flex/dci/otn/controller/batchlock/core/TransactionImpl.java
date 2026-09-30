/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.batchlock.core;

import static net.flex.dci.otn.controller.batchlock.core.RequireAssert.alreadyRequiredBy;

import net.flex.dci.otn.controller.batchlock.support.LockException;
import net.flex.dci.otn.controller.batchlock.support.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransactionImpl implements BatchLockTransaction {

    private String id;
    private Set<String> resources;
    private List<SimpleLock> locks;
    private LockProvider lockProvider;

//    private static final long DEFAULT_MILLISECOND_TIMEOUT = 100;
    private static final long CYCLE = 1000;
    private static final String MINUS = "-";

    TransactionImpl(LockProvider lockProvider) {
        this.id = genTransId();
        resources = new HashSet<String>();
        locks = new ArrayList<SimpleLock>();
        this.lockProvider = lockProvider;
        log.debug("transaction {} created", this.id);
    }

    public synchronized void lock(String resourceId) {
        resources.add(resourceId);
        log.debug("resource {} added to transaction", resourceId, this.id);
    }

    public synchronized void lock(Set<String> resourceIds) {
        resources.addAll(resourceIds);
        for (String resourceId : resourceIds) {
            log.debug("resource {} added to transaction", resourceId, this.id);
        }
    }

    public synchronized void require() throws LockException {
        try {
            require(-1);
        } catch (Exception e) {
            log.debug("fail to lock", e);
            throw new LockException("transaction " + this.id + " fails to lock");
        }
    }

    public synchronized void require(long millisecondTimeout) throws LockException, TimeoutException {
        if (alreadyRequiredBy(this.id)) {
            log.debug("transaction {} already locked", this.id);
        } else {
            doRequire(millisecondTimeout);
            RequireAssert.startRequire(this.id);
            log.debug("transaction {} locked", this.id);
        }
    }

    public synchronized void doRequire(long millisecondTimeout) throws LockException, TimeoutException {
        if (resources.isEmpty()) {
            return;
        }
        millisecondTimeout += CYCLE;
        List<String> resList = new ArrayList<>(this.resources);
        Collections.sort(resList);

        long totalWaitTime = 0L;
        for (String resource : resList) {
            do {
                Optional<SimpleLock> lock = lockProvider.lock(new LockConf(resource, this.id));
                if (lock.isPresent()) {
                    this.locks.add(lock.get());
                    break;
                } else {
                    log.error("resource {} can't be locked transiently, waiting", resource);
                    try {
                        Thread.sleep(CYCLE);
                    } catch (Exception e) {
                        //do nothing
                    }
                    totalWaitTime += CYCLE;
                    log.debug("try to lock resource {} again", resource);
                }
            } while (millisecondTimeout <= 0 || (millisecondTimeout > 0 && totalWaitTime < millisecondTimeout));
        }
        if (this.locks.size() != resList.size()) {
            dismiss();
            log.debug("timeout, can't lock all resources");
            throw new TimeoutException("can't lock all resources");
        }

    }

    public synchronized void dismiss() throws LockException {
        RequireAssert.endRequire();
        try {
            for (int i = this.locks.size() - 1; i >= 0; i--) {
                this.locks.get(i).unlock();
            }
        } catch (LockException e) {
            throw new LockException("can't dismiss trasaction", e);
        }
        this.locks.clear();
        this.resources.clear();
    }

    private String genTransId() {
        String id = Utils.getHostip() + MINUS + Utils.getCurrentProcessId() + MINUS
                + Utils.getCurrentThreadId() + MINUS + Long.toHexString(System.currentTimeMillis());
        return id;
    }

    public String toString() {
        return this.id;
    }

    @Override
    public synchronized boolean require_once() {
        if (alreadyRequiredBy(this.id)) {
            log.debug("transaction {} already locked", this.id);
            return true;
        } else {
            if (doRequire_once()) {
                RequireAssert.startRequire(this.id);
                log.debug("transaction {} locked", this.id);
                return true;
            }
            return false;
        }
    }

    public synchronized boolean doRequire_once() {
        if (resources.isEmpty()) {
            return true;
        }

        List<String> resList = new ArrayList<>(this.resources);
        Collections.sort(resList);
        for (String resource : resList) {
            Optional<SimpleLock> lock = lockProvider.lock(new LockConf(resource, this.id));
            if (lock.isPresent()) {
                this.locks.add(lock.get());
            } else {
                log.error("resource {} can't be locked", resource);
                break;
            }
        }

        if (this.locks.size() != resList.size()) {
            dismiss();
            log.debug("can't lock all resources");
            return false;
        }
        return true;
    }

}
