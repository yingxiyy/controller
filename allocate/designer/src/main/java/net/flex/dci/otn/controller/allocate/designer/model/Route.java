/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model;

import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.ne.bom.group.info.site.NeBomInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;

@Builder
@Data
public class Route {

    List<Node> nodes;
    List<Link> links;
    List<CrossConnections> xcs;

}
