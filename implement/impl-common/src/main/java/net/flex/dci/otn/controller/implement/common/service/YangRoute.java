/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.common.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import net.flex.dci.otn.controller.implement.common.utils.Constants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.ApsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otn.phy.topology.type.OtnPhyTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.RouteUsageInclude;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.link.LinkHop;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHop;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.tp.TpHopBuilder;

/**
 * based on route info, find out all requied node, and construct the node structure want to setting
 * to NE's XC, TP, Euip, and seting implState as Impling/ Deimpling of them
 *
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Data
public class YangRoute {

    public final static String PropKey_ErrorMsg = "ErrorMsg";
    public final static String PropKey_Impl = "Impl";
    public final static String PropKey_DeImpl = "DeImpl";
    public final static String PropKey_TTIMsgAuto = "tti-msg-auto";
    public final static String PropKey_TTIMsgTransmit = "tti-msg-transmit";
    public final static String PropKey_TTIMsgExpected = "tti-msg-expected";
    public final static String PropKey_OperationMode = "operationMode";
    public final static String PropKey_ExistedOnNe = "existedOnNe";
    public final static String PropValue_True = "true";
    private Map<String, NodeBuilder> phyNodeMap;

    private Map<String, NodeBuilder> todoPhyNodeMap;
    private Map<String, NodeBuilder> donePhyNodeMap;

    private Map<String, Node> dbNodeMap;  //for better performance, cache
    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link> phyLinkList;
    private ImplementState target;
    private ImplConfig implConfig;

    public YangRoute(ImplConfig implConfig) {
        dbNodeMap = new HashMap<>();
        phyNodeMap = new HashMap<>();
        todoPhyNodeMap = new HashMap<>();
        donePhyNodeMap = new HashMap<>();
        phyLinkList = new LinkedList<>();
        this.implConfig = implConfig;
    }

    public void parse(List<Route> routeList, ImplementState target) throws CommonException {
        this.target = target;

        List<ExplicitRouteObjects> eroList = new LinkedList<>();
        List<CrossConnections> xcList = new LinkedList<>();

        for (Route route : routeList) {
            eroList.addAll(route.getPrimary().getExplicitRouteObjects());
            xcList.addAll(route.getPrimary().getCrossConnections());

            if (route.getSecondary() != null) {
                eroList.addAll(route.getSecondary().getExplicitRouteObjects());
                xcList.addAll(route.getSecondary().getCrossConnections());
            }
        }

        processEro(eroList);
        processXc(xcList);
    }

    /**
     * this link is one OTS link, generate related node, and link
     *
     * @param phyLinkId
     */
    public void parse(String phyLinkId, ImplementState target) {
        this.target = target;

        parsePhyLink(phyLinkId);
    }

    public void parse(String tunnelId, List<Route> routeList, ImplementState target) {
        this.target = target;
        parseTunnel(tunnelId, routeList);
    }

    private void parseTunnel(String tunnelId, List<Route> routeList) {
        TpHop hop = new TpHopBuilder()
                .setTpRef(new TpId(PhysicalLinkIdNamingRule.getTpAId(tunnelId)))
                .setNodeRef(new NodeId(PhysicalTpIdNamingRule
                        .getNodeId(PhysicalLinkIdNamingRule.getTpAId(tunnelId))))
                .setEquipmentRef(PhysicalTpIdNamingRule
                        .getEquipId(PhysicalLinkIdNamingRule.getTpAId(tunnelId)))
                .setSiteRef(new NodeId(PhysicalTpIdNamingRule
                        .getSiteId(PhysicalLinkIdNamingRule.getTpAId(tunnelId))))
                .build();
        constructPhyNode(hop, false);

        hop = new TpHopBuilder()
                .setTpRef(new TpId(PhysicalLinkIdNamingRule.getTpZId(tunnelId)))
                .setNodeRef(new NodeId(PhysicalTpIdNamingRule
                        .getNodeId(PhysicalLinkIdNamingRule.getTpZId(tunnelId))))
                .setEquipmentRef(PhysicalTpIdNamingRule
                        .getEquipId(PhysicalLinkIdNamingRule.getTpZId(tunnelId)))
                .setSiteRef(new NodeId(PhysicalTpIdNamingRule
                        .getSiteId(PhysicalLinkIdNamingRule.getTpZId(tunnelId))))
                .build();
        constructPhyNode(hop, false);

        List<CrossConnections> xcList = new LinkedList<>();
        for (Route route : routeList) {
            xcList.addAll(route.getPrimary().getCrossConnections());
        }
        processXc(xcList);
    }

    private void processXc(List<CrossConnections> xcList) throws CommonException {
        for (CrossConnections xc : xcList) {
            constructPhyNode(xc);
        }
    }

    private String getNewXcId4MPO(String xcId) {
        String[] parts = xcId.split("-Site-");
        for (int j = 0, len = parts.length; j < len; j++) {
            if (parts[j].endsWith(Constants.LAG_SUFFIX)) {
                StringBuffer tmp = new StringBuffer("");
                for (int i = 1; i <= 8; i++) {
                    if (i == 1) {
                        tmp.append(
                                parts[j].replace(Constants.LAG_SUFFIX, Constants.MPO_PREFIX) + i);
                    } else {
                        tmp.append("-Site-" + parts[j].replace(Constants.LAG_SUFFIX,
                                Constants.MPO_PREFIX) + i);
                    }
                }
                parts[j] = tmp.toString();
            }
        }

        String newXcId = String.join("-Site-", parts);
        return newXcId;
    }

    private CrossConnections preprocessXc(CrossConnections xc) {
        String xcId = xc.getCrossConnectionId().getValue();
        if (!xcId.contains(Constants.LAG_SUFFIX)) {
            return xc;
        } else {
            String newXcId = getNewXcId4MPO(xcId);
            List<SourceTp> sTps = xc.getSourceTp();
            Optional<SourceTp> optStp = sTps.stream()
                    .filter(t -> t.getTpRef().getValue().endsWith(Constants.LAG_SUFFIX)).findAny();
            if (optStp.isPresent()) {
                sTps.removeIf(t -> t.getTpRef().getValue().endsWith(Constants.LAG_SUFFIX));
                for (int i = 1; i <= Constants.LAG_SIZE; i++) {
                    String newTpId = optStp.get().getTpRef().getValue()
                            .replace(Constants.LAG_SUFFIX, Constants.MPO_PREFIX + i);
                    SourceTpBuilder stb = new SourceTpBuilder(optStp.get()).setTpRef(
                            new TpId(newTpId));
                    sTps.add(stb.build());
                }
            }

            List<DestinationTp> dTps = xc.getDestinationTp();
            Optional<DestinationTp> optDtp = dTps.stream()
                    .filter(t -> t.getTpRef().getValue().endsWith(Constants.LAG_SUFFIX)).findAny();
            if (optDtp.isPresent()) {
                dTps.removeIf(t -> t.getTpRef().getValue().endsWith(Constants.LAG_SUFFIX));
                for (int i = 1; i <= Constants.LAG_SIZE; i++) {
                    String newTpId = optDtp.get().getTpRef().getValue()
                            .replace(Constants.LAG_SUFFIX, Constants.MPO_PREFIX + i);
                    DestinationTpBuilder dtb = new DestinationTpBuilder(optDtp.get()).setTpRef(
                            new TpId(newTpId));
                    dTps.add(dtb.build());
                }
            }
            return new CrossConnectionsBuilder(xc)
                    .setCrossConnectionId(new Uri(newXcId))
                    .setSourceTp(sTps)
                    .setDestinationTp(dTps)
                    .build();
        }
    }

    private void constructPhyNode(CrossConnections origXc) throws CommonException {
        String tpId = origXc.getSourceTp().get(0).getTpRef().getValue();
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);

        CrossConnections xc = preprocessXc(origXc);
        NodeBuilder nb = phyNodeMap.get(nodeId);
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> existedXcList = nb
                .getAugmentation(Node1.class).getPhysical().getCrossConnections();

        boolean found = false;
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections existedXc : existedXcList) {
            if (existedXc.getCrossConnectionId().getValue()
                    .equals(xc.getCrossConnectionId().getValue())) {
                found = true;
                break;
            }
        }
        if (!found) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections newXc = newXc(
                    xc);
            if (newXc != null) {
                existedXcList.add(newXc);
            }
        }
    }

    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections newXc(
            CrossConnections xc) throws CommonException {
        String tpId = xc.getSourceTp().get(0).getTpRef().getValue();
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);

        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> existedXcList = dbNodeMap
                .get(nodeId).getAugmentation(Node1.class).getPhysical().getCrossConnections();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections existedXc = null;
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections tmpXc : existedXcList) {
            if (tmpXc.getCrossConnectionId().getValue()
                    .equals(xc.getCrossConnectionId().getValue())) {
                existedXc = tmpXc;
                break;
            }
        }
        if (existedXc == null) {
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
//                    "cannot find crossConnection used by route " + xc.getCrossConnectionId());
            return null;
        }
        if (target.equals(existedXc.getImplementState())) {
            return null;
        }

        if (existedXc.getProperties() != null && existedXc.getProperties().getProperty() != null
                && !existedXc.getProperties().getProperty().isEmpty()) {
            for (Property property : existedXc.getProperties().getProperty()) {
                if (PropKey_ExistedOnNe.equals(property.getName())) {
                    if (!PropValue_True.equals(property.getValue())) {
                        return null;
                    }
                }
            }
        }

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder xcBuilder
                = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder()
                .setKey(existedXc.getKey())
                .setCrossConnectionId(existedXc.getCrossConnectionId())
                .setDestinationTp(existedXc.getDestinationTp())
                .setSourceTp(existedXc.getSourceTp())
                .setFixed(existedXc.isFixed())
                .setWssChannel(existedXc.getWssChannel())
                .setAps(existedXc.getAps())
                .setAmplifier(existedXc.getAmplifier());

        xcBuilder.setProperties(new PropertiesBuilder().setProperty(new LinkedList<>()).build());

        if (target.equals(ImplementState.Implement)) {
            return xcBuilder.setAdminState(AdminStatus.Up)
                    .setImplementState(ImplementState.Implement)
                    .build();
        } else {
            if (xcBuilder.getAps() != null) {
                return xcBuilder
                        .setAps(new ApsBuilder().setName(xc.getAps().getName())
                                .setForceToPort(ApsPath.NONE).build())
                        .setAdminState(AdminStatus.Down)
                        .setImplementState(ImplementState.Allocate)
                        .build();
            } else {
                return xcBuilder.setAdminState(AdminStatus.Down)
                        .setImplementState(ImplementState.Allocate)
                        .build();
            }
        }
    }

    private void processEro(List<ExplicitRouteObjects> eroList) throws CommonException {
        for (ExplicitRouteObjects ero : eroList) {
            if (ero.getExplicitRouteUsage().equals(RouteUsageInclude.class)) {
                List<PathRouteObject> proList = ero.getPathRouteObject();
                for (PathRouteObject pro : proList) {
                    if (pro.getResourceType().getImplementedInterface().getName()
                            .equals(Link.class.getName())) {
                        LinkHop hop = ((Link) pro.getResourceType()).getLinkHop();
                        if (hop.getTopologyRef().getValue()
                                .equals(OtnPhyTopology.QNAME.getLocalName())) {
                            constructPhyLink(hop);
                        }
                    } else if (pro.getResourceType().getImplementedInterface().getName()
                            .equals(Tp.class.getName())) {
                        TpHop hop = ((Tp) pro.getResourceType()).getTpHop();
                        constructPhyNode(hop, true, pro.getTopologyRef());
                    }
                }
            }
        }
    }

    //  "index": 9,
//  "topology-ref": "otn-phy-topology",
//  "tp-hop": {
//    "tp-ref": "Site-1614738646049#Ne-1614738679619#LINECARD-1-1#PORT-1-1-LINE",
//    "site-ref": "Site-1614738646049",
//    "node-ref": "Ne-1614738679619",
//    "equipment-ref": "Site-1614738646049#Ne-1614738679619#LINECARD-1-1"
//  }
    private NodeBuilder constructPhyNode(TpHop hop, boolean withEqp) throws CommonException {
        String nodeId = hop.getNodeRef().getValue();
        if (!nodeId.contains(Constant.HASH_TAG)) {
            nodeId = hop.getSiteRef().getValue() + Constant.HASH_TAG + hop.getNodeRef().getValue();
        }
        NodeBuilder nb = prepareNode(nodeId);

        addTp(nb, hop);
        addTransceiver(nb, hop);
        if (withEqp) {
            addEq(nb, hop);
        }

        return nb;
    }

    private NodeBuilder constructPhyNode(TpHop hop, boolean withEqp, TopologyId topoId)
            throws CommonException {
        String nodeId = hop.getNodeRef().getValue();
        if (!nodeId.contains(Constant.HASH_TAG)) {
            nodeId = hop.getSiteRef().getValue() + Constant.HASH_TAG + hop.getNodeRef().getValue();
        }
        NodeBuilder nb = prepareNode(nodeId);

        addTp(nb, hop, topoId);
        addTransceiver(nb, hop);
        if (withEqp) {
            addEq(nb, hop);
        }

        return nb;
    }

    private NodeBuilder prepareNode(String nodeId) {
        if (!dbNodeMap.containsKey(nodeId)) {
            PhyNodeDao mongoDaoUtil = SpringBeanFinder.getBean(PhyNodeDao.class);
            dbNodeMap.put(nodeId, mongoDaoUtil.getConfigPhyNodeById(nodeId));
        }

        NodeBuilder nb;
        if (!phyNodeMap.containsKey(nodeId)) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical dbPhysical
                    = dbNodeMap.get(nodeId).getAugmentation(Node1.class).getPhysical();
            nb = new NodeBuilder()
                    .setNodeId(new NodeId(nodeId))
                    .setTerminationPoint(new LinkedList<>())
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(
                                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder()
                                            .setAdminState(dbPhysical.getAdminState())
                                            .setImplementState(dbPhysical.getImplementState())
                                            .setFriendlyName(dbPhysical.getFriendlyName())
                                            .setIp(dbPhysical.getIp())
                                            .setOCMGripGroups(dbPhysical.getOCMGripGroups())
                                            .setNodeType(dbPhysical.getNodeType())
                                            .setCrossConnections(new LinkedList<>())
                                            .setEquipments(new LinkedList<>())
                                            .setInternalLinks(new LinkedList<>())
                                            .setProperties(new PropertiesBuilder()
                                                    .setProperty(new LinkedList<>()).build())
                                            .build())
                            .build());
            phyNodeMap.put(nodeId, nb);
        }
        nb = phyNodeMap.get(nodeId);
        return nb;
    }

    private void addEq(NodeBuilder nb, TpHop hop) {
        List<Equipments> eqList = nb.getAugmentation(Node1.class).getPhysical().getEquipments();
        boolean found = false;
        for (Equipments eq : eqList) {
            if (eq.getEquipmentId().endsWith(hop.getEquipmentRef())) {
                found = true;
                break;
            }
        }

        if (!found) {
            Equipments newEq = newEq(nb.getNodeId().getValue(), hop.getEquipmentRef());
            if (newEq != null) {
                nb.getAugmentation(Node1.class).getPhysical().getEquipments().add(newEq);
            }
        }
    }

    private void addTransceiver(NodeBuilder nb, TpHop hop) throws CommonException {
        Physical tpPhysical = getTpPhyscialFromDB(nb.getNodeId().getValue(),
                hop.getTpRef().getValue());
        if (tpPhysical == null) { //in case of MPO
            return;
        }
        String transceiverId = PhysicalTpIdNamingRule.getTransceiverId(
                hop.getTpRef().getValue(),
                tpPhysical.getPortType());
        if (transceiverId == null) {
            return;
        }

        List<Equipments> eqList = nb.getAugmentation(Node1.class).getPhysical().getEquipments();
        boolean found = false;
        for (Equipments eq : eqList) {
            if (eq.getEquipmentId().equals(transceiverId)) {
                found = true;
                break;
            }
        }

        if (!found) {
            Equipments newTransceiver = newEq(nb.getNodeId().getValue(), transceiverId);
            if (newTransceiver != null) {
                nb.getAugmentation(Node1.class).getPhysical().getEquipments().add(newTransceiver);
            }
        }
    }

    private Equipments newEq(String nodeId, String equipId) throws CommonException {
        Equipments existedEquip = getEquipmentsFromDB(nodeId, equipId);

        if (target.equals(existedEquip.getImplementState())) {
            return null;
        }

        EquipmentsBuilder eb = new EquipmentsBuilder()
                .setEquipmentId(existedEquip.getEquipmentId())
                .setFriendlyName(existedEquip.getFriendlyName())
                .setEquipType(existedEquip.getEquipType());
        eb.setProperties(new PropertiesBuilder().setProperty(new LinkedList<>()).build());
        if (target.equals(ImplementState.Implement)) {
            return eb.setAdminState(AdminStatus.Up)
                    .setImplementState(ImplementState.Implement)
                    .build();
        } else {
            return eb.setAdminState(AdminStatus.Down)
                    .setImplementState(ImplementState.Allocate)
                    .build();
        }
    }

    private Equipments getEquipmentsFromDB(String nodeId, String equipId) throws CommonException {
        Equipments existedEquip = null;
        List<Equipments> existedEqList = dbNodeMap.get(nodeId).getAugmentation(Node1.class)
                .getPhysical().getEquipments();
        for (Equipments eq : existedEqList) {
            if (eq.getEquipmentId().endsWith(equipId)) {
                existedEquip = eq;
                break;
            }
        }
        if (existedEquip == null) {

//      throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "cannot find equipment used by route " + equipId);
            //here is temp code
            String fullEqId = nodeId + Constants.POUND + equipId;
            return new EquipmentsBuilder()
                    .setEquipmentId(fullEqId)
                    .setKey(new EquipmentsKey(fullEqId))
                    .build();
        }
        return existedEquip;
    }

    private void addTp(NodeBuilder nb, TpHop hop) throws CommonException {
        addTp(nb, hop.getTpRef().getValue());
    }

    private void addTp(NodeBuilder nb, String tpId) throws CommonException {
        boolean found = false;
        for (TerminationPoint tp : nb.getTerminationPoint()) {
            if (tp.getTpId().getValue().equals(tpId)) {
                found = true;
                break;
            }
        }
        if (!found) {
            TerminationPoint newTp = newTp(nb.getNodeId().getValue(), tpId);
            if (newTp != null) {
                nb.getTerminationPoint().add(newTp);
            }
        }
    }

    private void addTp(NodeBuilder nb, TpHop hop, TopologyId topoId) throws CommonException {
        List<String> tpIds = new ArrayList<String>();
        tpIds.add(hop.getTpRef().getValue());
        if (hop.getTpRef().getValue().endsWith(Constants.LAG_SUFFIX)) {
            if (topoId.getValue().equals(OtnPhyTopology.QNAME.getLocalName())) {
                tpIds.removeIf(t -> t.equals(hop.getTpRef().getValue()));
                for (int i = 1; i <= Constants.LAG_SIZE; i++) {
                    tpIds.add(hop.getTpRef().getValue()
                            .replace(Constants.LAG_SUFFIX, Constants.MPO_PREFIX + i));
                }
            }
        }

        for (String tpId : tpIds) {
            addTp(nb, tpId);
        }
    }

    private TerminationPoint newTp(String nodeId, String tpId) throws CommonException {
        Physical existedTpPhy = getTpPhyscialFromDB(nodeId, tpId);
        if (existedTpPhy == null) { //in case of MPO
            return null;
        }
        if (target.equals(existedTpPhy.getImplementState())) {
            return null;
        }

        PhysicalBuilder tpBuilder = new PhysicalBuilder(existedTpPhy);
        if (existedTpPhy.getPortType() == PortType.OTULine
                || existedTpPhy.getPortType() == PortType.OTUClient) {
            List<Property> pros = new ArrayList<Property>();
            for (Property pro : existedTpPhy.getProperties().getProperty()) {
                if (!pro.getName().equals(PropKey_TTIMsgAuto) && !pro.getName()
                        .equals(PropKey_TTIMsgExpected) && !pro.getName()
                        .equals(PropKey_TTIMsgTransmit) && !pro.getName().equals(PropKey_ErrorMsg)
                        && !pro.getName().equals(PropKey_Impl) && !pro.getName()
                        .equals(PropKey_DeImpl)) {
                    pros.add(pro);
                }
            }
//            if (existedTpPhy.getPortType() == PortType.OTULine) {
//                pros.add(new PropertyBuilder().setName(PropKey_OperationMode)
//                        .setValue("{\"mode-id\":13}")
//                        .build());
//            }
            tpBuilder.setProperties(new PropertiesBuilder().setProperty(pros).build());
        } else {
            if (existedTpPhy != null && existedTpPhy.getProperties() != null) {
                tpBuilder.setProperties(
                        new PropertiesBuilder().setProperty(
                                        existedTpPhy.getProperties().getProperty())
                                .build());
            }
        }

        if (target.equals(ImplementState.Implement)) {
            tpBuilder.setAdminState(AdminStatus.Up)
                    .setImplementState(ImplementState.Implement)
                    .build();
        } else {
            tpBuilder.setAdminState(AdminStatus.Down)
                    .setImplementState(ImplementState.Allocate)
                    .build();
        }
        return new TerminationPointBuilder()
                .setTpId(new TpId(tpId))
                .setKey(new TerminationPointKey(new TpId(tpId)))
                .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                        .setPhysical(tpBuilder.build())
                        .build())
                .build();
    }

    private Physical getTpPhyscialFromDB(String nodeId, String tpId) throws CommonException {
        Physical existedTpPhy = null;
        for (TerminationPoint tp : dbNodeMap.get(nodeId).getTerminationPoint()) {
            if (tp.getTpId().getValue().equals(tpId)) {
                existedTpPhy = tp.getAugmentation(TerminationPoint1.class).getPhysical();
                break;
            }
        }

        if (existedTpPhy == null) {
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
//                    "cannot find TP used by route " + tpId);
            log.error("cannot find TP used by route " + tpId);
        }
        return existedTpPhy;
    }

    //  "index": 14,
//  "topology-ref": "otn-phy-topology",
//  "link-hop": {
//    "topology-ref": "otn-phy-topology",
//    "link-ref": "OMS-Site-1614738646049#Ne-1614738679619#LINECARD-1-3#PORT-1-3-LAG-Site-1614738646049#Ne-1614738679619#MUXPANEL-1-50#PORT-1-50-LAG"
//  }
    private void constructPhyLink(LinkHop hop) throws CommonException {
        //based on this hop value, construct NE's internal link, and change physical link's attribute.
        String hotLinkId = hop.getLinkRef().getValue();

        List<String> linkIds = new ArrayList<String>();
        if (hotLinkId.contains(Constants.LAG_SUFFIX) && hotLinkId.endsWith(Constants.LAG_SUFFIX)) {
            for (int i = 1; i <= Constants.LAG_SIZE; i++) {
                linkIds.add(hotLinkId.replace(Constants.LAG_SUFFIX, Constants.MPO_PREFIX + i));
            }
        } else {
            linkIds.add(hotLinkId);
        }
        for (String linkId : linkIds) {
            String nodeAId = PhysicalLinkIdNamingRule.getNodeAId(linkId);
            String nodeZId = PhysicalLinkIdNamingRule.getNodeZId(linkId);

            NodeBuilder nbA = prepareNode(nodeAId);
            NodeBuilder nbZ = prepareNode(nodeZId);

            addInternalLink(nbA, linkId);
            addInternalLink(nbZ, linkId);

            addPhyscialLink(linkId);
        }
    }

    private void addInternalLink(NodeBuilder nb, String linkId) {
        List<InternalLinks> ilList = nb.getAugmentation(Node1.class).getPhysical()
                .getInternalLinks();
        boolean found = false;
        for (InternalLinks il : ilList) {
            if (il.getLinkRef().equals(linkId)) {
                found = true;
                break;
            }
        }
        if (!found) {
            InternalLinks newIl = newInternalLink(nb.getNodeId().getValue(), linkId);
            if (newIl != null) {
                nb.getAugmentation(Node1.class).getPhysical().getInternalLinks().add(newIl);
            }
        }

    }

    private InternalLinks newInternalLink(String nodeId, String linkId) throws CommonException {
        InternalLinks existedIl = getInternalLinkFromDB(nodeId, linkId);

        if (existedIl == null) {
            return null;
        }
        if (target.equals(existedIl.getImplementState())) {
            return null;
        }

        String srcTmp[] = existedIl.getSrcTp().split("#");
        String desTmp[] = existedIl.getDstTp().split("#");
        String srcNodeId = PhysicalTpIdNamingRule.getNodeId(existedIl.getSrcTp());
        String desNodeId = PhysicalTpIdNamingRule.getNodeId(existedIl.getDstTp());

        Node operSrcNode = dbNodeMap.get(srcNodeId);
        Node operdesNode = dbNodeMap.get(desNodeId);

        String aTp = null;
        String zTp = null;
        if (this.implConfig.isWriteWithoutIP()) {
//        	if (NodeType.OPC4.equals(operSrcNode.getAugmentation(Node1.class).getPhysical().getNodeType())) {
            aTp = operSrcNode.getNodeId().getValue() + "," + srcTmp[3];
//        	} else {
//        		if (operSrcNode.getAugmentation(Node1.class).getPhysical().getIp() == null) {
//                		throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "src node " +  operSrcNode.getNodeId().getValue() + "has no ip address");
//                	}
//        	        aTp =
//        	                operSrcNode.getAugmentation(Node1.class).getPhysical().getIp()
//        	                        + "," + srcTmp[3];
//        	}
        } else {
            if (operSrcNode.getAugmentation(Node1.class).getPhysical().getIp() == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "src node " + operSrcNode.getNodeId().getValue() + "has no ip address");
            }
            aTp =
                    operSrcNode.getAugmentation(Node1.class).getPhysical().getIp()
                            + "," + srcTmp[3];
        }

        if (this.implConfig.isWriteWithoutIP()) {
//        	if (NodeType.OPC4.equals(operdesNode.getAugmentation(Node1.class).getPhysical().getNodeType())) {
            zTp = operdesNode.getNodeId().getValue() + "," + desTmp[3];
//        	} else {
//        		if (operdesNode.getAugmentation(Node1.class).getPhysical().getIp() == null) {
//                		throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "dest node " +  operdesNode.getNodeId().getValue() + "has no ip address");
//                	}
//        	        zTp =
//        	        		operdesNode.getAugmentation(Node1.class).getPhysical().getIp()
//        	                        + "," + desTmp[3];
//        	}
        } else {
            if (operdesNode.getAugmentation(Node1.class).getPhysical().getIp() == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "dest node " + operdesNode.getNodeId().getValue() + "has no ip address");
            }
            zTp =
                    operdesNode.getAugmentation(Node1.class).getPhysical().getIp()
                            + "," + desTmp[3];
        }
        List<String> tpIds = new ArrayList<String>();
        tpIds.add(aTp);
        tpIds.add(zTp);
        String connId = createId(tpIds);
        List<Property> pros = new ArrayList<Property>();
        Property pro = new PropertyBuilder().setName("link-indicator").setValue(connId)
                .build();
        pros.add(pro);

        InternalLinksBuilder eb = new InternalLinksBuilder()
                .setDstTp(existedIl.getDstTp())
                .setLinkName(existedIl.getLinkName())
                .setLinkType(existedIl.getLinkType())
                .setLinkRef(existedIl.getLinkRef())
                .setProperties(new PropertiesBuilder().setProperty(pros).build())
                .setSrcTp(existedIl.getSrcTp());

        if (target.equals(ImplementState.Implement)) {
            return eb.setAdminState(AdminStatus.Up)
                    .setImplementState(ImplementState.Implement)
                    .build();
        } else {
            return eb.setAdminState(AdminStatus.Down)
                    .setImplementState(ImplementState.Allocate)
                    .build();
        }
    }

    private InternalLinks getInternalLinkFromDB(String nodeId, String linkId)
            throws CommonException {
        InternalLinks existedIl = null;
        List<InternalLinks> existedIlList = dbNodeMap.get(nodeId).getAugmentation(Node1.class)
                .getPhysical().getInternalLinks();
        for (InternalLinks il : existedIlList) {
            if (il.getLinkRef().equals(linkId)) {
                existedIl = il;
                break;
            }
        }
//        if (existedIl == null) {
//             throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
//                     "cannot find internal link used by route " + linkId);
//         }
        return existedIl;
    }

    private void addPhyscialLink(String linkId) throws CommonException {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder pb =
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder();
        if (target.equals(ImplementState.Implement)) {
            pb.setAdminState(AdminStatus.Up)
                    .setImplementState(ImplementState.Implement);
        } else {
            pb.setAdminState(AdminStatus.Down)
                    .setImplementState(ImplementState.Allocate);
        }
        phyLinkList.add(new LinkBuilder()
                .setLinkId(new LinkId(linkId))
                .setKey(new LinkKey(new LinkId(linkId)))
                .addAugmentation(Link1.class, new Link1Builder()
                        .setPhysical(pb.build())
                        .build())
                .build());
    }

    public List<NodeBuilder> getPhyNodeList() {
        List<NodeBuilder> tmp = new LinkedList<>();
        for (NodeBuilder nb : phyNodeMap.values()) {
            tmp.add(nb);
        }
        return tmp;
    }

    public List<NodeBuilder> getToDoPhyNodeList() {
        List<NodeBuilder> tmp = new LinkedList<>();
        for (NodeBuilder nb : todoPhyNodeMap.values()) {
            tmp.add(nb);
        }
        return tmp;
    }

    public List<NodeBuilder> getDonePhyNodeList() {
        List<NodeBuilder> tmp = new LinkedList<>();
        for (NodeBuilder nb : donePhyNodeMap.values()) {
            tmp.add(nb);
        }
        return tmp;
    }

    private void parsePhyLink(String phyLinkId) {
        TpHop hop = new TpHopBuilder()
                .setTpRef(new TpId(PhysicalLinkIdNamingRule.getTpAId(phyLinkId)))
                .setNodeRef(new NodeId(PhysicalLinkIdNamingRule.getNodeAId(phyLinkId)))
                .setEquipmentRef(PhysicalTpIdNamingRule
                        .getEquipId(PhysicalLinkIdNamingRule.getTpAId(phyLinkId)))
                .setSiteRef(new NodeId(PhysicalTpIdNamingRule
                        .getSiteId(PhysicalLinkIdNamingRule.getTpAId(phyLinkId))))
                .build();
        NodeBuilder nbA = constructPhyNode(hop, true);

        if (!PhysicalLinkIdNamingRule.getNodeAId(phyLinkId)
                .equals(PhysicalLinkIdNamingRule.getNodeZId(phyLinkId))) {//external link
            hop = new TpHopBuilder()
                    .setTpRef(new TpId(PhysicalLinkIdNamingRule.getTpZId(phyLinkId)))
                    .setNodeRef(new NodeId(PhysicalLinkIdNamingRule.getNodeZId(phyLinkId)))
                    .setEquipmentRef(PhysicalTpIdNamingRule
                            .getEquipId(PhysicalLinkIdNamingRule.getTpZId(phyLinkId)))
                    .setSiteRef(new NodeId(PhysicalTpIdNamingRule
                            .getSiteId(PhysicalLinkIdNamingRule.getTpZId(phyLinkId))))
                    .build();

            NodeBuilder nbZ = constructPhyNode(hop, true);

            addInternalLink(nbZ, phyLinkId);
        }

        addInternalLink(nbA, phyLinkId);

        addPhyscialLink(phyLinkId);
    }

    private void sortList(List<String> list) {
        Collections.sort(list, new SortByIndex());
    }

    private String createId(List<String> list) {
        sortList(list);
        String id = "";
        for (int i = 0; i < list.size(); i++) {
            if (i == 0) {
                id = list.get(i);
            } else {
                id = id + "-" + list.get(i);
            }
        }
        log.debug(String.format("create id[%s]", id));
        return id;
    }

    static class SortByIndex implements Comparator<Object> {

        @Override
        public int compare(Object arg0, Object arg1) {
            String str1 = (String) arg0;
            String str2 = (String) arg1;
            return str1.compareTo(str2);
        }
    }
}
