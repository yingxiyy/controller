/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.flex.dci.otn.controller.batchlock.core;

import net.flex.dci.otn.controller.batchlock.support.LockException;

public abstract class AbstractSimpleLock implements SimpleLock {

    private boolean valid = true;
    protected final LockConf lockConfiguration;

    protected AbstractSimpleLock(LockConf lockConfiguration) {
        this.lockConfiguration = lockConfiguration;
    }

    @Override
    public final void unlock() throws LockException {
        checkValidity();
        doUnlock();
        valid = false;
    }

    protected abstract void doUnlock() throws LockException;

    private void checkValidity() {
        if (!valid) {
            throw new IllegalStateException(
                    "Lock is not valid, it has already been unlocked or extended");
        }
    }
}
