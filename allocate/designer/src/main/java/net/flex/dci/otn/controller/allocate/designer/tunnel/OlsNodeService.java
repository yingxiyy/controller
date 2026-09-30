/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.JsonOutputer;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.TpRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.XCRepo;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OlsNodeService {

    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    private LinkRepo linkRepo;

    @Autowired
    private TpRepo tpRepo;

    @Autowired
    private XCRepo xcRepo;


    @Autowired
    private JsonOutputer jsonOutputer;

    /**
     * 1. add internal Link
     * <p>
     * 2. set tp busy
     * <p>
     * 3. add xc
     *
     * @param olsNode
     * @param osLinks
     * @param olsTpId
     * @param newXcs
     * @return
     * @throws NeDesignerException
     */
    public Node updatedOlsNode(Node olsNode, List<Link> osLinks, String olsTpId, List<CrossConnections> newXcs) {
        //add internal Link
        List<InternalLinks> olsInternalLinks = olsNode.getAugmentation(Node1.class)
                .getPhysical()
                .getInternalLinks();

        for (Link osLink : osLinks) {
            if (osLink == null) {
                continue;
            }
            //这种情况是，当光电复用的时候，可能这个internalLink已经在处理OT那边加入进来了，此处就不需要加了
            boolean isInternalLinkExisted = olsInternalLinks
                    .stream()
                    .filter(item -> item.getLinkRef().equals(osLink.getLinkId().getValue()))
                    .findAny()
                    .isPresent();

            if (!isInternalLinkExisted) {
                InternalLinks olsInternalLink = linkRepo.createInternalLink(olsNode.getNodeId().getValue(), osLink);
                olsInternalLinks.add(olsInternalLink);
            }
        }

        //set mux TP busy
        List<TerminationPoint> olsTps = new ArrayList<>();
        olsTps.addAll(olsNode.getTerminationPoint());

        for (int i = 0; i < olsTps.size(); i++) {
            if (olsTpId.equals(olsTps.get(i).getTpId().getValue())) {
                TerminationPoint newOlsTp = tpRepo.getBusyTp(olsTps.get(i));
                olsTps.set(i, newOlsTp);
                log.debug("Set OLS TP busy: {}", newOlsTp);
            }
        }

        //Add new xcs
        List<CrossConnections> xcs = new ArrayList<>();
        xcs.addAll(olsNode.getAugmentation(Node1.class).getPhysical().getCrossConnections());
        xcs.addAll(newXcs);

        List<Equipments> equipments = new ArrayList<>(olsNode.getAugmentation(Node1.class).getPhysical().getEquipments());

        //update Ols node
        Physical olsPhysical = new PhysicalBuilder(
                olsNode.getAugmentation(Node1.class).getPhysical())
                .setInternalLinks(olsInternalLinks)
                .setCrossConnections(xcs)
                .setEquipments(equipments)
                .build();
        Node1 olsPhyNode = new Node1Builder().setPhysical(olsPhysical).build();
        Node newOlsNode = new NodeBuilder().setNodeId(olsNode.getNodeId())
                .setKey(new NodeKey(olsNode.getNodeId())).setTerminationPoint(olsTps)
                .addAugmentation(Node1.class, olsPhyNode).build();
//        log.trace("Update ols node as :{}", jsonOutputer.formatNode(newOlsNode));
        return newOlsNode;
    }

    public TerminationPoint getTpByTpId(Node node, String tpId) throws NeDesignerException {
        try {
            return node.getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().equals(tpId)).findFirst().get();
        } catch (NoSuchElementException e) {
            String msg = String.format("Failed to get TP for node: %s, by tpId: %s", node.getNodeId().getValue(), tpId);
            log.error(msg);
            throw new NeDesignerException(msg);
        }
    }

    public Available getAvailableByTp(TerminationPoint tp) {
        BigInteger lowerFrequency = new BigInteger("0");
        BigInteger upperFrequency = new BigInteger("0");

        if (tp.getAugmentation(TerminationPoint1.class) != null
                && tp.getAugmentation(TerminationPoint1.class).getPhysical() != null
                && tp.getAugmentation(TerminationPoint1.class).getPhysical().getProperties()
                != null && tp.getAugmentation(TerminationPoint1.class).getPhysical().getProperties()
                .getProperty() != null && !tp.getAugmentation(TerminationPoint1.class).getPhysical()
                .getProperties().getProperty().isEmpty()) {
            for (Property pro : tp.getAugmentation(TerminationPoint1.class).getPhysical()
                    .getProperties().getProperty()) {
                if (pro.getName().equalsIgnoreCase("slot")) {
                    String tmp[] = pro.getValue().split("="); //eg: "/frequency=191375,191425"
                    String frequency[] = tmp[1].split(",");
                    lowerFrequency = new BigInteger(frequency[0]);
                    upperFrequency = new BigInteger(frequency[1]);
                }
            }
        }

        Available ret = new AvailableBuilder()
                .setLowerFrequency(new FrequencyType(lowerFrequency))
                .setUpperFrequency(new FrequencyType(upperFrequency))
                .build();
        return ret;
    }

    public CrossConnections createXC(CrossConnection crossConnection, String nodeId, String srcTpId, List<String> destTpId, String frequencySlot, BigInteger cenFrequency) throws NeDesignerException {
        List<SourceTp> sTPs = new ArrayList<>();
        sTPs.add(new SourceTpBuilder().setTpRef(new TpId(srcTpId))
                .setSlot(frequencySlot).build());

        List<DestinationTp> dTPs = destTpId.stream().map(item -> new DestinationTpBuilder().setTpRef(new TpId(item))
                .setSlot(frequencySlot).build()).collect(Collectors.toList());

        List<String> tpIdList = xcRepo.getTpIdListWithCentralFrequency(sTPs, dTPs, cenFrequency);//目前只有这一处要求xcId加上中心频率
        String fromPort = PhysicalTpIdNamingRule.getPortNameByTpId(srcTpId);
        String toPort = null;
        if (destTpId.size() == 1) {
            toPort = PhysicalTpIdNamingRule.getPortNameByTpId(destTpId.get(0));
        }
        String description = xcRepo.getXCDescription(crossConnection, srcTpId, fromPort, toPort, cenFrequency.toString());
        return xcRepo.createXC(tpIdList, nodeId, crossConnection, sTPs, dTPs, false, description);//tunnel里面创建的交叉，都不需要care是否保护，所以isProtected设置成false
    }

    /**
     * Add new XC to node, if not existed
     *
     * @param node
     * @param newXcs
     */
    public void mergeXCs(Node node, List<CrossConnections> newXcs) {
        List<CrossConnections> xcs = node.getAugmentation(Node1.class).getPhysical().getCrossConnections();
        Set<String> existedXcIds = xcs.stream().map(xc -> xc.getCrossConnectionId().getValue()).collect(Collectors.toSet());

        Boolean hasAdded = false;
        for (CrossConnections newXc : newXcs) {
            if (existedXcIds.contains(newXc.getCrossConnectionId().getValue())) {
                log.debug("xc:{} exists already, no need to add to node.", newXc.getCrossConnectionId().getValue());
                continue;
            }
            hasAdded = true;
            xcs.add(newXc);
        }

        //create updated Ols node
        if (hasAdded) {
            Physical physical = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical())
                    .setCrossConnections(xcs)
                    .build();
            Node1 phyNode = new Node1Builder().setPhysical(physical).build();
            Node newNode = new NodeBuilder().setNodeId(node.getNodeId())
                    .setKey(new NodeKey(node.getNodeId()))
                    .addAugmentation(Node1.class, phyNode).build();

            log.trace("Merging OLS node :{}", jsonOutputer.formatNode(newNode));
            phyNodeDao.saveConfigPhyNode(newNode);
        } else {
            log.debug("No new xc created for node: {}.", node.getNodeId().getValue());
        }
    }

    public CrossConnections createOchXC(String nodeId, String expTpId, Card iraCardInfo, Long cenFrequency, String frequencyString) throws NeDesignerException {
        String expPortName = PhysicalTpIdNamingRule.getShortPortName(expTpId);
        CrossConnection xcInfo = NeInfoUtil.getOchXcInfo(iraCardInfo, expPortName);
        String lineTId = expTpId.replace(expPortName, xcInfo.getTo().getPort());
        return createXC(xcInfo, nodeId, expTpId, Arrays.asList(lineTId), frequencyString, new BigInteger(String.valueOf(cenFrequency)));

    }
}
