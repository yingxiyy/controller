/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.tunnel.manager;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TunnelManager {

    public List<String> getLockKeysByRoute(List<Route> routes) {
        List<String> lockKeys = new ArrayList<String>();
        for (Route route : routes) {
            if (route.getPrimary() != null
                    && route.getPrimary().getExplicitRouteObjects() != null
                    && !route.getPrimary().getExplicitRouteObjects().isEmpty()) {
                for (PathRouteObject pathRouteObject : route.getPrimary().getExplicitRouteObjects()
                        .get(0).getPathRouteObject()) {
                    if (pathRouteObject.getResourceType().getImplementedInterface()
                            .getName().equals(Tp.class.getName())) {
                        Tp tp = (Tp) pathRouteObject.getResourceType();
                        if (!lockKeys.contains(tp.getTpHop().getTpRef().getValue())) {
                            lockKeys.add(tp.getTpHop().getTpRef().getValue());
                        }
                        if (!lockKeys.contains(tp.getTpHop().getNodeRef().getValue())) {
                            lockKeys.add(tp.getTpHop().getNodeRef().getValue());
                        }
                    }
                }

                for (CrossConnections crossConnections : route.getPrimary().getCrossConnections()) {
                    if (!lockKeys.contains(crossConnections.getCrossConnectionId().getValue())) {
                        lockKeys.add(crossConnections.getCrossConnectionId().getValue());
                    }
                }
            }

            if (route.getSecondary() != null
                    && route.getSecondary().getExplicitRouteObjects() != null
                    && !route.getSecondary().getExplicitRouteObjects().isEmpty()) {
                for (PathRouteObject pathRouteObject : route.getSecondary()
                        .getExplicitRouteObjects()
                        .get(0).getPathRouteObject()) {
                    if (pathRouteObject.getResourceType().getImplementedInterface()
                            .getName().equals(Tp.class.getName())) {
                        Tp tp = (Tp) pathRouteObject.getResourceType();
                        if (!lockKeys.contains(tp.getTpHop().getTpRef().getValue())) {
                            lockKeys.add(tp.getTpHop().getTpRef().getValue());
                        }
                        if (!lockKeys.contains(tp.getTpHop().getNodeRef().getValue())) {
                            lockKeys.add(tp.getTpHop().getNodeRef().getValue());
                        }
                    }
                }

                for (CrossConnections crossConnections : route.getSecondary()
                        .getCrossConnections()) {
                    if (!lockKeys.contains(crossConnections.getCrossConnectionId().getValue())) {
                        lockKeys.add(crossConnections.getCrossConnectionId().getValue());
                    }
                }
            }
        }
        return lockKeys;
    }
}
