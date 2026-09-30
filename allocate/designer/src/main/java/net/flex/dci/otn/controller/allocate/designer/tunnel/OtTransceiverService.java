/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NameGenerator;
import net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.Port;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ETHERNETCOMPLIANCECODE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class OtTransceiverService {

    public static final String ETHERNET_PMD = "ETHERNET_PMD_TYPE";
    public static final String FEC_MODE = "FEC_MODE";
    public static final String ENABLE = "ENABLED";
    public static final String DISABLE = "DISABLED";
    @Autowired
    private EquipmentRepo equipmentRepo;

    @Autowired
    private TunnelUtils tunnelUtils;

    /**
     * Create L Port/C Port transceiver, if not exists
     *
     * @param node
     * @param ctp
     * @param ltp
     * @param otCardInfo
     * @param clientMedium
     * @return
     * @throws NeDesignerException
     */
    public List<Equipments> createTransceiver(Node node, String ctp, String ltp, Card otCardInfo,
            Class<? extends ETHERNETCOMPLIANCECODE> clientMedium) throws NeDesignerException {
        String cPortName = PhysicalTpIdNamingRule.getPortNameByTpId(ctp);
        String lPortName = PhysicalTpIdNamingRule.getPortNameByTpId(ltp);
        List<Equipments> transceivers = new ArrayList<>();
        for (Port port : otCardInfo.getPorts()) {
            if (port.getTransceiver() != null) {
                List<String> portNames = NeInfoUtil.getNameList(port.getName());
                if (portNames.contains(cPortName)) {

                    String transceiverName = NameGenerator.getTransceiverNameByTpName(port.getTransceiver().getName(), PhysicalTpIdNamingRule.getShortTpByTpId(ctp));
                    if (!equipmentRepo.isTransceiverExisted(node, transceiverName)) {
                        Properties property = new PropertiesBuilder().setProperty(new ArrayList() {{
                            add(new PropertyBuilder().setName(ETHERNET_PMD)
                                    .setValue(tunnelUtils.getClientMediumLocalName(clientMedium))
                                    .build());
                            add(getFecProperty(clientMedium));
                        }}).build();
                        transceivers.add(equipmentRepo.createTransceiver(node.getNodeId().getValue(), transceiverName, property, transceiverName));
                    }

                }
                if (portNames.contains(lPortName)) {

                    String transceiverName = NameGenerator.getTransceiverNameByTpName(port.getTransceiver().getName(), PhysicalTpIdNamingRule.getShortTpByTpId(ltp));
                    if (!equipmentRepo.isTransceiverExisted(node, transceiverName)) {
                        transceivers.add(
                                equipmentRepo.createTransceiver(node.getNodeId().getValue(), transceiverName, transceiverName));//deprecated, so just let transceiverName as friendlyName, for compile
                    }

                }
            }
        }
        return transceivers;
    }

    public String getTransceiverClientMedium(Equipments equip) throws NeDesignerException {
        Properties properties = equip.getProperties();

        if (properties != null && properties.getProperty() != null) {
            for (Property property : properties.getProperty()) {
                if (property.getName().equals(ETHERNET_PMD)) {
                    return property.getValue();
                }

            }
        }
        log.error("Failed to get property:{} from transceiver: {}.", ETHERNET_PMD, equip.getEquipmentId());
        throw new NeDesignerException("Failed to get property: client medium from " + equip.getEquipmentId());
    }

    /**
     * 对于newOch这种场景，就会创建： 1个L口的transceiver，和交叉数量一样的C口transceiver
     *
     * @param newXcsPerOch
     * @param otCardInfo
     * @param clientMedium
     * @param portIdFriendlyNameMap
     * @return
     */
    public List<Equipments> createTransceiversNewOch(String nodeId, List<CrossConnections> newXcsPerOch, Card otCardInfo, Class<? extends ETHERNETCOMPLIANCECODE> clientMedium,
            Map<String, String> portIdFriendlyNameMap)
            throws NeDesignerException {
        List<String> ctps = newXcsPerOch.stream()
                .map(item -> item.getSourceTp().get(0).getTpRef().getValue()).collect(Collectors.toList());//对于OT的交叉，C口固定为source，并且只有一个
        String ltp = newXcsPerOch.get(0).getDestinationTp().get(0).getTpRef().getValue();//因为是newOch场景，所以有且仅有一个L口
        List<Equipments> transceivers = new ArrayList<>();
        String lPortName = PhysicalTpIdNamingRule.getPortNameByTpId(ltp);
//        int totalSize = newXcsPerOch.size() + 1;
        int totalSize = newXcsPerOch.size();//不需要L口transceiver
        for (Port port : otCardInfo.getPorts()) {
            if (port.getTransceiver() != null) {
                List<String> portNames = NeInfoUtil.getNameList(port.getName());
                for (String ctp : ctps) {
                    String cPortName = PhysicalTpIdNamingRule.getPortNameByTpId(ctp);
                    if (portNames.contains(cPortName)) {
                        String transceiverName = NameGenerator.getTransceiverNameByTpName(port.getTransceiver().getName(), PhysicalTpIdNamingRule.getShortTpByTpId(ctp));
                        Properties property = new PropertiesBuilder().setProperty(new ArrayList() {{
                            add(new PropertyBuilder().setName(ETHERNET_PMD)
                                    .setValue(tunnelUtils.getClientMediumLocalName(clientMedium))
                                    .build());
                            add(getFecProperty(clientMedium));
                        }}).build();
                        String tpFriendlyName = portIdFriendlyNameMap.get(ctp);
                        String transceiverFriendlyName = NameGenerator.getTransceiverNameByTpName(port.getTransceiver().getName(), tpFriendlyName);

                        transceivers.add(equipmentRepo.createTransceiver(nodeId, transceiverName, property, transceiverFriendlyName));
                    }
                }
                if (portNames.contains(lPortName)) {
//                    String transceiverName = NameGenerator.getTransceiverNameByTpName(port.getTransceiver().getName(), PhysicalTpIdNamingRule.getShortTpByTpId(ltp));
//                    String tpFriendlyName = portIdFriendlyNameMap.get(ltp);
//                    String transceiverFriendlyName = NameGenerator.getTransceiverNameByTpName(port.getTransceiver().getName(), tpFriendlyName);
//
//                    transceivers.add(equipmentRepo.createTransceiver(nodeId, transceiverName, transceiverFriendlyName));
                }
                if (transceivers.size() == totalSize) {
                    break;
                }
            }
        }
        return transceivers;
    }

    private Property getFecProperty(Class<? extends ETHERNETCOMPLIANCECODE> clientMedium) {
        PropertyBuilder fecPropertyBuilder = new PropertyBuilder().setName(FEC_MODE);
        if (clientMedium.getName().toUpperCase().endsWith("LR4")) {
            return fecPropertyBuilder.setValue(DISABLE).build();
        }
        return fecPropertyBuilder.setValue(ENABLE).build();
    }



    public List<Equipments> createTransceiversReuseOch(String nodeId, List<CrossConnections> xcs, Card otCardInfo, Class<? extends ETHERNETCOMPLIANCECODE> clientMedium,
            Map<String, String> portIdFriendlyNameMap) throws NeDesignerException {
        return createTransceiversNewOch(nodeId, xcs, otCardInfo, clientMedium, portIdFriendlyNameMap);
    }
}

