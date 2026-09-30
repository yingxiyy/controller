/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.nbi.impl;

import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.implement.common.nbi.impl.BaseImpl;
import net.flex.dci.otn.controller.implement.common.utils.AsynchronousExecutor;
import net.flex.dci.otn.controller.implement.physical.util.PhysicalLinkUtils;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateScanLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateScanLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateScanLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.DeleteScanLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.DeleteScanLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateLinkInputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.springframework.stereotype.Component;

/**
 * @author YYX
 * @version 1.0
 */

@Slf4j
@Component
@RequiredArgsConstructor
public class ScanLink extends BaseImpl {

    private final PhysicalLink physicalLink;

//    private Link ntLink = null;
//    private Node srcNode;
//    private Node dstNode;
//    private TerminationPoint srcTp;
//    private TerminationPoint dstTp;
//    private LinkType linkType;


    /**
     * support change displayName and provider info
     *
     * @param input
     * @return
     * @throws CommonException
     */
    @Override
    public CreateScanLinkOutput doIt(CreateScanLinkInput input) throws CommonException {
//        checkParam(input);
//
//        LinkId linkId = new LinkId(PhysicalLinkIdNamingRule.createLinkId(srcTp.getTpId().getValue(),
//                dstTp.getTpId().getValue(), linkType));
//        String friendlyName = getFriendlyName(srcNode, dstNode, srcTp, dstTp);
//        Link link = new LinkBuilder()
//                .setLinkId(linkId)
//                .setKey(new LinkKey(linkId))
//                .setSource(new SourceBuilder()
//                        .setSourceNode(srcNode.getNodeId())
//                        .setSourceTp(srcTp.getTpId())
//                        .build())
//                .setDestination(new DestinationBuilder()
//                        .setDestNode(dstNode.getNodeId())
//                        .setDestTp(dstTp.getTpId())
//                        .build())
//                .addAugmentation(Link1.class, new Link1Builder()
//                        .setPhysical(new PhysicalBuilder()
//                                .setAdminState(AdminStatus.Down)
//                                .setOperationalState(OperStatus.Down)
//                                .setImplementState(ImplementState.Allocate)
//                                .setCreationTime(getCurrentTime())
//                                .setDirection(LinkDirection.Bidirection)
//                                .setLinkType(linkType)
//                                .setFriendlyName(friendlyName)
//                                .build())
//                        .build())
//                .build();
//
//        //make TP as busy
////        makeBusyOnTp(srcNode, srcTp);
////        makeBusyOnTp(dstNode, dstTp);
//        phyLinkDao.savePhyLink(link);
//        taskInfoMessage.setResourceId(link.getLinkId().getValue());
//        logMessage(BroadCastConstant.CREATE_PHY_LINK, friendlyName, null);
//
////        startImplIt(link);
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

//    private void startImplIt(Link link) {
//        TaskInfoMessage implTask = new TaskInfoMessage(taskInfoMessage)
//                .setActionType(TaskInfoMessage.ActionType.implement);
//        new Thread(() -> {
//            PhysicalLink impl = new PhysicalLink();
//            impl.setTaskInfo(implTask);
//            UpdateLinkInput input = new UpdateLinkInputBuilder()
//                    .setLinkId(link.getLinkId())
//                    .setPhysical(new PhysicalBuilder()
//                            .setImplementState(ImplementState.Implement)
//                            .setAdminState(AdminStatus.Up)
//                            .build())
//                    .build();
//            impl.doIt(input);
//        }).start();
//    }

    private void checkParam(CreateScanLinkInput input) throws CommonException {
//        log.info("Begin to create link {}--{}", input.getSrcTpId(), input.getDstTpId());
//        if (null == input.getSrcTpId() || null == input.getDstTpId()) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "tp id cannot be null");
//        }
//        srcTp = checkNodeTp(input.getSrcNodeId(), input.getSrcTpId(), true);
//        dstTp = checkNodeTp(input.getDstNodeId(), input.getDstTpId(), false);
//
//        //srcTp or dstTp must one of OTDR/OCM , one is OA/ILA
//        linkType = getLinkType(srcTp, dstTp);
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

//    private TerminationPoint checkNodeTp(String nodeId, String tpId, boolean isSrcNode)
//            throws CommonException {

    /// /        Node node = phyNodeDao.getOpPhyNodeById(nodeId); /        if (null == node) { /
    ///        throw new CommonException(CommonExceptionType.INVALID_PARAMETER, /
    /// "node is not managed now " + nodeId); /        } /        Optional<TerminationPoint> tpOp =
    /// node.getTerminationPoint().parallelStream() /                .filter(x ->
    /// x.getTpId().getValue().equals(tpId)).findAny(); /        if (!tpOp.isPresent()) { /
    ///   throw new CommonException(CommonExceptionType.INVALID_PARAMETER, /                    "tp
    /// is not managed now " + tpId); /        } /
    /// org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical
    /// nodeAttr = node.getAugmentation( /                Node1.class).getPhysical(); /
    /// String eqId = PhysicalTpIdNamingRule.getEquipId(tpId); /        Optional<Equipments> eqOp =
    /// nodeAttr.getEquipments().parallelStream() /                .filter(x ->
    /// x.getEquipmentId().equals(eqId)).findAny(); /        if (!eqOp.isPresent()) { /
    /// throw new CommonException(CommonExceptionType.INVALID_PARAMETER, /                    "tp
    /// related Equipment is not managed now " + tpId); /        } /        Equipments eq =
    /// eqOp.get(); /        List<EquipType> possibleCard = Arrays.asList(EquipType.OTDR,
    /// EquipType.OCM, EquipType.OA, /                EquipType.ILA); /        if
    /// (!possibleCard.contains(eq.getEquipType())) { /            throw new
    /// CommonException(CommonExceptionType.INVALID_PARAMETER, /                    "tp isn't
    /// OTDR/OCM possible" + tpId); /        } /        if (isSrcNode) { /            srcNode =
    /// node; /        } else { /            dstNode = node; /        } /        return tpOp.get();
//    }
    public void updatePhysical(UpdateLinkInput input) throws CommonException {
//        Link1 link1 = ntLink.getAugmentation(Link1.class);
//        PhysicalBuilder phyBuilder = new PhysicalBuilder(link1.getPhysical());
//        if (input.getPhysical().getProvider() != null) {
//            phyBuilder.setProvider(input.getPhysical().getProvider());
//        }
//
//        String friendlyNameDisplay = input.getPhysical().getFriendlyNameDisplay();
//        if (friendlyNameDisplay != null && !friendlyNameDisplay.equals("")) {
//            phyBuilder.setFriendlyNameDisplay(friendlyNameDisplay);
//        }
//
//        ChangedObject changedObject = new ChangedObject();
//        changedObject.addChangedPhyLink(new LinkBuilder(ntLink)
//                .addAugmentation(Link1.class,
//                        new Link1Builder(link1).setPhysical(phyBuilder.build()).build())
//                .build());
//        MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
//        mongoTransaction.save(changedObject);
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

    @Override
    public CreateScanLinkOutput createLink(CreateScanLinkInput input) {
        log.debug("create scan link ,the input is:{}", input);
        validateCreateScanLinkInput(input);
        String srcNeId = input.getSrcNodeId();
        String destNeId = input.getDstNodeId();
        String srcTpId = input.getSrcTpId();
        String destTpId = input.getDstTpId();

        Node srcNode = getNode(srcNeId);
        Node destNode = getNode(destNeId);
        TerminationPoint srcTp = getTerminationPoint(srcNode, srcTpId);
        TerminationPoint destTp = getTerminationPoint(destNode, destTpId);

        LinkType linkType = getScanLinkTypeByTp(srcTp, destTp);
        String linkId = PhysicalLinkIdNamingRule.createLinkId(srcTpId,
                destTpId, linkType);
        String friendlyName = PhysicalLinkUtils.generateLinkFriendlyName(srcNode, destNode, srcTp,
                destTp);
        Link link = PhysicalLinkUtils.generatePhysicalScanLink(linkId, friendlyName, linkType,
                srcNeId,
                destNeId, srcTpId, destTpId);

        phyLinkDao.savePhyLink(link);
        implementLink(link);
        taskInfoMessage.setResourceId(link.getLinkId().getValue());
        logMessage(BroadCastConstant.CREATE_PHY_LINK, friendlyName, BLANK);
        return super.createLink(input);
    }

    @Override
    public DeleteScanLinkOutput deleteLink(DeleteScanLinkInput input) {
        log.debug("delete scan link ,the input is:{}", input);
        validateDeleteScanLinkInput(input);
        String linkId = input.getLinkId();

        Link link = phyLinkDao.getPhyLinkById(linkId);
        String friendlyName = link.getAugmentation(Link1.class).getPhysical().getFriendlyName();
        deImplementLink(link);
        taskInfoMessage.setResourceId(link.getLinkId().getValue());
        phyLinkDao.deletePhyLink(linkId);
        logMessage(BroadCastConstant.DELETE_PHY_LINK, friendlyName, BLANK);
        return super.deleteLink(input);
    }

    private TerminationPoint getTerminationPoint(Node node, String tpId) {
        log.debug("get termination point by id:{}", tpId);
        String eqId = PhysicalTpIdNamingRule.getEquipId(tpId);
        List<TerminationPoint> terminationPoints = node.getTerminationPoint();
        List<Equipments> equipments = node.getAugmentation(Node1.class).getPhysical()
                .getEquipments();
        Optional<Equipments> equipmentsOptional = equipments.stream()
                .filter(equipment -> equipment.getEquipmentId().equals(eqId)).findAny();
        if (!equipmentsOptional.isPresent()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the tp :%s  related equipment is not existed", tpId));
        }
        Equipments relativeEquip = equipmentsOptional.get();
        EquipType relativeEquipType = relativeEquip.getEquipType();
        if (!PhysicalLinkUtils.supportScanEqType(relativeEquipType)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the tp:%s related equipments is not support  OTDR/OCM ", tpId));
        }
        Optional<TerminationPoint> terminationPointOptional = terminationPoints.stream()
                .filter(terminationPoint -> terminationPoint.getTpId().getValue().equals(tpId))
                .findAny();
        if (!terminationPointOptional.isPresent()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the termination point id:%s is not existed", tpId));
        }
        return terminationPointOptional.get();
    }

    private Node getNode(String neId) {
        log.debug("get node by id :{}", neId);
        Node node = phyNodeDao.getOpPhyNodeById(neId);
        if (null == node) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("node:%s should be supervised first", neId));
        }
        return node;
    }


    private void validateCreateScanLinkInput(CreateScanLinkInput input) {
        log.debug("validate the create scan link input");
        String srcNeId = input.getSrcNodeId();
        String destNeId = input.getDstNodeId();
        String srcTpId = input.getSrcTpId();
        String destTpId = input.getDstTpId();
        if (StringUtils.isEmpty(srcNeId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the source ne id should not be null");
        }
        if (StringUtils.isEmpty(destNeId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the destination ne id should not be null");
        }
        if (StringUtils.isEmpty(srcTpId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the source termination point id should not be null");
        }
        if (StringUtils.isEmpty(destTpId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the destination point id should not be null");
        }


    }


    private void validateDeleteScanLinkInput(DeleteScanLinkInput input) {
        log.debug("validate the delete scan link input");
        String linkId = input.getLinkId();
        if (!phyLinkDao.isExistedPhyLinkId(linkId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find required scan link " + linkId);
        }
    }


    private void implementLink(Link link) {
        log.debug("implement the link :{}", link);
        TaskInfoMessage implTask = new TaskInfoMessage(taskInfoMessage)
                .setActionType(TaskInfoMessage.ActionType.implement);
        AsynchronousExecutor.execute(() -> {
            physicalLink.setTaskInfo(implTask);
            UpdateLinkInput input = new UpdateLinkInputBuilder()
                    .setLinkId(link.getLinkId())
                    .setPhysical(new PhysicalBuilder()
                            .setImplementState(ImplementState.Implement)
                            .setAdminState(AdminStatus.Up)
                            .build())
                    .build();
            physicalLink.updatePhyLink(input);
        });
    }

    private void deImplementLink(Link link) {
        log.debug("deImplementLink the link :{}", link);
        TaskInfoMessage implTask = new TaskInfoMessage(taskInfoMessage)
                .setActionType(TaskInfoMessage.ActionType.deimplement);
        AsynchronousExecutor.execute(() -> {
            physicalLink.setTaskInfo(implTask);
            UpdateLinkInput input = new UpdateLinkInputBuilder()
                    .setLinkId(link.getLinkId())
                    .setPhysical(new PhysicalBuilder()
                            .setImplementState(ImplementState.Allocate)
                            .setAdminState(AdminStatus.Down)
                            .build())
                    .build();
            physicalLink.updatePhyLink(input);
        });
    }


    private LinkType getScanLinkTypeByTp(TerminationPoint srcTp, TerminationPoint destTp) {
        PortType srcPortType = srcTp.getAugmentation(TerminationPoint1.class).getPhysical()
                .getPortType();
        PortType destPortType = destTp.getAugmentation(TerminationPoint1.class).getPhysical()
                .getPortType();
        if (!srcPortType.equals(destPortType)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "create scan link source port type should be same as destination port type");
        }
        PortType generalPortType = srcPortType;
        LinkType linkType = null;
        if (generalPortType.equals(PortType.OTDR)) {
            linkType = LinkType.OtdrLink;
        } else if (generalPortType.equals(PortType.MON)) {
            linkType = LinkType.OcmLink;
        } else {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "create scan link only supporting create links related to OTDR/OCM.");
        }
        return linkType;
    }
}
