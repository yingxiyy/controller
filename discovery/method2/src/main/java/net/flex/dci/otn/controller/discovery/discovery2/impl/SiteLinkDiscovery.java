/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.discovery.discovery2.impl;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

@Slf4j
public class SiteLinkDiscovery extends LinkDiscovery {

    public SiteLinkDiscovery(DiscoveryResource discovery) {
        super(discovery);
    }

    public SiteLinkDiscovery start(Link link) throws CommonException {
        log.debug("start site link discovery");
        pass(link.getAugmentation(Link1.class).getSite().getExplictRoute().getRoute());
        return this;
    }

    private void pass(List<Route> routeList) throws CommonException {
        for (Route route : routeList) {
            checkXC(route.getPrimary().getCrossConnections());
            checkEro(route.getPrimary().getExplicitRouteObjects());
            if (route.getSecondary() != null) {
                checkXC(route.getSecondary().getCrossConnections());
                checkEro(route.getSecondary().getExplicitRouteObjects());
            }
        }
    }
}
