/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.nbi.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otn.controller.implement.common.nbi.impl.BaseImpl;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.GetNeDataInputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.GetNeDataInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.GetNeDataOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.GetNeDataOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @version 1.0
 */
@Slf4j
@Service
public class GetNeData extends BaseImpl {


    /**
     * only support retrieve current time of NE
     *
     * @param input
     * @return
     * @throws CommonException
     */
    public GetNeDataOutput getNeData(GetNeDataInput input) throws CommonException {
        checkParam(input);
        String neId = input.getNodeId().getValue();
        Adapter adapter = adapterDao.getAdapterByNeId(neId);
        if (adapter == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ne is not managed by any adapter,please manage ne first!");
        }
        return getPhysical(input);
    }

    private GetNeDataOutput getPhysical(GetNeDataInput input) throws CommonException {
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.GetNeDataInput emlInput = new GetNeDataInputBuilder()
                .setNodeId(input.getNodeId())
                .setProperties(input.getProperties())
                .build();

        NeManagerRpc config = SpringBeanFinder.getBean(NeManagerRpc.class);
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.GetNeDataOutput result = config.getNeData(
                emlInput);

        return new GetNeDataOutputBuilder()
                .setNodeId(input.getNodeId())
                .setProperties(result.getProperties())
                .build();

    }

    private void checkParam(GetNeDataInput input) throws CommonException {
        Node dbNode = phyNodeDao.getConfigPhyNodeById(input.getNodeId().getValue());
        ImplementState state = dbNode.getAugmentation(Node1.class).getPhysical()
                .getImplementState();
//        if (state.equals(ImplementState.Plan) || state.equals(ImplementState.Allocate)) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "the node must be implemented and reachable");
//        }
        if (input.getProperties() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "hasn't point out which attribute want to retrieve");
        }
        List<Property> propList = input.getProperties().getProperty();
        String unSupported = null;
        for (Property prop : propList) {
            if (!prop.getName().equalsIgnoreCase("system.current-datetime")) {
                unSupported = prop.getName();
                break;
            }
        }
        if (unSupported != null) {
            throw new CommonException(
                    CommonExceptionType.INVALID_PARAMETER,
                    "find unsupported attribute " + unSupported);
        }
    }
}
