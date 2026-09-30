package net.flex.dci.otn.controller.allocate.link.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.NodeUtils;
import net.flex.dci.otn.controller.allocate.designer.bom.BomMetaInfo;
import net.flex.dci.otn.controller.allocate.designer.bom.BomMetaService;
import net.flex.dci.otn.controller.allocate.designer.config.BomMetaConfig;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.BomInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.bom.info.NeBomGroup;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.bom.info.NeBomGroupBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.bom.info.SiteBomGroup;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.bom.info.SiteBomGroupBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.ne.bom.group.info.Site;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.ne.bom.group.info.SiteBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.ne.bom.group.info.site.NeBomInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.ne.bom.group.info.site.NeBomInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.allocate.link.output.BomInfoBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.Nodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BomGenerator {

    @Autowired
    private BomMetaService bomMetaService;
    @Autowired
    private SiteNodeDao siteNodeDao;
    @Autowired
    private BomMetaConfig bomMetaConfig;
    @Autowired
    private NodeUtils nodeUtils;

    public static String NO_BOM_TRANSCEIVER = "TRANSCEIVER";


    public Map<String, Map<String, NeBomInfo>> generateBomMap(Map<String, Node> bomNodes, String vendorName, String vendorType, List<Nodes> reusedNodesSnapshot) throws NeDesignerException {
        Map<String, Map<String, NeBomInfo>> bomInfoMap = new HashMap<>();//key is the same as the  key for bomMetaMap in BomMetaConfig.class, {bomMetaKey,{neId,neBomInfo}}

        Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.Nodes> reusedNodes = reusedNodesSnapshot.stream()
                .collect(Collectors.toMap(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.Nodes::getNodeId, Function.identity()));

        for (Entry<String, Node> entry : bomNodes.entrySet()) {

            String neId = entry.getKey();
            Nodes nodeInDBSnapshot = reusedNodes.get(neId);
            Map<String, org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.nodes.Equipments> oldEquipmentMap = null;
            if (nodeInDBSnapshot != null) {
                oldEquipmentMap = nodeInDBSnapshot.getEquipments().stream()
                        .collect(Collectors.toMap(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.nodes.Equipments::getEquipmentId, Function
                                .identity()));
            }

            Node newNode = entry.getValue();
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical
                    physical = newNode.getAugmentation(Node1.class).getPhysical();
            List<Equipments> newEquipments = physical.getEquipments();
            String neName = physical.getFriendlyName();

            for (Equipments equip : newEquipments) {
                if (oldEquipmentMap != null) {
                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.ui.ne.info.nodes.Equipments oldEquip = oldEquipmentMap.get(equip
                            .getEquipmentId());
                    if (oldEquip != null && oldEquip.getEquipType() != null && oldEquip.getEquipType().equals(equip.getEquipType())) {
                        continue;//这个equipment不是新产生的，不需要产生BOM}
                    }
                }

                String bomMetaKey = null;
                try {
                    bomMetaKey = bomMetaService.getBomMetaKey(equip, vendorName, vendorType);
                } catch (NeDesignerException e) {
                    log.error("Failed to update bom map for the equipment:{}", equip);
                    throw new NeDesignerException("Failed to get bomMetaKey for equipment: " + equip.getEquipmentId());
                }

                if (bomMetaKey.endsWith(NO_BOM_TRANSCEIVER)) {
                    log.debug("No bom for {}, continue.", NO_BOM_TRANSCEIVER);
                    continue;
                }

                Map<String, NeBomInfo> neBomInfoMap = bomInfoMap.get(bomMetaKey);
                if (neBomInfoMap == null) {
                    neBomInfoMap = new HashMap<>();
                    NeBomInfo neBomInfo = new NeBomInfoBuilder().setNeId(neId).setNeName(neName).setCount(1).setSlot(equip.getSlot()).build();
                    neBomInfoMap.put(neId, neBomInfo);
                } else {
                    NeBomInfo neBomInfo = neBomInfoMap.get(neId);
                    if (neBomInfo == null) {
                        neBomInfo = new NeBomInfoBuilder().setNeId(neId).setNeName(neName).setCount(1).setSlot(equip.getSlot()).build();
                        neBomInfoMap.put(neId, neBomInfo);
                    } else {
                        Integer count = neBomInfo.getCount() + 1;
                        String slot = String.format("%s,%s", neBomInfo.getSlot(), equip.getSlot());
                        neBomInfo = new NeBomInfoBuilder().setNeId(neId).setNeName(neName).setCount(count).setSlot(slot).build();
                        neBomInfoMap.put(neId, neBomInfo);
                    }
                }

                bomInfoMap.put(bomMetaKey, neBomInfoMap);
            }

        }
        return bomInfoMap;
    }


    public BomInfo constructBomInfo(Map<String, Map<String, NeBomInfo>> totalBomInfo) throws NeDesignerException {
        List<NeBomGroup> neBomGroups = new ArrayList();
        List<SiteBomGroup> siteBomGroups = new ArrayList();
        for (Map.Entry<String, Map<String, NeBomInfo>> bomInfoItem : totalBomInfo.entrySet()) {
            if(bomInfoItem.getKey().startsWith("VIRTUAL")){
                log.debug("No bom for :{}",bomInfoItem.getKey());
                continue;
            }

            BomMetaInfo bomMetaInfo = bomMetaConfig.getBomMetaInfo(bomInfoItem.getKey());
            if (bomMetaInfo == null) {
                String msg = String.format("No BomMetaInfo found by the key: %s, please check your BOM csv", bomInfoItem.getKey());
                log.error(msg);
                throw new NeDesignerException(msg);
            }
            Map<String, List<NeBomInfo>> neBomInfoMapGroupBySite = bomInfoItem.getValue()
                    .values()
                    .stream()
                    .collect(Collectors.groupingBy(item -> PhysicalNodeIdNamingRule.getSiteId(item.getNeId())));

            List<Site> neBomGroupSites = new ArrayList<>();
            List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.site.bom.group.info.Site> siteBomGroupSites = new ArrayList<>();
            for (Map.Entry<String, List<NeBomInfo>> entry : neBomInfoMapGroupBySite.entrySet()) {

                List<NeBomInfo> neBomInfosForSite = entry.getValue();
                String siteId = entry.getKey();
                String siteName = getSiteName(siteId);

                //分设备BOM表的站点信息
                Site neBomGroupSite = new SiteBuilder().setSiteId(siteId).setSiteName(siteName).setNeBomInfo(neBomInfosForSite).build();
                neBomGroupSites.add(neBomGroupSite);

                //分站点BOM表的站点信息
                Integer count = neBomInfosForSite.stream().mapToInt(item -> item.getCount().intValue()).sum();
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.site.bom.group.info.Site siteBomGroupSite = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.bom.rev200710.site.bom.group.info.SiteBuilder()
                        .setSiteId(siteId).setSiteName(siteName).setCount(count).build();
                siteBomGroupSites.add(siteBomGroupSite);


            }

            Integer count = bomInfoItem.getValue().values().stream().mapToInt(item -> item.getCount().intValue()).sum();

            NeBomGroup neBomGroup = new NeBomGroupBuilder()
                    .setComponentType(bomMetaInfo.getComponentType())
                    .setCount(count)
                    .setEquipType(bomMetaInfo.getEquipTypeConfiged())
                    .setName(bomMetaInfo.getName())
                    .setSerialNo(bomMetaInfo.getSerialNo())
                    .setSite(neBomGroupSites)
                    .setPn(bomMetaInfo.getPn())
                    .setFru(bomMetaInfo.getFru()).build();
            neBomGroups.add(neBomGroup);

            SiteBomGroup siteBomGroup = new SiteBomGroupBuilder().
                    setComponentType(bomMetaInfo.getComponentType())
                    .setCount(count)
                    .setEquipType(bomMetaInfo.getEquipTypeConfiged())
                    .setName(bomMetaInfo.getName())
                    .setSerialNo(bomMetaInfo.getSerialNo())
                    .setSite(siteBomGroupSites)
                    .setPn(bomMetaInfo.getPn())
                    .setFru(bomMetaInfo.getFru()).build();
            siteBomGroups.add(siteBomGroup);
        }
        return new BomInfoBuilder()
                .setNeBomGroup(neBomGroups)
                .setSiteBomGroup(siteBomGroups).build();
    }

    private String getSiteName(String siteId) {
        try {
            return siteNodeDao.getSiteNodeAttributeSite(siteId).getFriendlyName();
        } catch (Exception e) {
            log.error("Failed to get siteName for {], return siteId instead.", siteId, e);
            return siteId;
        }
    }

    private Map<String, Map<String, NeBomInfo>> generateBomMap(Map<String, Node> bomNodes, List<Node> reusedNodesSnapshot) throws NeDesignerException {
        Map<String, Map<String, NeBomInfo>> bomInfoMap = new HashMap<>();//key is the same as the  key for bomMetaMap in BomMetaConfig.class, {bomMetaKey,{neId,neBomInfo}}

        Map<String, Node> reusedNodes = reusedNodesSnapshot.stream()
                .collect(Collectors.toMap(item -> item.getNodeId().getValue(), Function.identity(), (first, second) -> first));

        for (Entry<String, Node> entry : bomNodes.entrySet()) {

            String neId = entry.getKey();
            Node nodeInDBSnapshot = reusedNodes.get(neId);
            Map<String, Equipments> oldEquipmentMap = null;
            if (nodeInDBSnapshot != null) {
                oldEquipmentMap = nodeInDBSnapshot.getAugmentation(Node1.class)
                        .getPhysical()
                        .getEquipments()
                        .stream()
                        .collect(Collectors.toMap(item -> item.getEquipmentId(), Function.identity()));
            }

            Node newNode = entry.getValue();
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical
                    physical = newNode.getAugmentation(Node1.class).getPhysical();
            List<Equipments> newEquipments = physical.getEquipments();
            String neName = physical.getFriendlyName();
            String vendorName = physical.getVendorName();
            String vendorType = physical.getVendorType();

            for (Equipments equip : newEquipments) {
                if (oldEquipmentMap != null) {
                    Equipments oldEquip = oldEquipmentMap.get(equip.getEquipmentId());
                    if (oldEquip != null && oldEquip.getEquipType() != null && oldEquip.getEquipType().equals(equip.getEquipType())) {
                        continue;//这个equipment不是新产生的，不需要产生BOM}
                    }
                }

                String bomMetaKey = null;
                try {
                    bomMetaKey = bomMetaService.getBomMetaKey(equip, vendorName, vendorType);
                } catch (NeDesignerException e) {
                    log.error("Failed to update bom map for the equipment:{}", equip);
                    throw new NeDesignerException("Failed to get bomMetaKey for equipment: " + equip.getEquipmentId());
                }

                if (bomMetaKey.endsWith(NO_BOM_TRANSCEIVER)) {
                    log.debug("No bom for {}, continue.", NO_BOM_TRANSCEIVER);
                    continue;
                }

                Map<String, NeBomInfo> neBomInfoMap = bomInfoMap.get(bomMetaKey);
                if (neBomInfoMap == null) {
                    neBomInfoMap = new HashMap<>();
                    NeBomInfo neBomInfo = new NeBomInfoBuilder().setNeId(neId).setNeName(neName).setCount(1).build();
                    neBomInfoMap.put(neId, neBomInfo);
                } else {
                    NeBomInfo neBomInfo = neBomInfoMap.get(neId);
                    if (neBomInfo == null) {
                        neBomInfo = new NeBomInfoBuilder().setNeId(neId).setNeName(neName).setCount(1).build();
                        neBomInfoMap.put(neId, neBomInfo);
                    } else {
                        Integer count = neBomInfo.getCount() + 1;
                        neBomInfo = new NeBomInfoBuilder().setNeId(neId).setNeName(neName).setCount(count).build();
                        neBomInfoMap.put(neId, neBomInfo);
                    }
                }

                bomInfoMap.put(bomMetaKey, neBomInfoMap);
            }

        }
        return bomInfoMap;
    }

    public BomInfo constructBomInfo(Map<String, Node> totalInMemoryNode, List<Node> nodeSnapShot) throws NeDesignerException {
        Map<String, Map<String, NeBomInfo>> bomMap = generateBomMap(totalInMemoryNode, nodeSnapShot);
        return constructBomInfo(bomMap);
    }
}
