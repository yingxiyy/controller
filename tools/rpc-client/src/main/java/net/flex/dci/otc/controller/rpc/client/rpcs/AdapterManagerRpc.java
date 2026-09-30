/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.rpcs;

import net.flex.dci.otc.common.exception.CommonException;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.CreateAdapterInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.DeleteAdapterInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/8/30 14:26
 */
public interface AdapterManagerRpc {

    /**
     * create-adapter
     */
    void createAdapter(Adapter adapter) throws CommonException;

    /**
     * delete-adapter
     */
    void deleteAdapter(Adapter adapter) throws CommonException;

    void createAdapter(CreateAdapterInput input) throws CommonException;


    void deleteAdapter(DeleteAdapterInput input) throws CommonException;
}
