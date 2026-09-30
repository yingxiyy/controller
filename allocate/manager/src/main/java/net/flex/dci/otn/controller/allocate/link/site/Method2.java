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

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.allocate.common.RouteYangDataConverter;
import net.flex.dci.otn.controller.allocate.link.common.CreateSiteLinkParam;
import net.flex.dci.otn.controller.allocate.designer.NeDesigner;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.node.site.SiteNodeCorrelateResource;
import org.apache.poi.ss.formula.functions.T;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateLink2Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe;

import static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType.WSS;


/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
public class Method2 implements CreatorMethod_I<CreateLink2Input> {

    private final boolean useMuxPanelEndpoint;

    public Method2() {
        this(false);
    }

    Method2(boolean useMuxPanelEndpoint) {
        this.useMuxPanelEndpoint = useMuxPanelEndpoint;
    }

    /**
     * 简单不带WSS/IRA的routeResource中的links 顺序排列
     * 0                   1                                 size-2                size-1
     * cableLink  siteLinkStartPointRelatedLink ........siteLinkEndPointRelatedLink  cableLink
     *
     * 但是带了WSS/IRA以后，此方法必须自己找到WSS/IRA，然后出口点作为SiteLink的起止点
     **/
    @Override
    public Source getLinkSrcTermination(RouteInfo routeResource) {
        Node endpointNode = routeResource.getMain().getNodes().get(0);
        if (useMuxPanelEndpoint) {
            TpId muxPanelMpoTp = getMuxPanelMpoTp(endpointNode);
            return new SourceBuilder()
                    .setSourceNode(endpointNode.getNodeId())
                    .setSourceTp(muxPanelMpoTp)
                    .build();
        }
        for (Link link : routeResource.getMain().getLinks()) {
            TerminationPoint wssTp = getWssOutputTp(endpointNode, link,
                    routeResource.getMain().getNodes());
            if (wssTp != null) {
                return new SourceBuilder()
                        .setSourceNode(new NodeId(PhysicalTpIdNamingRule.getNodeId(wssTp.getTpId().getValue())))
                        .setSourceTp(new TpId(wssTp.getTpId()))
                        .build();
            }
        }
        return routeResource.getMain().getLinks().get(1).getSource();
    }

    @Override
    public Destination getLinkDstTermination(RouteInfo routeResource) {
        int length = routeResource.getMain().getLinks().size();
        int nodeSize = routeResource.getMain().getNodes().size();
        Node endpointNode = routeResource.getMain().getNodes().get(nodeSize - 1);

        if (useMuxPanelEndpoint) {
            TpId muxPanelMpoTp = getMuxPanelMpoTp(endpointNode);
            return new DestinationBuilder()
                    .setDestNode(endpointNode.getNodeId())
                    .setDestTp(muxPanelMpoTp)
                    .build();
        }
        for (int i = length - 1; i >= 0; i--) {
            Link link = routeResource.getMain().getLinks().get(i);
            TerminationPoint wssTp = getWssOutputTp(endpointNode, link,
                    routeResource.getMain().getNodes());
            if (wssTp != null) {
                return new DestinationBuilder()
                        .setDestNode(new NodeId(PhysicalTpIdNamingRule.getNodeId(wssTp.getTpId().getValue())))
                        .setDestTp(wssTp.getTpId())
                        .build();
            }
        }
        return routeResource.getMain().getLinks().get(routeResource.getMain().getLinks().size() - 2).getDestination();
    }

    static TpId getMuxPanelMpoTp(Node endpointNode) {
        Node1 physicalNode = endpointNode.getAugmentation(Node1.class);
        if (physicalNode == null || physicalNode.getPhysical() == null) {
            throw missingMuxPanelEndpoint(endpointNode);
        }
        if (physicalNode.getPhysical().getEquipments() == null
                || endpointNode.getTerminationPoint() == null) {
            throw missingMuxPanelEndpoint(endpointNode);
        }
        Set<String> muxPanelEquipmentIds = physicalNode.getPhysical().getEquipments().stream()
                .filter(equipment -> EquipType.MUXPANEL.equals(equipment.getEquipType()))
                .map(Equipments::getEquipmentId)
                .collect(Collectors.toSet());
        return endpointNode.getTerminationPoint().stream()
                .map(TerminationPoint::getTpId)
                .filter(tpId -> muxPanelEquipmentIds.contains(
                        PhysicalTpIdNamingRule.getEquipId(tpId.getValue())))
                .filter(tpId -> tpId.getValue().contains("MPO"))
                .sorted(Comparator.comparing(TpId::getValue))
                .findFirst()
                .orElseThrow(() -> missingMuxPanelEndpoint(endpointNode));
    }

    private static CommonException missingMuxPanelEndpoint(Node endpointNode) {
        return new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "Cannot find Bone2.0 Flex64 MUXPANEL MPO endpoint in node "
                        + endpointNode.getNodeId().getValue());
    }

    private TerminationPoint getWssOutputTp(Node checkingNode, Link link, List<Node> nodes) {
        List<Equipments> wssCardList = checkingNode.getAugmentation(Node1.class).getPhysical().getEquipments().stream().filter(
                equipment -> equipment.getEquipType().equals(WSS)|| equipment.getEquipType().equals(EquipType.IRA)).collect(Collectors.toList());
        if (wssCardList.isEmpty())
            return null;

        String srcTp = link.getSource().getSourceTp().getValue();
        String dstTp = link.getDestination().getDestTp().getValue();
        for (Node node : nodes) {
            for (TerminationPoint tp : node.getTerminationPoint()) {
                String tpName = tp.getTpId().getValue();
                if (tpName.equals(srcTp) || tpName.equals(dstTp)) {
                    if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType().equals(PortType.OALine)) {
                        Optional<Equipments> tpOp = wssCardList.stream().filter(x -> tp.getTpId().getValue().contains(x.getEquipmentId())).findAny();
                        if (tpOp.isPresent()) {
                            return tp;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    public RouteInfo allocateResource(NeDesigner neDesigner, CreateLink2Input input, CreateSiteLinkParam param) throws NeDesignerException {
        RouteInfo routeInfo = RouteYangDataConverter.getRouteInfo(input.getLinkComputeResult().getMain(), input.getLinkComputeResult().getSlave(), input.getLinkComputeResult().getThird());

        return routeInfo;


    }

    /**
     * 对于一个复用段来说，一个复用段就是一个rack, 网元复用的情况下会出现，空的rack
     * @param siteNode
     * @param siteLink
     * @param phyNode
     * @return
     */
    @Override
    public SiteNodeCorrelateResource createSiteNodeCorrelateResource(Node siteNode, Link siteLink, Node phyNode) {
        String siteLinkFriendlyName = siteLink.getAugmentation(Link1.class).getSite().getFriendlyName();
        SiteNodeCorrelateResource correlateResource = new SiteNodeCorrelateResource(siteNode);

        correlateResource.updateRack(siteLink.getLinkId().getValue(), siteLinkFriendlyName, phyNode);
        return correlateResource;
    }
}
