/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler.impl.connections;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.handler.impl.INMSOperations;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.SecondaryBuilder;

/**
 * @date: 2021/4/2
 */
@Slf4j
public abstract class AbstractTopoLink implements INMSOperations {

    protected NetconfTopology netconfTopology;

    public AbstractTopoLink(NetconfTopology netconfTopology) {
        this.netconfTopology = netconfTopology;
    }

    public AbstractTopoLink() {
    }


    /**
     * get real route
     *
     * @param routeList
     * @return
     */
    protected List<Route> getRealRoute(List<Route> routeList) {
        List<Route> newRouteList = new ArrayList<>();
//        for (Route route : routeList) {
//            RouteBuilder rb = new RouteBuilder(route);
//            rb.setPrimary(new PrimaryBuilder(route.getPrimary())
//                    .setCrossConnections(replaceXC(route.getPrimary().getCrossConnections()))
//                    .build());
//            if (route.getSecondary() != null) {
//                rb.setSecondary(new SecondaryBuilder(route.getSecondary())
//                        .setCrossConnections(replaceXC(route.getSecondary().getCrossConnections()))
//                        .build());
//            }
//            newRouteList.add(rb.build());
//        }
        routeList.forEach(route -> {
            RouteBuilder routeBuilder = new RouteBuilder(route);
            routeBuilder.setPrimary(new PrimaryBuilder(route.getPrimary()).setCrossConnections(
                    getRealCrossConnection(route.getPrimary().getCrossConnections())).build());
            if (route.getSecondary() != null) {
                routeBuilder.setSecondary(
                        new SecondaryBuilder(route.getSecondary()).setCrossConnections(
                                        getRealCrossConnection(route.getSecondary().getCrossConnections()))
                                .build());
            }
            newRouteList.add(routeBuilder.build());
        });
        return newRouteList;
    }

    private List<CrossConnections> getRealCrossConnection(List<CrossConnections> crossConnections) {
        log.debug("get real cross connection");
        List<CrossConnections> realCrossConnection = new ArrayList<>();
        Map<String, List<CrossConnections>> crossConnectionMap = new HashMap<>();
        crossConnections.forEach(xc -> {
            String nodeId = PhysicalTpIdNamingRule.getNodeId(
                    xc.getSourceTp().get(0).getTpRef().getValue());
            List<CrossConnections> refCrossConnections = crossConnectionMap.getOrDefault(nodeId,
                    new ArrayList<>());
            refCrossConnections.add(xc);
            crossConnectionMap.put(nodeId, refCrossConnections);
        });
        realCrossConnection = _getRealCrossConnection(crossConnectionMap);
        return realCrossConnection;
    }

    private List<CrossConnections> _getRealCrossConnection(
            Map<String, List<CrossConnections>> crossConnectionMap) {
        log.debug("get real cross connection");
        List<CrossConnections> crossConnections = new ArrayList<>();
        crossConnectionMap.keySet().forEach(nodeId -> {
            Node phyNode = netconfTopology.getNeNode(nodeId);
            List<CrossConnections> refCrossConnection = crossConnectionMap.get(nodeId);
            List<CrossConnections> realCrossConnection = retrieveRealXC(phyNode,
                    refCrossConnection);
            crossConnections.addAll(realCrossConnection);
        });
        return crossConnections;
    }

    private List<CrossConnections> retrieveRealXC(Node phyNode,
            List<CrossConnections> refCrossConnection) {
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> realXcs = phyNode.getAugmentation(
                Node1.class).getPhysical().getCrossConnections();
        NodeId nodeId = phyNode.getNodeId();
        List<CrossConnections> refXcs = new ArrayList<>();
        for (CrossConnections crossConnections : refCrossConnection) {
            CrossConnections realCrossConnections = filterRealXc(crossConnections, realXcs);

            if (realCrossConnections != null) {
                CrossConnectionsBuilder connectionsBuilder = new CrossConnectionsBuilder(
                        realCrossConnections);
                connectionsBuilder.setNodeRef(nodeId);
                refXcs.add(connectionsBuilder.build());
            }
        }
//        List<CrossConnections> refXcs = xcIds.stream().filter(realXcMap::containsKey)
//                .map(xcId -> new CrossConnectionsBuilder(realXcMap.get(xcId)).build())
//                .collect(Collectors.toList());
//
//        List<CrossConnections> refDescription = descriptions.stream().filter()
        return refXcs;
    }

    private CrossConnections filterRealXc(CrossConnections crossConnections,
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> realXcs) {
        log.debug("filter real xc the id is:{}",
                crossConnections.getCrossConnectionId().getValue());
        Optional<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> opXc =
                realXcs.stream()
                        .filter(t -> t.getCrossConnectionId().getValue()
                                .equals(crossConnections.getCrossConnectionId().getValue()))
                        .findAny();
        if (opXc.isPresent()) {
            NodeId refNodeId = NodeId.getDefaultInstance(
                    PhysicalTpIdNamingRule.getNodeId(
                            opXc.get().getSourceTp().get(0).getTpRef().getValue()));
            return new CrossConnectionsBuilder(opXc.get()).setNodeRef(refNodeId).build();
        } else {
            opXc = realXcs.stream()
                    .filter(t -> t.getDescription() != null && t.getDescription()
                            .equals(crossConnections.getDescription()))
                    .findAny();
            if (opXc.isPresent()) {
                NodeId refNodeId = NodeId.getDefaultInstance(
                        PhysicalTpIdNamingRule.getNodeId(
                                opXc.get().getSourceTp().get(0).getTpRef().getValue()));
                return new CrossConnectionsBuilder(opXc.get()).setNodeRef(refNodeId).build();
            } else {
                log.error("hasn't find related xc in node, this is impossible");
                return crossConnections;
            }
        }
    }

    private List<CrossConnections> replaceXC(List<CrossConnections> xcList) {
        List<CrossConnections> newXcList = new ArrayList<>();

        String prevNodeId = "";
        Node node = null;
        for (CrossConnections xc : xcList) {
            String nodeId = PhysicalTpIdNamingRule.getNodeId(
                    xc.getSourceTp().get(0).getTpRef().getValue());
            if (!prevNodeId.equals(nodeId)) {
                node = netconfTopology.getNeNode(nodeId);
            }
            newXcList.add(fetchXc(node, xc));
        }
        return newXcList;
    }

    private CrossConnections fetchXc(Node node, CrossConnections xc) {
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        Optional<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> opXc =
                nodeAttr.getCrossConnections().stream()
                        .filter(t -> t.getCrossConnectionId().getValue()
                                .equals(xc.getCrossConnectionId().getValue())).findAny();
        if (opXc.isPresent()) {
            return new CrossConnectionsBuilder(opXc.get()).build();
        } else {
            opXc = nodeAttr.getCrossConnections().stream()
                    .filter(t -> t.getDescription().equals(xc.getDescription())).findAny();
            if (opXc.isPresent()) {
                return new CrossConnectionsBuilder(opXc.get()).build();
            } else {
                log.error("hasn't find related xc in node, this is impossible");
                return xc;
            }
        }
    }
}
