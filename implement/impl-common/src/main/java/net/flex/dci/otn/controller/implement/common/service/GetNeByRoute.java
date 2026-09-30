/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.common.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.RouteUsageInclude;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHop;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;

/**
 * based on route info, find out all requied node, and construct the node structure want to setting
 * to NE's XC, TP, Euip, and seting implState as Impling/ Deimpling of them
 *
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Data
public class GetNeByRoute {

    private Map<String, NodeBuilder> phyNodeMap;

    public GetNeByRoute() {
        phyNodeMap = new HashMap<>();
    }

    public void parse(List<Route> routeList) throws CommonException {
        List<ExplicitRouteObjects> eroList = new LinkedList<>();
        for (Route route : routeList) {
            eroList.addAll(route.getPrimary().getExplicitRouteObjects());
            if (route.getSecondary() != null
                    && route.getSecondary().getExplicitRouteObjects() != null && !route
                    .getSecondary().getExplicitRouteObjects().isEmpty()) {
                eroList.addAll(route.getSecondary().getExplicitRouteObjects());
            }
        }
        processEro(eroList);
    }

    private void processEro(List<ExplicitRouteObjects> eroList) throws CommonException {
        List<String> nodeIds = new ArrayList<String>();
        for (ExplicitRouteObjects ero : eroList) {
            if (ero.getExplicitRouteUsage().equals(RouteUsageInclude.class)) {
                List<PathRouteObject> proList = ero.getPathRouteObject();
                for (PathRouteObject pro : proList) {
                    if (pro.getResourceType().getImplementedInterface().getName()
                            .equals(Tp.class.getName())) {
                        TpHop hop = ((Tp) pro.getResourceType()).getTpHop();
                        String nodeId = hop.getNodeRef().getValue();
                        if (!nodeId.contains(Constant.HASH_TAG)) {
                        	 nodeId = hop.getSiteRef().getValue() + Constant.HASH_TAG + hop.getNodeRef().getValue();
                        }
                        if (!nodeIds.contains(nodeId)) {
                            nodeIds.add(nodeId);
                        }
                    }
                }
            }
        }

        for (String nodeId : nodeIds) {
            prepareNode(nodeId);
        }
    }

    private void prepareNode(String nodeId) {
        PhyNodeDao mongoDaoUtil = SpringBeanFinder.getBean(PhyNodeDao.class);
        Node dbNode = mongoDaoUtil.getConfigPhyNodeById(nodeId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical dbPhysical
                = dbNode.getAugmentation(Node1.class).getPhysical();
        NodeBuilder nb = new NodeBuilder()
                .setNodeId(new NodeId(nodeId))
                .setTerminationPoint(new LinkedList<>())
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder()
                                        .setAdminState(dbPhysical.getAdminState())
                                        .setIp(dbPhysical.getIp())
                                        .setImplementState(dbPhysical.getImplementState())
                                        .setFriendlyName(dbPhysical.getFriendlyName())
                                        .setCrossConnections(new LinkedList<>())
                                        .setEquipments(new LinkedList<>())
                                        .setInternalLinks(new LinkedList<>())
                                        .setProperties(new PropertiesBuilder()
                                                .setProperty(new LinkedList<>()).build())
                                        .build())
                        .build());
        phyNodeMap.put(nodeId, nb);
    }

    public List<NodeBuilder> getPhyNodeList() {
        List<NodeBuilder> tmp = new LinkedList<>();
        for (NodeBuilder nb : phyNodeMap.values()) {
            tmp.add(nb);
        }
        return tmp;
    }
}
