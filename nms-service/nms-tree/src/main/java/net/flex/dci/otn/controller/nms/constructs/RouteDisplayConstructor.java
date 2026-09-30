/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.constructs;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.LinkRole;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.display.info.PhyLink;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.display.info.PhyLinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.display.info.Site;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.display.info.SiteBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.display.info.SiteKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.display.info.site.PhyNeBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.display.info.site.PhyNeKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.display.info.site.phy.ne.EquipmentBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.display.info.site.phy.ne.EquipmentKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.display.info.site.phy.ne.equipment.TpBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.display.info.site.phy.ne.equipment.TpKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.display.output.RouteDisplayInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.route.display.output.RouteDisplayInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.Tp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.tp.hop.PhyNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.tp.hop.PhyTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.tp.tp.hop.SiteNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.sequence.RouteSequence;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;

/**
 * @date: 2021/3/31
 */
@Slf4j
public class RouteDisplayConstructor {

    private List<RouteInfo> routes;
    private List<Site> displaySiteList;
    private List<CrossConnections> displayXcList;
    private List<PhyLink> displayLinkList;
    private NetconfTopology netconfTopology;
    private Map<String, List<PhyNeBuilder>> siteNeMap;  //key is site id
    private Map<String, List<EquipmentBuilder>> neEquipMap; //key is ne id
    private Map<String, List<TpBuilder>> equipTpMap; //key is equip id

    private Map<String, PhyNeBuilder> neMap;
    private Map<String, EquipmentBuilder> equipMap;
    Map<String, TpBuilder> tpMap;
    List<PhyTp> tpSequence;
    List<SiteBuilder> siteSequence;

    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link linkObj;
    private org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel tunnelObj;


    public RouteDisplayConstructor(
            NetconfTopology netconfTopology,
            TopologyId topologyRef, LinkId linkRef, Uri tunnelRef,
            List<RouteInfo> routes) throws Exception {
        this.routes = routes;
        this.netconfTopology = netconfTopology;

        if ((topologyRef.getValue().equals(Constants.SITE_TOPO_KEY) && linkRef != null) || (
                topologyRef.getValue().equals(Constants.PHY_TOPO_KEY) && linkRef != null)) {
            String linkId = linkRef.getValue();
            linkObj = netconfTopology.getSiteLink(linkId);
        } else if (topologyRef.getValue().equals(Constants.SITE_TOPO_KEY) && tunnelRef != null) {
            tunnelObj = netconfTopology.getTunnel(tunnelRef);
        }
        this.displayLinkList = new LinkedList<>();
        this.displaySiteList = new LinkedList<>();
        this.displayXcList = new LinkedList<>();
        this.siteNeMap = new HashMap<>();
        this.neEquipMap = new HashMap<>();
        this.equipTpMap = new HashMap<>();
        this.neMap = new HashMap<>();
        this.equipMap = new HashMap<>();
        this.tpMap = new HashMap<>();

        this.tpSequence = new LinkedList<>();
        this.siteSequence = new LinkedList<>();
        loadResource();
    }


    /**
     * load display data
     */
    private void loadResource() throws Exception {
        List<Link> linkList = new LinkedList<>();
        for (RouteInfo ri : routes) {
            extractedRouteInfo(ri.getPrimary().getRouteSequence(), linkList, ri, LinkRole.Main);
            displayXcList.addAll(ri.getPrimary().getCrossConnections());
            linkList.clear();
            if (ri.getSecondary() != null) {
                extractedRouteInfo(ri.getSecondary().getRouteSequence(), linkList, ri,
                        LinkRole.Spare);
                displayXcList = ri.getSecondary().getCrossConnections();
            }
        }
    }

    private void extractedRouteInfo(List<RouteSequence> rsl, List<Link> linkList, RouteInfo ri,
            LinkRole role) throws Exception {

        for (RouteSequence rs : rsl) {
            if (rs.getResourceType() != null) {
                String className = rs.getResourceType().getImplementedInterface().getName();
                if (className.equals(Tp.class.getName())) {
                    processTp((Tp) rs.getResourceType());
                }
                if (className.equals(Link.class.getName())) {
                    linkList.add((Link) rs.getResourceType());
                }
            }
        }
        processSequence(ri.getIndex(), role, linkList);
    }

    /**
     * process sequence
     *
     * @param index
     * @param role
     * @param linkList
     */
    private void processSequence(Short index, LinkRole role, List<Link> linkList) throws Exception {
        log.info("process sequence for the link role {}", role);
        int seq = 0;
        boolean left = true;
        for (Link link : linkList) {
            if (seq == 0 && tunnelObj != null) {
                left = true;
                //this is tunnel end points.
                TpBuilder tb = tpMap.get(tpSequence.get(seq).getTpId().getValue());
                tb.setIsLeaf(left);
                seq++;
                left = !left;
            } else if (seq == 0 && tunnelObj == null) {
                left = false;
            }

            if (link.getLinkHop().getSource().getSourceTp().getValue()
                    .equals(tpSequence.get(seq).getTpId().getValue())) {
                TpBuilder tb = tpMap.get(tpSequence.get(seq).getTpId().getValue());
                tb.setIsLeaf(left);
                seq++;
                left = !left;
                if (link.getLinkHop().getDestination().getDestTp().getValue()
                        .equals(tpSequence.get(seq).getTpId().getValue())) {
                    tb = tpMap.get(tpSequence.get(seq).getTpId().getValue());
                    tb.setIsLeaf(left);
                    seq++;
                } else {
                    throw new Exception("bad data format the route info is wrong.");
                }
            } else if (link.getLinkHop().getDestination().getDestTp().getValue()
                    .equals(tpSequence.get(seq).getTpId().getValue())) {
                TpBuilder tb = tpMap.get(tpSequence.get(seq).getTpId().getValue());
                tb.setIsLeaf(left);
                seq++;
                left = !left;
                if (link.getLinkHop().getSource().getSourceTp().getValue()
                        .equals(tpSequence.get(seq).getTpId().getValue())) {
                    tb = tpMap.get(tpSequence.get(seq).getTpId().getValue());
                    tb.setIsLeaf(left);
                    seq++;
                } else {
                    throw new Exception("bad data format the route info is wrong.");
                }
            } else {
                throw new Exception("bad data format the route info is wrong.");
            }

            PhyLinkBuilder pb = new PhyLinkBuilder();
            pb.fieldsFrom(link.getLinkHop());
            if (index == 0) {
                pb.setLinkRole(role);
            } else {
                if (role.equals(LinkRole.Main)) {
                    pb.setLinkRole(LinkRole.PreAllocatedMain);
                } else {
                    pb.setLinkRole(LinkRole.PreAllocatedSpare);
                }
            }

            displayLinkList.add(pb.build());
        }
    }

    private void processTp(Tp tpObject) {
        SiteNode site = tpObject.getTpHop().getSiteNode();
        String siteKey = site.getNodeId().getValue();

        PhyNode ne = tpObject.getTpHop().getPhyNode();
        String neKey = ne.getNodeId().getValue();

        Equipments equips = tpObject.getTpHop().getPhyNode().getPhysical().getEquipments().get(0);
        String equipKey = equips.getEquipmentId();

        PhyTp tp = tpObject.getTpHop().getPhyTp();
        String tpKey = tp.getTpId().getValue();

        this.tpSequence.add(tp);

        if (!siteNeMap.containsKey(siteKey)) {
            //this is new site.
            newSite(tpObject);
        } else if (!neEquipMap.containsKey(neKey)) {
            newNe(tpObject);
        } else if (!equipTpMap.containsKey(equipKey)) {
            newEquip(tpObject);
        } else if (!tpMap.containsKey(tpKey)) {
            // this tp related parent is exist, add it into tpMap only.
            newTp(tpObject);
        }
    }

    private void newTp(Tp tpObject) {
        String tpName = tpObject.getTpHop().getPhyTp().getPhysical().getFriendlyName();
        log.debug("process new tp by tp.{}", tpName);
        Equipments equip = tpObject.getTpHop().getPhyNode().getPhysical().getEquipments().get(0);
        String equipKey = equip.getEquipmentId();

        PhyTp tp = tpObject.getTpHop().getPhyTp();
        String tpKey = tp.getTpId().getValue();

        List<TpBuilder> tpList;

        if (equipTpMap.containsKey(tpKey)) {
            tpList = equipTpMap.get(tpKey);
        } else {
            tpList = new LinkedList<>();
        }

        TpBuilder tb = new TpBuilder();
        tb.fieldsFrom(tp);
        tb.setKey(new TpKey(tp.getTpId()));
        tpList.add(tb);
        equipTpMap.put(equipKey, tpList);
        tpMap.put(tpKey, tb);
    }

    private void newEquip(Tp tpObject) {
        String tpName = tpObject.getTpHop().getPhyTp().getPhysical().getFriendlyName();
        log.debug("process new equip by tp.{}", tpName);

        PhyNode ne = tpObject.getTpHop().getPhyNode();
        String neKey = ne.getNodeId().getValue();

        Equipments equip = tpObject.getTpHop().getPhyNode().getPhysical().getEquipments().get(0);
        String equipKey = equip.getEquipmentId();

        List<EquipmentBuilder> eqList;

        if (this.neEquipMap.containsKey(neKey)) {
            eqList = this.neEquipMap.get(neKey);
        } else {
            eqList = new LinkedList<>();
        }
        EquipmentBuilder eb = new EquipmentBuilder();
        eb.fieldsFrom(equip);
        eb.setKey(new EquipmentKey(equip.getEquipmentId()));
        eqList.add(eb);
        this.neEquipMap.put(neKey, eqList);
        this.equipMap.put(equipKey, eb);

        newTp(tpObject);
    }

    private void newNe(Tp tpObject) {
        String tpName = tpObject.getTpHop().getPhyTp().getPhysical().getFriendlyName();
        log.debug("process new ne by tp.{}", tpName);

        SiteNode site = tpObject.getTpHop().getSiteNode();
        String siteKey = site.getNodeId().getValue();

        PhyNode ne = tpObject.getTpHop().getPhyNode();
        String neKey = ne.getNodeId().getValue();

        List<PhyNeBuilder> neList;
        if (this.siteNeMap.containsKey(siteKey)) {
            neList = this.siteNeMap.get(siteKey);
        } else {
            neList = new LinkedList<>();
        }
        PhyNeBuilder nb = new PhyNeBuilder();
        nb.fieldsFrom(ne);
        nb.setKey(new PhyNeKey(ne.getNodeId()));
        neList.add(nb);
        this.siteNeMap.put(siteKey, neList);
        this.neMap.put(neKey, nb);

        newEquip(tpObject);
    }

    private void newSite(Tp tpObject) {
        log.debug("process new site by tp  {}",
                tpObject.getTpHop().getPhyTp().getPhysical().getFriendlyName());
        SiteNode site = tpObject.getTpHop().getSiteNode();

        SiteBuilder sb = new SiteBuilder();
        sb.fieldsFrom(site);
        sb.setKey(new SiteKey(site.getNodeId()));
        this.siteSequence.add(sb);
        newNe(tpObject);
    }


    /**
     * dispalay route info
     *
     * @return
     */
    public List<RouteDisplayInfo> extractedRouteInfo() {
        RouteDisplayInfoBuilder rb = new RouteDisplayInfoBuilder();
        rb.setCrossConnections(this.displayXcList);
        rb.setPhyLink(this.displayLinkList);
        rb.setSite(this.displaySiteList);

        for (SiteBuilder sb : siteSequence) {
            sb.setPhyNe(new LinkedList<>());
            for (PhyNeBuilder pb : siteNeMap.get(sb.getNodeId().getValue())) {
                pb.setEquipment(new LinkedList<>());
                for (EquipmentBuilder eb : neEquipMap.get(pb.getNodeId().getValue())) {
                    eb.setTp(new LinkedList<>());
                    for (TpBuilder tb : equipTpMap.get(eb.getEquipmentId())) {
                        eb.getTp().add(tb.build());
                    }
                    pb.getEquipment().add(eb.build());
                }
                sb.getPhyNe().add(pb.build());
            }
            displaySiteList.add(sb.build());
        }
        rb.setIndex((short) 0);
        List<RouteDisplayInfo> output = new LinkedList<>();
        output.add(rb.build());
        return output;
    }
}
