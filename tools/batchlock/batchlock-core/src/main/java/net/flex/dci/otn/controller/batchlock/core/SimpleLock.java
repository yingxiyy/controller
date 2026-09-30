/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.flex.dci.otn.controller.batchlock.core;

import net.flex.dci.otn.controller.batchlock.support.LockException;

public interface SimpleLock {

    /**
     * Unlocks the lock. Once you unlock it, you should not use for any other operation.
     *
     * @throws IllegalStateException if the lock has already been unlocked or extended
     */
    void unlock() throws LockException;
}
