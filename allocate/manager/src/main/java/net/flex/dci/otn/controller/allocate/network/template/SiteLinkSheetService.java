/*
 * Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.network.template;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.Links;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.Nodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.network.sitelinks.SiteLinks;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SiteLinkSheetService {

    public static final String TP_PREFIX = "PORT";
    public static final String PRIMARY = "主";
    public static final String SECONDARY = "备";

    //从性能角度考虑，在分析siteLink数据时，顺便把该sitelink涉及到的wss板卡信息存下来，后续处理SiteLinkRelationSheetData会用到
    public Pair<List<SiteLinkSheetData>, Map<String, String>> createSheetData(SiteLinks siteLinkItem) {
        List<SiteLinkSheetData> result = new ArrayList<>();

//        List<Links> links = siteLinkItem.getLinks();
        List<Links> links = Collections.EMPTY_LIST;//todo: comment for compile
        //kye is nodeId
//        Map<String, Nodes> nodeMap = siteLinkItem.getNodes().stream().collect(Collectors.toMap(node -> node.getNodeId().getValue(), Function.identity()));
        Map<String, Nodes> nodeMap = Collections.EMPTY_MAP;//todo: comment for compile
        //<equipId,equip-type-vendor-specific>
        Pair<Map<String, String>, Map<String, String>> equipMapPair = getEquipSpecMap(Collections.EMPTY_LIST);//todo: comment for compile
//        Pair<Map<String, String>, Map<String, String>> equipMapPair = getEquipSpecMap(siteLinkItem.getNodes());
        Map<String, String> equipMap = equipMapPair.getLeft();
        Map<String, String> wssEquipMap = equipMapPair.getRight();

        for (Links link : links) {
            if (link.getPhysical().getLinkType() == LinkType.CableLink) {
                continue;
            }
            String linkName = siteLinkItem.getFriendlyName();

            //get neInfo, equipTypeVendorSpecific from source
            Source source = link.getSource();
            String aNeId = source.getSourceNode().getValue();
            Nodes aNe = nodeMap.get(aNeId);
            if (aNe == null) {
                log.error("Invalid compute input for siteLink:{}, because failed to get source  nodeInfo:{}", siteLinkItem, aNeId);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, String.format("Invalid input, failed to get source nodeInfo :%s for siteLink:%s", aNeId, linkName));
            }
            String aTpId = source.getSourceTp().getValue();//e.g.  "Site-1686060441896#Ne-1686211011211#LINECARD-1-51#PORT-1-51-MUXDMUX"
            String aEquipId = PhysicalTpIdNamingRule.getEquipId(aTpId);
            String aEquipTypeVendorSpecific = equipMap.get(aEquipId);
            if (aEquipTypeVendorSpecific == null) {
                log.error("Invalid compute input for siteLink:{}, because failed to get equipTypeVendorSpecific for equip:{}", siteLinkItem, aEquipId);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        String.format("Invalid input, failed to get source equipTypeVendorSpecific :%s for siteLink:%s", aEquipId, linkName));
            }
            String aTpName = getTpName(aEquipTypeVendorSpecific, aTpId);

            //get neInfo, equipTypeVendorSpecific from dest
            Destination destination = link.getDestination();
            String zNeId = destination.getDestNode().getValue();
            Nodes zNe = nodeMap.get(zNeId);
            if (zNe == null) {
                log.error("Invalid compute input for siteLink:{}, because failed to get dest nodeInfo:{}", siteLinkItem, aNeId);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, String.format("Invalid input, failed to get dest nodeInfo :%s for siteLink:%s", aNeId, linkName));
            }
            String zTpId = destination.getDestTp().getValue();
            String zEquipId = PhysicalTpIdNamingRule.getEquipId(zTpId);
            String zEquipTypeVendorSpecific = equipMap.get(zEquipId);
            if (zEquipTypeVendorSpecific == null) {
                log.error("Invalid compute input for siteLink:{}, because failed to get dest equipTypeVendorSpecific for equip:{}", siteLinkItem, zEquipId);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        String.format("Invalid input, failed to get dest equipTypeVendorSpecific :%s for siteLink:%s", zEquipId, linkName));
            }
            String zTpName = getTpName(zEquipTypeVendorSpecific, zTpId);

            SiteLinkSheetData siteLinkSheetData = SiteLinkSheetData.builder()
                    .primarySecondary(PRIMARY)
                    .aNeId(aNeId)
                    .aTpName(aTpName)
                    .zNeId(zNeId)
                    .zTpName(zTpName)
                    .build();
            result.add(siteLinkSheetData);

        }
        return Pair.of(result, wssEquipMap);
    }

    static String getTpName(String equipTypeVendorSpecific, String tpId) {
        return PhysicalTpIdNamingRule.getShortTpByTpId(tpId).replaceFirst(TP_PREFIX, equipTypeVendorSpecific);
    }

    //key is equipId, value is EquipTypeVendorSpecific
    private Pair<Map<String, String>, Map<String, String>> getEquipSpecMap(List<Nodes> nodesList) {
        Map<String, String> equipMap = new HashMap<>();
        Map<String, String> wssEquipMap = new HashMap<>();
        for (Nodes node : nodesList) {
            List<Equipments> equipments = node.getPhysical().getEquipments();
            for (Equipments equip : equipments) {
                if (equip.getEquipTypeVendorSpecific() == null) {
                    continue;
                }
                String equipId = equip.getEquipmentId();
                String equipVendorSpecific = equip.getEquipTypeVendorSpecific();
                equipMap.put(equipId, equipVendorSpecific);
                if (equip.getEquipType().equals(EquipType.WSS)) {
                    wssEquipMap.put(equipId, equipVendorSpecific);
                }
            }
        }
        return Pair.of(equipMap, wssEquipMap);
    }
}
