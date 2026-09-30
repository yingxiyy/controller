/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel.reg;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.RegInput;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor._new.och.tunnel.TunnelRouteInfos;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.vendor._new.och.tunnel.TunnelRouteInfosBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.OchRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.TpcRoute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RegRepo {

    @Autowired
    private RegOchRepo regOchRepo;
    @Autowired
    private RegTpcRepo regTpcRepo;


    public Pair<TunnelRouteInfos, Map<String, Node>> allocate(RegInput regInput) throws NeDesignerException {
        Map<String, Node> totalNodesMap = regInput.getTotalNodes().stream().collect(Collectors.toMap(item -> item.getNodeId().getValue(), Function.identity()));

        OchRoute ochRoute = regOchRepo.allocateOchRoute(regInput, totalNodesMap);
        List<TpcRoute> tpcRoutes = regTpcRepo.createTpcRoutes(ochRoute.getSourceTp(), ochRoute.getDestTp(), regInput, totalNodesMap);
        TunnelRouteInfos tunnelRouteInfo = new TunnelRouteInfosBuilder().setOchRoute(ochRoute).setTpcRoute(tpcRoutes).build();

        return Pair.of(tunnelRouteInfo, totalNodesMap);
    }
}
