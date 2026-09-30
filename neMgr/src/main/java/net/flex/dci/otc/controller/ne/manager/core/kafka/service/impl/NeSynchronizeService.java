package net.flex.dci.otc.controller.ne.manager.core.kafka.service.impl;

import static net.flex.dci.otc.common.constants.Constants.HYPHEN;
import static net.flex.dci.otc.common.constants.Constants.POUND;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.BLANK;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.BLANK_SLOT_PREFIX;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.CURRENT_SOFTWARE_VERSION;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.INSTALL_NONE;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.MISALIGN;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.SLOT_PREFIX;

import com.alibaba.fastjson.JSON;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.data.YangDataOperator;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.ne.NeStatus;
import net.flex.dci.otc.common.model.ne.NeStatusMessage;
import net.flex.dci.otc.common.model.type.NeStatusType;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.ne.manager.cache.RegisterNeCache;
import net.flex.dci.otc.controller.ne.manager.components.PhyNodeManager;
import net.flex.dci.otc.controller.ne.manager.components.balancer.AdapterBalancer;
import net.flex.dci.otc.controller.ne.manager.core.filter.impl.EquipmentFilter;
import net.flex.dci.otc.controller.ne.manager.core.kafka.service.NeSynchronized;
import net.flex.dci.otc.controller.ne.manager.core.service.NeResourceService;
import net.flex.dci.otc.controller.ne.manager.core.state.impl.PhyNodeDataSynchronizeImpl;
import net.flex.dci.otc.controller.ne.manager.dto.MissAlignDto;
import net.flex.dci.otc.controller.ne.manager.dto.MissAlignEquipmentDto;
import net.flex.dci.otc.controller.ne.manager.dto.NeSynchronizedDto;
import net.flex.dci.otc.controller.ne.manager.dto.SlotInfo;
import net.flex.dci.otc.controller.ne.manager.utils.FriendlyNameGenerator;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import net.flex.dci.otc.controller.ne.manager.utils.NeSortUtils;
import net.flex.dci.otc.controller.ne.manager.utils.SlotInfoUtils;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.StatusMessageSender;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.nes.top.nes.Ne;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.CommonAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CommunicationStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.PhyEquipAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/28 15:20
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NeSynchronizeService extends AbstractService implements NeSynchronized {


    private final AdapterBalancer adapterBalancer;

    private final PhyNodeDao phyNodeDao;

    private final EquipmentFilter equipmentFilter;


    private final PhyNodeManager phyNodeManager;

    private final PhyNodeDataSynchronizeImpl phyNodeDataSynchronize;

    private final RegisterNeCache registerNeCache;

    private final NeResourceService neResourceService;

    /**
     * enrich the ne friendly name and merge operational state and
     *
     * @param neId
     */
    @Override
    public void handleStateChange(String neId, boolean mutable) {
        try {
            log.debug("start to handle ne state change synchronized change ,ne id is :{}", neId);
            Adapter adapter = adapterBalancer.getAdapterForNe(neId);
            if (null == adapter) {
                log.debug("useless neId :{} discard it", neId);
                return;
            }
//
            Node configPhyNode = phyNodeDao.getConfigPhyNodeById(neId);
//        phyNodeDataSynchronize.synchronizingData(configPhyNode);
            if (configPhyNode.getTerminationPoint() == null) {
                //todo merge from op to config
                Node operationalNode = phyNodeManager.mergeConfNeDataFromOp(configPhyNode, neId);
                phyNodeDao.rewriteConfigPhyNode(operationalNode);
                updateNodeStatusImplement(configPhyNode);
            } else {
                updateNodeStatus(configPhyNode);
            }
            Physical physical = configPhyNode.getAugmentation(Node1.class).getPhysical();
            SupervisionStatusType supervisionStatusType = physical.getSupervisionStatus();
            if (supervisionStatusType.equals(SupervisionStatusType.Monitoring)) {
                AdminStatus adminStatus = physical.getAdminState();
                phyNodeDao.batchUpdateConfigPhyNodeSupervisionStateAndAdminStateAndCommunicateStatus(
                        Collections.singletonList(neId), supervisionStatusType, adminStatus,
                        CommunicationStatusType.SyncFinished);
                log.info("current mutable is:{}", mutable);
                if (!mutable) {
                    sendNotification(configPhyNode);
                }
            }
        } catch (Exception ex) {
            log.error("failed to handle the state change ,the reason is:{}", ex.getMessage(), ex);
        }
    }

    private void updateNodeStatusImplement(Node configPhyNode) {
        String neId = configPhyNode.getNodeId().getValue();
        log.debug("update config ne node status ,the neId is :{}", neId);
        Node realNode = phyNodeDao.getOpPhyNodeById(neId);
        Physical emlNePhysical = realNode.getAugmentation(Node1.class).getPhysical();
        phyNodeDao.updateConfigNodeState(neId, emlNePhysical.getAlarmState(),
                emlNePhysical.getOperationalState(), emlNePhysical.getImplementState(),
                AdminStatus.Up, emlNePhysical.getAlignmentStatus());
    }

    /**
     * update change the status for the node
     *
     * @param configPhyNode
     */
    private void updateNodeStatus(Node configPhyNode) {
        String neId = configPhyNode.getNodeId().getValue();
        log.debug("update config ne node status ,the neId is :{}", neId);
        Node realNode = phyNodeDao.getOpPhyNodeById(neId);
        Physical emlNePhysical = realNode.getAugmentation(Node1.class).getPhysical();
        phyNodeDao.updateConfigNodeState(neId, emlNePhysical.getAlarmState(),
                emlNePhysical.getOperationalState(), null,
                AdminStatus.Up, emlNePhysical.getAlignmentStatus());
        //upload the software version info etc
        String softwareVersion = PropertyTool.getValue(emlNePhysical.getProperties(),
                CURRENT_SOFTWARE_VERSION);
        log.info("current node :{}software version is:{}", neId, softwareVersion);
        Property softwareProperty = new PropertyBuilder().setName(CURRENT_SOFTWARE_VERSION)
                .setValue(softwareVersion).build();
        phyNodeDao.updateConfigPhyNodeProperties(neId, Collections.singletonList(softwareProperty));
    }

    /**
     * change the status for the node
     *
     * @param realNe
     */
    @SneakyThrows
    private void updateNodeStatus(Ne realNe, Node node) {
        String neId = realNe.getNodeId().getValue();
        log.debug("update config ne node status,neId is :{}", neId);
        AlignmentStatusType statusType = node.getAugmentation(Node1.class).getPhysical()
                .getAlignmentStatus();

        Physical emlNePhysical = realNe.getPhysical();
        phyNodeDao.updateConfigNodeState(neId, emlNePhysical.getAlarmState(),
                emlNePhysical.getOperationalState(), emlNePhysical.getImplementState(),
                AdminStatus.Up, statusType);
        //properties

    }

    private Node mergeNeData(Node configPhyNode, Ne realNe) {
        log.debug("start to merge real ne {} and config phy node {}", realNe, configPhyNode);

        List<Equipments> configEqs = configPhyNode.getAugmentation(Node1.class).getPhysical()
                .getEquipments();
        List<Equipments> realEqs = realNe.getPhysical().getEquipments();
        CalculateEquipmentsDto calculateEqs = calculateAndMergeEquipments(configEqs, realEqs);
        List<TerminationPoint> configTps = configPhyNode.getTerminationPoint();
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPoint> realTps = realNe.getTerminationPoint();
        List<TerminationPoint> mergeTps = mergeTerminationPoint(configTps, realTps);
        Node opNode = mergeNode(configPhyNode, realNe, calculateEqs, mergeTps);
        return opNode;
    }

    private Node mergeNode(Node configPhyNode, Ne realNe, CalculateEquipmentsDto calculateEqs,
            List<TerminationPoint> mergeTps) {
        try {
            NodeBuilder mergeNodeBuilder = new NodeBuilder(configPhyNode);
            Physical mergePhysical = (Physical) YangDataOperator.merge(
                    configPhyNode.getAugmentation(Node1.class).getPhysical(),
                    realNe.getPhysical());
            PhysicalBuilder physicalBuilder = new PhysicalBuilder(mergePhysical);
            physicalBuilder.setAdminState(AdminStatus.Up);

            List<Equipments> equipments = NeSortUtils.sortEquipments(
                    calculateEqs.getMergeEquips());

            Map<String, String> equipIdRefFriendlyNameMap = equipments.stream()
                    .collect(Collectors.toMap(PhyEquipAttributes::getEquipmentId,
                            CommonAttributes::getFriendlyName));

            physicalBuilder.setEquipments(equipments);
            physicalBuilder.setAlignmentStatus(calculateEqs.getAlignmentStatus());
            Properties properties = addMissMatchEquipToProperties(mergePhysical.getProperties(),
                    calculateEqs.missConfigEquips, calculateEqs.missMatchEquips);
            physicalBuilder.setProperties(properties);
            mergeNodeBuilder.addAugmentation(Node1.class,
                    new Node1Builder().setPhysical(physicalBuilder.build()).build());
            List<TerminationPoint> tps = rebuildTps(mergeTps, equipIdRefFriendlyNameMap);
            mergeNodeBuilder.setTerminationPoint(NeSortUtils.sortTerminationPoints(tps));

            return mergeNodeBuilder.build();
        } catch (Exception exception) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to merge the node", exception);
        }
    }

    /**
     * construct the missAlign description for the missMatch;
     *
     * @param properties
     * @param missConfigEquips
     * @param missMatchEquips
     * @return
     */
    private Properties addMissMatchEquipToProperties(Properties properties,
            List<MissAlignEquipmentDto> missConfigEquips,
            List<MissAlignEquipmentDto> missMatchEquips) {
        log.debug("add miss match equip to the properties");
        List<Property> propertyList = properties.getProperty();
//        Map<String, String> propertyMap = propertyList.stream().collect(
//                Collectors.toMap(property -> property.getKey().getName(), Property::getValue));

        List<MissAlignEquipmentDto> wrongEquips = new ArrayList<>(missConfigEquips);
        wrongEquips.addAll(missMatchEquips);
        List<MissAlignDto> misAligns = wrongEquips.stream().map(this::buildMissAlign)
                .collect(Collectors.toList());
        String misAlignJson = JSON.toJSONString(misAligns);
        PropertyTool.putKeyValue(propertyList, MISALIGN, misAlignJson);
//        propertyList = propertyMap.keySet().stream().map(key -> {
//            PropertyBuilder propertyBuilder = new PropertyBuilder();
//            propertyBuilder.setKey(new PropertyKey(key));
//            propertyBuilder.setValue(propertyMap.get(key));
//            return propertyBuilder.build();
//        }).collect(Collectors.toList());
        PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
        return propertiesBuilder.setProperty(propertyList).build();
    }


    /**
     * calculate equipments between config and real
     *
     * @param configEqs config equipments
     * @param realEqs real equipments from ne
     * @return merge equipment ,miss config equipments,miss match equipments
     */
    private CalculateEquipmentsDto calculateAndMergeEquipments(List<Equipments> configEqs,
            List<Equipments> realEqs) {
        log.debug("calculate and merge the equipments");
        Map<String, Equipments> configEqsMap = buildEquipmentsMap(configEqs);
        Map<String, Equipments> realEqsMap = buildEquipmentsMap(realEqs);
        Set<String> configEqsIds = configEqsMap.keySet();
        Set<String> realEqsIds = realEqsMap.keySet();
        //config have but real do not have
        Set<String> missConfigEqsIds = NeManagerUtils.getDifferenceSetByGuava(configEqsIds,
                realEqsIds);
        Set<String> notInConfigEqsIds = NeManagerUtils.getDifferenceSetByGuava(realEqsIds,
                configEqsIds);
        log.info("miss config eqs,{}", missConfigEqsIds);
        //both have eqs
        Set<String> intersectionEqsIds = NeManagerUtils.getIntersectionSetByGuava(configEqsIds,
                realEqsIds);
        //miss config equip config have and real not have
        List<MissAlignEquipmentDto> missConfigEquips = getMissConfigEquips(missConfigEqsIds,
                realEqsMap, configEqsMap);
        List<Equipments> specifiedInRealEquips = realEqsMap.keySet().stream()
                .filter(notInConfigEqsIds::contains).map(realEqsMap::get)
                .filter(equipment -> StringUtils.isNotBlank(equipment.getEquipTypeInstalled()))
                .map(this::rebuildEquipments)
                .map(eq -> new EquipmentsBuilder(eq).setAlignmentStatus(
                                AlignmentStatusType.CardMissMatch)
                        .build()).collect(
                        Collectors.toList());
        //realEquip
        MergeEquipmentsDto mergeEquipmentsDto = mergeEquipments(intersectionEqsIds, configEqsMap,
                realEqsMap);
        List<Equipments> mergeEquipments = new ArrayList<>();
        mergeEquipments.addAll(specifiedInRealEquips);
        mergeEquipments.addAll(mergeEquipmentsDto.getMergeEquips());
        CalculateEquipmentsDto calculateEquipmentsDto = CalculateEquipmentsDto.builder()
                .mergeEquips(mergeEquipments)
                .alignmentStatus(mergeEquipmentsDto.getAlignmentStatusType())
                .missConfigEquips(missConfigEquips)
                .missMatchEquips(mergeEquipmentsDto.getMissMatchEquips())
                .build();
        return calculateEquipmentsDto;
    }

    private List<MissAlignEquipmentDto> getMissConfigEquips(Set<String> missConfigEqsIds,
            Map<String, Equipments> realEqsMap, Map<String, Equipments> configEqsMap) {
        log.debug("get miss config equips tuple");
//        List<MissAlignEquipmentDto> missAlignEquipmentDtos = configEqsMap.keySet().stream()
//                .filter(missConfigEqsIds::contains).map(configEqsMap::get)
//                .map(equipments -> new EquipmentsBuilder(equipments)
//                        .setAlignmentStatus(
//                                equipments.getEquipType().equals(EquipType.TRANSCEIVER)
//                                        ? AlignmentStatusType.ModularLack
//                                        : AlignmentStatusType.CardLack)
//                        .build()).map(this::rebuildEquipments).collect(
//                        Collectors.toList());
        List<MissAlignEquipmentDto> missAlignEquipmentDtos = configEqsMap.keySet().stream()
                .filter(missConfigEqsIds::contains).map(equipId -> {
                    Equipments configEquip = configEqsMap.get(equipId);
                    Equipments realEquip = realEqsMap.get(equipId);
                    return MissAlignEquipmentDto.builder().realEquipment(realEquip)
                            .designEquipment(configEquip).build();
                }).collect(Collectors.toList());
        return missAlignEquipmentDtos;
    }

    private Equipments rebuildEquipments(Equipments equipments) {

        String installType =
                equipments.getEquipTypeInstalled() == null ? equipments.getEquipTypeConfiged()
                        : equipments.getEquipTypeInstalled();
        String friendlyName = equipments.getFriendlyName();
        String equipTypeSpecific = equipments.getEquipTypeVendorSpecific();
        log.info("equipments id is:{},equipments:{}", equipments.getEquipmentId(),
                equipTypeSpecific);
        EquipType equipType = equipments.getEquipType();
        String configType = equipments.getEquipTypeConfiged();
        String equipSegment = equipments.getEquipmentId().split(POUND)[2];
        SlotInfo slotInfo = SlotInfoUtils.extractSlotInfoFromEqId(equipments.getEquipmentId());
        Boolean empty = equipments.isEmpty();

        if (installType.equals(INSTALL_NONE)) {
            equipTypeSpecific = BLANK;
            equipType = EquipType.EMPTY;
            equipTypeSpecific = BLANK;
            friendlyName = FriendlyNameGenerator.generate(BLANK_SLOT_PREFIX, slotInfo);
            empty = true;
            return new EquipmentsBuilder(equipments)
                    .setFriendlyName(friendlyName)
                    .setEquipTypeVendorSpecific(equipTypeSpecific)
                    .setEquipType(equipType)
                    .setEquipTypeConfiged(configType)
                    .setEmpty(true)
                    .setProperties(null)
                    .setAdminState(AdminStatus.Unknown)
                    .setAlignmentStatus(AlignmentStatusType.Unknown)
                    .setAlarmState(AlarmSeverity.Unknown)
                    .setImplementState(ImplementState.Allocate)
                    .build();

        } else {
            String prefix = installType.split(HYPHEN)[0];
            equipTypeSpecific = installType;
            equipType = getEquipType(installType);
            friendlyName = FriendlyNameGenerator.generate(prefix, slotInfo);

            return new EquipmentsBuilder(equipments)
                    .setFriendlyName(friendlyName)
                    .setEquipTypeVendorSpecific(equipTypeSpecific)
                    .setEquipType(equipType)
                    .setEquipTypeConfiged(configType)
                    .setEmpty(empty)
                    .build();
        }

    }


    private EquipType getEquipType(String installType) {
        List<String> equipTypes = Arrays.stream(EquipType.values())
                .map(equipType -> equipType.name()).collect(
                        Collectors.toList());
        if (equipTypes.contains(installType)) {
            return EquipType.valueOf(installType);
        }
        return EquipType.Other;
    }


    /**
     * merge the equipments and calculate align state
     *
     * @param equipsIds
     * @param configEqsMap
     * @param realEqsMap
     * @return
     */
    private MergeEquipmentsDto mergeEquipments(Set<String> equipsIds,
            Map<String, Equipments> configEqsMap, Map<String, Equipments> realEqsMap) {
        List<Equipments> equipments = new ArrayList<>();
        List<MissAlignEquipmentDto> missMatchEquipments = new ArrayList<>();
        Integer alignState = 0;
        for (String equipId : equipsIds) {
            Equipments configEquip = configEqsMap.get(equipId);
            Equipments realEquip = realEqsMap.get(equipId);

            Equipments mergeEquip = null;
            //the real state for the ne
//            AdminStatus adminState = realEquip.getAdminState();
//            AlarmSeverity alarmSeverity = realEquip.getAlarmState();
//            Boolean usedInLink = configEquip.isUsedInLink();
            boolean isCardMissMatch = cardIsMissMatch(configEquip, realEquip);

            if (!isCardMissMatch) {
                mergeEquip = enrichEquipment(configEquip, realEquip, false);
            } else {
                mergeEquip = enrichEquipment(configEquip, realEquip, true);
//                mergeEquip = rebuildEquipments(mergeEquip);
//                missMatchEquipments.add(mergeEquip);
                missMatchEquipments.add(MissAlignEquipmentDto.builder().designEquipment(configEquip)
                        .designEquipment(realEquip).build());
                alignState = +1;
            }
            equipments.add(mergeEquip);
        }
        equipments = equipmentFilter.filter(equipments);
        return MergeEquipmentsDto.builder().mergeEquips(equipments)
                .missMatchEquips(missMatchEquipments).alignmentStatusType(
                        alignState >= 1 ? AlignmentStatusType.CardMissMatch
                                : AlignmentStatusType.Aligned)
                .build();

    }

    private boolean cardIsMissMatch(Equipments configEq, Equipments realEq) {
        log.info("equipment id is :{}", configEq.getEquipmentId());

        EquipType configType = configEq.getEquipType();
        EquipType realType = realEq.getEquipType();
        log.info("config equipment type:{},realType is :{}", configType, realType);

        if (realType != null) {
            if (configType.equals(realType)) {
                return false;
            } else {
                return !configType.equals(EquipType.OP) || !realType.equals(EquipType.OP);
            }
        } else {
            String installedType = realEq.getEquipTypeInstalled();
            log.info("installedType installedType:{}", installedType);
            return !installedType.equals(INSTALL_NONE) || !configType.equals(EquipType.EMPTY);
        }
    }

    /**
     * enrich the equipment from real equipment
     *
     * @return
     */
    private Equipments enrichEquipment(Equipments dbEqu, Equipments realEqu,
            boolean cardMissMatch) {
        log.debug("enrich the equipment :{},is CardMiss match:{}", dbEqu, cardMissMatch);
        EquipmentsBuilder equipmentBuilder;
        //in db equipment
        String friendlyName = dbEqu.getFriendlyName();
        String equipTypeVendorSpecific = dbEqu.getEquipTypeVendorSpecific();
        String shelf = dbEqu.getShelf();
        Boolean usedInLink = dbEqu.isUsedInLink();
        String slot = dbEqu.getSlot();
        String equipTypeConfig = dbEqu.getEquipTypeConfiged();
        Properties dbProperties = dbEqu.getProperties();
        //real equipment
        String equipClass = realEqu.getEquipClass();
        String equipInstalledType = realEqu.getEquipTypeInstalled();
        ImplementState implementState = dbEqu.getImplementState();
        OperStatus operationalState = realEqu.getOperationalState();
        AdminStatus adminStatus = realEqu.getAdminState();
        String vendorName = realEqu.getVendorName();
        String serialNo = realEqu.getSerialNo();
        String softwareVersion = realEqu.getSoftwareVersion();
        String hardwareVersion = realEqu.getHardwareVersion();
        Properties realProperties = realEqu.getProperties();
        Properties mergeProperties = mergeProperties(dbProperties, realProperties);

        if (cardMissMatch) {
            equipmentBuilder = new EquipmentsBuilder(dbEqu);
            if (equipInstalledType.equals(INSTALL_NONE)) {
                equipmentBuilder.setAlignmentStatus(AlignmentStatusType.CardLack);
            } else {
                equipmentBuilder.setAlignmentStatus(AlignmentStatusType.CardMissMatch);
            }
//            equipmentBuilder.setProperties(mergeProperties);
//            equipmentBuilder.setEquipTypeConfiged(equipTypeConfig);
//            equipmentBuilder.setEquipTypeVendorSpecific(equipInstalledType);
//            equipmentBuilder.setEquipTypeInstalled(equipInstalledType);
        } else {
            //enrich the db real equ
            equipmentBuilder = new EquipmentsBuilder(realEqu);
            equipmentBuilder.setAlignmentStatus(AlignmentStatusType.Aligned);
            equipmentBuilder.setFriendlyName(friendlyName);
            equipmentBuilder.setEquipTypeVendorSpecific(equipTypeVendorSpecific);
            equipmentBuilder.setUsedInLink(usedInLink);
            equipmentBuilder.setShelf(shelf);
            equipmentBuilder.setSlot(slot);
            equipmentBuilder.setEquipTypeConfiged(equipTypeConfig);
            equipmentBuilder.setEquipClass(equipClass);
            equipmentBuilder.setEquipTypeInstalled(equipInstalledType);
            equipmentBuilder.setImplementState(implementState);
            equipmentBuilder.setOperationalState(operationalState);
            equipmentBuilder.setAdminState(adminStatus);
            equipmentBuilder.setVendorName(vendorName);
            equipmentBuilder.setSerialNo(serialNo);
            equipmentBuilder.setHardwareVersion(hardwareVersion);
            equipmentBuilder.setSoftwareVersion(softwareVersion);
            equipmentBuilder.setProperties(mergeProperties);
            equipmentBuilder.setEquipType(
                    realEqu.getEquipType() != null ? realEqu.getEquipType() : EquipType.EMPTY);
        }

        return equipmentBuilder.build();
    }

    /**
     * merge properties between config and real
     *
     * @param dbProperties
     * @param realProperties
     * @return
     */
    private Properties mergeProperties(Properties dbProperties, Properties realProperties) {
        Set<Property> mergeProperties = new HashSet<>();
        PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
        if (dbProperties != null && dbProperties.getProperty() != null) {
            List<Property> dbPropertiesProperty = dbProperties.getProperty();
            mergeProperties.addAll(dbPropertiesProperty);
        }
        if (realProperties != null && realProperties.getProperty() != null) {
            List<Property> realPropertiesProperty = realProperties.getProperty();
            mergeProperties.addAll(realPropertiesProperty);
        }
        propertiesBuilder.setProperty(new ArrayList<>(mergeProperties));

        return propertiesBuilder.build();
    }


    /**
     * merge the termination point
     *
     * @param configTps
     * @param realTps
     * @return
     */
    private List<TerminationPoint> mergeTerminationPoint(List<TerminationPoint> configTps,
            List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPoint> realTps) {
        log.debug("merge the termination point");
        Map<String, TerminationPoint> configTpMap = configTps.stream()
                .collect(Collectors.toMap(tp -> tp.getTpId().getValue(), tp -> tp));
        Map<String, org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPoint> realTpMap = realTps.stream()
                .collect(Collectors.toMap(tp -> tp.getTpId().getValue(), tp -> tp));
        Set<String> configTpIds = configTpMap.keySet();
        Set<String> realTpIds = realTpMap.keySet();
        Set<String> intersectionTpIds = NeManagerUtils.getIntersectionSetByGuava(configTpIds,
                realTpIds);
        Set<String> notInConfigTpIds = NeManagerUtils.getDifferenceSetByGuava(realTpIds,
                configTpIds);
        Set<String> notInRealTpIds = NeManagerUtils.getDifferenceSetByGuava(configTpIds,
                realTpIds);
        List<TerminationPoint> intersectTps = getIntersectTps(intersectionTpIds,
                realTpMap);
        List<TerminationPoint> notInRealTps = notInRealTpIds.stream().map(id -> configTpMap.get(id))
                .collect(
                        Collectors.toList());
        List<TerminationPoint> notInConfigTps = notInConfigTpIds.stream()
                .map(realTpMap::get)
                .map(this::convertEmlTp2CtrlTp).collect(
                        Collectors.toList());
        List<TerminationPoint> tps = new ArrayList<>();
        tps.addAll(intersectTps);
        tps.addAll(notInRealTps);
//        tps.addAll(notInConfigTps);
        return tps;
    }

    /**
     * add abnormal oper status to the properties
     *
     * @param properties
     * @param missConfigEquips
     * @param missMatchEquips
     * @return
     */
    private Properties addProperties(Properties properties, List<Equipments> missConfigEquips,
            List<Equipments> missMatchEquips) {
        List<Property> propertyList = properties.getProperty();
        Map<String, String> propertyMap = propertyList.stream().collect(
                Collectors.toMap(property -> property.getKey().getName(), Property::getValue));

        List<Equipments> wrongEquips = new ArrayList<>(missConfigEquips);
        wrongEquips.addAll(missMatchEquips);
        List<String> wrongEqNames = wrongEquips.stream()
                .map(equipments -> equipments.getFriendlyName()).collect(
                        Collectors.toList());
        String misAlign =
                wrongEqNames.stream().collect(Collectors.joining(","));
        propertyMap.put(MISALIGN, misAlign);
        propertyList = propertyMap.keySet().stream().map(key -> {
            PropertyBuilder propertyBuilder = new PropertyBuilder();
            propertyBuilder.setKey(new PropertyKey(key));
            propertyBuilder.setValue(propertyMap.get(key));
            return propertyBuilder.build();
        }).collect(Collectors.toList());
        PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
        return propertiesBuilder.setProperty(propertyList).build();
    }

    /**
     * base on op termination point
     *
     * @param intersectionTpIds
     * @param realTpMap
     * @return
     */
    private List<TerminationPoint> getIntersectTps(Set<String> intersectionTpIds,
            Map<String, org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPoint> realTpMap) {
        List<TerminationPoint> intersectTps = new ArrayList<>();
        for (String tpId : intersectionTpIds) {
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPoint realTp = realTpMap.get(
                    tpId);
            TerminationPoint realCtrlTp = convertEmlTp2CtrlTp(realTp);
            intersectTps.add(realCtrlTp);
        }
        return intersectTps;
    }

    private List<TerminationPoint> rebuildTps(List<TerminationPoint> tps,
            Map<String, String> equipIdRefFriendlyNameMap) {
        return tps.stream().filter(tp -> {
            String tpId = tp.getTpId().getValue();
            String equipId = PhysicalTpIdNamingRule.getEquipId(tpId);
            String friendlyName = FriendlyNameGenerator.generate(tpId,
                    equipIdRefFriendlyNameMap.get(equipId));
            assert friendlyName != null;
            return !friendlyName.startsWith(SLOT_PREFIX);

        }).map(tp -> {
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpPhysical = tp.getAugmentation(
                    TerminationPoint1.class).getPhysical();
            String tpId = tp.getTpId().getValue();
            String equipId = PhysicalTpIdNamingRule.getEquipId(tpId);
            String friendlyName = FriendlyNameGenerator.generate(tpId,
                    equipIdRefFriendlyNameMap.get(equipId));
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder physicalBuilder = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                    tpPhysical);
            physicalBuilder.setFriendlyName(friendlyName);
            TerminationPointBuilder terminationPointBuilder = new TerminationPointBuilder(tp);
            terminationPointBuilder.addAugmentation(TerminationPoint1.class,
                    new TerminationPoint1Builder(tp.getAugmentation(
                            TerminationPoint1.class)).setPhysical(physicalBuilder.build()).build());
            return terminationPointBuilder.build();
        }).collect(Collectors.toList());
    }


    /**
     * build equipments Map
     *
     * @param equipments
     * @return
     */
    private Map<String, Equipments> buildEquipmentsMap(List<Equipments> equipments) {
        Map<String, Equipments> equipsMap = equipments.stream().collect(Collectors.toMap(
                PhyEquipAttributes::getEquipmentId, equip -> equip));
        return equipsMap;
    }

    private TerminationPoint convertEmlTp2CtrlTp(
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPoint tp) {
        TerminationPoint realCtrlTp = new TerminationPointBuilder()
                .setTpId(tp.getTpId())
                .setKey(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey(
                        new TpId(tp.getTpId())))
                .addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder(tp).build())
                .build();
        return realCtrlTp;
    }

    @Override
    public void sendNotification(Node phyNode) {
        log.debug("broad cast the ne message");
        Physical nodePhysical = phyNode.getAugmentation(Node1.class).getPhysical();
        String friendlyName = nodePhysical.getFriendlyName();
        NeSynchronizedDto synchronizedDto = NeSynchronizedDto.builder()
                .ip(nodePhysical.getIp())
                .friendlyName(friendlyName)
                .neId(phyNode.getNodeId().getValue())
                .port(String.valueOf(nodePhysical.getPort().getValue()))
                .build();
        BroadcastMessager.publishKafkaMessage(BroadcastMessage.builder().title(
                        BroadCastConstant.NE_SYNCHRONIZE_STATUS)
                .message(synchronizedDto.toString()).error(false).build());
        log.info("send ne status synchronized :{}", phyNode.getNodeId());
        StatusMessageSender.sendMessage(NeStatusMessage.builder().neStatuses(
                Collections.singletonList(NeStatus.builder()
                        .neStatus(NeStatusType.SYNCED)
                        .neId(phyNode.getNodeId().getValue())
                        .build())).build());
    }

    private MissAlignDto buildMissAlign(MissAlignEquipmentDto missAlignEquipmentDto) {
        log.debug("build miss align description");
        Equipments configEquip = missAlignEquipmentDto.getDesignEquipment();
        Equipments realEquip = missAlignEquipmentDto.getRealEquipment();
        log.info("build miss align equipment id is:{}", configEquip.getEquipmentId());
        String configEquipType = configEquip.getEquipTypeVendorSpecific();

        String realEquipType =
                realEquip == null ? EquipType.EMPTY.name() : realEquip.getEquipTypeInstalled();
        String slot = PhysicalEqpIdNamingRule.getSlotFromEquipId(configEquip.getEquipmentId());
        return MissAlignDto.builder().slot(slot).designType(configEquipType)
                .actualType(realEquipType).build();
    }

    @Override
    public void startSynchronizingNe(String neId) {
        log.info("start synchronizing ne neId:{}", neId);
        if (!registerNeCache.trySyncLock(neId)) {
            log.info("[sync duplicate] NE {} is already processing, skip", neId);
            return;
        }
        String friendlyName = phyNodeDao.getFriendlyName(neId);
        log.debug("start to synchronized the ne :{}", friendlyName);
        neResourceService.SyncNe(neId, friendlyName);
        log.debug("synchronized finished!");
    }


    @Data
    @Builder
    @AllArgsConstructor
    public static class CalculateEquipmentsDto {

        private List<MissAlignEquipmentDto> missMatchEquips;

        private List<MissAlignEquipmentDto> missConfigEquips;

        private AlignmentStatusType alignmentStatus;

        private List<Equipments> mergeEquips;


    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class MergeEquipmentsDto {

        private List<Equipments> mergeEquips;

        private AlignmentStatusType alignmentStatusType;

        private List<MissAlignEquipmentDto> missMatchEquips;
    }


}
