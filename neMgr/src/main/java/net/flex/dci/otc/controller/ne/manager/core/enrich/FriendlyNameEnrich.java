/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.ne.manager.core.enrich;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.nes.top.nes.Ne;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.nes.top.nes.NeBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "prototype", proxyMode = ScopedProxyMode.TARGET_CLASS)
@Slf4j
public class FriendlyNameEnrich {

    @Autowired
    private PhyNodeDao phyNodeDao;

    private Map<String, Equipments> equipMap = new HashMap<String, Equipments>();

    private Map<String, String> equipFriendlyNameMap = new HashMap<>();

    private Map<String, String> equipConfigFriendNameMap = new HashMap<>();

    public Ne enrich(String neId, Ne emlNe) {
        Ne ne = setDefaultEquipFriendlyName(neId, emlNe);
        ne = setDefaultTpFriendlyName(ne, emlNe);
        return ne;
    }

    private Ne setDefaultEquipFriendlyName(String neId, Ne emlNe) {
        log.debug("Begin to configure equip default friendly name for ne {}", neId);
        NeBuilder neBuilder = new NeBuilder(emlNe);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical configPhy = phyNodeDao.getConfigPhysicalByNode(
                neId);
        if (configPhy != null) {
            if (configPhy.getEquipments() != null) {
                extractedEquipment(configPhy);
            } else {
                extractedEquipment(emlNe.getPhysical());
            }

        }

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical phy = emlNe
                .getPhysical();
        List<Equipments> adapterEquips = phy.getEquipments();
        List<Equipments> equipments = new ArrayList<>();
        if (adapterEquips != null) {
            for (Equipments equip : adapterEquips) {
                String equipSegment = equip.getEquipmentId().split("#")[2];
                equipMap.put(equipSegment, equip);
                EquipmentsBuilder equipBuilder = new EquipmentsBuilder(equip);
                equipBuilder.setEquipmentId(equip.getEquipmentId());
                equipBuilder.setKey(new EquipmentsKey(equip.getEquipmentId()));
                String friendlyName = constructEquipFriendlyName(equip);
                if (friendlyName == null) {
                    continue;
                }
//                if (!StringUtils.isBlank(equipConfigFriendNameMap.get(equip.getEquipmentId()))) {
//                    friendlyName = equipConfigFriendNameMap.get(equip.getEquipmentId());
//                }
                equipBuilder.setFriendlyName(friendlyName);
                if (!StringUtils.isBlank(equip.getEquipTypeInstalled())) {
                    equipBuilder.setEquipTypeVendorSpecific(equip.getEquipTypeInstalled());
                }
                equipments.add(equipBuilder.build());
                equipFriendlyNameMap.put(equip.getEquipmentId(), friendlyName);
            }
        }
        PhysicalBuilder physicalBuilder = new PhysicalBuilder(phy);
        physicalBuilder.setEquipments(equipments);
        neBuilder.setPhysical(physicalBuilder.build());
        return neBuilder.build();
    }

    private void extractedEquipment(Physical configPhy) {
        for (Equipments equip : configPhy.getEquipments()) {
            if (equip.getEquipmentId().contains("LINECARD") && !StringUtils
                    .isBlank(equip.getFriendlyName())) {
                equipConfigFriendNameMap.put(equip.getEquipmentId(),
                        equip.getFriendlyName());
            }
        }
    }

    private Ne setDefaultTpFriendlyName(Ne ne, Ne emlNe) {
        log.debug("Begin to configure tp default friendly name for ne {}",
                ne.getNodeId().getValue());
        NeBuilder neBuilder = new NeBuilder(ne);
        List<TerminationPoint> terminationPoints = emlNe.getTerminationPoint();
        List<TerminationPoint> tps = new ArrayList<>();
        if (terminationPoints != null) {
            for (TerminationPoint tp : terminationPoints) {
                if (tp.getPhysical() != null) {
                    String tpId = tp.getTpId().getValue();
                    String friendlyName = this.constructTpFriendlyName(tpId);
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder phyBuilder =
                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                                    tp.getPhysical());
                    phyBuilder.setFriendlyName(friendlyName);
                    tps.add(new TerminationPointBuilder().setKey(tp.getKey()).setTpId(tp.getTpId())
                            .setPhysical(phyBuilder.build()).build());
                }
            }

        }
        neBuilder.setTerminationPoint(tps);
        return neBuilder.build();
    }

    private String constructEquipFriendlyName(Equipments equip) {
        String equipId = equip.getEquipmentId();
        try {
            String arr[] = equipId.split("#");
            if (arr.length != 3) {
                return null;
            }
            String neId = new StringBuilder().append(arr[0]).append("#").append(arr[1]).toString();
            String equipSegment = equipId.split("#")[2];
            String equipClass = equip.getEquipClass();
            Boolean isEmpty = equip.isEmpty();
            if (isEmpty != null && isEmpty) {
                return equipSegment;
            } else {
                if (equipId.contains("LINECARD")) {
                    String slotInfo = equipSegment.substring(equipSegment.indexOf("-"));
                    if (StringUtils.isBlank(equipClass)) {
                        String name = this.getEquipFriendlyNameFromConfig(neId, equipId);
                        if (StringUtils.isBlank(name)) {
                            return equipSegment;
                        } else {
                            return name;
                        }
                    } else {
                        return new StringBuilder(equipClass).append(slotInfo).toString();
                    }
                } else {
                    return equipSegment;
                }
            }
        } catch (Exception e) {
            log.error("Failed to configure friendly name for equip {}", equipId, e);
        }
        return null;
    }

    private String getEquipFriendlyNameFromConfig(String neId, String equipId) {
        return equipConfigFriendNameMap.get(equipId);
    }

    private String constructTpFriendlyName(String tpId) {
        try {
            String arr[] = tpId.split("#");
            if (arr.length != 4) {
                return null;
            }
            String tpSegment = arr[3];
            String equipId = new StringBuilder().append(arr[0]).append("#").append(arr[1])
                    .append("#").append(arr[2]).toString();
            String equipFriendName = this.equipFriendlyNameMap.get(equipId);
            if (StringUtils.isBlank(equipFriendName)) {
                equipFriendName = arr[2];
            }
            String splits[] = tpSegment.split("-");
            String tpFriendName = new StringBuilder().append(equipFriendName).append("-")
                    .append(splits[splits.length - 1]).toString();
            return tpFriendName;
        } catch (Exception e) {
            log.error("Failed to configure friendly name for tp {}", tpId);
        }
        return null;
    }
}
