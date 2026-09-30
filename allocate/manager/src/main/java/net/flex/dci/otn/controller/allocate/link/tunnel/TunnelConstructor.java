/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.tunnel;

import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.namingrule.CrossConnectionSlotNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.TunnelIdNamingRule;
import net.flex.dci.otn.controller.allocate.common.namingrule.TunnelFriendlyName;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.link.common.Route;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.attributes.SupportingLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.base.attributes.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.TunnelLayer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author YYX
 * @version 1.0
 */
public class TunnelConstructor {

    private ParamCreate param;

    private Link ochLink;

    public TunnelConstructor(ParamCreate param) {
        this.param = param;
    }

    public Tunnel create(String[] tpIds, RouteInfo info, Link ochLink, Node srcNode, Node dstNode) {
        this.ochLink = ochLink;

        Tunnel tunnel = new TunnelBuilder()
                .setTunnelId(new Uri(TunnelIdNamingRule.generateId(tpIds[0], tpIds[1])))
                .setSupportingLink(getSupportingLink())
                .setSignalRate(param.getTunnelSignalRate())
                .setSourceTp(getSourceTp(tpIds[0]))
                .setDestinationTp(getDestinationTp(tpIds[1]))
                .setImplementState(ImplementState.Allocate)
                .setOperationalState(OperStatus.Unknown)
                .setAlignmentStatus(AlignmentStatusType.Unknown)
                .setFriendlyName(TunnelFriendlyName.buildFriendlyName(
                        srcNode, dstNode,
                        tpIds[0],
                        tpIds[1],
                        ochLink.getSource().getSourceTp().getValue(),
                        ochLink.getAugmentation(Link1.class).getOch().getOdukType()))
                .setExplictRoute(new Route(getTunnelRouteInfo(info), Route.RouteType.Tunnel)
                        .getExplictRoute(tpIds[0], tpIds[1]))
                .setRiskGroupName(param.getRiskGroupName())
                .setPlaneName(param.getPlaneName())
                .setPlaneId(param.getPlaneId())
                .setOrderId(getOrderId())
                .setCustomer(param.getCustomer())
                .setTunnelLayer(TunnelLayer.ETH)
                .setCreationTime(param.getCreationTime())
                .setAlarmState(AlarmSeverity.Unknown)
                .setAdminState(AdminStatus.Down)
                .setCustomer(param.getCustomer())
//            .setProtectionType(ProtectionUnprotected.class)
                .setProtectionType(param.getProtectionType())
                .setProperties(getExternalProperties(ochLink))
//            .setClientPhysicalMedium(param.getClientMediumA())
                .setClientPhysicalMediumA(param.getClientMediumA())
                .setClientPhysicalMediumZ(param.getClientMediumZ())
                .setServiceType(param.getServiceType())
                .build();

        return tunnel;
    }

    private Properties getExternalProperties(Link ochLink) {
        Properties tunnelProps = null;

        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();

        String value = PropertyTool.getValue(ochLinkAttr.getProperties(), "over-network");
        if (value.equalsIgnoreCase("true")) {
            tunnelProps = PropertyTool.addProperty(tunnelProps, "over-network", "true");
        }
        String legValue = PropertyTool.getValue(ochLinkAttr.getProperties(), TunnelBinder.LEG_REQUIRED);
        if (legValue != null && legValue.equalsIgnoreCase("true")) {
            tunnelProps = PropertyTool.addProperty(tunnelProps, TunnelBinder.LEG_REQUIRED, "true");
        }

        boolean found = false;
        String frequency = ochLinkAttr.getLowerFrequency().getValue().toString() + "," + ochLinkAttr.getUpperFrequency().getValue().toString();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink sl : ochLink.getSupportingLink()) {
            String linkId = sl.getLinkRef().getValue();
            if (PhysicalLinkIdNamingRule.isOsLink(linkId)) {
                //是osLink，通过namingRule找到TP点
                String tpId = PhysicalLinkIdNamingRule.getTpAId(linkId);
                String regStr = "M([0-9]+)D([0-9]+)";
                Pattern pattern = Pattern.compile(regStr);
                Matcher matcher = pattern.matcher(tpId);
                if (!matcher.find()) {
                    tpId = PhysicalLinkIdNamingRule.getTpZId(linkId);
                    matcher = pattern.matcher(tpId);
                    found = matcher.find();
                } else {
                    found = true;
                }

                if (found) {
                    frequency = matcher.group(1) + "-" + frequency;

                    PropertyTool.addProperty(tunnelProps, "frequency", frequency);
                    break;
                }
            }
        }

        if (!found) {
            // 电中继， 无光层 tunnel都会到这个代码

            frequency = "1-" + frequency;
            PropertyTool.addProperty(tunnelProps, "frequency", frequency);
        }
        long centFrequency = getCenterFrequency(ochLinkAttr.getUpperFrequency(), ochLinkAttr.getLowerFrequency());
        PropertyTool.addProperty(tunnelProps, "centreFrequency", centFrequency + "");

        return tunnelProps;
    }

    private long getCenterFrequency(FrequencyType upperFrequency, FrequencyType lowerFrequency) {
        return lowerFrequency.getValue().longValue() + (upperFrequency.getValue().longValue() - lowerFrequency.getValue().longValue()) / 2;
    }

    private List<String> getOrderId() {
        List<String> rst = new LinkedList<>();
        rst.add(param.getOrderId());

        return rst;
    }

    private List<DestinationTp> getDestinationTp(String tpId) {
        List<DestinationTp> rst = new LinkedList<>();
        TpId id = new TpId(tpId);
        rst.add(new DestinationTpBuilder()
                .setTpRef(id)
                .setKey(new DestinationTpKey(id))
//            .setAlarmStatus(AlarmSeverity.Unknown)
                .build());

        return rst;
    }

    private List<SourceTp> getSourceTp(String tpId) {
        List<SourceTp> rst = new LinkedList<>();
        TpId id = new TpId(tpId);
        rst.add(new SourceTpBuilder()
                .setTpRef(id)
                .setKey(new SourceTpKey(id))
//            .setAlarmStatus(AlarmSeverity.Unknown)
                .build());

        return rst;
    }

    private List<SupportingLink> getSupportingLink() {
        List<SupportingLink> rst = new LinkedList<>();
        rst.add(new SupportingLinkBuilder()
                .setTopologyRef(new TopologyId(TopoNameConstants.Och_Topo_Key))
                .setLinkRef(ochLink.getLinkId())
                .setKey(new SupportingLinkKey(ochLink.getLinkId(), new TopologyId(TopoNameConstants.Och_Topo_Key)))
                .build());

        return rst;
    }

    private RouteInfo getTunnelRouteInfo(RouteInfo info) {
        RouteInfo tunnelInfo = RouteInfo.builder()
                .main(net.flex.dci.otn.controller.allocate.designer.model.Route.builder()
                        .nodes(new LinkedList<>())  //nodes info useless in route
                        .links(new LinkedList<>())
                        .xcs(new LinkedList<>())
                        .build())
                .build();

        tunnelInfo.getMain().getLinks().add(ochLink);
//    if (info.getMain().getXcs().size() >= 2) {
//      //first and latest XC is ODU XC, middle is OCH XC
//      tunnelInfo.getMain().getXcs().add(info.getMain().getXcs().get(0)); //first and latest XC is ODU XC, middle is OCH XC
//      tunnelInfo.getMain().getXcs().add(info.getMain().getXcs().get(info.getMain().getXcs().size() - 1));
//    }

        tunnelInfo.getMain().getXcs().add(info.getMain().getXcs().get(0));
        tunnelInfo.getMain().getXcs().add(info.getMain().getXcs().get(info.getMain().getXcs().size() - 1));

        return tunnelInfo;
    }

    /**
     * 基于oduXC 的slot信息发现C口，C口就是tunnelTP
     *
     * @return
     */
    public static String[] getTunnelTerminationPoint(CrossConnections oduXcA, CrossConnections oduXcZ) {
        String[] tpIds = new String[2];

        tpIds[0] = CrossConnectionSlotNamingRule.getClientPortFromOduXc(oduXcA);
        tpIds[1] = CrossConnectionSlotNamingRule.getClientPortFromOduXc(oduXcZ);

        return tpIds;
    }

    //slot format is  "slot": "/odu4x2=1/odu4=2"
    public static String getOduSlot(CrossConnections oduXc) {
        String slot = oduXc.getDestinationTp().get(0).getSlot();
        if (slot.indexOf("odu") < 0) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "this isn't a ODU XC slot info" + slot + ", " + oduXc);
        }

        String slotId;
        String[] tmp = slot.split("/");
        if (tmp.length >= 3) {
            slotId = tmp[tmp.length - 1];
        } else {
            slot = oduXc.getSourceTp().get(0).getSlot();
            tmp = slot.split("/");
            slotId = tmp[tmp.length - 1];
        }

        tmp = slotId.split("=");
        if (tmp.length == 2) {
            return tmp[1];
        }
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "error happen on XC's slot format " + slot);
    }

    //slot format is  "slot": "/odu4x2=1/odu4=2"
    public static String getOduSlot(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections oduXc) {
        CrossConnections xc = new CrossConnectionsBuilder()
                .setCrossConnectionId(oduXc.getCrossConnectionId())
                .setSourceTp(oduXc.getSourceTp())
                .setDestinationTp(oduXc.getDestinationTp())
                .build();
        return getOduSlot(xc);
    }
}
