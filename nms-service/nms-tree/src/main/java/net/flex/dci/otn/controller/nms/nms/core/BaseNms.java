/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.core;

import net.flex.dci.otn.controller.nms.rpc.BaseRpc;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;

/**
 * @date: 2021/4/21
 */
public abstract class BaseNms extends BaseRpc {


    public BaseNms(NetconfTopology netconfTopology) {
        super(netconfTopology);
        this.RPC_NAMESPACE = "nms";
    }


}
