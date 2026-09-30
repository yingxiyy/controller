package net.flex.dci.otn.controller.nms.constructs;

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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.object.resource.type.link.LinkHopBuilder;

/**
 * @version 1.0
 * @date 2022/11/30 17:01
 */
@Slf4j
public class MUXPANELConstructor {


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
                .setDestTp(new TpId(linkDto.getDestinationTp()));
        SourceBuilder sb = new SourceBuilder()
                .setSourceNode(NodeId.getDefaultInstance(linkDto.getSourceNodeId()))
                .setSourceTp(
                        new TpId(linkDto.getSourceTp()));
        lb.setDestination(db.build()).setSource(sb.build());
        return lb;
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

}
