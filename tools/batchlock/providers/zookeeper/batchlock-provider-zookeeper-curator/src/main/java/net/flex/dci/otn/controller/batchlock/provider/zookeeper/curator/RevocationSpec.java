/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.flex.dci.otn.controller.batchlock.provider.zookeeper.curator;

import java.util.concurrent.Executor;

class RevocationSpec {

    private final Runnable runnable;
    private final Executor executor;

    RevocationSpec(Executor executor, Runnable runnable) {
        this.runnable = runnable;
        this.executor = executor;
    }

    Runnable getRunnable() {
        return runnable;
    }

    Executor getExecutor() {
        return executor;
    }
}
