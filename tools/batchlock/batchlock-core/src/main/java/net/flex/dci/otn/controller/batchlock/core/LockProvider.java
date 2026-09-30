/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.flex.dci.otn.controller.batchlock.core;

import net.flex.dci.otn.controller.batchlock.support.LockException;
import net.flex.dci.otn.controller.batchlock.support.annotation.NonNull;
import java.util.Optional;

/**
 * Provides lock implementation.
 */
public interface LockProvider {

    /**
     * @return If empty optional has been returned, lock could not be acquired. The lock has to be
     *         released by the callee.
     */
    @NonNull
    Optional<SimpleLock> lock(@NonNull LockConf lockConfiguration);

    @NonNull
    void setNameSpace(String nameSpace);

    @NonNull
    int getId() throws LockException;
}
