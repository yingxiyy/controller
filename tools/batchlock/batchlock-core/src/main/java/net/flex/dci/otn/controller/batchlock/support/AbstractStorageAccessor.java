/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package net.flex.dci.otn.controller.batchlock.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractStorageAccessor implements StorageAccessor {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected String getHostip() {
        return Utils.getHostip();
    }
}
