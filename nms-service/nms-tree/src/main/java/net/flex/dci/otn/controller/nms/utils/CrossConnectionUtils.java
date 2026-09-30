package net.flex.dci.otn.controller.nms.utils;

import static net.flex.dci.otc.common.constants.Constants.HYPHEN;
import static net.flex.dci.otc.common.constants.Constants.SLASH;
import static net.flex.dci.otn.controller.nms.utils.Constants.BLANK;
import static net.flex.dci.otn.controller.nms.utils.Constants.COMMA;
import static net.flex.dci.otn.controller.nms.utils.Constants.Parentheses_LEFT;
import static net.flex.dci.otn.controller.nms.utils.Constants.Parentheses_RIGHT;
import static net.flex.dci.otn.controller.nms.utils.Constants.WSS_SIG_PORT;
import static net.flex.dci.otn.controller.nms.utils.Constants.XC_PREFIX;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.MuxCardPortFormatting;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.dto.wss.WssXcTp;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsKey;

/**
 * @version 1.0
 * @date 2022/7/8 12:41
 */
@Slf4j
public class CrossConnectionUtils {

    private static final String FREQUENCY_PREFIX = "/frequency=";

    private static final String CROSS_CONNECTION_PREFIX = "XC-";


    public static List<String> getCrossConnectionRefTpIds(CrossConnections xc) {
        log.debug("get all xc ref tpIds");
        List<String> tpIds = new ArrayList<>();
        List<String> sourceTpIds = xc.getSourceTp().stream()
                .map(sourceTp -> sourceTp.getTpRef().getValue()).collect(
                        Collectors.toList());
        List<String> destTpIds = xc.getDestinationTp().stream()
                .map(destinationTp -> destinationTp.getTpRef().getValue()).collect(
                        Collectors.toList());
        tpIds.addAll(sourceTpIds);
        tpIds.addAll(destTpIds);
        return tpIds;
    }

    public static String getRefWssChannelName(NeYangModel model, CrossConnections xc) {
        log.debug("get wss xc channel name");
        MuxCardPortFormatting formatting = new MuxCardPortFormatting(model);

        List<String> tpIds = getCrossConnectionRefTpIds(xc);
        List<String> muxTpIds = tpIds.stream().filter(tpId -> {
            Pattern pattern = Pattern.compile(formatting.getPortMatchingRegex());
            Matcher matcher = pattern.matcher(tpId);
            return matcher.find();
        }).collect(Collectors.toList());
        //assem the tp only have one
        if (!muxTpIds.isEmpty()) {
            String muxTpId = muxTpIds.get(0);
            Pattern pattern = Pattern.compile(model.muxPortMatchingRegex());
            Matcher matcher = pattern.matcher(muxTpId);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return BLANK;
    }

    public static String generateWssCrossName(CrossConnections xc, long center) {
        String xcId = xc.getCrossConnectionId().getValue();
        log.debug("generate wss channel name for the cross connection id is:{}", xcId);
        //assume src and dest tp is only one tp
        String xcNamePrefix = getXcPrefix(xc);
        String srcTpId = xc.getSourceTp().get(0).getTpRef().getValue();
        String destTpId = xc.getDestinationTp().get(0).getTpRef().getValue();
        String srcSimpleId = PhysicalTpIdNamingRule.getShortTpByTpId(srcTpId);
        String destSimpleId = PhysicalTpIdNamingRule.getShortTpByTpId(destTpId);
        WssXcTp xcTp = getWssXcTp(srcSimpleId, destSimpleId);
        //assume the wss cross connection is exp/adddrop ->sig
        String xcNameBuilder = xcNamePrefix + HYPHEN + xcTp.getNotSigPort()
                + SLASH
                + xcTp.getSigPort()
                + Parentheses_LEFT
                + center
                + Parentheses_RIGHT;
        return xcNameBuilder;
    }

    private static String getXcPrefix(CrossConnections xc) {
        String description = xc.getDescription();
        int index = description.indexOf(HYPHEN);
        return xc.getDescription().substring(0, index);
    }

    /**
     * route sequence cross connection
     *
     * @param xc
     * @return
     */
    public static boolean isInternalCrossConnection(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections xc) {
        log.debug("the cross connection is internal or external ");
        String srcTpId = xc.getSourceTp().get(0).getTpRef().getValue();
        String destTpId = xc.getDestinationTp().get(0).getTpRef().getValue();
        return PhysicalTpIdNamingRule.getEquipId(srcTpId)
                .equals(PhysicalTpIdNamingRule.getEquipId(destTpId));
    }

    private static WssXcTp getWssXcTp(String srcSimpleId, String destSimpleId) {
        WssXcTp xcTp = null;
        String sigPort = null;
        String notSigPort = null;
        if (srcSimpleId.contains(WSS_SIG_PORT)) {
            sigPort = srcSimpleId.substring(srcSimpleId.lastIndexOf(HYPHEN) + 1);
            notSigPort = destSimpleId;
        } else {
            sigPort = destSimpleId.substring(destSimpleId.lastIndexOf(HYPHEN) + 1);
            notSigPort = srcSimpleId;
        }
        xcTp = WssXcTp.builder().notSigPort(notSigPort).sigPort(sigPort).build();
        return xcTp;
    }


    public static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections generateMuxCrossConnection(
            String sourceTpId, String destTpId,
            FrequencyType lower, FrequencyType upper) {
        String frequency = constructFrequency(lower, upper);
        long centerFrequency = (lower.getValue().longValue() + upper.getValue().longValue()) >> 1;
        String xcId = XC_PREFIX + sourceTpId + FREQUENCY_PREFIX
                + centerFrequency
                + HYPHEN + destTpId + FREQUENCY_PREFIX + centerFrequency;
//        Uri xcId = new Uri(
//                "XC-" + srcTp.getValue() + "/frequency=" + centorFrequency + "-" + dstTp
//                        .getValue() + "/frequency=" + centorFrequency);
        List<SourceTp> srcTps = new LinkedList<>();
        srcTps.add(new SourceTpBuilder()
                .setTpRef(new TpId(sourceTpId))
                .setSlot(frequency)
                .build());

        List<DestinationTp> dstTps = new LinkedList<>();
        dstTps.add(new DestinationTpBuilder()
                .setTpRef(new TpId(destTpId))
                .setSlot(frequency)
                .build());
        NodeId refNodeId = NodeId.getDefaultInstance(
                PhysicalTpIdNamingRule.getNodeId(sourceTpId));
        CrossConnectionsBuilder cb = new CrossConnectionsBuilder()
                .setNodeRef(refNodeId)
                .setSourceTp(srcTps)
                .setDestinationTp(dstTps)
                .setAdminState(AdminStatus.Up)
                .setDirection(LinkDirection.Bidirection)
                .setFixed(true)
                .setImplementState(ImplementState.Implement)
                .setOperationalState(OperStatus.Up)
                .setCrossConnectionId(Uri.getDefaultInstance(xcId));
        return cb.build();
    }


    private static String constructFrequency(FrequencyType lower, FrequencyType upper) {
        String frequencyBuilder = FREQUENCY_PREFIX + lower.getValue().toString()
                + COMMA
                + upper.getValue().toString();
        return frequencyBuilder;
    }

    /**
     * filter not in one card cross connection and transfer cross connection to route style cross
     * connection
     *
     * @param crossConnections
     * @return
     */
    public static List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> refactorSequenceCrossConnections(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> crossConnections) {
        Long seqNo = 1L;
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> refactorXcs = new ArrayList<>();
        //to filter cross connection on one card
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> inOneCardCrossConnections = crossConnections.stream()
                .filter(xc -> {
                    Set<String> sourceEquip = xc.getSourceTp().stream()
                            .map(tp -> PhysicalTpIdNamingRule.getEquipId(tp.getTpRef().getValue()))
                            .collect(Collectors.toSet());
                    Set<String> destEquip = xc.getDestinationTp().stream()
                            .map(tp -> PhysicalTpIdNamingRule.getEquipId(tp.getTpRef().getValue()))
                            .collect(Collectors.toSet());
                    if (sourceEquip.size() > 1) {
                        return false;
                    }
                    return CommonUtils.getDifferenceSetByGuava(sourceEquip, destEquip).isEmpty();
                }).collect(
                        Collectors.toList());
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections crossConnection : inOneCardCrossConnections) {
            CrossConnectionsBuilder connectionsBuilder = new CrossConnectionsBuilder(
                    crossConnection);
            connectionsBuilder.setSequence(seqNo);
            connectionsBuilder.setKey(new CrossConnectionsKey(seqNo));
            seqNo++;
            refactorXcs.add(connectionsBuilder.build());
        }
        return refactorXcs;
    }

    public static Uri generateMuxCrossConnectionId(String aTpId, String zTpId) {
        String stringBuilder = CROSS_CONNECTION_PREFIX + aTpId
                + HYPHEN
                + zTpId;

        return Uri.getDefaultInstance(stringBuilder);
    }
}
