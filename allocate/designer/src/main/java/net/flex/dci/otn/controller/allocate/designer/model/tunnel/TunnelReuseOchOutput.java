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
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.TpcRoute;

@Builder
@Data
@Slf4j
public class TunnelReuseOchOutput {
    @NonNull
    private List<TpcRoute> tpcRoutes;//one tunnel have one TpcRoute
    @NonNull
    private List<Node> tpcNodeSnapshot;
    @NonNull
    private Link ochLinkSnapshot;
    @NonNull
    private Map<String, Node> inMemoryTpcNode;
}
