/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.flex.dci.otn.controller.batchlock.core;

import net.flex.dci.otn.controller.batchlock.support.annotation.NonNull;
import java.util.Objects;

/**
 * Lock configuration.
 */
public class LockConf {

    private final String name;

    private final String tid;

    /**
     * Creates LockConfiguration. There are two types of lock providers. One that uses "db time"
     * which requires relative values of lockAtMostFor and lockAtLeastFor (currently it's only
     * JdbcTemplateLockProvider). Second type of lock provider uses absolute time calculated from
     * `createdAt`.
     *
     * @param createdAt
     * @param name
     * @param lockAtMostFor
     * @param lockAtLeastFor
     */
    public LockConf(@NonNull String name,
            @NonNull String tid) {
        this.name = Objects.requireNonNull(name);
        this.tid = Objects.requireNonNull(tid);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("lock name can not be empty");
        }
        if (tid.isEmpty()) {
            throw new IllegalArgumentException("transaction id can not be empty");
        }
    }

    public String getName() {
        return name;
    }

    public String getTid() {
        return tid;
    }

    @Override
    public String toString() {
        return "LockConf{" +
                "name='" + name + '\'' +
                ", tid=" + tid +
                '}';
    }
}
