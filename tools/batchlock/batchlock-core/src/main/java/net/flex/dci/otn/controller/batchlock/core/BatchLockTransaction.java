/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.batchlock.core;

import java.util.Set;
import java.util.concurrent.TimeoutException;

import net.flex.dci.otn.controller.batchlock.support.LockException;

public interface BatchLockTransaction {

    void lock(String resourceId);

    void lock(Set<String> resourceIds);

    // blocking api, will wait for resources until attaining them.
    void require() throws LockException;

    // non-blocking api, will wait for millisecondTimeout
    void require(long millisecondTimeout) throws LockException, TimeoutException;

    // non-blocking api, will not wait. only if the resources are all available, it returns true
    boolean require_once();

    void dismiss() throws LockException;
}
