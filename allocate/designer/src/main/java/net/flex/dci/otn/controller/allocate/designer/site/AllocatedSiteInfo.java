/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.site;


import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import net.flex.dci.otn.controller.allocate.designer.model.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

@Builder
@Data
class AllocatedSiteInfo {

    @NonNull
    Route siteRoute;
    @NonNull
    List<SiteNodeInfo> protectedNodes;

    @NonNull
    List<Node> updatedProtectedNodes; //if the route is slave, then add internal link(link between main and slave) to main nodes

}
