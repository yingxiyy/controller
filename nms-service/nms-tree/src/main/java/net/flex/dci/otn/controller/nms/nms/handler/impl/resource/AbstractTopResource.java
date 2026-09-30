/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler.impl.resource;

import net.flex.dci.otn.controller.nms.nms.handler.impl.INMSOperations;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;

/**
 * @author: xinyzhao
 * @date: 2021/4/8
 */
public abstract class AbstractTopResource implements INMSOperations {

    protected NetconfTopology netconfTopology;

    public AbstractTopResource(NetconfTopology netconfTopology) {
        this.netconfTopology = netconfTopology;
    }
}
