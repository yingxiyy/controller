/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.ne;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.JsonYangConverter;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NEIdGenerator;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NameGenerator;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.FixEquipModel;
import net.flex.dci.otn.controller.allocate.ne.Port;
import net.flex.dci.otn.controller.allocate.ne.TpModel;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.DirectionTerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.osc.attributes.OscBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.Wdm;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.WdmBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TpRepo {

    public static final String SLOT = "slot";
    @Autowired
    private JsonYangConverter jsonYangConverter;

    /**
     * PortName as key, one map for one card
     *
     * @param equipment
     * @param card
     * @param neInfo
     * @return
     */
    public Map<String, TerminationPoint> createCardTp(Equipments equipment, Card card, NeInfo neInfo) throws NeDesignerException {
        return neInfo.getPort(card.getCardType()).entrySet().stream()
                .filter(port -> !port.getValue().getIsSlave())
                .sorted(Map.Entry.comparingByKey())
                .flatMap(item -> {
                    try {
                        return createTpForPort(equipment, card.getCardType(), item.getValue()).entrySet().stream();
                    } catch (NeDesignerException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> second, LinkedHashMap::new));
    }

    public Map<String, TerminationPoint> createSlaveCardTp(Equipments equipment, Card card, NeInfo neInfo) throws NeDesignerException {
        return neInfo.getPort(card.getCardType()).entrySet().stream()
                .filter(port -> port.getValue().getIsSlave())
                .sorted(Map.Entry.comparingByKey())
                .flatMap(item -> {
                    try {
                        return createTpForPort(equipment, card.getCardType(), item.getValue()).entrySet().stream();
                    } catch (NeDesignerException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (first, second) -> second, LinkedHashMap::new));
    }


    private Map<String, TerminationPoint> createTpForPort(Equipments equipment, String cardType, Port port)
            throws NeDesignerException {
        List<String> nameList = NeInfoUtil.getNameList(port.getName());
        List<String> indexList = NeInfoUtil.getNameList(port.getIndex());
        int size = nameList.size();
        Map<String, TerminationPoint> result = new LinkedHashMap<>();
        for (int i = 0; i < size; i++) {
            String portName = nameList.get(i);
            String portIndex = indexList.get(i);
            TerminationPoint tp = createTp(equipment, cardType, portName, port, i,Integer.parseInt(portIndex));
            result.put(portName, tp);

        }
        return result;
    }

    public TerminationPoint createTp(Equipments equipment, String cardType, String portName, Port port, int frequencyIndex, int index) throws NeDesignerException {
        String tpIdString = NEIdGenerator.createTPId(equipment, portName);
        TpId tpId = new TpId(tpIdString);

        Properties properties= jsonYangConverter.getProperties(port.getParams());
        String centralFrequency = port.getCentralFrequency();
        if (centralFrequency != null) {
            List<Property> propertyList = properties.getProperty();
            try {
                Property slotProperty = new PropertyBuilder().setName(SLOT)
                        .setValue(NeInfo.getSlotByCentralFrequency(centralFrequency, portName, frequencyIndex))
                        .build();
                propertyList.add(slotProperty);
            } catch (NeDesignerException e) {
                throw new NeDesignerException("Failed to get frequency for card: " + cardType, e);
            }
            properties = new PropertiesBuilder().setProperty(propertyList).build();
        }

        PortType portType = jsonYangConverter.getPortType(port.getPortType());
        String friendlyName;
        if (port.getFriendlyName() != null && !port.getFriendlyName().isEmpty()) {
            friendlyName = NameGenerator.createTpFriendlyName(equipment, port.getFriendlyName());
        } else {
            friendlyName = NameGenerator.createTpFriendlyName(equipment, portName);
        }
        return new TerminationPointBuilder().setTpId(tpId)
                .setKey(new TerminationPointKey(tpId))
                .addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder()
                                .setPhysical(new PhysicalBuilder()
                                        .setAdminState(getAdminState(port))
                                        .setAlarmState(AlarmSeverity.Unknown)
                                        .setImplementState(ImplementState.Allocate)
                                        .setOperationalState(OperStatus.Unknown)
                                        .setAlignmentStatus(AlignmentStatusType.Unknown)
                                        .setEquipmentRef(equipment.getEquipmentId())
                                        .setDirection(DirectionTerminationPoint.Bidirection)
                                        .setProperties(properties)
                                        .setNodeRef(equipment.getNodeRef())
                                        .setConnectionStatus(ConnectionStatus.Idle)
                                        .setFriendlyName(friendlyName)
                                        .setPortType(portType)
                                        .setWdm(getWdm(portType))
                                        .setIndex(index)
                                        .build())
                                .build())
                .build();
    }

    private AdminStatus getAdminState(Port port) {
        if (port.getAdminEnable() != null && port.getAdminEnable()) {
            return AdminStatus.Up;
        }
        return AdminStatus.Unknown;
    }

    private Wdm getWdm(PortType portType) {
        return PortType.OALine.equals(portType)
                ? new WdmBuilder().setOsc(new OscBuilder().setAutoAttenuationMode(true).build()).build()
                : null;
    }

    public Map<String, TerminationPoint> getPortNameTpMap(List<TerminationPoint> tps) {
        return tps.stream().collect(Collectors.toMap(tp -> getPortNameForTP(tp), Function.identity(), (first, second) -> first));
    }

    public String getPortNameForTP(TerminationPoint tp) {
        return NameGenerator.getPortNameByTpId(tp.getTpId().getValue());
    }

    public TerminationPoint getBusyTp(TerminationPoint tp) {
        PhysicalBuilder physicalBuilder = new PhysicalBuilder(
                tp.getAugmentation(TerminationPoint1.class).getPhysical());
        physicalBuilder.setConnectionStatus(ConnectionStatus.Busy);
        physicalBuilder.setAdminState(AdminStatus.Up);

        TerminationPoint1Builder terminationPoint1Builder = new TerminationPoint1Builder(
                tp.getAugmentation(TerminationPoint1.class)).setPhysical(physicalBuilder.build());

        return new TerminationPointBuilder(tp)
                .addAugmentation(TerminationPoint1.class, terminationPoint1Builder.build()).build();
    }

    public String getFriendlyNameByTP(TerminationPoint tp) {
        return tp.getAugmentation(TerminationPoint1.class).getPhysical().getFriendlyName();
    }

    public PortType getPortType(TerminationPoint tp) {
        return tp.getAugmentation(TerminationPoint1.class).getPhysical().getPortType();
    }

    public List<TerminationPoint> createFixedTps(String nodeId, List<FixEquipModel> fixEquipModels) throws NeDesignerException {
        List<TerminationPoint> output = new ArrayList<>();
        for (FixEquipModel fixEquipModel : fixEquipModels) {
            List<TpModel> tpModels = fixEquipModel.getTpModel();
            if (tpModels == null || tpModels.isEmpty()) {
                continue;
            }
            String equipSlotInfo = fixEquipModel.getName();
            int index=0; //todo: fix tp暂时不排序
            for (TpModel tpModel : tpModels) {
                List<ImmutablePair<String, String>> tpInfoNames = NeInfoUtil.getTpInfoNamePairList(tpModel);
                for (ImmutablePair<String, String> tpInfoName : tpInfoNames) {
                    output.add(createNonTrafficTp(nodeId, equipSlotInfo, tpInfoName.left, tpInfoName.right, tpModel.getTpType(),index));
                    index++;
                }

            }
        }
        return output;
    }

    private TerminationPoint createNonTrafficTp(String nodeId, String equipSlotInfo, String tpInfo, String tpFriendlyName, String tpType, int index) {
        String tpId = new StringBuilder(nodeId).append("#").append(equipSlotInfo).append("#").append(tpInfo).toString();
        TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
        tpBuilder.setKey(new TerminationPointKey(new TpId(tpId)));
        tpBuilder.setTpId(new TpId(tpId));

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder phyBuilder =
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder();
        phyBuilder.setAdminState(AdminStatus.Unknown);
        phyBuilder.setAlarmState(AlarmSeverity.Unknown);
        phyBuilder.setOperationalState(OperStatus.Unknown);
        phyBuilder.setImplementState(ImplementState.Allocate);
        phyBuilder.setNodeRef(nodeId);
        phyBuilder.setAlignmentStatus(AlignmentStatusType.Unknown);
        phyBuilder.setConnectionStatus(ConnectionStatus.Idle);
        phyBuilder.setDirection(DirectionTerminationPoint.Bidirection);
        phyBuilder.setProperties(new PropertiesBuilder().build());
        phyBuilder.setPortType(PortType.valueOf(tpType));
        phyBuilder.setFriendlyName(tpFriendlyName);
        phyBuilder.setEquipmentRef(
                new StringBuilder(nodeId).append("#").append(equipSlotInfo)
                        .toString());
        phyBuilder.setIndex(index);

        tpBuilder.addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder().setPhysical(phyBuilder.build()).build());
        return tpBuilder.build();
    }

    public TerminationPoint getLookbackEnableTp(TerminationPoint tp) {
        PhysicalBuilder physicalBuilder = new PhysicalBuilder(
                tp.getAugmentation(TerminationPoint1.class).getPhysical());

        List<Property> properties = new ArrayList<>();
        List<Property> oldProperties = physicalBuilder.getProperties() == null ? null : physicalBuilder.getProperties().getProperty();
        if (oldProperties != null && !oldProperties.isEmpty()) {
            properties.addAll(oldProperties);
        }
        properties.add(new PropertyBuilder().setName("loopback-mode").setValue("FACLITY").build());

        physicalBuilder.setProperties(new PropertiesBuilder()
                .setProperty(properties).build());

        TerminationPoint1Builder terminationPoint1Builder = new TerminationPoint1Builder(
                tp.getAugmentation(TerminationPoint1.class)).setPhysical(physicalBuilder.build());

        return new TerminationPointBuilder(tp)
                .addAugmentation(TerminationPoint1.class, terminationPoint1Builder.build()).build();
    }
    public TerminationPoint getApcTp(TerminationPoint tp) {
        PhysicalBuilder physicalBuilder = new PhysicalBuilder(
                tp.getAugmentation(TerminationPoint1.class).getPhysical());

        List<Property> properties = new ArrayList<>();
        List<Property> oldProperties = physicalBuilder.getProperties() == null ? null : physicalBuilder.getProperties().getProperty();
        if (oldProperties != null && !oldProperties.isEmpty()) {
            properties.addAll(oldProperties);
        }
        properties.add(new PropertyBuilder().setName("channel-optical-power-adjustment.control-mode").setValue("APC").build());

        physicalBuilder.setProperties(new PropertiesBuilder()
                .setProperty(properties).build());

        TerminationPoint1Builder terminationPoint1Builder = new TerminationPoint1Builder(
                tp.getAugmentation(TerminationPoint1.class)).setPhysical(physicalBuilder.build());

        return new TerminationPointBuilder(tp)
                .addAugmentation(TerminationPoint1.class, terminationPoint1Builder.build()).build();
    }
}
