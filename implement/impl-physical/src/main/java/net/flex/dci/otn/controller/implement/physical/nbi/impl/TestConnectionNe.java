/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.nbi.impl;

import java.util.List;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.rpc.client.rpcs.AdapterRpc;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TestConnectionStatusInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TestConnectionStatusOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TestConnectionStatusOutputBuilder;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/10/18 10:43
 */
@Component
@Slf4j
public class TestConnectionNe {


    /**
     * @param input
     * @return
     */
    public TestConnectionStatusOutput doIt(TestConnectionStatusInput input) {
        log.info("start to test the connection status for the ne {}", input.getNodeId().getValue());
        List<Adapter> adapters = SpringBeanFinder.getBean(AdapterDao.class).getAdapters();
        Adapter adapter = adapters.get(((int) Math.random()) * adapters.size());
        Runnable testConnectionTask = () -> {
            //todo:do the execute command
            AdapterRpc emlRpc = SpringBeanFinder.getBean(AdapterRpc.class);
            emlRpc.testNeConnection(adapter, input);
        };
        Executors.newCachedThreadPool().submit(testConnectionTask);

        return new TestConnectionStatusOutputBuilder().setReturnCode(
                RpcResultType.Success).build();
    }
}
