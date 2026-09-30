/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.constructs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.dto.PlaneViewInfoDto;
import net.flex.dci.otn.controller.nms.nms.dto.ViewLinkDto;
import net.flex.dci.otn.controller.nms.nms.dto.ViewNodeDto;
import net.flex.dci.otn.controller.nms.nms.dto.ViewTopoDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.groupby.plane.output.Topology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.groupby.plane.output.TopologyBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.groupby.plane.output.TopologyKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.groupby.plane.output.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.groupby.plane.output.topology.LinkKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.groupby.plane.output.topology.Node;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.groupby.plane.output.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.groupby.plane.output.topology.NodeKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.ViewLinkType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.ViewBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

/**
 * @version 1.0
 * @date 2021/11/29 14:26
 */
@Slf4j
public class ViewLinkPanelConstructor {

    /**
     * generate topologies
     *
     * @param planeViewInfoMap
     * @return
     */
    public static List<Topology> buildTopologies(Map<String, PlaneViewInfoDto> planeViewInfoMap) {
        List<Topology> topologies = new LinkedList<>();
        for (PlaneViewInfoDto topo : planeViewInfoMap.values()) {
            Topology topology = constructTopo(topo);
            topologies.add(topology);
        }
        return topologies;
    }


    private static Topology constructTopo(PlaneViewInfoDto topo) {
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.groupby.plane.output.topology.Link> links = new LinkedList();
        for (ViewLinkDto mLink : topo.getPlaneTopo().getLinks()) {
            List<SupportingLink> supportingLinkList = new LinkedList<>();
            for (String sl : mLink.getSupportingLink()) {
                supportingLinkList.add(new SupportingLinkBuilder()
                        .setLinkRef(new LinkId(sl))
                        .setKey(new SupportingLinkKey(new LinkId(sl)))
                        .build());
            }
            LinkBuilder lb = new LinkBuilder()
                    .setLinkId(new LinkId(mLink.getLinkId()))
                    .setSource(new SourceBuilder().setSourceNode(new NodeId(mLink.getSource()))
                            .build())
                    .setDestination(
                            new DestinationBuilder().setDestNode(new NodeId(mLink.getDestination()))
                                    .build())
                    .setSupportingLink(supportingLinkList)
                    .setView(new ViewBuilder().setBundleNumber(mLink.getBundle())
                            .setAlarmState(mLink.getAlarmState()).setLevel(mLink.getLevel())
                            .build())
                    .setKey(
                            new LinkKey(
                                    new LinkId(mLink.getLinkId())));

            links.add(lb.build());
        }

        List<Node> nodes = new LinkedList();
        for (ViewNodeDto mNode : topo.getPlaneTopo().getNodes()) {
            NodeBuilder nb = new NodeBuilder()
                    .setNodeId(new NodeId(mNode.getNodeId()))
                    .setSupportingNode(mNode.getSupportingNodes())
//                    .setView(
//                            new ViewBuilder()
//                                    .setAlarmState(mNode.getAlarmState())
//                                    .setFriendlyName(mNode.getFriendlyName())
//                                    .setPosX(mNode.getPosX())
//                                    .setPosY(mNode.getPosY()).build())
                    .setKey(
                            new NodeKey(
                                    new NodeId(mNode.getNodeId())));

            nodes.add(nb.build());
        }

        TopologyBuilder
                tb = new TopologyBuilder();
        tb.setPlane(topo.getPlane())
                .setKey(new TopologyKey(topo.getPlane()))
                .setLink(links)
                .setNode(nodes);

        return tb.build();
    }


    /**
     * build default plane view info topo
     *
     * @param planeName
     * @return
     */
    public static PlaneViewInfoDto constructDefaultViewTopo(String planeName) {
        return PlaneViewInfoDto.builder()
                .plane(planeName)
                .planeTopo(ViewTopoDto.builder().topologyId(TopoNameConstants.Site_View_Topo_Key)
                        .links(new LinkedList<>()).nodes(new LinkedList<>()).build()).build();
    }

    public static ViewLinkDto buildViewLinkDto(Link viewLink, Link link) {
        AlarmSeverity alarmState = viewLink.getAugmentation(Link1.class).getView().getAlarmState();
        List<String> supportedLinks = new ArrayList<>();
        supportedLinks.add(link.getLinkId().getValue());
        ViewLinkType level = viewLink.getAugmentation(Link1.class).getView().getLevel();
        return ViewLinkDto.builder().LinkId(viewLink.getLinkId().getValue())
                .source(viewLink.getSource().getSourceNode().getValue())
                .destination(viewLink.getDestination().getDestNode().getValue())
                .bundle(1)
                .alarmState(alarmState)
                .supportingLink(supportedLinks)
                .level(level)
                .build();
    }

    /**
     * view node stand for the site node
     *
     * @param link
     * @return
     */
    public static List<ViewNodeDto> buildViewNodeDto(Link link) {
        log.debug("construct view node info from ");
        String srcNodeId = getViewNodeId(link.getSource().getSourceNode().getValue());
        String desNodeId = getViewNodeId(link.getDestination().getDestNode().getValue());
        List<ViewNodeDto> viewNodeDtos = new LinkedList<>();
        ViewNodeDto source = ViewNodeDto.builder().nodeId(srcNodeId).build();
        ViewNodeDto dest = ViewNodeDto.builder().nodeId(desNodeId).build();
        viewNodeDtos.add(source);
        viewNodeDtos.add(dest);
        return viewNodeDtos;
    }

    private static String getViewNodeId(String value) {
        String result = value;
        if (PhysicalNodeIdNamingRule.isPhyNodeId(value)) {
            result = PhysicalNodeIdNamingRule.getSiteId(value);
        }
        return result;
    }

    public static List<Topology> buildTopologies(PlaneViewInfoDto planeViewInfoDto) {

        Topology topology = constructTopo(planeViewInfoDto);

        return Collections.singletonList(topology);
    }

    public static List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology> buildByPlaneTopologies(
            PlaneViewInfoDto planeViewInfoDto) {
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology topology = constructTopoByPlane(
                planeViewInfoDto);
        return Collections.singletonList(topology);
    }

    private static org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.Topology constructTopoByPlane(
            PlaneViewInfoDto topo) {
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.topology.Link> links = new LinkedList();
        for (ViewLinkDto mLink : topo.getPlaneTopo().getLinks()) {
            List<SupportingLink> supportingLinkList = new LinkedList<>();
            for (String sl : mLink.getSupportingLink()) {
                supportingLinkList.add(new SupportingLinkBuilder()
                        .setLinkRef(new LinkId(sl))
                        .setKey(new SupportingLinkKey(new LinkId(sl)))
                        .build());
            }
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.topology.LinkBuilder lb = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.topology.LinkBuilder()
                    .setLinkId(new LinkId(mLink.getLinkId()))
                    .setSource(new SourceBuilder().setSourceNode(new NodeId(mLink.getSource()))
                            .build())
                    .setDestination(
                            new DestinationBuilder().setDestNode(new NodeId(mLink.getDestination()))
                                    .build())
                    .setSupportingLink(supportingLinkList)
                    .setView(new ViewBuilder().setBundleNumber(mLink.getBundle())
                            .setAlarmState(mLink.getAlarmState()).setLevel(mLink.getLevel())
                            .build())
                    .setKey(
                            new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.topology.LinkKey(
                                    new LinkId(mLink.getLinkId())));

            links.add(lb.build());
        }

        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.topology.Node> nodes = new LinkedList();
        for (ViewNodeDto mNode : topo.getPlaneTopo().getNodes()) {
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.topology.NodeBuilder nb = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.topology.NodeBuilder()
                    .setNodeId(new NodeId(mNode.getNodeId()))
                    .setSupportingNode(mNode.getSupportingNodes())
//                    .setView(
//                            new ViewBuilder()
//                                    .setAlarmState(mNode.getAlarmState())
//                                    .setFriendlyName(mNode.getFriendlyName())
//                                    .setPosX(mNode.getPosX())
//                                    .setPosY(mNode.getPosY()).build())
                    .setKey(
                            new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.topology.NodeKey(
                                    new NodeId(mNode.getNodeId())));

            nodes.add(nb.build());
        }

        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.TopologyBuilder
                tb = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.TopologyBuilder();
        tb.setPlane(topo.getPlane())
                .setKey(new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.view.link.by.plane.output.TopologyKey(
                        topo.getPlane()))
                .setLink(links)
                .setNode(nodes);

        return tb.build();
    }
}
