/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel;

import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.TunnelInput;
import net.flex.dci.otn.controller.allocate.designer.tunnel.model.PickedOtResource;
import net.flex.dci.otn.controller.allocate.ne.Card;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SignalProtocolType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OtReusedService {

/*
    @Autowired
    private NodeUtils nodeUtils;

    @Autowired
    private TunnelUtils tunnelUtils;

*/

    @Autowired
    private OtReusedStrategy otReusedStrategy;

  /*  *//**
     * 1. 首先优先选取input里提供的利旧优先池里面选取
     *
     * 2. 如果场景1没有，再从input提供的利旧大池里面选取, 并且要产生利旧的node的snapshot
     *
     * @param siteId
     * @param otCardInfo
     * @param tunnelInput
     * @return
     * @throws NeDesignerException
     *//*
    public PickedOtResource getReusedNode(String siteId, Card otCardInfo, TunnelInput tunnelInput) throws NeDesignerException {
        Class<? extends SignalProtocolType> lineSignalRate = tunnelInput.getLineSignalRate();

        PickedOtResource pickedOtResource;
        //场景1
        if (tunnelInput.getReusedNodePoolFirst() != null) {
            Node reusedNode = tunnelInput.getReusedNodePoolFirst().get(siteId);
            if (reusedNode != null) {
                return otReusedStrategy.pickedOtResource(reusedNode, otCardInfo, lineSignalRate);
            }
        }

        //场景2
        if (tunnelInput.getReusedNodePool() != null) {
            Node reusedNode = tunnelInput.getReusedNodePoolFirst().get(siteId);
            if (reusedNode != null) {
                return otReusedStrategy.pickedOtResource(reusedNode, otCardInfo, lineSignalRate);
            }
        }
        pickedOtResource = getReusedNodeFromPool(siteId, otCardInfo, tunnelInput, lineSignalRate, tunnelInput.getReusedNodePoolFirst());
        if (pickedOtResource != null) return pickedOtResource;

        //场景2
        return getReusedNodeFromPool(siteId, otCardInfo, tunnelInput, lineSignalRate, tunnelInput.getReusedNodePool());
    }

    private PickedOtResource getReusedNodeFromPool(String siteId, Card otCardInfo, TunnelInput tunnelInput, Class<? extends SignalProtocolType> lineSignalRate, Map<String, List<Node>> reusedNodePoolMap) throws NeDesignerException {
        if (reusedNodePoolMap != null) {
            Node reusedNode = tunnelInput.getReusedNodePoolFirst().get(siteId);
            if (reusedNode != null) {
                return otReusedStrategy.pickedOtResource(reusedNode, otCardInfo, lineSignalRate);
            }
        }
        return null;
    }*/


}
