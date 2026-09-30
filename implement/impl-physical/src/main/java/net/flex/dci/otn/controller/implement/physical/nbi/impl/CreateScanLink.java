/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.nbi.impl;

import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.implement.common.nbi.impl.BaseImpl;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateScanLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateScanLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateScanLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;

/**
 * @author YYX
 * @version 1.0
 */

@Slf4j
public class CreateScanLink extends BaseImpl {

    private final PhyLinkDao phyLinkDao;
    private final PhyNodeDao phyNodeDao;

    private Node srcNode;
    private Node dstNode;
    private TerminationPoint srcTp;
    private TerminationPoint dstTp;
    private LinkType linkType;

    public CreateScanLink() {
        phyLinkDao = SpringBeanFinder.getBean(PhyLinkDao.class);
        phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
    }

    public static DateAndTime getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String str = sdf.format(new Date());
        String str1 = str.substring(0, str.length() - 2);
        String str2 = str.substring(str.length() - 2);
        String sb = str1
                + ":"
                + str2;
        return DateAndTime.getDefaultInstance(sb);

    }

    /**
     * support change displayName and provider info
     *
     * @param input
     * @return
     * @throws CommonException
     */
    @Override
    public CreateScanLinkOutput doIt(CreateScanLinkInput input) throws CommonException {
        checkParam(input);

        LinkId linkId = new LinkId(PhysicalLinkIdNamingRule.createLinkId(srcTp.getTpId().getValue(),
                dstTp.getTpId().getValue(), linkType));
        String friendlyName = getFriendlyName(srcNode, dstNode, srcTp, dstTp);
        Link link = new LinkBuilder()
                .setLinkId(linkId)
                .setKey(new LinkKey(linkId))
                .setSource(new SourceBuilder()
                        .setSourceNode(srcNode.getNodeId())
                        .setSourceTp(srcTp.getTpId())
                        .build())
                .setDestination(new DestinationBuilder()
                        .setDestNode(dstNode.getNodeId())
                        .setDestTp(dstTp.getTpId())
                        .build())
                .addAugmentation(Link1.class, new Link1Builder()
                        .setPhysical(new PhysicalBuilder()
                                .setAdminState(AdminStatus.Down)
                                .setOperationalState(OperStatus.Down)
                                .setImplementState(ImplementState.Allocate)
                                .setCreationTime(getCurrentTime())
                                .setDirection(LinkDirection.Unidirection)
                                .setLinkType(linkType)
                                .setFriendlyName(friendlyName)
                                .build())
                        .build())
                .build();

        //make TP as busy
        makeBusyOnTp(srcNode, srcTp);
        makeBusyOnTp(dstNode, dstTp);
        phyLinkDao.savePhyLink(link);
        taskInfoMessage.setResourceId(link.getLinkId().getValue());
        logMessage(BroadCastConstant.CREATE_PHY_LINK, friendlyName, BLANK);

        startImplIt(link);
        return new CreateScanLinkOutputBuilder()
                .setReturnCode(RpcResultType.Success)
                .build();
    }

    private void makeBusyOnTp(Node node, TerminationPoint tp) {
        Physical tpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();
        TerminationPoint newTp = new TerminationPointBuilder(tp)
                .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                        .setPhysical(
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                                        tpAttr)
                                        .setConnectionStatus(ConnectionStatus.Busy)
                                        .build())
                        .build())
                .build();
        phyNodeDao.saveConfigPhyTpById(node.getNodeId().getValue(), newTp);
    }

    private void startImplIt(Link link) {
        log.debug("start impl phy link");

        new Thread(() -> {
            PhysicalLinkImpl impl = new PhysicalLinkImpl(taskInfoMessage.getWho(), link);
            impl.start();
        }).start();
    }

    private void checkParam(CreateScanLinkInput input) throws CommonException {
        log.info("Begin to create link {}--{}", input.getSrcTpId(), input.getDstTpId());
        if (null == input.getSrcTpId() || null == input.getDstTpId()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "tp id cannot be null");
        }
        srcTp = checkNodeTp(input.getSrcNodeId(), input.getSrcTpId(), true);
        dstTp = checkNodeTp(input.getDstNodeId(), input.getDstTpId(), false);

        //srcTp or dstTp must one of OTDR/OCM , one is OA/ILA
        linkType = getLinkType(srcTp, dstTp);
    }

    private LinkType getLinkType(TerminationPoint srcTp, TerminationPoint dstTp)
            throws CommonException {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical srcTpAttr = srcTp.getAugmentation(
                TerminationPoint1.class).getPhysical();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical dstTpAttr = dstTp.getAugmentation(
                TerminationPoint1.class).getPhysical();
        if (PortType.OTDR.equals(srcTpAttr.getPortType()) && PortType.OTDR.equals(
                dstTpAttr.getPortType())) {
            return LinkType.OtdrLink;  //otdr port
        }
        if (PortType.MON.equals(srcTpAttr.getPortType()) && PortType.MON.equals(
                dstTpAttr.getPortType())) {
            return LinkType.OcmLink;  //ocm port
        }

        throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "create link only support create OTDR/OCM related. ");
    }

    private TerminationPoint checkNodeTp(String nodeId, String tpId, boolean isSrcNode)
            throws CommonException {
        Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
        if (null == node) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "node is not managed now " + nodeId);
        }
        Optional<TerminationPoint> tpOp = node.getTerminationPoint().parallelStream()
                .filter(x -> x.getTpId().getValue().equals(tpId)).findAny();
        if (!tpOp.isPresent()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "tp is not managed now " + tpId);
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical nodeAttr = node.getAugmentation(
                Node1.class).getPhysical();
        String eqId = PhysicalTpIdNamingRule.getEquipId(tpId);
        Optional<Equipments> eqOp = nodeAttr.getEquipments().parallelStream()
                .filter(x -> x.getEquipmentId().equals(eqId)).findAny();
        if (!eqOp.isPresent()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "tp related Equipment is not managed now " + tpId);
        }
        Equipments eq = eqOp.get();
        List<EquipType> possibleCard = Arrays.asList(EquipType.OTDR, EquipType.OCM, EquipType.OA,
                EquipType.ILA, EquipType.WSS);
        if (!possibleCard.contains(eq.getEquipType())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "tp isn't OTDR/OCM possible" + tpId);
        }
        TerminationPoint tp = tpOp.get();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr = tp.getAugmentation(
                TerminationPoint1.class).getPhysical();
        if (tpAttr.getConnectionStatus() != null && tpAttr.getConnectionStatus()
                .equals(ConnectionStatus.Busy)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("tp %s has been used " + tpAttr.getFriendlyName()));
        }

        if (isSrcNode) {
            srcNode = node;
        } else {
            dstNode = node;
        }
        return tp;
    }

    private String getFriendlyName(Node srcNode, Node dstNode, TerminationPoint srcTp,
            TerminationPoint dstTp) {
        String srcNodeName = srcNode.getAugmentation(Node1.class).getPhysical().getFriendlyName();
        String dstNodeName = dstNode.getAugmentation(Node1.class).getPhysical().getFriendlyName();
        String srcTpName = srcTp.getAugmentation(TerminationPoint1.class).getPhysical()
                .getFriendlyName();
        String dstTpName = dstTp.getAugmentation(TerminationPoint1.class).getPhysical()
                .getFriendlyName();

        String friendlyName = "";
        if (srcNodeName.equals(dstNodeName)) {
            friendlyName = srcNodeName;
            friendlyName = srcNodeName + "#" + srcTpName + "--" + dstTpName;
        } else {
            friendlyName = srcNodeName + "#" + srcTpName + "--" + dstNodeName + "#" + dstTpName;
        }
        return friendlyName;
    }
}
