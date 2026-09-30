/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.och;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.frequency.FrequencyInterval;
import net.flex.dci.otc.common.util.namingrule.CrossConnectionSlotNamingRule;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.link.common.Route;
import net.flex.dci.otn.controller.allocate.link.tunnel.ParamCreate;
import net.flex.dci.otn.controller.allocate.link.tunnel.TunnelBinder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.AvailableBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.AvailableKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ocm.attributes.OCMGripGroups;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;

import java.math.BigInteger;
import java.util.*;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
public class OchLinkConstructor {

    private static String ODUJ_SPLIT = "-";

    private ParamCreate param;
    private ChangedObject changedObject;

    private Available frequency;

    private String vendorName;
    private String productType;
    private boolean overNetwork;

    public OchLinkConstructor(ChangedObject changedObject, ParamCreate param) {
        this.changedObject = changedObject;
        this.param = param;
    }

    public Available getFrequency() {
        return frequency;
    }


    /**
     * RouteInfo of OchLink CrossConnection list without OCHP include [opticalXC]+ with OCHP main  include  opXC, [opticalXC]+ , opXC, spare include  [opticalXC]+
     *
     * Link list without OCHP include L---MUX, [(siteLink, wssLink, SiteLink) | siteLink]+ , L---MUX with OCHP main  include L---opSig, opA--Mux, [(siteLink, wssLink, SiteLink) | siteLink]+ ,
     * opA--Mux, L---opSig spare include opB--Mux, [(siteLink, wssLink, SiteLink) | siteLink]+ , opB--Mux
     *
     * Node List without/with OCHP tpcNode, allOpcNode included in siteLink
     */
    public Link create(String vendorName, String productType,
            String aTp, String zTp, String friendlyName,
            boolean isOverNetwork, RouteInfo info) {
        this.vendorName = vendorName;
        this.productType = productType;
        this.overNetwork = isOverNetwork;

        LinkId linkId = new LinkId(OchLinkIdNamingRule.generateId(aTp, zTp));

        Available frequency;
        List<SupportingLink> supportingLinkList = getSupporingLink(info);
        boolean hasSiteLink = hasSiteLink(supportingLinkList);
        if (hasSiteLink) {
            frequency = getOchLinkFrequency(info);
        } else {
            //对于电中继 或者 无光层业务 没有提供frequency, 生成一个frequency
            frequency = getOchLinkFrequency(aTp);
        }

        log.debug("start create och link at {}--{}", frequency.getLowerFrequency().getValue().toString(), frequency.getUpperFrequency().getValue().toString());
        Link ochLink = new LinkBuilder()
                .setLinkId(linkId)
                .setKey(new LinkKey(linkId))
                .setSupportingLink(supportingLinkList)
                .addAugmentation(Link1.class,
                        new Link1Builder()
                                .setOch(getOchLinkAttr(param.getProtectionType(), info, frequency, friendlyName, aTp, zTp))
                                .build())
                .setSource(new SourceBuilder()
                        .setSourceNode(new NodeId(PhysicalTpIdNamingRule.getNodeId(aTp)))
                        .setSourceTp(new TpId(aTp))
                        .build())
                .setDestination(new DestinationBuilder()
                        .setDestNode(new NodeId(PhysicalTpIdNamingRule.getNodeId(zTp)))
                        .setDestTp(new TpId(zTp))
                        .build())
                .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class,
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1Builder()
                                .setSupportedTunnel(new LinkedList<>())
                                .build())
                .build();

        changedObject.addChangedOchNode(updateOchNode(ochLink.getSource().getSourceNode(), ochLink.getSource().getSourceTp(), frequency));
        changedObject.addChangedOchNode(updateOchNode(ochLink.getDestination().getDestNode(), ochLink.getDestination().getDestTp(), frequency));

        new OchLinkPhyResource(changedObject).updatePhyLink(info, param.getPlaneName(), param.getPlaneId());
//        new OchLinkPhyResource(changedObject).copyPhyResource(ochLink, info);
        return ochLink;
    }

    private Available getOchLinkFrequency(String aTp) {
        //            Available ava = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableBuilder()
//                    .setLowerFrequency(new FrequencyType(FrequencyInterval.MUX96.getLowerFrequency()))
//                    .setUpperFrequency(new FrequencyType(BigInteger.valueOf(FrequencyInterval.MUX96.getLowerFrequency().longValue() + FrequencyInterval.MUX96.getGridSpan().longValue())))
//                    .setKey(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableKey(new FrequencyType(FrequencyInterval.MUX96.getLowerFrequency())))
//                    .build();

        String nodeId = PhysicalTpIdNamingRule.getNodeId(aTp);
        Node node = changedObject.getChangedPhyNode(nodeId);
        Optional<TerminationPoint> tpOP = node.getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().equals(aTp)).findFirst();
        if (tpOP.isPresent()) {
            //allocate 采用的是MUX64 频谱
            TerminationPoint tp = tpOP.get();
            Physical tpPhyAttr = tp.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1.class).getPhysical();
            if (tpPhyAttr.getOtuLine() != null && tpPhyAttr.getOtuLine().getCentralFrequency() != null) {
                long step = FrequencyInterval.MUX64.getGridSpan().longValue() / 2;
                long centFrequency = tpPhyAttr.getOtuLine().getCentralFrequency().getValue().longValue();
                Available ava = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableBuilder()
                        .setLowerFrequency(new FrequencyType(BigInteger.valueOf(centFrequency - step)))
                        .setUpperFrequency(new FrequencyType(BigInteger.valueOf(centFrequency + step)))
                        .setKey(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableKey(
                                new FrequencyType(BigInteger.valueOf(centFrequency - step))))
                        .build();
                return ava;
            } else {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "the OCH Link aTp hasn't provide frequency " + aTp);
            }
        } else {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "the OCH Link aTp isn't exist " + aTp);
        }
    }

    private boolean hasSiteLink(List<SupportingLink> supportingLinkList) {
        return supportingLinkList.stream()
                .filter(t -> SiteLinkIdNamingRule.isSiteLink(t.getLinkRef().getValue()))
                .findAny().isPresent();
    }

    public void updateOcmInfo(Link ochLink, List<CrossConnectionAttributes> amplifierXcs) {
        Map<String, List<OCMGripGroups>> updatedOcmGroup = new HashMap<>();

        //allocate 的时候没有分配ocmGroup，这里的算法是每次处理完ocm后放到changedObject中缓存，
        //由于allocate没有计算，它提供的info.getnodes中的点信息需要修改, 于是基于XC，看这个网元是否有amplifier 相关的交叉，然后修改
        //所以做了一个新的map保持已经计算过的ocmMap，这个需要尹甜后面修改后可以删除
        OcmUpdater ocmUpdater = new OcmUpdater(changedObject);
        amplifierXcs.stream().filter(xc -> xc.getDescription().toLowerCase().contains("amplifier"))
                .forEach(xc -> {
                    String nodeId = xc.getNodeRef().getValue();
                    Node updatedNode = changedObject.getChangedPhyNode(nodeId);
                    updatedOcmGroup.put(nodeId, updatedNode.getAugmentation(Node1.class).getPhysical().getOCMGripGroups());
                    ocmUpdater.setOldOcmGroupMap(updatedOcmGroup).updateOcmGroup(ochLink, xc, false);
                });

    }


    private Node updateOchNode(NodeId nodeId, TpId tpId, Available frequency) {

        Node ochNode = changedObject.getChangedOchNode(nodeId.getValue());
        if (ochNode == null) {
            log.debug("create a new OCH node");
            ochNode = new NodeBuilder()
                    .setNodeId(nodeId)
                    .setSupportingNode(new LinkedList<>())
                    .setTerminationPoint(new LinkedList<>())
                    .setKey(new NodeKey(nodeId))
                    .build();

            TopologyId phyTopoId = new TopologyId(TopoNameConstants.Phy_Topo_Key);
            ochNode.getSupportingNode().add(new SupportingNodeBuilder()
                    .setNodeRef(nodeId)
                    .setTopologyRef(phyTopoId)
                    .setKey(new SupportingNodeKey(nodeId, phyTopoId))
                    .build());
        }

        log.debug("add a new OCH TP");
        FrequencyType lowerFrequency = frequency.getLowerFrequency();
        FrequencyType upperFrequency = frequency.getUpperFrequency();
        long central = (upperFrequency.getValue().longValue() - lowerFrequency.getValue().longValue()) / 2 + lowerFrequency.getValue().longValue();
        FrequencyType centralFrequency = new FrequencyType(BigInteger.valueOf(central));

        ochNode.getTerminationPoint().add(new TerminationPointBuilder()
                .setTpId(tpId)
                .setKey(new TerminationPointKey(tpId))
                .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                        .setOch(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.tp.attributes.OchBuilder()
                                .setCentralFrequency(centralFrequency).build())
                        .build())
                .build());

        return ochNode;
    }

    /**
     * XC sequence is ODU OCH ... OCH ODU 加入OCHP 以后，frequency 不在第一个
     *
     * @param info
     * @return
     */
    private Available getOchLinkFrequency(RouteInfo info) {
//        return CrossConnectionSlotNamingRule.getFrequencyScope(info.getMain().getXcs().get(1));
        CrossConnections freqXc = null;

        for (CrossConnections xc : info.getMain().getXcs()) {
            if (xc.getSourceTp() != null && xc.getSourceTp().get(0).getSlot() != null) {
                String slot = xc.getDestinationTp().get(0).getSlot();
                if (slot.contains("frequency")) {
                    freqXc = xc;
                    break;
                }
            } else if (xc.getDestinationTp() != null && xc.getDestinationTp().get(0).getSlot() != null) {
                String slot = xc.getDestinationTp().get(0).getSlot();
                if (slot.contains("frequency")) {
                    freqXc = xc;
                    break;
                }
            }
        }
        if (freqXc != null) {
            return CrossConnectionSlotNamingRule.getFrequencyScope(freqXc);
        }

        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "cannot find any frequency XC");
    }

    private Och getOchLinkAttr(Class<? extends ProtectionType> protectionType, RouteInfo info, Available frequencyScope, String friendlyName, String aTp, String zTp) {
        Boolean legRequired = protectionType.equals(ProtectionBidir1To2.class) && info.getThird() == null;
        Och och = new OchBuilder()
                .setPlaneName(param.getPlaneName())
                .setRiskGroupName(param.getRiskGroupName())
                .setFriendlyName(friendlyName)
                .setCreationTime(param.getCreationTime())
                .setAdminState(AdminStatus.Down)
                .setAlarmState(AlarmSeverity.Unknown)
                .setProtectionType(protectionType)
                .setOchpCardType((info.getSlave() != null) ? param.getOpCardType() : null)
                .setOperationalState(OperStatus.Unknown)
                .setImplementState(ImplementState.Allocate)
                .setAlignmentStatus(AlignmentStatusType.Unknown)  //this status is useless for link
                .setExplictRoute(new Route(info, Route.RouteType.OchLink).getExplictRoute(aTp, zTp))
                .setLowerFrequency(frequencyScope.getLowerFrequency())
                .setUpperFrequency(frequencyScope.getUpperFrequency())
                .setOrderId(getOrderId())
                .setOdukType(param.getOdukType())
                .setSlotGranularity(param.getOchOduGranularity())
//          .setBandwidth(String.valueOf(param.getLineRateNumber()))
                .setAvailable(buildOduAvailable())
                .setProperties(getProperties(legRequired))
                .setCardType(param.getCardType())
                .setProductType(productType)
                .setVendorName(vendorName)
                .setPlaneName(param.getPlaneName())
                .setPlaneId(param.getPlaneId())
                .build();

//    available value changed at outside, because this is common attribte of OCH
//  will be changed after new tunnel created.

        return och;
    }
    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.Available> buildOduAvailable() {
        List avaList = new ArrayList();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.Available oduAvailable = new AvailableBuilder()
                .setSupportedOduj(param.getOchOduGranularity())
                .setAvailableOdujSlot(getInitialOdujSlot(1, param.getClientLineRate()))
                .setKey(new AvailableKey(param.getOchOduGranularity()))
                .build();
        avaList.add(oduAvailable);
        return avaList;
    }


    private static String getInitialOdujSlot(int start, int stop) {
        String tmp = "";

        for (int i = start; i <= stop; ++i) {
            if (i == stop) {
                tmp = tmp + i;
            } else {
                tmp = tmp + i + ODUJ_SPLIT;
            }
        }

        return tmp;
    }

    private Properties getProperties(Boolean legRequired) {
        ArrayList<Property> properties = new ArrayList<>();
//        Property elem = new PropertyBuilder()
//                .setName("product-type")
//                .setKey(new PropertyKey("product-type"))
//                .setValue(productType)
//                .build();
//        properties.add(elem);
//        elem = new PropertyBuilder()
//                .setName("card-type")
//                .setKey(new PropertyKey("card-type"))
//                .setValue(param.getCardType())
//                .build();
//        properties.add(elem);
        if (overNetwork) {
            Property elem = new PropertyBuilder()
                    .setName("over-network")
                    .setKey(new PropertyKey("over-network"))
                    .setValue("true")
                    .build();
            properties.add(elem);
        }
        if (legRequired) {
            Property elem = new PropertyBuilder()
                    .setName(TunnelBinder.LEG_REQUIRED)
                    .setKey(new PropertyKey(TunnelBinder.LEG_REQUIRED))
                    .setValue("true")
                    .build();
            properties.add(elem);
        }

        return new PropertiesBuilder().setProperty(properties).build();
    }

    private List<String> getOrderId() {
        List<String> rst = new LinkedList<>();
        rst.add(param.getOrderId());
        return rst;
    }

    public RouteInfo getOchRouteInfo(RouteInfo info, List<String> primaryOpticalLayerLinkIds, List<String> secondaryOpticalLayerLinkIds) {
        RouteInfo.RouteInfoBuilder ochInfoBuilder = RouteInfo.builder()
                .main(net.flex.dci.otn.controller.allocate.designer.model.Route.builder()
                        .nodes(info.getMain().getNodes())  //nodes info useless in route
                        .links(info.getMain().getLinks())
                        .xcs(info.getMain().getXcs())
                        .build());

        if (info.getSlave() != null) {
            ochInfoBuilder.slave(net.flex.dci.otn.controller.allocate.designer.model.Route.builder()
                    .nodes(info.getSlave().getNodes())  //nodes info useless in route
                    .links(info.getSlave().getLinks())
                    .xcs(info.getSlave().getXcs())
                    .build());
        }

        RouteInfo ochInfo = ochInfoBuilder.build();

        List<Link> opticalLayerLinks = new ArrayList<>();
        if (primaryOpticalLayerLinkIds != null) {
            for (String linkId : primaryOpticalLayerLinkIds) {
                if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                    opticalLayerLinks.add(changedObject.getChangedSiteLink(linkId));
                } else {
                    //this is wss link
                    opticalLayerLinks.add(changedObject.getChangedPhyLink(linkId));
                }
            }

            ochInfo.getMain().getLinks().addAll(ochInfo.getMain().getLinks().size() / 2, opticalLayerLinks);
        }

        opticalLayerLinks.clear();
        if (secondaryOpticalLayerLinkIds != null) {
            for (String linkId : secondaryOpticalLayerLinkIds) {
                if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                    opticalLayerLinks.add(changedObject.getChangedSiteLink(linkId));
                } else {
                    //this is wss link
                    opticalLayerLinks.add(changedObject.getChangedPhyLink(linkId));
                }
            }

            ochInfo.getSlave().getLinks().addAll(ochInfo.getSlave().getLinks().size() / 2, opticalLayerLinks);
        }

        return ochInfo;
    }

    /**
     * 有瑕疵，一般情况下OCH link由两条osLink （L口到MUX/DMUX口）和一系列siteLink构成 但是如果osLink有多条的情况，这个方法就有问题
     *
     * @param info
     */
    private List<SupportingLink> getSupporingLink(RouteInfo info) {
        List<SupportingLink> rst = new ArrayList<>();
        Set<SupportingLink> supportingLinkList = new HashSet<>();

        for (Link link : info.getMain().getLinks()) {
            supportingLinkList.add(new SupportingLinkBuilder()
                    .setLinkRef(link.getLinkId())
                    .setKey(new SupportingLinkKey(link.getLinkId()))
                    .build());

        }

        if (info.getSlave() != null) {
            if (info.getSlave().getLinks() != null) {
                for (Link link : info.getSlave().getLinks()) {
                    supportingLinkList.add(new SupportingLinkBuilder()
                            .setLinkRef(link.getLinkId())
                            .setKey(new SupportingLinkKey(link.getLinkId()))
                            .build());

                }
            }
        }

        if (info.getThird() != null) {
            if (info.getThird().getLinks() != null) {
                for (Link link : info.getThird().getLinks()) {
                    supportingLinkList.add(new SupportingLinkBuilder()
                            .setLinkRef(link.getLinkId())
                            .setKey(new SupportingLinkKey(link.getLinkId()))
                            .build());
                }
            }
        }

        rst.addAll(supportingLinkList);
        return rst;
    }

    /**
     * 基于oduXC 的slot信息发现L口，L口就是ochTP
     *
     * @param info
     * @return
     */
    private String[] getOchLinkTp(RouteInfo info) {
        String[] tpIds = new String[2];

        CrossConnections oduXc = info.getMain().getXcs().get(0);
        tpIds[0] = CrossConnectionSlotNamingRule.getLinePortFromOduXc(oduXc);

        oduXc = info.getMain().getXcs().get(info.getMain().getXcs().size() - 1);
        tpIds[1] = CrossConnectionSlotNamingRule.getLinePortFromOduXc(oduXc);

        return tpIds;
    }

}
