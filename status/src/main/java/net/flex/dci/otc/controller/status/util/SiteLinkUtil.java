/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.Aps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Secondary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SiteLinkUtil {

    @Autowired
    private CrossConnectionsDao crossConnectionsDao;

    public List<String> getPrimaryRouteTps(Primary primary) {
        return this.getRouteTps(primary.getExplicitRouteObjects().get(0));
    }


    public List<String> getSecondaryRouteTps(Secondary secondary) {
        return this.getRouteTps(secondary.getExplicitRouteObjects().get(0));
    }

    public List<String> getCommonLinks(Primary primary, Set<String> switchPorts) {
        List<String> list = new ArrayList<>();
        List<PathRouteObject> proList = primary.getExplicitRouteObjects().get(0)
                .getPathRouteObject();
        java.util.Collections.sort(proList, new ProComparator());
        boolean shouldAdd = true;
        if (proList != null) {
            for (PathRouteObject pro : proList) {
                if (pro.getResourceType() instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) {
                    String linkId = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) pro
                            .getResourceType()).getLinkHop().getLinkRef().getValue();
                    if (shouldAdd) {
                        list.add(linkId);
                    }
                } else if (pro
                        .getResourceType() instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp) {
                    String tpId = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp) pro
                            .getResourceType()).getTpHop().getTpRef().getValue();
                    if (switchPorts.contains(tpId)) {
                        shouldAdd = !shouldAdd;
                    }

                }
            }
        }
        return list;
    }

    public List<String> getCommonTps(Primary primary, Set<String> switchPorts) {
        List<String> commonTps = new ArrayList<>();
        List<String> primaryTps = this.getPrimaryRouteTps(primary);
        boolean shouldAdd = true;
        for (String tp : primaryTps) {
            if (switchPorts.contains(tp)) {
                shouldAdd = !shouldAdd;
                continue;
            }
            if (shouldAdd) {
                commonTps.add(tp);
            }
        }
        return commonTps;
    }

    public List<String> getRouteTps(ExplicitRouteObjects ero) {
        List<String> list = new ArrayList<>();
        List<PathRouteObject> proList = ero.getPathRouteObject();
        java.util.Collections.sort(proList, new ProComparator());
        if (proList != null) {
            for (PathRouteObject pro : proList) {
                if (pro.getResourceType() instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp) {
                    String tpId = ((org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp) pro
                            .getResourceType()).getTpHop().getTpRef().getValue();
                    list.add(tpId);
                }
            }
        }
        return list;
    }

    /**
     * 1: primary 2: secondary 3: primary + secondary
     *
     * @param link
     * @return
     */
    public int getProtectPathType(Link link) {
        Site site = link.getAugmentation(Link1.class).getSite();
        Route route = site.getExplictRoute().getRoute().get(0);
        List<CrossConnections> xcs = route.getPrimary().getCrossConnections();
        boolean isContainPrimary = false;
        boolean isContainSecondary = false;
        int xcCnt = 0;
        if (xcs != null) {
            for (CrossConnections xc : xcs) {
                String tpRef = xc.getSourceTp().get(0).getTpRef().getValue();
                String[] ids = tpRef.split("#");
                NodeId nodeId = new NodeId(ids[0] + "#" + ids[1]);
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections phyXc =
                        crossConnectionsDao.getXCByNodeIdAndXcRef(nodeId.getValue(),
                                xc.getCrossConnectionId().getValue());
                Aps aps = phyXc.getAps();
                if (aps != null) {
                    if (aps.getActivePath() == ApsPath.PRIMARY) {
                        isContainPrimary = true;
                        xcCnt++;
                    } else if (aps.getActivePath() == ApsPath.SECONDARY) {
                        isContainSecondary = true;
                        xcCnt++;
                    }

                }
            }

        }
        if (xcCnt != 2) {
            return -1;
        }
        if (isContainPrimary && !isContainSecondary) {
            return 1;
        } else if (!isContainPrimary && isContainSecondary) {
            return 2;
        } else if (isContainPrimary && isContainSecondary) {
            return 3;
        }
        return -1;
    }

    public void getSwitchPort(Link link, Set<String> switchPorts, Set<String> selectPort) {
        Site site = link.getAugmentation(Link1.class).getSite();
        Route route = site.getExplictRoute().getRoute().get(0);
        List<CrossConnections> xcs = route.getPrimary().getCrossConnections();
        if (xcs != null) {
            for (CrossConnections xc : xcs) {
                String tpRef = xc.getSourceTp().get(0).getTpRef().getValue();
                String[] ids = tpRef.split("#");
                NodeId nodeId = new NodeId(ids[0] + "#" + ids[1]);
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections phyXc =
                        crossConnectionsDao.getXCByNodeIdAndXcRef(nodeId.getValue(),
                                xc.getCrossConnectionId().getValue());
                Aps aps = phyXc.getAps();
                if (aps != null) {
                    if (xc.getDestinationTp() != null) {
                        for (DestinationTp tp : xc.getDestinationTp()) {
                            switchPorts.add(tp.getTpRef().getValue());
                        }
                    }
                    if (aps.getActivePath() == ApsPath.PRIMARY) {
                        selectPort.add(xc.getDestinationTp().get(0).getTpRef().getValue());
                    } else if (aps.getActivePath() == ApsPath.SECONDARY) {
                        selectPort.add(xc.getDestinationTp().get(1).getTpRef().getValue());
                    }
                }
            }
        }
    }
}
