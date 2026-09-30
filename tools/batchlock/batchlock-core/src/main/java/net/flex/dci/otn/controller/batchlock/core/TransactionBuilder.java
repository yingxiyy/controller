/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.batchlock.core;

import net.flex.dci.otn.controller.batchlock.support.LockException;

public class TransactionBuilder implements ITransactionBuilder {

    private static TransactionBuilder _instance = null;
    private LockProvider lockProvider = null;

    protected TransactionBuilder() {
    }

    public static TransactionBuilder instance() {
        if (_instance == null) {
            _instance = new TransactionBuilder();
        }
        return _instance;
    }

    public void initLockProvider(LockProvider lockProvider) {
        this.lockProvider = lockProvider;
    }

    public BatchLockTransaction newTransaction() throws LockException {
        if (lockProvider == null) {
            throw new LockException(
                    "No lockProvider found, should invoke initLockProvider firstly.");
        }
        return new TransactionImpl(lockProvider);
    }
}
