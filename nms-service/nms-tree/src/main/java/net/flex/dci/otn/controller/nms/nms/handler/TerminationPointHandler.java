/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler;

import static net.flex.dci.otc.common.constants.Constants.POUND;
import static net.flex.dci.otc.common.util.Constant.EquipmentClass.TRANSCEIVER;
import static net.flex.dci.otn.controller.nms.utils.Constants.MPO_SUFFIX;
import static net.flex.dci.otn.controller.nms.utils.Constants.MPO_SUFFIX_REGEX;
import static net.flex.dci.otn.controller.nms.utils.Constants.PORT;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.properties.equip.EquipmentTypeConfiguration;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetCardPortsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyTpInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetPhyTpPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetRealMpoPortInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTransceiverByTpInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTransceiverByTpOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetTransceiverByTpOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.real.mpo.port.output.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @date: 2021/4/9
 */
@Slf4j
@Component
public class TerminationPointHandler extends AbstractBaseHandler {

    private final EquipmentTypeConfiguration equipmentTypeConfiguration;

    public TerminationPointHandler(
            NetconfTopology netconfTopology,
            EquipmentTypeConfiguration equipmentTypeConfiguration) {
        super(netconfTopology);
        this.equipmentTypeConfiguration = equipmentTypeConfiguration;
    }

    @Override
    public GetTransceiverByTpOutput getTransceiverByTp(GetTransceiverByTpInput input) {
        String tpId = input.getTpRef().getValue();
        if (tpId.split(POUND).length != 4) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    MessageFormat.format("Error termination point tpId {0}", tpId));
        }

        TerminationPoint tp = netconfTopology.getTerminationPoint(tpId);
        if (tp == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    MessageFormat.format("can't find the  termination point tpId {0}", tpId));
        }
        String transceiverId = tpId.replace(PORT, TRANSCEIVER);
        Equipments eqp = netconfTopology
                .getEquipment(
                        transceiverId);
        if (eqp == null) {
            GetTransceiverByTpOutput output = new GetTransceiverByTpOutputBuilder()
                    .build();
            return output;
        }

        GetTransceiverByTpOutput output = new GetTransceiverByTpOutputBuilder()
                .setActivationTime(eqp.getActivationTime())
                .setAdminState(eqp.getAdminState())
                .setAlarmState(eqp.getAlarmState())
//                .setCreatedBy(eqp.getCreatedBy())
                .setCreationTime(eqp.getCreationTime())
                .setDeviceRef(eqp.getDeviceRef())
                .setEquipmentId(eqp.getEquipmentId())
                .setEquipType(eqp.getEquipType())
                .setEquipTypeConfiged(eqp.getEquipTypeConfiged())
                .setEquipTypeInstalled(eqp.getEquipTypeInstalled())
                .setFriendlyName(eqp.getFriendlyName())
                .setGlobalIdentify(eqp.getGlobalIdentify())
                .setHardwareVersion(eqp.getHardwareVersion())
                .setImplementState(eqp.getImplementState())
                .setNodeRef(eqp.getNodeRef())
                .setOperationalState(eqp.getOperationalState())
                .setOrderId(eqp.getOrderId())
                .setPlaneName(eqp.getPlaneName())
                .setProperties(eqp.getProperties())
                .setRemoveable(eqp.isRemoveable())
                .setRiskGroupName(eqp.getRiskGroupName())
                .setSerialNo(eqp.getSerialNo())
                .setSoftwareVersion(eqp.getSoftwareVersion())
                .setVendorName(eqp.getVendorName())
                .build();
        return output;

    }


    @Override
    public List<Node> getPhyTp(GetPhyTpInput input) {
        TopologyId topologyRef = input.getTopologyRef();
        NodeId nodeRef = input.getNodeRef();
        String rackRef = input.getRackRef();
        String equipRef = input.getEquipmentRef();
        TpId tpRef = input.getTpRef();
        String tunnelRef = input.getTunnelRef();
        LinkId linkRef = input.getLinkRef();
        return getPhyTp(topologyRef, nodeRef, rackRef, equipRef, tpRef, linkRef, tunnelRef);
    }

    @Override
    public List<Node> getPhyTpPaged(GetPhyTpPagedInput input) {
        TopologyId topologyRef = input.getTopologyRef();
        NodeId nodeRef = input.getNodeRef();
        String rackRef = input.getRackRef();
        String equipRef = input.getEquipmentRef();
        TpId tpRef = input.getTpRef();
        String tunnelRef = input.getTunnelRef();
        LinkId linkRef = input.getLinkRef();
        return getPhyTp(topologyRef, nodeRef, rackRef, equipRef, tpRef, linkRef, tunnelRef);
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.real.mpo.port.output.TerminationPoint> getRealMpoTpByVisualTp(
            GetRealMpoPortInput input) {
        String visualTpId = input.getMpoId();
        log.debug("get real mpo port by visual mpo port:{}", visualTpId);
        if (!StringUtils.hasText(visualTpId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "get real mpo port visual tp id should not be null");
        }
        validateMpoTpId(visualTpId);
        List<TerminationPoint> realMPOPorts = getRealMpoPort(visualTpId);
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.real.mpo.port.output.TerminationPoint> mpoPorts
                = realMPOPorts.stream().map(this::buildRealMpoTp)
                .collect(
                        Collectors.toList());

        return mpoPorts;
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.card.ports.output.TerminationPoint> getCardPhysicalPort(
            GetCardPortsInput input) {
        if (!StringUtils.hasText(input.getCardId())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "get termination point physically on card,the card id should not be null");
        }
        String cardId = input.getCardId();
        log.debug("get card physical termination point,card id:{}", cardId);
        Equipments equipment = netconfTopology.getEquipment(cardId);
        if (Objects.isNull(equipment)) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "current card :" + cardId + " is not found,please try again");
        }
        List<TerminationPoint> physicalTerminationPoints = netconfTopology.getTerminationPointIdByRefEquipIds(
                cardId);
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.card.ports.output.TerminationPoint> physicalTps
                = physicalTerminationPoints.stream().map(this::buildPhysicalTp)
                .collect(
                        Collectors.toList());
        return physicalTps;
    }

    private org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.card.ports.output.TerminationPoint buildPhysicalTp(
            TerminationPoint tp) {
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.card.ports.output.TerminationPointBuilder terminationPointBuilder = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.card.ports.output.TerminationPointBuilder();
        terminationPointBuilder.setTpId(tp.getTpId());
        terminationPointBuilder.setPhysical(
                tp.getAugmentation(TerminationPoint1.class).getPhysical());
        return terminationPointBuilder.build();
    }

    private org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.real.mpo.port.output.TerminationPoint buildRealMpoTp(
            TerminationPoint mpoPort) {
        TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder();
        terminationPointBuilder.setTpId(mpoPort.getTpId());
        terminationPointBuilder.setPhysical(
                mpoPort.getAugmentation(TerminationPoint1.class).getPhysical());
        return terminationPointBuilder.build();
    }

    /**
     * if current tp real just get the real one
     *
     * if is visual just get real mpo
     *
     * @param visualTpId
     * @return
     */
    private List<TerminationPoint> getRealMpoPort(String visualTpId) {
        log.debug("get real mpo port by visual mpo port:{}", visualTpId);
        String nodeId = PhysicalTpIdNamingRule.getNodeId(visualTpId);
        String equipmentId = PhysicalTpIdNamingRule.getEquipId(visualTpId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node refNode = netconfTopology.getNeNode(
                nodeId);
        List<TerminationPoint> terminationPoints = refNode.getTerminationPoint();

        if (visualTpId.endsWith(MPO_SUFFIX)) {
            return terminationPoints.stream()
                    .filter(tp -> tp.getTpId().getValue().contains(equipmentId) &&
                            tp.getTpId().getValue().matches(".*MPO\\d+$"))
                    .sorted(Comparator.comparing(tp -> tp.getTpId().getValue()))
                    .collect(Collectors.toList());
        } else {
            return terminationPoints.stream()
                    .filter(tp -> tp.getTpId().getValue().equals(visualTpId))
                    .findFirst()
                    .map(Collections::singletonList)
                    .orElse(Collections.emptyList());
        }
    }

    private void validateMpoTpId(String visualTpId) {
        log.debug("validate mpo visual tp :{}", visualTpId);
        if (!visualTpId.matches(MPO_SUFFIX_REGEX)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "invalid visual tp id:" + visualTpId);
        }
        String equipmentId = PhysicalTpIdNamingRule.getEquipId(visualTpId);
        Equipments refEquipment = netconfTopology.getEquipment(equipmentId);
        if (refEquipment == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the visualTp " + visualTpId
                            + "relative equipment is not existed,the ref mpo is not existed");
        }
        EquipType equipType = refEquipment.getEquipType();
        if (!equipmentTypeConfiguration.isMpoPortSupported(equipType)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the visual Tp " + visualTpId
                            + " relative equipment is not contains any mpo port ");
        }
    }

    private List<Node> getPhyTp(
            TopologyId topologyRef,
            NodeId nodeRef, String rackRef, String equipRef,
            TpId tpRef,
            LinkId linkRef, String tunnelRef) {
        log.debug(
                "start get all PHY Tp, topology:{}, node:{}, rack:{}, equip:{}, tp:{}, link:{}, tunnel:{}",
                topologyRef == null ? "null" : topologyRef.getValue(),
                nodeRef == null ? "null" : nodeRef.getValue(),
                rackRef == null ? "null" : rackRef, equipRef == null ? "null" : equipRef,
                tpRef == null ? "null" : tpRef.getValue(),
                linkRef == null ? "null" : linkRef.getValue(),
                tunnelRef == null ? "null" : tunnelRef);

        return new LinkedList<>();
    }
}
