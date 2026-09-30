/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.utils;

import static net.flex.dci.otc.common.constants.Constants.HYPHEN;
import static net.flex.dci.otc.common.constants.Constants.POUND;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEFAULT_BAND;
import static net.flex.dci.otn.controller.nms.utils.Constants.MPO_SUFFIX;
import static net.flex.dci.otn.controller.nms.utils.Constants.PORT;
import static net.flex.dci.otn.controller.nms.utils.Constants.UNKNOWN;
import static net.flex.dci.otn.controller.nms.utils.Constants.YANG_MODEL;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.GridFrequency;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.dto.route.LinkRouteDetailsDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.UpdateNodeLocationInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.Prot100GE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTU4;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTUc2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTUc4;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTUc6;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SignalProtocolType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.ThirdBuilder;

/**
 * @date: 2021/3/31
 */
@Slf4j
public class NMSUtils {

//    public static String LAG2MPO(String muxChannelTpId, String tpId) throws Exception {
//        String[] ids = tpId.split(POUND);
//        if (ids.length != 4) {
//            throw new Exception(
//                    "tp ID format must be ");
//        }
//        String equipId = ids[0] + POUND + ids[1] + POUND + ids[2];
//        String[] channelTpIds = muxChannelTpId.split(POUND);
//        String[] channelKeys = channelTpIds[channelTpIds.length - 1].split(MD_SUFFIX); //M1D1
//        if (channelKeys.length != 2) {
//            throw new Exception(
//                    "cannot find out channelID on ");
//        }
//
//        String channelId = channelKeys[1];
//        Integer id = (Integer.parseInt(channelId) - 1) / 8 + 1;
//        String[] slotIds = ids[2].split(HYPHEN); //ids[2] is equip part,  such as MUXPANEL-1-50

    /// /        String mpoId = /                equipId + "#" + "PORT" + "-" + slotIds[1] + "-" +
    /// slotIds[2] + "-" + "MPO" + id;
//        StringBuilder sb = new StringBuilder();
//        sb.append(equipId).append(POUND).append(PORT).append(HYPHEN).append(slotIds[1])
//                .append(HYPHEN)
//                .append(slotIds[2]).append(MPO).append(id);
//        return sb.toString();
//    }
    public static Integer getGigbit(Class<? extends SignalProtocolType> signalRate) {
        if (signalRate.equals(Prot100GE.class)) {
            return 100;
        } else if (signalRate.equals(ProtOTU4.class)) {
            return 100;
        } else if (signalRate.equals(ProtOTUc2.class)) {
            return 200;
        } else if (signalRate.equals(ProtOTUc4.class)) {
            return 400;
        } else if (signalRate.equals(ProtOTUc6.class)) {
            return 600;
        } else {
            return 0;
        }
    }

    public static void checkUpdateNodeInput(UpdateNodeLocationInput input) throws Exception {
        if (input.getSiteId() == null || "".equals(input.getSiteId())) {
            throw new Exception("SiteId is mandatory");
        }
        if (input.getRackId() == null || "".equals(input.getRackId())) {
            throw new Exception("RackId is mandatory");
        }
        if (input.getNodeId() == null || "".equals(input.getNodeId())) {
            throw new Exception("NodeId is mandatory");
        }

        if (input.getUptLocation() == null || "".equals(input.getUptLocation())) {
            throw new Exception("Location is mandatory");
        }
    }


    public static String getEquipIdFromTpId(String tpId) {
        String[] ids = tpId.split(POUND);
        return ids[0] + POUND + ids[1] + POUND + ids[2];
    }

    public static String getEquipRefFromTpId(String tpId) {
        String[] ids = tpId.split(POUND);
        return ids[2];
    }

    public static String getTpRefPhyNodeId(String tpRef) {
        String[] ids = tpRef.split(POUND);
        return ids[0] + POUND + ids[1];
    }

    public static String getNodeRefFromTpId(String tpRef) {
        String[] ids = tpRef.split(POUND);
        return ids[1];
    }


    public static short getOchLinkRefMuxPortIndex(NeYangModel model, Link ochLink) {
//        MuxCardPortFormatting formatting = new MuxCardPortFormatting(model);
        if (ochLink.getSupportingLink() != null) {
            for (SupportingLink sLink : ochLink.getSupportingLink()) {
                Pattern pattern = Pattern.compile(model.muxPortMatchingRegex());
                Matcher matcher = pattern.matcher(sLink.getLinkRef().getValue());
                if (matcher.find()) {
                    String name = matcher.group(0);
                    String[] ids = name.split(model.muxChannelIdKeyword());
                    return Short.parseShort(ids[1]);
                }
            }
        }
        return 0;
    }

    public static String richSiteLinkName(String friendlyName, GridType gridType,
            String bandwidth) {
        GridFrequency gridFrequency = GridFrequency.getFrequencyByGrid(gridType);
        String richLinkName = String.format("%s  (grid=%s  res=%s)",
                friendlyName,
                gridFrequency == null ? UNKNOWN : gridFrequency.getFrequency(),
                bandwidth);
        return richLinkName;
    }


    public static Secondary getSecondary(LinkRouteDetailsDto linkRouteDetailsDto) {
        SecondaryBuilder secondaryBuilder = new SecondaryBuilder();
        secondaryBuilder.setRouteSequence(
                linkRouteDetailsDto.getRouteDetailDto().getPrimary().getRouteSequences());
        secondaryBuilder.setCrossConnections(linkRouteDetailsDto.getRefCrossConnections());
        secondaryBuilder.setSiteSequence(
                linkRouteDetailsDto.getRouteDetailDto().getPrimary().getSites());
        return secondaryBuilder.build();
    }

    public static Third getThird(LinkRouteDetailsDto linkRouteDetailsDto, Short index) {
        ThirdBuilder thirdBuilder = new ThirdBuilder();
        thirdBuilder.setRouteSequence(
                linkRouteDetailsDto.getRouteDetailDto().getPrimary().getRouteSequences());
        thirdBuilder.setCrossConnections(linkRouteDetailsDto.getRefCrossConnections());
        thirdBuilder.setSiteSequence(
                linkRouteDetailsDto.getRouteDetailDto().getPrimary().getSites());
        thirdBuilder.setIndex(index);
        return thirdBuilder.build();

    }


    public static NeYangModel getYangModelBySiteLinks(List<Link> siteLinks) {
        if (siteLinks == null || siteLinks.isEmpty()) {
            return NeYangModel.Tencent;
        }

        for (Link link : siteLinks) {
            if (link == null) {
                continue;
            }
            Properties properties = link.getAugmentation(Link1.class).getSite().getProperties();
            String currentModelName = PropertyTool.getValue(properties, YANG_MODEL);
            if (currentModelName != null) {
                // 成功获取到第一个有效的 YANG model，直接返回
                return NeYangModel.valueOf(currentModelName);
            }
        }
        // 如果遍历后都未获取到有效的YANG model，返回默认值
        return NeYangModel.Tencent;
    }

    public static WDM_Band getWDMBandBySiteLinks(List<Link> siteLinks) {
        if (siteLinks == null || siteLinks.isEmpty()) {
            return WDM_Band.fromString(DEFAULT_BAND);
        }
        // 选取第一条链路作为默认依据
        Link link = siteLinks.get(0);
        String bandStr = link.getAugmentation(Link1.class).getSite().getLinkGroup();
        // 如果对应的 band 为空，则设置默认值
        if (bandStr == null) {
            bandStr = DEFAULT_BAND;
        }
        return WDM_Band.fromString(bandStr);
    }

    public static String generateLogicalMpoTpId(String equipmentId) {
        log.debug("generate logical mpo tp id by equipmentId:{}", equipmentId);
        String slot = PhysicalEqpIdNamingRule.getSlotFromEquipId(equipmentId);
        String shelf = PhysicalEqpIdNamingRule.getShelfFromEquipId(equipmentId);
        return equipmentId + POUND + PORT + HYPHEN + shelf + HYPHEN + slot + HYPHEN + MPO_SUFFIX;
    }

    public static PortType getPortType(TerminationPoint terminationPoint) {
        return terminationPoint.getAugmentation(TerminationPoint1.class).getPhysical()
                .getPortType();
    }
}
