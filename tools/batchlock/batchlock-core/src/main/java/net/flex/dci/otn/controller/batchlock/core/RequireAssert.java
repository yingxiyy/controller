/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.flex.dci.otn.controller.batchlock.core;

import net.flex.dci.otn.controller.batchlock.support.annotation.NonNull;

/**
 * Asserts lock presence. This class
 * makes sure that the task is indeed locked.
 */
public class RequireAssert {
    private static final ThreadLocal<String> currentTransactionName = ThreadLocal.withInitial(() -> null);

    static void startRequire(String name) {
        currentTransactionName.set(name);
    }

    static boolean alreadyRequiredBy(@NonNull String name) {
        return name.equals(currentTransactionName.get());
    }

    static void endRequire() {
        currentTransactionName.remove();
    }

    /**
     * Throws an exception if the lock is not present.
     */
    public static void assertRequired() {
        if (currentTransactionName.get() == null) {
            throw new IllegalStateException("The task is not locked.");
        }
    }
}
