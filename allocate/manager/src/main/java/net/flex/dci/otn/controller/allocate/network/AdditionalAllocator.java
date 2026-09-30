/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.network;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otn.controller.allocate.common.RouteYangDataConverter;
import net.flex.dci.otn.controller.allocate.common.YangDataConverter;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.model.Route;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteInput;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteNodeInput;
import net.flex.dci.otn.controller.allocate.designer.ne.LinkRepo;
import net.flex.dci.otn.controller.allocate.designer.ne.NeNodeRepo;
import net.flex.dci.otn.controller.allocate.link.common.BomGenerator;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.BomInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.ne.bom.group.info.site.NeBomInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.AllocateNetworkAdditionalInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.AllocateNetworkAdditionalOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.AllocateNetworkAdditionalOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.RoutingType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.network.additional.output.BomInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.network.additional.output.Additional;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.network.additional.output.AdditionalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.network.additional.output.ReusedNodesSnapshot;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.network.additional.output.ReusedNodesSnapshotBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.network.additional.output.ReusedNodesSnapshotKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.Nodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AdditionalAllocator {

    public static final String LINK_MODEL_P2P = "2";
    public static final String EXP8_SUFFIX = "-EXP8";
    @Autowired
    private SiteLinkDao siteLinkDao;
    @Autowired
    private NeDesigner neDesigner;
    @Autowired
    private PhyNodeDao phyNodeDao;
    @Autowired
    private NodeUtils nodeUtils;
    @Autowired
    private LinkRepo linkRepo;
    @Autowired
    private NeNodeRepo neNodeRepo;
    @Autowired
    private BomGenerator bomGenerator;

    public AllocateNetworkAdditionalOutput doIt(AllocateNetworkAdditionalInput input) {
        Link siteLink = siteLinkDao.getSiteLinkById(input.getSiteLinkId());
        validateSiteLink(siteLink);

        try {
            //step1: allocate P2P
            Pair<SiteInput, List<Node>> siteInputPair = prepareAllocateInput(siteLink);
            SiteInput siteInput = siteInputPair.getLeft();
            List<Node> nodeSnapshot = siteInputPair.getRight();
            //先按照正常的P2P2复用段allocate
            RouteInfo info;
            try {
                info = neDesigner.allocateSite(siteInput);
            } catch (NeDesignerException e) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "neDesigner error" + e.getCause().getMessage(), e);
            }
            log.debug("build site link route info done. {}");

            //step2: 再改成additional的配置
            Route addRoute = getAdditionalRouteResource(info, siteLink);
            Additional additional = new AdditionalBuilder()
                    .setCrossConnections(addRoute.getXcs())
                    .setLinks(RouteYangDataConverter.getRouteLinks(addRoute.getLinks()))
                    .setNodes(RouteYangDataConverter.getRouteNodes(addRoute.getNodes()))
                    .build();

            //step3 BOM
            BomInfo bomInfo;
            try {
                bomInfo = constructBomInfo(info, siteInput.getVendorName(), siteInput.getVendorType(), nodeSnapshot);
            } catch (NeDesignerException e) {
                log.error("Failed to construct output.", e);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Failed to generate BOM." + e.getCause().getMessage(), e);
            }

            //step4: construct output
            List<ReusedNodesSnapshot> reusedNodesSnapshot = nodeSnapshot.stream().map(node -> getReusedNodesSnapshot(node))
                    .collect(Collectors.toList());

            return new AllocateNetworkAdditionalOutputBuilder()
                    .setAdditional(additional)
                    .setReusedNodesSnapshot(reusedNodesSnapshot)
                    .setBomInfo(new BomInfoBuilder(bomInfo).build()).build();

        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to construct output.", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "construct output error" + e.toString(), e);
        }
    }

    private BomInfo constructBomInfo(RouteInfo routeInfo, String vendorName, String productType, List<Node> nodeSnapshot) throws NeDesignerException {
        Map<String, Node> bomNodes = routeInfo.getMain().getNodes().stream().collect(Collectors.toMap(item -> item.getNodeId().getValue(), Function.identity(), (n1, n2) -> n2));
        List<Nodes> reusedNodesSnapshotUI = YangDataConverter.convertToUINodeList(nodeSnapshot);
        Map<String, Map<String, NeBomInfo>> bomMap = bomGenerator.generateBomMap(bomNodes, vendorName, productType, reusedNodesSnapshotUI);
        return bomGenerator.constructBomInfo(bomMap);
    }

    private ReusedNodesSnapshot getReusedNodesSnapshot(Node node) {
        return new ReusedNodesSnapshotBuilder().setNodeId(node.getNodeId())
                .setKey(new ReusedNodesSnapshotKey(node.getNodeId()))
                .setPhysical(node.getAugmentation(Node1.class).getPhysical())
                .setTerminationPoint(RouteYangDataConverter.getRouteTps(node.getTerminationPoint())).build();
    }

    private Route getAdditionalRouteResource(RouteInfo routeInfo, Link siteLink) throws NeDesignerException {
        Route main = routeInfo.getMain();
        List<Link> newLinks = new ArrayList<>();
        List<Node> newNodes = new ArrayList<>();
        for (Link link : main.getLinks()) {
            if (!PhysicalLinkIdNamingRule.isOtsLink(link.getLinkId().getValue())) {
                newLinks.add(link);
                continue;
            }

            //replace ots link as two 级联 link
            Link additionalLinkSrc = createAdditionalLink(link.getSource().getSourceTp(), siteLink);
            Link additionalLinkDst = createAdditionalLink(link.getDestination().getDestTp(), siteLink);
            newLinks.add(additionalLinkSrc);
            newLinks.add(additionalLinkDst);

            //update internal link
            for (Node node : main.getNodes()) {
                List<InternalLinks> newInternalLinks = nodeUtils.getInternalLinks(node).stream().filter(l -> !l.getLinkRef().equals(link.getLinkId().getValue())).collect(Collectors.toList());
                InternalLinks additionalInternalLink;

                if (link.getSource().getSourceNode().getValue().equals(node.getNodeId().getValue())) {
                    additionalInternalLink = linkRepo.createInternalLink(node.getNodeId().getValue(), additionalLinkSrc);
                } else {
                    additionalInternalLink = linkRepo.createInternalLink(node.getNodeId().getValue(), additionalLinkDst);
                }
                newInternalLinks.add(additionalInternalLink);
                Set<String> busyTpIds = new HashSet<>(Arrays.asList(additionalInternalLink.getSrcTp(), additionalInternalLink.getDstTp()));
                Node updatedNode = neNodeRepo.refreshNode(node, newInternalLinks, busyTpIds);
                newNodes.add(updatedNode);
            }
        }
        return Route.builder().xcs(main.getXcs()).nodes(newNodes).links(newLinks).build();
    }

    private Link createAdditionalLink(TpId fromTp, Link siteLink) throws NeDesignerException {
        String fromTpId = fromTp.getValue();
        String siteLinkTpId;
        if (PhysicalTpIdNamingRule.getSiteId(fromTpId).equals(siteLink.getSource().getSourceNode().getValue())) {
            siteLinkTpId = siteLink.getSource().getSourceTp().getValue();
        } else {
            siteLinkTpId = siteLink.getDestination().getDestTp().getValue();
        }
        String toTpId = siteLinkTpId.replace("-LINE", EXP8_SUFFIX);//todo: 根据需求，暂时是定死的
        return linkRepo.createLink(fromTpId, toTpId, LinkType.OmsLink);
    }

    private Pair<SiteInput, List<Node>> prepareAllocateInput(Link siteLink) {

        //prepare designer nodesMap
        SiteNodeInput aSiteNodeInput = SiteNodeInput.builder()
                .nodeType("T")
                .siteId(siteLink.getSource().getSourceNode().getValue())
                .ipNode(phyNodeDao.getConfigPhyNodeById(PhysicalTpIdNamingRule.getNodeId(siteLink.getSource().getSourceTp().getValue())))
                .build();
        SiteNodeInput zSiteNodeInput = SiteNodeInput.builder()
                .nodeType("T")
                .siteId(siteLink.getDestination().getDestNode().getValue())
                .ipNode(phyNodeDao.getConfigPhyNodeById(PhysicalTpIdNamingRule.getNodeId(siteLink.getDestination().getDestTp().getValue())))
                .build();

        List<Node> reusedNodeSnapShots = new ArrayList<>();
        reusedNodeSnapShots.add(aSiteNodeInput.getIpNode());
        reusedNodeSnapShots.add(zSiteNodeInput.getIpNode());

        List<SiteNodeInput> mainNodes = Arrays.asList(aSiteNodeInput, zSiteNodeInput);//todo:protected not supported
        Map<RoutingType, List<SiteNodeInput>> nodesMap = new HashMap<>();
        nodesMap.put(RoutingType.Main, mainNodes);

        Site siteLinkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
        SiteInput siteInput = SiteInput.builder()
                .nodesMap(nodesMap)
                .vendorName(siteLinkAttr.getVendorName())
                .vendorType(siteLinkAttr.getProductType())
                .grid(siteLinkAttr.getGrid().getIntValue())
                .isProtected(false)
                .plane(siteLinkAttr.getPlaneName())
                .riskGroupName(siteLinkAttr.getRiskGroupName())
                .linkModel(LINK_MODEL_P2P)
                .wdmBand(WDM_Band.fromString(siteLinkAttr.getLinkGroup()))
                .build();
        return Pair.of(siteInput, reusedNodeSnapShots);
    }

    public void validateSiteLink(Link siteLink) {
        Site siteLinkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
        if (!siteLinkAttr.getLinkGroup().equals(WDM_Band.C_L.toString())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "invalid siteLink, Additional is only supported for:" + WDM_Band.C_L.toString());
        }
        if(siteLinkAttr.getAExternal().getAddDropLink().stream().filter(al->al.getLinkRef().equals(EXP8_SUFFIX)).findAny().isPresent()){
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "invalid siteLink, AExternal already has EXP8 .");
        }
        if (siteLinkAttr.getZExternal().getAddDropLink().stream().filter(zl -> zl.getLinkRef().equals(EXP8_SUFFIX)).findAny().isPresent()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "invalid siteLink, ZExternal already has EXP8 .");
        }
        validateImplementStatus(siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite().getImplementState());

    }
    private void validateImplementStatus(ImplementState implementState) {
        if (implementState.equals(ImplementState.Implement) || implementState.equals(ImplementState.Allocate) || implementState.equals(ImplementState.PartialImplement)) {
            return;
        }

        throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                String.format("Can't do bind for implementState: %s", implementState.name()));
    }
}
