/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.site;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.designer.model.site.WssNetworkInfo;
import net.flex.dci.otn.controller.allocate.designer.model.site.WssNetworkInput;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.TpRepo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.RoadmService;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NetworkRepo {

    @Autowired
    private LinkRepo linkRepo;
    @Autowired
    private TpRepo tpRepo;
    @Autowired
    private SiteNodeDao siteNodeDao;
    @Autowired
    private NodeUtils nodeUtils;

    @Autowired
    private RoadmService roadmService;

    public WssNetworkInfo allocateWssConnection(WssNetworkInput input) throws NeDesignerException {
        //prepare
        String siteId = input.getSiteId();
        Map<String, Node> totalNodesMap = input.getTotalNodesMap();
        RouteInfo linkA = input.getLinkA();
        RouteInfo linkZ = input.getLinkZ();

        //pick wss tp
        Pair<Node, String> aWssPair = geWssNodeEquipPair(linkA, totalNodesMap, siteId);//todo:现在是规则定死的，不支持指定port连接。所以暂时不用比如：input.getLinkAPort()
        Pair<Node, String> zWssPair = geWssNodeEquipPair(linkZ, totalNodesMap, siteId);
        Optional<Pair<TerminationPoint, TerminationPoint>> tpPairOpt = roadmService.pickAvailableTpPair(
                aWssPair.getLeft(), aWssPair.getRight(),
                zWssPair.getLeft(), zWssPair.getRight(),
                input.getUsedDimensions()
        );
        if (!tpPairOpt.isPresent()) {
            String msg = String.format("Failed to allocate roadm for site:%s, between sitelink:%s, and sitelink:%s, because no available wssTP.",
                    siteId, input.getLinkAName(), input.getLinkZName());
            throw new NeDesignerException(msg);
        }

        Pair<TerminationPoint, TerminationPoint> wssTpPair = tpPairOpt.get();
        TerminationPoint aWssTp = wssTpPair.getLeft();
        TerminationPoint zWssTp = wssTpPair.getRight();

        if (nodeUtils.isSameEquip(aWssTp, zWssTp)) {
            log.error("Failed to get available wssTp ,because linkA and linkZ have the same wssEquip. {}", input);
            String msg = String.format("Invalid compute output, because linkA:%s and linkZ%s have the same wssEquip. Check log for details.", input.getLinkAName(), input.getLinkZName());
            throw new NeDesignerException(msg);
        }

        //create wss link,internalLink
        List<String> siteLinks = Arrays.asList(input.getLinkAName(), input.getLinkZName());
        String siteName = siteNodeDao.getSiteFriendlyName(siteId);
        String aWssTpId = aWssTp.getTpId().getValue();
        String zWssTpId = zWssTp.getTpId().getValue();
        String aNodeId = PhysicalTpIdNamingRule.getNodeId(aWssTpId);
        String zNodeId = PhysicalTpIdNamingRule.getNodeId(zWssTpId);

        Link newWssLink = linkRepo.createLink(aWssTpId, zWssTpId, LinkType.WssLink, siteLinks);
        InternalLinks aNodeInternalLink = linkRepo.createInternalLink(aNodeId, newWssLink);
        InternalLinks zNodeInternalLink = linkRepo.createInternalLink(zNodeId, newWssLink);
        //set tp busy
        TerminationPoint busyANodeTp = tpRepo.getBusyTp(aWssTp);
        TerminationPoint busyZNodeTp = tpRepo.getBusyTp(zWssTp);

        //update routeInfo for link a
        Node newANode = getUpdatedNode(aNodeId, aNodeInternalLink, busyANodeTp, totalNodesMap);
        Node newZNode = getUpdatedNode(zNodeId, zNodeInternalLink, busyZNodeTp, totalNodesMap);
        if (aNodeId.equals(zNodeId)) {
            newANode = newZNode;
        }
        updatedRouteInfo(input.getLinkAName(), linkA, newANode);
        updatedRouteInfo(input.getLinkZName(), linkZ, newZNode);
        return WssNetworkInfo.builder().wssLink(newWssLink).linkA(linkA).linkZ(linkZ).totalNodesMap(totalNodesMap).build();

    }

    private void updatedRouteInfo(@NonNull String linkName, RouteInfo siteLink, Node updatedNode) throws NeDesignerException {
        List<Node> nodes = siteLink.getMain().getNodes();
        int size = nodes.size();
        String updateNodeId = updatedNode.getNodeId().getValue();
        for (int i = 0; i < size; i++) {
            Node node = nodes.get(i);
            if (node.getNodeId().getValue().equals(updateNodeId)) {
                nodes.set(i, updatedNode);
                return;
            }
        }
        throw new NeDesignerException(String.format("Internal error, not found node:%s in %s", updateNodeId, linkName));
    }

    private Pair<Node, String> geWssNodeEquipPair(RouteInfo siteLink, Map<String, Node> totalNodesMap, String siteId) throws NeDesignerException {
        //get node
        Optional<Node> nodeOptional = siteLink.getMain().getNodes().stream().filter(node -> node.getNodeId().getValue().contains(siteId)).findAny();
        if (!nodeOptional.isPresent()) {
            log.error("Failed to geWssNodeEquipPair, because {} is not included in siteLink:{}", siteId, siteLink);
            throw new NeDesignerException("Failed to geWssNodeEquipPair, because invalid input for siteLinkRelation for site:" + siteId);
        }
        String nodeId = nodeOptional.get().getNodeId().getValue();
        Node node = totalNodesMap.get(nodeId);

        //get IRA equip
        Optional<Equipments> siteLinkInvolvedEquipOptioanl = node.getAugmentation(Node1.class).getPhysical().getEquipments().stream().filter(item -> item.getEquipType().equals(EquipType.IRA))
                .findAny();//todo:现在只需要考虑IRA

        if (!siteLinkInvolvedEquipOptioanl.isPresent()) {
            throw new NeDesignerException("Failed to get wss equip id for node:" + nodeId);
        }

        return Pair.of(node, siteLinkInvolvedEquipOptioanl.get().getEquipmentId());
    }

    private boolean isWssTpAvailable(String portName, TerminationPoint tp) throws NeDesignerException {
        String tpId = tp.getTpId().getValue();
        Boolean isIdle = isIdleWssTp(tp);

        //对于指定了portName的，不是该port，返回false；是该port并且当idle即返回true，否则抛出异常
        if (tp != null && portName != null && !portName.isEmpty()) {
            if (!tpId.endsWith(portName)) {
                return false;
            }
            if (!isIdle) {
                log.error("Specified wss port:{} is not IDLE in TP:{}", portName, tp);
                throw new NeDesignerException(String.format("Specified wss port:%s is not IDLE in TP:%s", portName, tpId));
            }
            return true;
        }

        //对于没有指定port的，是否available取决于是否idle
        return isIdle;

    }


    private Node getUpdatedNode(String nodeId, InternalLinks newInternalLink, TerminationPoint busyTp, Map<String, Node> totalNodesMap) {
        Node node = totalNodesMap.get(nodeId);
        List<InternalLinks> internalLinks = node.getAugmentation(Node1.class).getPhysical().getInternalLinks();
        internalLinks.add(newInternalLink);

        String busyTpId = busyTp.getTpId().getValue();
        List<TerminationPoint> tps = node.getTerminationPoint();
        tps.replaceAll(tp -> tp.getTpId().getValue().equals(busyTpId) ? busyTp : tp);

        PhysicalBuilder physicalBuilder = new PhysicalBuilder(node.getAugmentation(Node1.class).getPhysical()).setInternalLinks(internalLinks);

        Node1 phyNode = new Node1Builder().setPhysical(physicalBuilder.build()).build();

        Node outputNode = new NodeBuilder()
                .setNodeId(node.getNodeId())
                .setKey(new NodeKey(node.getNodeId()))
                .addAugmentation(Node1.class, phyNode)
                .setTerminationPoint(tps)
                .build();
        totalNodesMap.put(nodeId, outputNode);

        return outputNode;
    }


    private boolean isIdleWssTp(TerminationPoint tp) {
        Physical physical = tp.getAugmentation(TerminationPoint1.class).getPhysical();
        if (physical == null) {
            log.error("Invalid tp:{}, because has null physical.", tp.getTpId().getValue());
            return false;
        }
        return physical.getPortType().equals(PortType.WSSMesh) && physical.getConnectionStatus().equals(ConnectionStatus.Idle);
    }

    private ImmutablePair<Integer, Node> getConnectedNode(RouteInfo linkA, String siteId, Map<String, Node> totalNodesMap) throws NeDesignerException {
        int linkANodeSize = linkA.getMain().getNodes().size();
        Integer changedNodeIndex = null;
        Node changedNode = null;
        for (int i = 0; i < linkANodeSize; i++) {
            Node node = linkA.getMain().getNodes().get(i);
            if (node.getNodeId().getValue().contains(siteId)) {
                changedNode = node;
                changedNodeIndex = i;
                break;
            }
        }
        if (changedNode == null || changedNodeIndex == null) {
            throw new NeDesignerException("Failed to find node in site: " + siteId);
        }
        Node reuseNode = totalNodesMap.get(changedNode.getNodeId().getValue());
        if (reuseNode != null) {
            changedNode = reuseNode;
        }

        return new ImmutablePair<>(changedNodeIndex, changedNode);
    }
}
