/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.constructs;

import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_TOPO_KEY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.type.DataStoreType;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.SiteLink;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.SiteTunnel;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.OtdrPortDirection;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otn.phy.topology.type.OtnPhyTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Topology1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Component;

/**
 * @date: 2021/4/12
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NEResourceHolder {

    private final NetconfTopology netconfTopology;


    public void removeResourceByOrderId(String orderId) throws Exception {
        List<Node> nodes = getTopoNodes(
                orderId, true);
        Iterator<Node> nodeIter = nodes
                .iterator();
        while (nodeIter.hasNext()) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node node = nodeIter
                    .next();
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1 phyNode = node
                    .getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class);
            Iterator<Equipments> eqIter = phyNode.getPhysical().getEquipments().iterator();
            while (eqIter.hasNext()) {
                Equipments eq = eqIter.next();
                if (eq.getOrderId().size() == 1 && eq.getOrderId().get(0).equals(orderId)) {
                    removeEquipOrder(node, eq, orderId);
                } else {
                    netconfTopology.removeEquip(node, eq);
                    eqIter.remove();
                }
            }
            if (phyNode.getPhysical().getEquipments().isEmpty()) {
                netconfTopology.removeNode(node);
                nodeIter.remove();
            } else {
                removeNodeOrder(node, orderId);
            }
        }
    }

    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link> getLinks(
            String orderId, boolean isRecycle) throws Exception {
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link> ret = new ArrayList<>();
        Set<Link> set = new HashSet<Link>();

        if (isRecycle) {
            //when recycle flag = true, the resource cannot be used in any link
            set.addAll(this.getPhyLinkResourceForSiteLink(orderId));
            if (!set.isEmpty()) {
                return ret;
            }

            set.addAll(this.getPhyLinkResourceForTunnel(orderId));
            if (!set.isEmpty()) {
                return ret;
            }
        }

        set.addAll(this.getPhyLinkResourceForSiteLink(orderId));
        if (set.isEmpty()) {//site link and tunnel orderId cannot be same
            set.addAll(this.getPhyLinkResourceForTunnel(orderId));
        }
        for (Link link : set) {
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link l = this
                    .convertLink(link, orderId);
            if (l != null) {
                ret.add(l);
            }
        }
        log.debug("Finish GetResourceByOrderId {} for link.", orderId);
        return ret;
    }

    private org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link convertLink(
            Link link, String orderId) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 link1 = link
                .getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);
        if (link1 != null
                && link1.getPhysical() != null) {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical phy = link1
                    .getPhysical();
            if (phy.getOrderId() != null
                    && phy.getOrderId().contains(orderId)) {
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.LinkBuilder linkBuilder = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.LinkBuilder();
                linkBuilder.setPhysical(phy);
                linkBuilder
                        .setKey(new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.LinkKey(
                                link.getLinkId(),
                                new TopologyId(OtnPhyTopology.QNAME.getLocalName())));
                return linkBuilder.build();
            }
        } else {
            return null;
        }

        return null;
    }

    private List<Link> getPhyLinkResourceForSiteLink(String orderId) throws Exception {
        List<Link> ret = new ArrayList<Link>();
        Topology topo = netconfTopology.getSiteTopology();
        if (topo != null) {
            List<Link> links = topo.getLink();
            Set<Link> set = new HashSet<Link>();
            if (links != null) {
                SiteLink siteLinkOp = new SiteLink(netconfTopology);
                for (Link link : links) {
                    Link1 link1 = link.getAugmentation(Link1.class);
                    if (link1 != null) {
                        Site site = link1.getSite();
                        if (site != null
                                && site.getOrderId() != null
                                && site.getOrderId().contains(orderId)) {
                            set.addAll(siteLinkOp
                                    .getPhyLinks(new TopologyId(SITE_TOPO_KEY), link.getLinkId()));
                        }
                    }
                }
            }
            ret.addAll(set);
        }
        return ret;
    }

    private List<Link> getPhyLinkResourceForTunnel(String orderId) throws Exception {
        List<Link> ret = new ArrayList<Link>();
        Topology topo = netconfTopology.getSiteTopology();
        if (topo != null) {
            Topology1 topology1 = topo.getAugmentation(Topology1.class);
            if (topology1 != null) {
                List<Tunnel> tunnels = topology1.getTunnel();
                if (tunnels != null) {
                    Set<Link> set = new HashSet<Link>();
                    SiteTunnel siteTunnelOp = new SiteTunnel(netconfTopology);
                    for (Tunnel tunnel : tunnels) {
                        if (tunnel.getOrderId() != null
                                && tunnel.getOrderId().contains(orderId)) {
                            set.addAll(siteTunnelOp
                                    .getPhyLinks(new TopologyId(SITE_TOPO_KEY),
                                            tunnel.getTunnelId().getValue()));
                        }
                    }
                    ret.addAll(set);
                }
            }
        }
        return ret;
    }


    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node> getNodes(
            String orderId, boolean isRecycle) throws Exception {
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node> ret = new ArrayList<>();
        Set<Node> set = new HashSet<Node>();

        //when recycle flag = true, the resource cannot be used in any link
        if (isRecycle) {
            set.addAll(this.getSiteLinkResource(orderId));
            if (!set.isEmpty()) {
                return ret;
            }
            if (!set.isEmpty()) {
                return ret;
            }
        }

        //one orderid cannot existed in siteLink or tunnel
        set.addAll(this.getSiteLinkResource(orderId));
        if (set.isEmpty()) {
            set.addAll(this.getTunnelResource(orderId));
        }

        if (set.isEmpty()) {
            log.debug("the order ID related link (siteLink/tunnel) has been removed.");
            List<Node> phyNodes = netconfTopology
                    .getTopology(new TopologyId(TopoNameConstants.Phy_Topo_Key)).getNode();
            for (Node node : phyNodes) {
                Physical phy = node.getAugmentation(Node1.class).getPhysical();
                if (phy.getOrderId() != null && phy.getOrderId().contains(orderId)) {
                    set.add(node);
                } else {
                    for (Equipments eq : phy.getEquipments()) {
                        if (eq.getOrderId() != null && eq.getOrderId().contains(orderId)) {
                            set.add(node);
                        }
                    }
                }
            }
        }
        for (Node node : set) {
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node n = this
                    .convertNode(node, orderId);
            if (n != null) {
                ret.add(n);
            }
        }
        log.debug("Finish GetResourceByOrderId {} for node.", orderId);
        return ret;
    }

    private org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node convertNode(
            Node node, String orderId) {
        Node1 node1 = node.getAugmentation(Node1.class);
        if (node1 != null && node1.getPhysical() != null) {
            Physical phy = node1.getPhysical();
            if (phy == null) {
                return null;
            } else {
                List<Equipments> equips = this.getEquip(phy, orderId);
                if (equips != null
                        && equips.size() > 0) {
                    NodeBuilder nodeBuilder = new NodeBuilder();
                    nodeBuilder
                            .setKey(new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.NodeKey(
                                    node.getNodeId(),
                                    new TopologyId(OtnPhyTopology.QNAME.getLocalName())));
                    nodeBuilder.setNodeId(node.getNodeId());
                    PhysicalBuilder phyBuilder = new PhysicalBuilder();
                    phyBuilder.setEquipments(equips);
                    phyBuilder.setFriendlyName(
                            phy.getFriendlyName() != null ? phy.getFriendlyName() : "");
                    phyBuilder
                            .setDomainName(phy.getDomainName() != null ? phy.getDomainName() : "");
                    phyBuilder.setLoginName(phy.getLoginName() != null ? phy.getLoginName() : "");
                    phyBuilder.setLoginPasswd(
                            phy.getLoginPasswd() != null ? phy.getLoginPasswd() : "");
                    phyBuilder.setIp(phy.getIp() != null ? phy.getIp()
                            : "127.0.0.1");
                    phyBuilder.setPort(phy.getPort() != null ? phy.getPort() : new PortNumber(1));
                    nodeBuilder.setPhysical(phyBuilder.build());
                    return nodeBuilder.build();
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

    private List<Equipments> getEquip(Physical phy, String orderId) {
        List<Equipments> ret = new ArrayList<Equipments>();
        if (phy != null
                && phy.getEquipments() != null
                && phy.getEquipments().size() > 0) {
            for (Equipments equip : phy.getEquipments()) {
                if (equip.getOrderId() != null
                        && equip.getOrderId().contains(orderId)) {
                    EquipmentsBuilder eb = new EquipmentsBuilder();
                    eb.fieldsFrom(equip);
                    eb.setProperties(this.getEquipProperty(equip.getEquipmentId()));
                    ret.add(eb.build());
                }
            }
        }
        return ret;
    }

    private Properties getEquipProperty(String equipId) {
        String strArr[] = equipId.split("#");
        String siteId = strArr[0];
        String neId = strArr[0] + "#" + strArr[1];
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site site = netconfTopology
                .getSiteNode(siteId).getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                .getSite();
        String friendlyName = site.getFriendlyName() != null ? site.getFriendlyName() : "";
        String rackLocation = "";
        String neLocation = "";
        String slot = "";
        if (strArr[2].indexOf('-') > 0) {
            slot = strArr[2].substring(strArr[2].indexOf('-') + 1);
        }

        if (site.getSupportingRack() != null) {
            boolean isFound = false;
            for (SupportingRack rack : site.getSupportingRack()) {
                if (rack.getSupportingNe() != null) {
                    for (SupportingNe sn : rack.getSupportingNe()) {
                        if (sn.getNodeRef() != null
                                && sn.getNodeRef().getValue() != null
                                && sn.getNodeRef().getValue().equals(neId)) {
                            rackLocation = rack.getLocation() != null ? rack.getLocation() : "";
                            neLocation = sn.getLocation() != null ? sn.getLocation() : "";
                            isFound = true;
                        }
                    }
                }
                if (isFound) {
                    break;
                }
            }
        }

        PropertiesBuilder pb = new PropertiesBuilder();
        List<Property> list = new ArrayList<Property>();
        list.add(this.generateProperty("friendlyName", friendlyName));
        list.add(this.generateProperty("rackLocation", rackLocation));
        list.add(this.generateProperty("neLocation", neLocation));
        list.add(this.generateProperty("slot", slot));
        pb.setProperty(list);
        return pb.build();
    }

    private Property generateProperty(String key, String val) {
        PropertyBuilder builder = new PropertyBuilder();
        builder.setKey(new PropertyKey(key));
        builder.setName(key);
        builder.setValue(val);
        return builder.build();
    }


    private void removeNodeOrder(Node node, String orderId) {
        Node yangNode = netconfTopology.getPhyNode(node.getNodeId().getValue());
        Node1 phyNode = node.getAugmentation(Node1.class);
        Iterator<String> iter = phyNode.getPhysical().getOrderId().iterator();
        while (iter.hasNext()) {
            String existedId = iter.next();
            if (existedId.equals(orderId)) {
                iter.remove();
                break;
            }
        }

        netconfTopology.updateNode(DataStoreType.CONFIG, yangNode);
        yangNode = netconfTopology.getPhyNode(node.getNodeId().getValue());
        phyNode = node.getAugmentation(Node1.class);
        iter = phyNode.getPhysical().getOrderId().iterator();
        while (iter.hasNext()) {
            String existedId = iter.next();
            if (existedId.equals(orderId)) {
                iter.remove();
                break;
            }
        }
        netconfTopology.updateNode(DataStoreType.OPERATIONAL, yangNode);
    }

    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node> getTopoNodes(
            String orderId, boolean isRecycle) throws Exception {
        //when recycle flag = true, the resource cannot be used in any link
        Set<Node> set = new HashSet<>();
        if (isRecycle) {
            set.addAll(this.getSiteLinkResource(orderId));
            if (!set.isEmpty()) {
                throw new Exception(
                        "this order related resource has been used in site link");
            }

            set.addAll(this.getTunnelResource(orderId));
            if (!set.isEmpty()) {
                throw new Exception(
                        "this order related resource has been used in tunnel");
            }
        }

        //one orderid cannot existed in siteLink or tunnel
        set.addAll(this.getSiteLinkResource(orderId));
        if (set.isEmpty()) {
            set.addAll(this.getTunnelResource(orderId));
        }

        if (set.isEmpty()) {
            log.debug("the order ID related link (siteLink/tunnel) has been removed.");
            List<Node> phyNodes = netconfTopology
                    .getTopology(new TopologyId(TopoNameConstants.Phy_Topo_Key)).getNode();
            for (Node node : phyNodes) {
                Physical phy = node.getAugmentation(Node1.class).getPhysical();
                if (phy.getOrderId() != null && phy.getOrderId().contains(orderId)) {
                    set.add(node);
                } else {
                    for (Equipments eq : phy.getEquipments()) {
                        if (eq.getOrderId() != null && eq.getOrderId().contains(orderId)) {
                            set.add(node);
                        }
                    }
                }
            }
        }
        return new ArrayList<>(set);
    }

    private List<Node> getSiteLinkResource(String orderId) throws Exception {
        List<Node> ret = new ArrayList<Node>();
        Topology topo = netconfTopology.getTopology(new TopologyId(SITE_TOPO_KEY));
        if (topo != null) {
            List<Link> links = topo.getLink();
            Set<Node> set = new HashSet<Node>();
            if (links != null) {
                SiteLink siteLinkOp = new SiteLink(netconfTopology);
                for (Link link : links) {
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 link1 = link
                            .getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class);
                    if (link1 != null) {
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site site = link1
                                .getSite();
                        if (site != null
                                && site.getOrderId() != null
                                && site.getOrderId().contains(orderId)) {
                            set.addAll(siteLinkOp
                                    .getPhyNodes(new TopologyId(SITE_TOPO_KEY), link.getLinkId()));
                        }
                    }
                }
            }
            ret.addAll(set);
        }
        return ret;
    }

    private List<Node> getTunnelResource(String orderId) throws Exception {
        List<Node> ret = new ArrayList<Node>();
        Topology topo = netconfTopology.getTopology(new TopologyId(SITE_TOPO_KEY));
        if (topo != null) {
            Topology1 topology1 = topo.getAugmentation(Topology1.class);
            if (topology1 != null) {
                List<Tunnel> tunnels = topology1.getTunnel();
                if (tunnels != null) {
                    Set<Node> set = new HashSet<Node>();
                    SiteTunnel siteTunnelOp = new SiteTunnel(netconfTopology);
                    for (Tunnel tunnel : tunnels) {
                        if (tunnel.getOrderId() != null
                                && tunnel.getOrderId().contains(orderId)) {
                            set.addAll(siteTunnelOp
                                    .getPhyNodes(new TopologyId(SITE_TOPO_KEY),
                                            tunnel.getTunnelId().getValue()));
                        }
                    }
                    ret.addAll(set);
                }
            }
        }
        return ret;

    }

    private void removeEquipOrder(Node node, Equipments eq, String orderId) {

        Equipments yangEq = netconfTopology
                .getEquipment(new TopologyId(TopoNameConstants.Phy_Topo_Key),
                        new NodeId(node.getNodeId()),
                        eq.getEquipmentId());/*op.read(LogicalDatastoreType.CONFIGURATION, opName);*/
        Iterator<String> iter = yangEq.getOrderId().iterator();
        while (iter.hasNext()) {
            String existedId = iter.next();
            if (existedId.equals(orderId)) {
                iter.remove();
                break;
            }
        }
        netconfTopology.updateEquip(node, yangEq, DataStoreType.CONFIG);

        yangEq = netconfTopology
                .getEquipment(new TopologyId(TopoNameConstants.Phy_Topo_Key),
                        new NodeId(node.getNodeId()), eq.getEquipmentId());
        iter = yangEq.getOrderId().iterator();
        while (iter.hasNext()) {
            String existedId = iter.next();
            if (existedId.equals(orderId)) {
                iter.remove();
                break;
            }
        }
        netconfTopology.updateEquip(node, yangEq, DataStoreType.OPERATIONAL);
    }

    public List<TerminationPoint> getScanTpsInSite(NodeId siteRef, PortType portType) {
        return netconfTopology.getTerminationPointByTypeAndRegex(siteRef.getValue() + ".*",
                portType);
    }

    public List<TerminationPoint> getScanTpByTp(String tpId, PortType portType,
            OtdrPortDirection direction) throws CommonException {
        return netconfTopology.getTerminationPoint(tpId, portType, direction);

    }
}
