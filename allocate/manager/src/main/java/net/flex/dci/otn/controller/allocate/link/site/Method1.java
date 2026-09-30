/*
 *
 *  * Copyright (c) 2021-2020 Network Flex Any Comp. and others.  All rights reserved.
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package net.flex.dci.otn.controller.allocate.link.site;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.allocate.link.common.CreateSiteLinkParam;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteInput;
import net.flex.dci.otn.controller.allocate.designer.model.site.SiteNodeInput;
import net.flex.dci.otn.controller.allocate.node.site.SiteNodeCorrelateResource;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkTerminationNodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.creation.params.Segment;

import java.util.*;


/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
public class Method1 implements CreatorMethod_I<CreateLinkInput> {


    /**
     * 要求routeResource中的nodes顺序排列
     * 0                   1                                 size-2                size-1
     * siteNodeA              ILA                                  ILA                siteNodeZ
     **/
    //根据node的internalLink， 如果card只出现一次的就是起点
    @Override
    public Source getLinkSrcTermination(RouteInfo routeResource) {
        Node node = routeResource.getMain().getNodes().get(0);
        String tpId = getUniqueCardTp(node);
        return new SourceBuilder().setSourceNode(node.getNodeId()).setSourceTp(new TpId(tpId)).build();
    }

    @Override
    public Destination getLinkDstTermination(RouteInfo routeResource) {
        Node node = routeResource.getMain().getNodes().get(routeResource.getMain().getNodes().size() - 1);
        String tpId = getUniqueCardTp(node);
        return new DestinationBuilder().setDestNode(node.getNodeId()).setDestTp(new TpId(tpId)).build();
    }

    @Override
    public RouteInfo allocateResource(NeDesigner neDesigner, CreateLinkInput input, CreateSiteLinkParam param) throws NeDesignerException {
        SiteInput siteInput = getSiteInput(param);
        return neDesigner.allocateSite(siteInput);
    }

    @Override
    public SiteNodeCorrelateResource createSiteNodeCorrelateResource(Node siteNode, Link siteLink, Node phyNode) {
        String siteLinkFriendlyName = siteLink.getAugmentation(Link1.class).getSite().getFriendlyName();
        return new SiteNodeCorrelateResource(siteNode)
                .newRack(siteLink.getLinkId().getValue(), siteLinkFriendlyName)
                .insertRack(siteLink.getLinkId().getValue(), phyNode);
    }

    private SiteInput getSiteInput(CreateSiteLinkParam param) {
        List<SiteNodeInput> primaryNodes = getNodesBySegment(param.getMainSegment());
        List<SiteNodeInput> secondaryNodes = getNodesBySegment(param.getSpareSegment());

        /**
         * 输入的secondary segment 是 O---I--I--O
         * 开始、结尾的O 已经在primaryNodes中体现
         */
        if (!secondaryNodes.isEmpty()) {
            secondaryNodes.remove(0);
            secondaryNodes.remove(secondaryNodes.size() - 1);
        }
        SiteInput input = SiteInput.builder()
                .nodesMap(Collections.EMPTY_MAP)//todo: comment for compile
//                .nodes(primaryNodes)
//                .slaveNodes(secondaryNodes)
                .vendorName(param.getVendorName())
                .vendorType(param.getVendorType())
                .bandwidth(param.getBandwidth())
                .grid(param.getGrid().getIntValue())
                .isProtected(param.isProtected())
                .plane(param.getPlaneName())
                .riskGroupName(param.getRiskGroupName())
                .linkModel(param.getLinkModel())
                .build();
        return input;
    }

    private List<SiteNodeInput> getNodesBySegment(List<Segment> segments) {
        if (segments == null || segments.isEmpty()) {
            return Collections.emptyList();
        }

        List<SiteNodeInput> nodes = new ArrayList<>();
        Segment firstSegment = segments.get(0);
        nodes.add(createDesignerSiteNode(firstSegment.getSource(), firstSegment.getSourceNodeType()));
        for (Segment segment : segments) {
            nodes.add(createDesignerSiteNode(segment.getDestination(), segment.getDestinationNodeType()));
        }
        return nodes;

    }

    private SiteNodeInput createDesignerSiteNode(String nodeId, LinkTerminationNodeType nodeType) {
        String siteNodeType = "";
        switch (nodeType) {
            case SITE:
                siteNodeType = "T";
                break;
            case ILA:
                siteNodeType = "I";
                break;
            case DGE:
                siteNodeType = "D";
                break;
            case ROADM:
                siteNodeType = "R";
                break;
        }
        return SiteNodeInput.builder()
                .nodeType(siteNodeType)
                .siteId(nodeId)
                .build();
    }


    //遍历所有internalLink，
    private String getUniqueCardTp(Node node) {
        List<InternalLinks> iternalLinkList = node.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class).getPhysical().getInternalLinks();
        Map<String, Integer> cardVisited = new HashMap<>();
        Map<String, String> uniqueTp = new HashMap<>();
        for (InternalLinks il : iternalLinkList) {
            if (il.getLinkType().equals(LinkType.CableLink)) {
                continue;   //管理用link
            }
            getUniqueTp(node.getNodeId().getValue(), il.getSrcTp(), cardVisited, uniqueTp);
            getUniqueTp(node.getNodeId().getValue(), il.getDstTp(), cardVisited, uniqueTp);
        }

        for (String cardName : cardVisited.keySet()) {
            if (cardVisited.get(cardName) == 1 || cardVisited.get(cardName) == 8)
            //==8 special for CMUX MPO port
            {
                return uniqueTp.get(cardName);
            }
        }
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "cannot find link's termination");
    }

    private void getUniqueTp(String nodeId, String tpId, Map<String, Integer> cardVisited, Map<String, String> uniqueTp) {
        String tpNodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        if (tpNodeId.equals(nodeId)) {
            String cardName = PhysicalTpIdNamingRule.getEquipId(tpId);
            uniqueTp.put(cardName, tpId);

            Integer visitNumber = cardVisited.get(cardName);
            if (visitNumber == null) {
                cardVisited.put(cardName, 1);
            } else {
                cardVisited.put(cardName, visitNumber + 1);
            }
        }
    }

}
