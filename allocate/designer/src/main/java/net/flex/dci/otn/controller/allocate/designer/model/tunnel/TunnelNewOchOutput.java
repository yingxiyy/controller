/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model.tunnel;

import java.util.List;
import java.util.Map;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor._new.och.tunnel.TunnelRouteInfos;

@Builder
@Data
@Slf4j
public class TunnelNewOchOutput {
    @NonNull
    private List<TunnelRouteInfos> tunnelRouteInfos;
    @NonNull
    private List<Node> nodeSnapshot;
    @NonNull
    private List<Link> siteLinkSnapshot;
    @NonNull
    private Map<String, Node> inMemoryNode;//include TPC and OPC node
    @NonNull
    private List<Long> usedCentralFrequencies;
    @NonNull
    private List<String> removedResourceIds;

   /* @NonNull
    private Map<String, Node> inMemoryTpcNode;//only have TPC node,later for bom use*/
//    @NonNull
//    private Map<String, Map<String, NeBomInfo>> bomMap;//key is the same as the  key for bomMetaMap in BomMetaConfig.class, {bomMetaKey,{neId,neBomInfo}}
}
