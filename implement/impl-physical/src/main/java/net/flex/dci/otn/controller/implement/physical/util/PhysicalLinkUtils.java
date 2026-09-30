package net.flex.dci.otn.controller.implement.physical.util;

import static net.flex.dci.otn.controller.implement.common.utils.CommonUtils.getCurrentTime;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.LINK_SEPARATOR;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.POUND;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;

/**
 * @version 1.0
 * @date 11/22/2023 4:04 PM
 */
@Slf4j
public class PhysicalLinkUtils {


    private static final List<EquipType> SUPPORTED_SCAN_EQUIPS;

    static {
        SUPPORTED_SCAN_EQUIPS = Arrays.asList(EquipType.ILA, EquipType.OTDR, EquipType.OCM,
                EquipType.OA);
    }

    public static Link generatePhysicalScanLink(String linkId, String friendlyName,
            LinkType linkType,
            String srcNeId, String destNeId, String srcTpId, String destTpId) {
        log.debug("generate physical scan link");
        Link link = new LinkBuilder()
                .setLinkId(LinkId.getDefaultInstance(linkId))
                .setKey(new LinkKey(LinkId.getDefaultInstance(linkId)))
                .setSource(new SourceBuilder()
                        .setSourceNode(NodeId.getDefaultInstance(srcNeId))
                        .setSourceTp(TpId.getDefaultInstance(srcTpId))
                        .build())
                .setDestination(new DestinationBuilder()
                        .setDestNode(NodeId.getDefaultInstance(destNeId))
                        .setDestTp(TpId.getDefaultInstance(destTpId))
                        .build())
                .addAugmentation(Link1.class, new Link1Builder()
                        .setPhysical(new PhysicalBuilder()
                                .setAdminState(AdminStatus.Down)
                                .setOperationalState(OperStatus.Down)
                                .setImplementState(ImplementState.Allocate)
                                .setCreationTime(getCurrentTime())
                                .setDirection(LinkDirection.Bidirection)
                                .setLinkType(linkType)
                                .setFriendlyName(friendlyName)
                                .setSupportedLink(new ArrayList<>())
                                .build())
                        .build())
                .build();
        return link;
    }

    public static String generateLinkFriendlyName(Node srcNode, Node destNode,
            TerminationPoint srcTp, TerminationPoint destTp) {
        log.debug("generate link friendly name");
        String srcNodeId = srcNode.getNodeId().getValue();
        String destNodeId = destNode.getNodeId().getValue();
        String sourceNodeFriendlyName = PhysicalNodeUtils.getNodeFriendlyName(srcNode);
        String destinationNodeFriendlyName = PhysicalNodeUtils.getNodeFriendlyName(destNode);
        String srcTpName = PhysicalNodeUtils.getTerminationPointFriendlyName(srcTp);
        String destTpName = PhysicalNodeUtils.getTerminationPointFriendlyName(destTp);
        StringBuilder sb = new StringBuilder();
        if (srcNodeId.equals(destNodeId)) {
            sb.append(sourceNodeFriendlyName)
                    .append(POUND)
                    .append(srcTpName)
                    .append(LINK_SEPARATOR)
                    .append(destTpName);
        } else {
            sb.append(sourceNodeFriendlyName)
                    .append(POUND)
                    .append(srcTpName)
                    .append(LINK_SEPARATOR)
                    .append(destinationNodeFriendlyName)
                    .append(POUND)
                    .append(destTpName);
        }
        return sb.toString();
    }


    public static boolean supportScanEqType(EquipType relativeEquipType) {
        return SUPPORTED_SCAN_EQUIPS.contains(relativeEquipType);
    }
}
