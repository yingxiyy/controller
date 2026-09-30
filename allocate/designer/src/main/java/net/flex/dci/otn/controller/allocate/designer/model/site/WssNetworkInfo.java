/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model.site;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

@Builder
@Data
public class WssNetworkInfo {

    @NonNull
    private RouteInfo linkA;
    @NonNull
    private RouteInfo linkZ;
    @NonNull
    private Link wssLink;
    @NonNull
    Map<String, Node> totalNodesMap;

}
