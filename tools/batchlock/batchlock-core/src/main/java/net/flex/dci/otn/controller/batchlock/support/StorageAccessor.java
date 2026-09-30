/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.flex.dci.otn.controller.batchlock.support;

import net.flex.dci.otn.controller.batchlock.core.LockConf;
import net.flex.dci.otn.controller.batchlock.support.annotation.NonNull;

public interface StorageAccessor {

    /**
     * Inserts a record, if it does not already exists. If it exists, returns false.
     *
     * @param lockConfiguration LockConfiguration
     * @return true if inserted
     */
    boolean insertRecord(@NonNull LockConf lockConfiguration);

    /**
     * Tries to update the lock record. If there is already a valid lock record (the lock is held by
     * someone else) update should not do anything and this method returns false.
     *
     * @param lockConfiguration LockConfiguration
     * @return true if updated
     */
    boolean updateRecord(@NonNull LockConf lockConfiguration);

    void unlock(@NonNull LockConf lockConfiguration) throws LockException;

    void setNameSpace(String nameSpace);
}
