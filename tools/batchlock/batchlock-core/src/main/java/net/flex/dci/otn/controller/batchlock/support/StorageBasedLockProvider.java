/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.flex.dci.otn.controller.batchlock.support;

import net.flex.dci.otn.controller.batchlock.core.AbstractSimpleLock;
import net.flex.dci.otn.controller.batchlock.core.LockConf;
import net.flex.dci.otn.controller.batchlock.core.LockProvider;
import net.flex.dci.otn.controller.batchlock.core.SimpleLock;
import net.flex.dci.otn.controller.batchlock.support.annotation.NonNull;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;

/**
 * Distributed lock using abstract storage
 * <p>
 * It uses a table/collection that contains ID = lock name and a field tid.
 * <ol>
 * <li>Attempts to insert a new lock record. As an optimization, we keep in-memory track of created
 * lock records. If the record has been inserted, returns lock.</li>
 * <li>We will try to update lock record using filter ID == name AND tid == :tid</li>
 * <li>If the update succeeded (1 updated row/document), we have the lock. If the update failed (0
 * updated documents) somebody else holds the lock</li>
 * <li>When unlocking, tid is set to null.</li>
 * </ol>
 */
public class StorageBasedLockProvider implements LockProvider {

    @NonNull
    private final StorageAccessor storageAccessor;
    private final LockRecordRegistry lockRecordRegistry = new LockRecordRegistry();
    private String nameSpace;

    protected StorageBasedLockProvider(@NonNull StorageAccessor storageAccessor) {
        this.storageAccessor = storageAccessor;
    }

    /**
     * Clears cache of existing lock records.
     */
    public void clearCache() {
        lockRecordRegistry.clear();
    }

    @Override
    @NonNull
    public Optional<SimpleLock> lock(@NonNull LockConf lockConfiguration) {
        boolean lockObtained = doLock(lockConfiguration);
        if (lockObtained) {
            return Optional.of(new StorageLock(lockConfiguration, storageAccessor));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Sets lockUntil according to LockConfiguration if current lockUntil &lt;= now
     */
    protected boolean doLock(LockConf lockConfiguration) {
        String name = lockName(lockConfiguration);

        if (!lockRecordRegistry.lockRecordRecentlyCreated(name)) {
            // create record in case it does not exist yet
            if (storageAccessor.insertRecord(lockConfiguration)) {
                lockRecordRegistry.addLockRecord(name);
                // we were able to create the record, we have the lock
                return true;
            }
            // we were not able to create the record, it already exists, let's put it to the cache
            // so we do not try again
            lockRecordRegistry.addLockRecord(name);
        }

        // let's try to update the record, if successful, we have the lock
        return storageAccessor.updateRecord(lockConfiguration);
    }

    private String lockName(@NonNull LockConf lockConfiguration) {
        if (StringUtils.isEmpty(nameSpace)) {
            return lockConfiguration.getName();
        } else {
            return nameSpace + "::" + lockConfiguration.getName();
        }
    }

    private static class StorageLock extends AbstractSimpleLock {

        private final StorageAccessor storageAccessor;

        StorageLock(LockConf lockConfiguration, StorageAccessor storageAccessor) {
            super(lockConfiguration);
            this.storageAccessor = storageAccessor;
        }

        @Override
        public void doUnlock() {
            storageAccessor.unlock(lockConfiguration);
        }
    }

    @Override
    public void setNameSpace(String nameSpace) {
        this.nameSpace = nameSpace;
        storageAccessor.setNameSpace(this.nameSpace);
    }

    @Override
    public int getId() {
        // TODO Auto-generated method stub
        return 0;
    }

}
