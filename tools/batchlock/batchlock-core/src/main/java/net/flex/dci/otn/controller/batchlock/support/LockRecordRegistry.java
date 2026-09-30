/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.flex.dci.otn.controller.batchlock.support;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Some LockProviders have to decide if a new record has to be created or an old one updated.
 * This class helps them keep track of existing lock records, so they know if a lock record exists.
 */
class LockRecordRegistry {
    private final Set<String> lockRecords = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));

    public void addLockRecord(String lockName) {
        lockRecords.add(lockName);
    }

    public boolean lockRecordRecentlyCreated(String lockName) {
        return lockRecords.contains(lockName);
    }

    int getSize() {
        return lockRecords.size();
    }

    public void clear() {
        lockRecords.clear();
    }
}
