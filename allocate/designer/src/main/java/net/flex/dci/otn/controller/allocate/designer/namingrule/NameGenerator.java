/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.namingrule;

import static net.flex.dci.otn.controller.allocate.designer.model.NeInfo.EMPTY_CARD_TYPE;
import static net.flex.dci.otn.controller.allocate.designer.ne.EquipmentRepo.CHASSIS_CARD_TYPE;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;

@Slf4j
public class NameGenerator {

    public static final String EMPTY_CARD_FRIENDLY_NAME = "SLOT";

    public static String createTpFriendlyName(Equipments equipment, String portName) {
        return PhysicalTpIdNamingRule.createTpFriendlyName(equipment, portName);
    }

    public static String createEquipFriendlyName(String cardType, Integer shelf, Integer slot) {
        switch (cardType) {
            case CHASSIS_CARD_TYPE:
                return PhysicalEqpIdNamingRule.createChassisFriendlyName(cardType, shelf);
            case EMPTY_CARD_TYPE:
                return PhysicalEqpIdNamingRule.createEquipFriendlyName(EMPTY_CARD_FRIENDLY_NAME, shelf, slot);

            default:
                return PhysicalEqpIdNamingRule.createEquipFriendlyName(cardType, shelf, slot);
        }
    }

    /**
     * @param transceiverName e.g. "TRANSCEIVER?OSC"
     * @param tpFriendlyName e.g. "OA-1-1-SIG"
     * @return e.g "TRANSCEIVER-1-1-SIGOSC"
     */
    public static String getTransceiverNameByTpName(String transceiverName, String tpFriendlyName)
            throws NeDesignerException {
        String tpSuffix = getTpSuffixByTpName(tpFriendlyName);

        return transceiverName.replace("?", tpSuffix);
    }

    /**
     * @param nameTemplate e.g. "TRANSCEIVER?OSC"
     * @param portName e.g LINE
     * @param slot e.g. 2
     * @param shelf e.g. 1
     * @return "TRANSCEIVER-1-2-LINEOSC"
     */
    public static String getTransceiverName(String nameTemplate, String portName, Integer slot, Integer shelf) {
        if (nameTemplate.contains("?")) {
            return nameTemplate.replace("?", String.format("-%d-%d-%s", shelf, slot, portName));
        }
        return String.format("TRANSCEIVER-%d-%d-%s", shelf, slot, nameTemplate);
    }

    public static String getCardTypeByTpName(String tpFriendlyName)
            throws NeDesignerException {

        String tpSuffix = getTpSuffixByTpName(tpFriendlyName);

        return tpFriendlyName.replace(tpSuffix, "");
    }


    public static String getTpSuffixByTpName(String tpFriendlyName)
            throws NeDesignerException {
        String[] tpFriendlyNameArray = tpFriendlyName.split("-");
        int arrayLength = tpFriendlyNameArray.length;
        if (arrayLength < 4) {
            throw new NeDesignerException(
                    "Failed to get transceiverName by invalid tpFriendlyName: " + tpFriendlyName);
        }

        String tpSuffix = new StringBuilder("-").append(tpFriendlyNameArray[arrayLength - 3])
                .append("-").append(tpFriendlyNameArray[arrayLength - 2])
                .append("-").append(tpFriendlyNameArray[arrayLength - 1]).toString();

        return tpSuffix;
    }

    public static Properties getFakeProperty() {
      /*  return new PropertiesBuilder().setProperty(
                Arrays.asList(
                        new PropertyBuilder().setName("ErrorMsg")
                                .setValue("").build()))
                .build();*/

        return null;
    }


    public static String createNodeFriendlyName(String nodeId) {
        return PhysicalNodeIdNamingRule.createNodeFriendlyName(nodeId);
    }

    public static String createLinkFriendlyName(LinkType linkType) {
        return PhysicalLinkIdNamingRule.getNewFriendlyName(linkType);
    }


    public static String getPortNameByTpId(String tpId) {
        return PhysicalTpIdNamingRule.getPortNameByTpId(tpId);
    }
}
