/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.constructs;

import static net.flex.dci.otn.controller.nms.utils.Constants.MD_PORT_PATTERN;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.dto.LinkDto;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.link.LinkHopBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;

/**
 * @version 1.0
 * @date 2021/11/25 13:08
 */
@Slf4j
public class CMUX64Constructor {


    /**
     * virtualize the mpo xc merge 8 mpo port to general port num mpo
     *
     * @param xc
     * @param muxChannelTpId
     * @return
     */
    public static CrossConnections virtualizeTheXC(CrossConnections xc,
            String muxChannelTpId) {
        log.debug("virtualize the mpo xc connection");
        CrossConnectionsBuilder crossConnectionsBuilder = new CrossConnectionsBuilder(xc);
        List<SourceTp> sourceTps = crossConnectionsBuilder.getSourceTp();
        SourceTp virtualTp = generateVirtualTp(sourceTps, muxChannelTpId);
        crossConnectionsBuilder.setSourceTp(Arrays.asList(virtualTp));
        return crossConnectionsBuilder.build();
    }

    /**
     * merge into one virtual port
     *
     * @param sourceTps
     * @param muxChannelTpId
     * @return
     */
    private static SourceTp generateVirtualTp(List<SourceTp> sourceTps,
            String muxChannelTpId) {
        log.debug("start to merge into one virtual port,tp list is {}", sourceTps);
        SourceTp sourceTp = sourceTps.get(0);
        SourceTpBuilder sourceTpBuilder = new SourceTpBuilder();
        String tpId = sourceTp.getTpRef().getValue();
        String virtualTpId = getMPOTpId(tpId.substring(0, tpId.length()), muxChannelTpId);
        sourceTpBuilder.setTpRef(new TpId(virtualTpId));
        return sourceTpBuilder.build();
    }

    /**
     * virtual mpo tp
     *
     * @return
     */
    public static TerminationPoint virtualizeMPOTP(TerminationPoint tp, String tpId) {
        log.debug("generate virtual tp from relative tp");
        TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder(
                tp);
        String logicalMpoId = tpId.substring(0, tpId.length() - 1);
        terminationPointBuilder.setTpId(TpId.getDefaultInstance(logicalMpoId));
        terminationPointBuilder.setKey(new TerminationPointKey(
                TpId.getDefaultInstance(logicalMpoId)));
        TerminationPoint1 terminationPoint1Builder = terminationPointBuilder.getAugmentation(
                TerminationPoint1.class);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder physicalTp = new TerminationPoint1Builder(
                terminationPoint1Builder);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder physicalBuilder = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                physicalTp.getPhysical());
        physicalBuilder.setFriendlyName(physicalBuilder.getFriendlyName()
                .substring(0, physicalBuilder.getFriendlyName().length() - 1));
        physicalTp.setPhysical(physicalBuilder.build());
        terminationPointBuilder.addAugmentation(TerminationPoint1.class, physicalTp.build());
        return terminationPointBuilder.build();
    }

    /**
     * generate virtual phy link for the connection
     *
     * @param muxChannelTpId
     * @param lb
     * @param linkId
     * @return
     */
    public static LinkHopBuilder generateMpoLinkRouteDetail(String muxChannelTpId,
            LinkHopBuilder lb, String linkId) {
        log.debug("generate phy link for the tp have the mpo,link id :{}", linkId);
        lb.setLinkId(LinkId.getDefaultInstance(linkId));
        LinkDto linkDto = PhysicalLinkIdNamingRule.extractPhyLinkDetail(linkId);

        DestinationBuilder db = new DestinationBuilder()
                .setDestNode(NodeId.getDefaultInstance(linkDto.getDestinationNodeId()))
                .setDestTp(new TpId(getMPOTpId(linkDto.getDestinationTp(), muxChannelTpId)));
        SourceBuilder sb = new SourceBuilder()
                .setSourceNode(NodeId.getDefaultInstance(linkDto.getSourceNodeId()))
                .setSourceTp(
                        new TpId(getMPOTpId(linkDto.getSourceTp(), muxChannelTpId)));
        lb.setDestination(db.build()).setSource(sb.build());
        return lb;
    }

    /**
     * get tp id
     *
     * @param tp
     * @param muxChannelTpId
     * @return
     */
    public static String getMPOTpId(String tp, String muxChannelTpId) {
        if (muxChannelTpId == null) {
            return tp;
        }
        Pattern pattern = Pattern.compile(MD_PORT_PATTERN);
        Matcher matcher = pattern.matcher(muxChannelTpId);
        String channelIndex = matcher.find() ? matcher.group(1) : "1";
//        char channelIndex = muxChannelTpId.charAt(muxChannelTpId.length() - 1);
        int index = Integer.parseInt(channelIndex);
        double mpoIndex = Math.ceil((double) index / 8d);
        return tp + (int) mpoIndex;
    }
}
