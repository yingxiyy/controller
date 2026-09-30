/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel.reg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.RegInput;
import net.flex.dci.otn.controller.allocate.designer.model.tunnel.RegSegment;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import net.flex.dci.otn.controller.allocate.designer.tunnel.OtXcService;
import net.flex.dci.otn.controller.allocate.ne.Card;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.OchRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.OchRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.och.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.och.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.och.route.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.by.och.och.route.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route._2.Links;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route._2.LinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route._2.LinksKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RegOchRepo {

    @Autowired
    private RegNodeRepo regNodeRepo;
    @Autowired
    private LinkRepo linkRepo;
    @Autowired
    private NodeUtils nodeUtils;
    @Autowired
    private NeNodeRepo neNodeRepo;
    @Autowired
    private OtXcService otXcService;

    protected OchRoute allocateOchRoute(RegInput regInput, Map<String, Node> totalNodesMap) throws NeDesignerException {

        //create primary
        Pair<Primary, Pair<String, String>> primaryRoute = allocateOchPrimary(regInput, totalNodesMap);
        String ochSrcTp = primaryRoute.getRight().getLeft();
        String ochDstTp = primaryRoute.getRight().getRight();
        Primary primary = primaryRoute.getLeft();
        OchRouteBuilder ochRouteBuilder = new OchRouteBuilder().setSourceTp(ochSrcTp).setDestTp(ochDstTp).setPrimary(primary);

        //create secondary
        if (regInput.isProtected()) {
            Secondary secondary = allocateOchSecondary(regInput, totalNodesMap);
            ochRouteBuilder.setSecondary(secondary);
        }

        return ochRouteBuilder.build();
    }

    private Secondary allocateOchSecondary(RegInput regInput, Map<String, Node> totalNodesMap) throws NeDesignerException {
        List<RegSegment> secondarySegments = regInput.getSecondarySegments();
        List<Link> links = new ArrayList<>();
        List<CrossConnections> ochXcs = new ArrayList<>();
        Set<String> busyTpSet=new HashSet<>();
        for (RegSegment segment : secondarySegments) {
            String srcNodeId = segment.getSrcNodeId();
            String destNodeId = segment.getDestNodeId();
            allocateOchBasicRoute(regInput, totalNodesMap, links, ochXcs, segment, srcNodeId, destNodeId,busyTpSet);
        }
        return new SecondaryBuilder().setCrossConnections(ochXcs).setLinks(yangConvertToLinksList(links)).build();
    }

    /**
     * @param regInput
     * @param totalNodesMap
     * @return return primary and ochStartTpId,ochEndEpId pair
     * @throws NeDesignerException
     */
    private Pair<Primary, Pair<String, String>> allocateOchPrimary(RegInput regInput, Map<String, Node> totalNodesMap) throws NeDesignerException {
        List<RegSegment> primarySegments = regInput.getPrimarySegments();
        int segSize = primarySegments.size();
        List<Link> links = new ArrayList<>();
        List<CrossConnections> ochXcs = new ArrayList<>();
        String ochStartTpId = null;
        String ochEndTpId = null;
        Set<String> busyTpSet=new HashSet<>();
        for (int i = 0; i < segSize; i++) {
            RegSegment segment = primarySegments.get(i);
            String srcNodeId = segment.getSrcNodeId();
            String destNodeId = segment.getDestNodeId();

            //create equip,tp,link,internal link,xc
            Pair<TerminationPoint, TerminationPoint> pairTps = allocateOchBasicRoute(regInput, totalNodesMap, links, ochXcs, segment, srcNodeId, destNodeId,busyTpSet);
            TerminationPoint srcTp = pairTps.getLeft();
            String srcTpId = srcTp.getTpId().getValue();
            TerminationPoint destTp = pairTps.getRight();
            String destTpId = destTp.getTpId().getValue();

            //create op start xc, if protected
            if (i == 0) {
                if (!isLPortTp(srcTp)) {
                    log.error("Invalid segment:{}, the source tp of the fist segment should be {}, but is: {}", segment, PortType.OTULine, srcTp);
                    throw new NeDesignerException("The source tp of the fist segment is not " + PortType.OTULine.name());
                }
                ochStartTpId = srcTpId;
                if (regInput.isProtected()) {
                    //find or create start OP xc
                    if (!isSigPort(destTp)) {
                        log.error("Invalid segment:{}, the first segment dest tp should be {}, but is: {}", segment, PortType.OPSig, destTp);
                        throw new NeDesignerException("The source tp of the fist segment is not " + PortType.OPSig.name());
                    }
                    CrossConnections startOp6Xc = createOPXc(destTpId, destNodeId, totalNodesMap);
                    ochXcs.add(startOp6Xc);
                }
            }
            //create op end xc, if protected
            if (ochEndTpId == null && destTpId.contains(regInput.getDestSite()) && isLPortTp(destTp)) {//two scenario: 1. Sig-L1, 2, L1-L1
                ochEndTpId = destTpId;
                if (regInput.isProtected()) {
                    //find or create start OP xc
                    if (!isSigPort(srcTp)) {
                        log.error("Invalid segment:{}, the dest site segment src tp should be {}, but is: {}", segment, PortType.OPSig, srcTp);
                        throw new NeDesignerException("The source tp of the fist segment is not " + PortType.OPSig.name());
                    }
                    CrossConnections endOp6Xc = createOPXc(srcTpId, srcNodeId, totalNodesMap);
                    ochXcs.add(endOp6Xc);
                }
            }

        }

        if (ochEndTpId == null) {
            throw new NeDesignerException("Failed to find ochEndTpId by primary segments.");
        }
        Primary primary = new PrimaryBuilder().setCrossConnections(ochXcs).setLinks(yangConvertToLinksList(links)).build();
        return Pair.of(primary, Pair.of(ochStartTpId, ochEndTpId));
    }

    private Pair<TerminationPoint, TerminationPoint> allocateOchBasicRoute(RegInput regInput, Map<String, Node> totalNodesMap, List<Link> links, List<CrossConnections> ochXcs, RegSegment segment,
            String srcNodeId, String destNodeId,Set<String> busyTpSet) throws NeDesignerException {
        //update source
        TerminationPoint srcTp = regNodeRepo.getOrCreateCardEquipAndTp(srcNodeId, segment.getSrcTpFriendlyName(), totalNodesMap, regInput,busyTpSet);
        String srcTpId = srcTp.getTpId().getValue();

        //update dest
        TerminationPoint destTp = regNodeRepo.getOrCreateCardEquipAndTp(destNodeId, segment.getDestTpFriendlyName(), totalNodesMap, regInput, busyTpSet);
        String destTpId = destTp.getTpId().getValue();

        //create link
        Link osLink = linkRepo.createLink(srcTpId, destTpId, LinkType.OsLink, Collections.EMPTY_LIST, LinkDirection.Unidirection);
        links.add(osLink);

        //create internal link
        createInternalLink(srcNodeId, totalNodesMap, osLink);
        if (!srcNodeId.equals(destNodeId)) {
            createInternalLink(destNodeId, totalNodesMap, osLink);
        }

        //create L-L och xc
        if (nodeUtils.isLPortTp(srcTp) && nodeUtils.isLPortTp(destTp)) {
            ochXcs.add(createLPortOchXc(srcTpId, srcNodeId, totalNodesMap));
            ochXcs.add(createLPortOchXc(destTpId, destNodeId, totalNodesMap));
        }

        return Pair.of(srcTp, destTp);
    }

    private boolean isSigPort(TerminationPoint tp) {
        return tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OPSig);
    }

    private CrossConnections createOPXc(String opSigPortTpId, String nodeId, Map<String, Node> totalNodesMap) throws NeDesignerException {

        Node node = totalNodesMap.get(nodeId);

        //validate existed XC
        List<CrossConnections> existedXcs = nodeUtils.getXcs(node);
        Optional<CrossConnections> existedOptional = existedXcs.stream()
                .filter(xc -> xc.getSourceTp().get(0).getTpRef().getValue().equals(opSigPortTpId) || xc.getDestinationTp().get(0).getTpRef().getValue().equals(opSigPortTpId)).findAny();
        if (existedOptional.isPresent()) {
            String msg = String.format("Xc:%s existed already.", existedOptional.get().getCrossConnectionId().getValue());
            throw new NeDesignerException(msg);
        }

        //not found exited xc, continue  to create
        Card opCardInfo = nodeUtils.getCardInfoByTpId(node, opSigPortTpId);
        CrossConnections newOp6Xc = otXcService.createOp6XCs(opSigPortTpId, node, opCardInfo).get(0);//实际只有一条

        //fresh node
        existedXcs.add(newOp6Xc);
        node = neNodeRepo.refreshNodeByXcs(node, existedXcs);
        totalNodesMap.put(nodeId, node);

        return newOp6Xc;

    }

    private CrossConnections createLPortOchXc(String lPortTpId, String nodeIp, Map<String, Node> totalNodesMap) throws NeDesignerException {

        //fetch node
        Node node = totalNodesMap.get(nodeIp);
        String nodeId = node.getNodeId().getValue();
        Card otCardInfo = nodeUtils.getCardInfoByTpId(node, lPortTpId);
        CrossConnections newOchXc = otXcService.createOchXc(nodeId, lPortTpId, otCardInfo);

        List<CrossConnections> existedXcs = nodeUtils.getXcs(node);
        Optional<CrossConnections> existed = existedXcs.stream().filter(xc -> xc.getCrossConnectionId().getValue().equals(newOchXc.getCrossConnectionId().getValue())).findAny();
        if (existed.isPresent()) {
            return existed.get();
        }

        //fresh node
        existedXcs.add(newOchXc);
        node = neNodeRepo.refreshNodeByXcs(node, existedXcs);
        totalNodesMap.put(nodeIp, node);
        return newOchXc;
    }

    private void createInternalLink(String nodeId, Map<String, Node> totalNodesMap, Link osLink) {
        Node node = totalNodesMap.get(nodeId);
        InternalLinks newInternalLink = linkRepo.createInternalLink(node.getNodeId().getValue(), osLink);
        List<InternalLinks> existedInternalLinks = nodeUtils.getInternalLinks(node);
        Optional<InternalLinks> existed = existedInternalLinks.stream().filter(internalLink -> internalLink.getLinkRef().equals(newInternalLink)).findAny();
        if (existed.isPresent()) {
            return;
        }

        //fresh node
        existedInternalLinks.add(newInternalLink);
        node = neNodeRepo.refreshNode(node, existedInternalLinks);
        totalNodesMap.put(nodeId, node);
    }

    private boolean isLPortTp(TerminationPoint tp) {
        return tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OTULine);
    }

    private List<Links> yangConvertToLinksList(List<Link> links) {
        if (links == null) {
            return null;
        }

        return links.stream().map(item -> yangConvertToLinks(item)).collect(Collectors.toList());
    }

    private Links yangConvertToLinks(Link link) {
        return new LinksBuilder().setLinkId(link.getLinkId())
                .setKey(new LinksKey(link.getKey().getLinkId()))
                .setSource(link.getSource())
                .setDestination(link.getDestination())
                .setPhysical(link.getAugmentation(Link1.class).getPhysical())
                .build();
    }

}
