/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler;

import net.flex.dci.otn.controller.nms.nms.convertors.NmsOutputConverters;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @date: 2021/4/8
 */
public abstract class AbstractBaseHandler implements IBaseNMSOperations {

    protected NetconfTopology netconfTopology;

    @Autowired
    protected NmsOutputConverters nmsOutputConverters;

    public AbstractBaseHandler(NetconfTopology netconfTopology) {
        this.netconfTopology = netconfTopology;
    }


}
