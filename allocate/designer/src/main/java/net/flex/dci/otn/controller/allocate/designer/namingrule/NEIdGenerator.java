/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.namingrule;

import static net.flex.dci.otn.controller.allocate.designer.model.NeInfo.PANEL_CARD_TYPE;
import static net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo.CHASSIS_CARD_TYPE;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;

/**
 * Site id is unique in the cluster env, then for this class, no need to consider the unique in cluster.
 */
public class NEIdGenerator {


    public static String createTPId(Equipments equipment, String portName) {
        return PhysicalTpIdNamingRule.createTPId(equipment, portName);
    }

    //e.g. Site-1617160781850#Ne-1617160840343
    public static String createNodeId(@NonNull String siteId) {
//        return String.format("%s#Ne-%s", siteId, System.currentTimeMillis());
        return PhysicalNodeIdNamingRule.getNewNodeId(siteId);
    }

    public static String getSiteIdByNodeId(String nodeId) {
        return PhysicalNodeIdNamingRule.getSiteId(nodeId);
    }

    /**
     * required: XC-<unique id>
     *
     * output:  XC-<siteId number>-<currentTime>
     *
     * @param nodeId
     * @return
     */
    public static String createXCDescription(@NonNull String nodeId) {

        return PhysicalXcIdNamingRule.createXCDescription(nodeId);
    }

    public static String createLinkId(String fromTpId, String toTpId, LinkType linkType) {

        return PhysicalLinkIdNamingRule.createLinkId(fromTpId, toTpId, linkType);

    }

    public static DateAndTime getCurrentTime() {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String str = sdf.format(new Date());
        String str1 = str.substring(0, str.length() - 2);
        String str2 = str.substring(str.length() - 2);
        StringBuilder sb = new StringBuilder();
        sb.append(str1);
        sb.append(":");
        sb.append(str2);
        return DateAndTime.getDefaultInstance(sb.toString());

    }

    public static String createXCId(List<String> tpIds, Boolean biDirection) {
        if (biDirection) {
            return tpIds.stream().sorted().collect(Collectors.joining("-", "XC-", ""));
        }
        return tpIds.stream().collect(Collectors.joining("-", "XC-", ""));
    }

    public static String createEquipId(String nodeId, EquipType equipType, String cardType, Integer slot,
            Integer shelf, Boolean isReplacedAsEmpty) {
        if (cardType.equals(CHASSIS_CARD_TYPE)) {
            return String.format("%s#%s-%d", nodeId, cardType, shelf);
        }
        if (cardType.equals(PANEL_CARD_TYPE)) {
            return String.format("%s#%s-%d-%d", nodeId, cardType, shelf, slot);
        }
        if (equipType.equals(EquipType.MUX) || equipType.equals(EquipType.MUXPANEL)) {
            return String.format("%s#%s-%d-%d", nodeId, EquipType.MUX.name(), shelf, slot);
        }
        if (isReplacedAsEmpty) {
            return String.format("%s#LINECARD-%d-%d", nodeId, shelf, slot);
        }
        return String.format("%s#%s-%d-%d", nodeId, equipType.name(), shelf, slot);

    }

    public static String createTransceiverEquipId(String nodeId, String transceiverName) {
        return PhysicalEqpIdNamingRule.createTransceiverEquipId(nodeId, transceiverName);

    }

    public static String createEquipIdForFix(String nodeId, String equipName) {
        return PhysicalEqpIdNamingRule.createEquipIdForFix(nodeId, equipName);
    }

    public static String getNodeIdByTpId(String tpId) {
        String[] tpIds = tpId.split("#");
        if (tpIds.length < 2) {
            new NeDesignerException("Invalid tpId: " + tpId);
        }
        return String.format("%s#%s", tpIds[0], tpIds[1]);
    }

    public static String getSlotFromTp(TpId tpId) {
        String id = tpId.getValue();
        return PhysicalTpIdNamingRule.getSlotFromTp(id);
    }


    public static String getSlotFromEquipId(String equipId) {
        return PhysicalEqpIdNamingRule.getSlotFromEquipId(equipId);
    }

    public static String createXCIdWithLayer(List<String> tpIdList, CrossConnection crossConnection) {
        String tpSuffix = String.format("/%s", crossConnection.getFrom().getLayer());
        tpIdList = tpIdList.stream().map(tpId -> tpId + tpSuffix).collect(Collectors.toList());
        return createXCId(tpIdList, crossConnection.getBiDirection());
    }
}

//    public static String createSupportedLinkId(String fromTpId, String toTpId,
//            SupportedSignal supportedSignal) {
//        switch (supportedSignal){
//            case OMS:
//                return createLinkId(fromTpId,toTpId)
//        }
//
//    }

