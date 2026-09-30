package net.flex.dci.otc.controller.ne.manager.core.state.impl;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.BLANK;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.BLANK_SLOT_PREFIX;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.ne.manager.core.filter.impl.TelecomModelEquipmentFilter;
import net.flex.dci.otc.controller.ne.manager.core.state.AbstractPhyNodeDataSynchronize;
import net.flex.dci.otc.controller.ne.manager.dto.SlotInfo;
import net.flex.dci.otc.controller.ne.manager.dto.TelecomNonBusinessEquipmentDto;
import net.flex.dci.otc.controller.ne.manager.utils.FriendlyNameGenerator;
import net.flex.dci.otc.controller.ne.manager.utils.SlotInfoUtils;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.PhyEquipAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 11/17/2023 4:19 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TelecomModelPhyNodeDataSynchronize extends AbstractPhyNodeDataSynchronize {

    private final TelecomModelEquipmentFilter telecomModelEquipmentFilter;

    private final EquipmentsDao equipmentsDao;

    private final TerminationPointDao terminationPointDao;

    @Override
    public NeYangModel supportNeYangModel() {
        return NeYangModel.ChinaTelecom;
    }

    @Override
    public void synchronizingData(Node phyNe) {
        String neId = phyNe.getNodeId().getValue();
        log.debug("start to synchronizing data for telecom model ne,the ne id is :{}", neId);
        Node realNe = phyNodeDao.getOpPhyNodeById(neId);
        List<Equipments> configEquipments = getNodeEquipments(phyNe);
        List<Equipments> realEquipments = getNodeEquipments(realNe);
        TelecomNonBusinessEquipmentDto nonBusinessEquipmentDto = telecomModelEquipmentFilter.filterNoneBusinessEquip(
                configEquipments, realEquipments);

        List<Equipments> notInConfigBusinessEquips = nonBusinessEquipmentDto.getNotInConfigEquipments();
        List<Equipments> inConfigBusinessEquips = nonBusinessEquipmentDto.getInConfigEquipments();
        List<TerminationPoint> configTerminationPoints = phyNe.getTerminationPoint();
        List<TerminationPoint> opTerminationPoints = realNe.getTerminationPoint();

        unplugConfigNonBusinessEquips(neId, inConfigBusinessEquips, configTerminationPoints);
        pluginConfigNonBusinessEquips(neId, notInConfigBusinessEquips, opTerminationPoints);
        updateNodeStatus(phyNe);
    }

    /**
     * plugin config non business equips
     *
     * @param neId
     * @param pluginConfigBusinessEquips
     * @param terminationPoints
     */
    private void pluginConfigNonBusinessEquips(String neId,
            List<Equipments> pluginConfigBusinessEquips,
            List<TerminationPoint> terminationPoints) {
        log.debug("plugin the config none business card for ne:{}", neId);
        List<Equipments> pluginEquipments = pluginConfigBusinessEquips.stream()
                .map(this::pluginConfigEquipment).collect(
                        Collectors.toList());
        updateEquipments(neId, pluginEquipments);
        List<String> equipIds = pluginConfigBusinessEquips.stream().map(
                PhyEquipAttributes::getEquipmentId).collect(
                Collectors.toList());
        List<TerminationPoint> pluginTerminationPoints = new ArrayList<>();
        for (TerminationPoint terminationPoint : terminationPoints) {
            String tpId = terminationPoint.getTpId().getValue();
            String refEqId = PhysicalTpIdNamingRule.getEquipId(tpId);
            if (equipIds.contains(refEqId)) {
                pluginTerminationPoints.add(terminationPoint);
            }
        }
        pluginTerminationPoints.stream().map(this::pluginTerminationPoint)
                .forEach(terminationPoint -> {
                    terminationPointDao.addConfigTerminationPointByNeIdTerminationPoint(neId,
                            terminationPoint);
                });
    }


    private void unplugConfigNonBusinessEquips(String neId,
            List<Equipments> unplugNonBusinessEquips,
            List<TerminationPoint> terminationPoints) {
        log.debug("plugin the config none business card for ne:{}", neId);
        List<Equipments> unPlugEquipments = unplugNonBusinessEquips.stream()
                .map(this::unPlugConfigEquipment).collect(
                        Collectors.toList());
        updateEquipments(neId, unPlugEquipments);
        List<String> equipIds = unPlugEquipments.stream().map(
                PhyEquipAttributes::getEquipmentId).collect(
                Collectors.toList());
        List<String> pluginTerminationPointIds = terminationPoints.stream()
                .filter(terminationPoint -> equipIds.contains(
                        PhysicalTpIdNamingRule.getEquipId(terminationPoint.getTpId().getValue())))
                .map(TpAttributes::getTpId)
                .map(Uri::getValue).collect(
                        Collectors.toList());
        terminationPointDao.batchRemoveConfigTerminationPointByIds(neId, pluginTerminationPointIds);
    }

    private List<Equipments> getNodeEquipments(Node phyNe) {
        log.debug("list all node equipments for the ne : {}", phyNe.getNodeId().getValue());
        Physical physical = phyNe.getAugmentation(Node1.class).getPhysical();
        List<Equipments> equipments = physical.getEquipments();
        return equipments;
    }

    /**
     * change the config data for the equipment to unplug state
     *
     * @param equipment
     * @return
     */
    private Equipments unPlugConfigEquipment(Equipments equipment) {
        log.debug("unplug the equipment,the equipment id is:{}", equipment.getEquipmentId());
        SlotInfo slotInfo = SlotInfoUtils.extractSlotInfoFromEqId(equipment.getEquipmentId());
        String friendlyName = FriendlyNameGenerator.generate(BLANK_SLOT_PREFIX, slotInfo);
        EquipmentsBuilder equipmentsBuilder = new EquipmentsBuilder();
        equipmentsBuilder.setEquipmentId(equipment.getEquipmentId());
        equipmentsBuilder.setEquipType(EquipType.EMPTY);
        equipmentsBuilder.setImplementState(ImplementState.Allocate);
        equipmentsBuilder.setCreationTime(equipment.getCreationTime());
        equipmentsBuilder.setEquipTypeConfiged(BLANK);
        equipmentsBuilder.setOperationalState(OperStatus.Unknown);
        equipmentsBuilder.setAdminState(AdminStatus.Unknown);
        equipmentsBuilder.setAlarmState(AlarmSeverity.Unknown);
        equipmentsBuilder.setNodeRef(equipment.getNodeRef());
        equipmentsBuilder.setEquipTypeVendorSpecific(BLANK);
        equipmentsBuilder.setAlignmentStatus(AlignmentStatusType.Unknown);
        equipmentsBuilder.setFriendlyName(friendlyName);
        equipmentsBuilder.setEmpty(true);
        return equipmentsBuilder.build();
    }

    /**
     * add the config blank equipment to plugin equipment
     *
     * @param equipment
     * @return
     */
    private Equipments pluginConfigEquipment(Equipments equipment) {
        log.debug("plugin the equipment,the equipment id is:{}", equipment.getEquipmentId());
        EquipmentsBuilder equipmentsBuilder = new EquipmentsBuilder();
        equipmentsBuilder.setEmpty(false);
        equipmentsBuilder.setEquipmentId(equipment.getEquipmentId());
        equipmentsBuilder.setImplementState(ImplementState.Allocate);
        equipmentsBuilder.setOperationalState(OperStatus.Unknown);
        equipmentsBuilder.setAdminState(AdminStatus.Unknown);
        equipmentsBuilder.setAlarmState(AlarmSeverity.Unknown);
        equipmentsBuilder.setAlignmentStatus(AlignmentStatusType.Unknown);
        equipmentsBuilder.setEquipType(equipment.getEquipType());
        equipmentsBuilder.setNodeRef(equipment.getNodeRef());
        equipmentsBuilder.setCreationTime(equipment.getCreationTime());
        SlotInfo slotInfo = SlotInfoUtils.extractSlotInfoFromEqId(equipment.getEquipmentId());
        equipmentsBuilder.setSlot(slotInfo.getSlot());
        equipmentsBuilder.setEquipTypeConfiged(equipment.getEquipTypeConfiged());
        equipmentsBuilder.setFriendlyName(equipment.getFriendlyName());
        equipmentsBuilder.setEquipTypeVendorSpecific(equipment.getEquipTypeVendorSpecific());
        return equipmentsBuilder.build();
    }


    /**
     * plugin the termination point for the config data
     *
     * @return
     */
    public TerminationPoint pluginTerminationPoint(TerminationPoint terminationPoint) {
        log.debug("plugin the termination point for the config data,the termination point id is:{}",
                terminationPoint.getTpId().getValue());
        TpId tpId = terminationPoint.getTpId();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical
                tpPhysical = terminationPoint.getAugmentation(TerminationPoint1.class)
                .getPhysical();

        TerminationPoint1Builder terminationPoint1Builder = new TerminationPoint1Builder();
        PhysicalBuilder tpPhysicalBuilder = new PhysicalBuilder(tpPhysical);
        tpPhysicalBuilder.setAdminState(AdminStatus.Unknown);
        tpPhysicalBuilder.setAlarmState(AlarmSeverity.Unknown);
        tpPhysicalBuilder.setAlignmentStatus(AlignmentStatusType.Unknown);
        tpPhysicalBuilder.setConnectionStatus(ConnectionStatus.Idle);
        tpPhysicalBuilder.setImplementState(ImplementState.Allocate);
        tpPhysicalBuilder.setOperationalState(OperStatus.Unknown);
        terminationPoint1Builder.setPhysical(tpPhysicalBuilder.build());
        TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder();
        terminationPointBuilder.setTpId(tpId);
        terminationPointBuilder.addAugmentation(TerminationPoint1.class,
                terminationPoint1Builder.build());
        return terminationPointBuilder.build();
    }

    private void updateEquipments(String neId, List<Equipments> equipments) {
        log.debug("update config equipments,the ne id is:{}", neId);
        equipments.forEach(equipment -> {
            equipmentsDao.rewriteConfigEquipments(neId, equipment);
        });
    }

}
