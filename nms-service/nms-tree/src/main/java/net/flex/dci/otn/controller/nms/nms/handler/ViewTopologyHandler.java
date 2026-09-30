/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler;

import static net.flex.dci.otc.common.util.Constant.GLOBAL_ROOT_NODE_ID;
import static net.flex.dci.otn.controller.nms.utils.Constants.SITE_VIEW_TOPO_KEY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.dto.ViewNodeDto;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.ViewNodeNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.ViewLinkDao;
import net.flex.dci.otc.mongo.dao.ViewNodeDao;
import net.flex.dci.otc.mongo.dto.NeSubTypeInfo;
import net.flex.dci.otc.mongo.dto.NeSubTypeQuery;
import net.flex.dci.otn.controller.nms.constructs.ViewLinkPanelConstructor;
import net.flex.dci.otn.controller.nms.nms.component.viewTopo.ViewLinkTopology;
import net.flex.dci.otn.controller.nms.nms.component.viewTopo.ViewTopology;
import net.flex.dci.otn.controller.nms.nms.convertors.NmsOutputConverters;
import net.flex.dci.otn.controller.nms.nms.dto.PlaneViewInfoDto;
import net.flex.dci.otn.controller.nms.nms.dto.ViewLinkDto;
import net.flex.dci.otn.controller.nms.nms.dto.viewTopo.ViewLinkTopoDto;
import net.flex.dci.otn.controller.nms.nms.dto.viewTopo.ViewLinkTopoDto.ViewLinkPhysical;
import net.flex.dci.otn.controller.nms.nms.dto.viewTopo.ViewNodeTopoDto;
import net.flex.dci.otn.controller.nms.nms.dto.viewTopo.ViewNodeTopoDto.ViewNodePhysical;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import net.flex.dci.otn.controller.nms.utils.SiteRoleBitCalcUtil;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteViewTopologyInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteViewTopologyOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteViewTopologyOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneStartwithInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkByPlaneStartwithOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkGroupbyPlaneOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetViewLinkGroupbyPlaneOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ListViewPlaneOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ListViewPlaneOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.TopologyBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.groupby.plane.output.Topology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1Builder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.View;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.ViewBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.SiteType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2021/11/29 13:26
 */
@Slf4j
@Component
public class ViewTopologyHandler extends AbstractBaseHandler {

    @Autowired
    private ViewTopology viewTopology;

    @Autowired
    private ViewLinkTopology viewLinkTopology;

    @Autowired
    private ViewLinkDao viewLinkDao;

    @Autowired
    private ViewNodeDao viewNodeDao;

    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    private NmsOutputConverters nmsOutputConverters;

    public ViewTopologyHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public GetViewLinkGroupbyPlaneOutput getViewLinkGroupedByPlane() {
        log.debug("start to get view link group by plane");
        GetViewLinkGroupbyPlaneOutputBuilder outputBuilder = new GetViewLinkGroupbyPlaneOutputBuilder();
        List<Topology> topologyList = getTopologyList();
        if (topologyList == null || topologyList.isEmpty()) {
            topologyList = new ArrayList<>();
        }
        outputBuilder.setTopology(topologyList);
        return outputBuilder.build();
    }

    @Override
    public GetViewLinkByPlaneOutput getViewLinkByPlane(GetViewLinkByPlaneInput input) {
        log.debug("start to get view link group by plane");
        GetViewLinkByPlaneOutputBuilder outputBuilder = new GetViewLinkByPlaneOutputBuilder();
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology> viewTopologyList = getViewLinkTopology(
                input);
        outputBuilder.setTopology(viewTopologyList);
//        List<Topology> topologyList = getTopologyList();
//        if (topologyList == null || topologyList.isEmpty()) {
//            topologyList = new ArrayList<>();
//        }
//        outputBuilder.setTopology(topologyList);
        return outputBuilder.build();
    }

    @Override
    public GetViewLinkByPlaneStartwithOutput getViewLinkByPlaneStartwith(
            GetViewLinkByPlaneStartwithInput input) {
        log.debug("get view link topology by input:{}", input);
        validateGetViewLinkTopology(input);
        String planeName = input.getPlane();
        ViewLinkType viewLinkType = input.getLinkLevel();
        GetViewLinkByPlaneStartwithOutput viewTopo = viewLinkTopology.getLinkTopologyByPlaneStartwithAndLinkType(
                viewLinkType, planeName);
        return viewTopo;
    }

    /**
     * get view link topology view link
     *
     * @param input
     * @return
     */
    private List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology> getViewLinkTopology(
            GetViewLinkByPlaneInput input) {
        //just for link level
        log.debug("get view link topology by input:{}", input);
        validateGetViewLinkTopology(input);
        String planeName = input.getPlane();
        ViewLinkType viewLinkType = input.getLinkLevel();
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology> viewTopologies = viewLinkTopology.getLinkTopologyByPlaneAndLinkType(
                viewLinkType, planeName);
        return viewTopologies;
    }


    private void validateGetViewLinkTopology(GetViewLinkByPlaneInput input) {
        if (input.getLinkLevel() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the link level should be not null");
        }
        if (StringUtils.isEmpty(input.getPlane())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the required plane name should not be null");
        }
    }

    private void validateGetViewLinkTopology(GetViewLinkByPlaneStartwithInput input) {
        if (input.getLinkLevel() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the link level should be not null");
        }
        if (StringUtils.isEmpty(input.getPlane())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the required plane name should not be null");
        }
    }

    /**
     * get detail info topology list information
     *
     * @return
     */
    private List<Topology> getTopologyList() {
        try {
            List<Topology> topologies = new ArrayList<>();
//            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology viewTopo = netconfTopology.getTopology(
//                    new TopologyId(TopoNameConstants.Site_View_Topo_Key));
            Map<String, PlaneViewInfoDto> planeViewInfoMap = new HashMap<>();

            List<Link> viewLinks = netconfTopology.listAllViewLinks();
            if (viewLinks == null || viewLinks.isEmpty()) {
                log.debug("the view topology is empty");
                return topologies;
            }
            viewLinks.forEach(link -> {
                List<SupportingLink> supportLink = link.getSupportingLink();
                supportLink.forEach(sl -> {
                    getSupportLinkPlaneInfo(link, sl, planeViewInfoMap);
                });
            });

            topologies = ViewLinkPanelConstructor.buildTopologies(planeViewInfoMap);
            return topologies;
        } catch (Exception ex) {
            log.error("construct plane topo error", ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to construct plane topology,the reason is " + ex.getLocalizedMessage());
        }
    }

    private void getSupportLinkPlaneInfo(
            Link viewLink,
            SupportingLink sl,
            Map<String, PlaneViewInfoDto> planeViewInfoMap) {
        Link siteLink = netconfTopology.getSiteLink(sl.getLinkRef().getValue());
        if (siteLink == null) {
            return;
        }
        Site siteLinkAttr = siteLink
                .getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                .getSite();
        String planeName = siteLinkAttr.getPlaneName();
        if (planeViewInfoMap.containsKey(planeName)) {
            boolean viewLinkIsFound = false;
            PlaneViewInfoDto topo = planeViewInfoMap.get(planeName);
            for (ViewLinkDto mLink : topo.getPlaneTopo().getLinks()) {
                if (mLink.getLinkId().equals(viewLink.getLinkId().getValue())) {
                    //this plane related view link has created.
                    mLink.getSupportingLink().add(sl.getLinkRef().getValue());
                    mLink.setBundle(mLink.getBundle() + 1);
                    if (mLink.getAlarmState().getIntValue() < siteLinkAttr.getAlarmState()
                            .getIntValue()) {
                        mLink.setAlarmState(siteLinkAttr.getAlarmState());
                    }
                    viewLinkIsFound = true;
                    break;
                }
            }
            if (!viewLinkIsFound) {
                topo.getPlaneTopo().getLinks()
                        .add(ViewLinkPanelConstructor.buildViewLinkDto(viewLink, siteLink));
                topo.getPlaneTopo().getNodes()
                        .addAll(ViewLinkPanelConstructor.buildViewNodeDto(siteLink));

            }
        } else {
            PlaneViewInfoDto topo = ViewLinkPanelConstructor.constructDefaultViewTopo(planeName);
            topo.getPlaneTopo().getLinks()
                    .add(ViewLinkPanelConstructor.buildViewLinkDto(viewLink, siteLink));

            topo.getPlaneTopo().getNodes()
                    .addAll(ViewLinkPanelConstructor.buildViewNodeDto(siteLink));
            planeViewInfoMap.put(planeName, topo);
        }
    }

    @Override
    public ListViewPlaneOutput listAllViewPlane() {
        log.debug("list all the view ref plane");
        List<String> planes = viewLinkTopology.listAllViewPLane();
        ListViewPlaneOutputBuilder listViewPlaneOutputBuilder = new ListViewPlaneOutputBuilder();
        listViewPlaneOutputBuilder.setPlane(planes);
        return listViewPlaneOutputBuilder.build();
    }

    @Override
    public GetSiteViewTopologyOutput getSiteViewTopology(GetSiteViewTopologyInput input) {
        log.debug("retrieve current site view topology");
        String subnetId = GLOBAL_ROOT_NODE_ID;
        ViewLinkType linkLevel = ViewLinkType.SiteLink;
        if (input != null) {
            subnetId = input.getSubnetId();
            linkLevel = input.getLinkLevel();
        }
        List<Node> viewNodes = viewTopology.listViewNodes(subnetId);
        List<Link> viewLinks = viewTopology.listViewLinks(subnetId, linkLevel);
        List<Link> rebuildViewLinks = rebuildViewLinksWithFilteredNodes(viewNodes, viewLinks);
//        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Link> viewTopoLinks = nmsOutputConverters.convert2NmsOutput(
//                rebuildViewLinks);
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Link> viewTopoLinks = convertToViewLinks(
                rebuildViewLinks);

//        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Node> viewTopoNodes = nmsOutputConverters.convert2NmsOutput(
//                viewNodes);
        List<ViewNodeTopoDto> viewTopoNodes = convertToViewNodes(viewNodes);
//        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Node> viewFilterTopoNodes = filterViewTopoNodes(
//                viewTopoNodes, linkLevel);
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Node> viewFilterTopoNodes = filterViewTopoNodesByLinkLevel(
                viewTopoNodes,
                linkLevel);

        TopologyBuilder topologyBuilder = new TopologyBuilder();
        topologyBuilder.setTopologyId(TopologyId.getDefaultInstance(SITE_VIEW_TOPO_KEY));
        topologyBuilder.setLink(viewTopoLinks);
        topologyBuilder.setNode(viewFilterTopoNodes);
        GetSiteViewTopologyOutputBuilder getSiteViewTopologyOutputBuilder = new GetSiteViewTopologyOutputBuilder();
        getSiteViewTopologyOutputBuilder.setTopology(
                Collections.singletonList(topologyBuilder.build()));
        return getSiteViewTopologyOutputBuilder.build();
    }

    private List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Link> convertToViewLinks(
            List<Link> rebuildViewLinks) {
        List<ViewLinkTopoDto> viewLinkTopoDtos = rebuildViewLinks.stream().map(link -> {
                    List<String> supportLinks = link.getSupportingLink().stream()
                            .map(sl -> sl.getLinkRef().getValue()).collect(
                                    Collectors.toList());
                    String sourceNode = link.getSource().getSourceNode().getValue();
                    String destNode = link.getDestination().getDestNode().getValue();
                    View viewLinkPhy = link.getAugmentation(Link1.class).getView();
                    ViewLinkPhysical viewLinkPhysical = ViewLinkPhysical.builder()
                            .linkType(viewLinkPhy.getLevel())
                            .bundleNum(viewLinkPhy.getBundleNumber())
                            .alarmState(viewLinkPhy.getAlarmState())
                            .subnetName(viewLinkPhy.getSubnetName())
                            .subnetId(viewLinkPhy.getSubnetId())
                            .subnetLevel(viewLinkPhy.getSubnetLevel())
                            .build();
                    return ViewLinkTopoDto.builder().viewLinkId(link.getLinkId().getValue())
                            .supportLinkIds(supportLinks).sourceNode(sourceNode).destNode(destNode)
                            .viewLinkPhysical(viewLinkPhysical).build();
                })
                .collect(Collectors.toList());
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Link> viewLinks = viewLinkTopoDtos.stream()
                .map(viewLinkTopoDto -> {
                    org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.LinkBuilder linkBuilder = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.LinkBuilder();
                    linkBuilder.setLinkId(
                                    LinkId.getDefaultInstance(viewLinkTopoDto.getViewLinkId()))
                            .setSource(new SourceBuilder().setSourceNode(
                                            NodeId.getDefaultInstance(viewLinkTopoDto.getSourceNode()))
                                    .build())
                            .setDestination(new DestinationBuilder().setDestNode(
                                            NodeId.getDefaultInstance(viewLinkTopoDto.getDestNode()))
                                    .build())
                            .setSupportingLink(viewLinkTopoDto.getSupportLinkIds().stream()
                                    .map(linkId -> new SupportingLinkBuilder().setLinkRef(
                                            LinkId.getDefaultInstance(linkId)).build()).collect(
                                            Collectors.toList()))
                            .setView(
                                    new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.ViewBuilder()
                                            .setBundleNumber(viewLinkTopoDto.getViewLinkPhysical()
                                                    .getBundleNum())
                                            .setSubnetName(viewLinkTopoDto.getViewLinkPhysical()
                                                    .getSubnetName())
                                            .setLevel(viewLinkTopoDto.getViewLinkPhysical()
                                                    .getLinkType())
                                            .setSubnetId(viewLinkTopoDto.getViewLinkPhysical()
                                                    .getSubnetId())
                                            .setSubnetName(viewLinkTopoDto.getViewLinkPhysical()
                                                    .getSubnetName())
                                            .setSubnetLevel(viewLinkTopoDto.getViewLinkPhysical()
                                                    .getSubnetLevel())
                                            .setAlarmState(viewLinkTopoDto.getViewLinkPhysical()
                                                    .getAlarmState())
                                            .build())
                    ;

                    return linkBuilder.build();
                })
                .collect(Collectors.toList());
        return viewLinks;
    }


    private List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Node> filterViewTopoNodesByLinkLevel(
            List<ViewNodeTopoDto> viewTopoNodes,
            ViewLinkType linkLevel) {
        List<ViewNodeTopoDto> refViewNodeTopo = viewTopoNodes;
        if (linkLevel.equals(ViewLinkType.SiteLink)) {
            refViewNodeTopo = refViewNodeTopo.stream()
                    .filter(viewNode -> viewNode.getSiteType().equals(SiteType.OTM)
                            || viewNode.getSiteType().equals(SiteType.ROADM)
                            || viewNode.getSiteType().equals(SiteType.SITE)).collect(
                            Collectors.toList());
        }
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Node> viewNodes =
                buildViewNodes(refViewNodeTopo);
        return viewNodes;
    }

    private List<ViewNodeTopoDto> convertToViewNodes(List<Node> viewNodes) {
        List<String> viewNodeIds = viewNodes.stream().map(node -> node.getNodeId().getValue())
                .collect(
                        Collectors.toList());
        log.debug("convert to view nodes:{}", viewNodeIds);

        Map<String, List<NeSubTypeInfo>> siteNeSubTypeMap = buildSiteNeSubTypeMap(viewNodeIds);
        List<ViewNodeTopoDto> viewNodeTopoDtos = viewNodes.stream().map(
                        node -> {
                            String viewNodeId = node.getNodeId().getValue();
                            String siteNodeSubTypeKey = buildSiteNodeSubTypeKey(viewNodeId);
                            List<NeSubTypeInfo> neSubTypeInfos = siteNeSubTypeMap.getOrDefault(
                                    siteNodeSubTypeKey,
                                    new ArrayList<>());
                            boolean hasElement = !neSubTypeInfos.isEmpty();
                            SiteType siteType = SiteRoleBitCalcUtil.calcByNeSubType(
                                    neSubTypeInfos.stream().map(NeSubTypeInfo::getNeSubType)
                                            .collect(Collectors.toList()));
                            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.View view = node.getAugmentation(
                                    Node1.class).getView();
                            ViewNodeTopoDto.ViewNodePhysical viewNodePhysical = ViewNodePhysical.builder()
                                    .posX(view.getPosX())
                                    .posY(view.getPosY())
                                    .alarmState(view.getAlarmState())
                                    .friendlyName(view.getFriendlyName())
                                    .subnetName(view.getSubnetName())
                                    .subnetLevel(view.getSubnetLevel())
                                    .subnetId(view.getSubnetId())
                                    .build();
                            return ViewNodeTopoDto.builder().hasNetworkElements(hasElement)
                                    .viewNodeId(viewNodeId)
                                    .view(viewNodePhysical)
                                    .siteType(siteType).build();
                        }
                )
                .collect(Collectors.toList());
        return viewNodeTopoDtos;
    }

    private String buildSiteNodeSubTypeKey(String viewNodeId) {
        ViewNodeDto viewNodeInfo = ViewNodeNamingRule.parseViewNode(viewNodeId);
        return viewNodeInfo.getSiteId() + "|" + viewNodeInfo.getSubnetId();
    }

    private Map<String, List<NeSubTypeInfo>> buildSiteNeSubTypeMap(List<String> viewNodeIds) {
        List<NeSubTypeQuery> queryList = new ArrayList<>();
        for (String viewNodeId : viewNodeIds) {
            ViewNodeDto viewNodeIdDto = ViewNodeNamingRule.parseViewNode(viewNodeId);
            queryList.add(
                    new NeSubTypeQuery(viewNodeIdDto.getSiteId(), viewNodeIdDto.getSubnetId()));
        }
        return phyNodeDao.batchListNeSubTypeBySiteAndSubnet(queryList);
    }

    private List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Node> buildViewNodes(
            List<ViewNodeTopoDto> refViewNodeTopo) {
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Node> nodes =
                refViewNodeTopo.stream().map(viewNode -> {
                            NodeBuilder nodeBuilder = new NodeBuilder();
                            nodeBuilder.setNodeId(
                                    NodeId.getDefaultInstance(viewNode.getViewNodeId()));
                            nodeBuilder.setHasNetworkElements(viewNode.isHasNetworkElements());
                            nodeBuilder.setSiteType(viewNode.getSiteType());
                            ViewBuilder viewBuilder = new ViewBuilder();
                            viewBuilder.setAlarmState(viewNode.getView().getAlarmState());
                            viewBuilder.setSubnetName(viewNode.getView().getSubnetName());
                            viewBuilder.setFriendlyName(viewNode.getView().getFriendlyName());
                            viewBuilder.setSubnetId(viewNode.getView().getSubnetId());
                            viewBuilder.setSubnetLevel(viewNode.getView().getSubnetLevel());
                            viewBuilder.setPosX(Math.toIntExact(viewNode.getView().getPosX()));
                            viewBuilder.setPosY(Math.toIntExact(viewNode.getView().getPosY()));
                            nodeBuilder.setView(viewBuilder.build());
                            return nodeBuilder.build();
                        })
                        .collect(Collectors.toList());
        return nodes;
    }

    private List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Node> filterViewTopoNodes(
            List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.site.view.topology.output.topology.Node> viewTopoNodes,
            ViewLinkType linkLevel) {
        if (linkLevel.equals(ViewLinkType.SiteLink)) {
            return viewTopoNodes.stream()
                    .filter(viewNode -> viewNode.getSiteType().equals(SiteType.OTM)
                            || viewNode.getSiteType().equals(SiteType.ROADM)
                            || viewNode.getSiteType().equals(SiteType.SITE)).collect(
                            Collectors.toList());
        }
        return viewTopoNodes;
    }

    private List<Link> rebuildViewLinksWithFilteredNodes(List<Node> filteredViewNodes,
            List<Link> originalViewLinks) {
        List<Link> rebuiltLinks = new ArrayList<>();
        if (CollectionUtils.isEmpty(filteredViewNodes) || CollectionUtils.isEmpty(
                originalViewLinks)) {
            log.warn("no filtered nodes or original links, return empty links");
            return rebuiltLinks;
        }

        Map<String, Node> siteIdToFilteredNodeMap = filteredViewNodes.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        node -> ViewNodeNamingRule.extractSiteId(node.getNodeId().getValue()),
                        node -> node, (oldNode, newNode) -> oldNode));
        for (Link originLink : originalViewLinks) {
            if (originLink == null) {
                continue;
            }
            String sourceViewNodeId = originLink.getSource().getSourceNode().getValue();
            String destViewNodeId = originLink.getDestination().getDestNode().getValue();

            String sourceSiteId = ViewNodeNamingRule.extractSiteId(sourceViewNodeId);
            String destSiteId = ViewNodeNamingRule.extractSiteId(destViewNodeId);

            Node filteredSrcNode = siteIdToFilteredNodeMap.get(sourceSiteId);
            Node filteredDestNode = siteIdToFilteredNodeMap.get(destSiteId);
            Link rebuiltLink = copyAndUpdateLinkSrcDest(originLink, filteredSrcNode,
                    filteredDestNode);
            if (rebuiltLink != null) {
                rebuiltLinks.add(rebuiltLink);
            }
        }
        return rebuiltLinks;
    }

    /**
     * copy and update view link source and destination
     *
     * @param originLink
     * @param filteredSrcNode
     * @param filteredDestNode
     * @return
     */
    private Link copyAndUpdateLinkSrcDest(Link originLink, Node filteredSrcNode,
            Node filteredDestNode) {
        if (filteredSrcNode == null || filteredDestNode == null) {
            log.warn("copy link skip, src or dest node is null, linkId:{}", originLink.getLinkId());
            return null;
        }
        String sourceNodeId = filteredSrcNode.getNodeId().getValue();
        String destNodeId = filteredDestNode.getNodeId().getValue();
        log.debug("copy and update link:{} src:{} and dest:{}", originLink.getLinkId(),
                sourceNodeId, destNodeId);
        View view = originLink.getAugmentation(Link1.class).getView();
        List<SupportingLink> supportingLinks = originLink.getSupportingLink();
        Source source = originLink.getSource();
        Destination destination = originLink.getDestination();

        SourceBuilder sourceBuilder = new SourceBuilder(source);
        DestinationBuilder destinationBuilder = new DestinationBuilder(destination);

        sourceBuilder.setSourceNode(NodeId.getDefaultInstance(sourceNodeId));
        destinationBuilder.setDestNode(NodeId.getDefaultInstance(destNodeId));

        LinkBuilder linkBuilder = new LinkBuilder(originLink);
        linkBuilder.setSupportingLink(supportingLinks);
        Link1Builder link1Builder = new Link1Builder();
        link1Builder.setView(view);
        linkBuilder.addAugmentation(Link1.class, link1Builder.build());
        linkBuilder.setSource(sourceBuilder.build());
        linkBuilder.setDestination(destinationBuilder.build());

        return linkBuilder.build();
    }


}
