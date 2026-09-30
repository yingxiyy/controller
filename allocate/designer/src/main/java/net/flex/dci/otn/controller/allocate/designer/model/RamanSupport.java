/*
 * Copyright (c) 2019 Network Flex Any Comp. and others. All rights reserved.
 */

package net.flex.dci.otn.controller.allocate.designer.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.ne.ExternalLinkTo;

public final class RamanSupport {

    public static final String CARD_VENDOR_TYPE = "RAMANCL_16";
    public static final int SINGLE_SLOT = 5;
    public static final int DUAL_EAST_SLOT = 6;
    public static final String WEST_PORT = "LINE_WEST";
    public static final String EAST_PORT = "LINE_EAST";

    private RamanSupport() {
    }

    public static void validatePaired(List<String> sourceCardVendors, List<String> destinationCardVendors,
                                      String source, String destination) throws NeDesignerException {
        if (containsRaman(sourceCardVendors) != containsRaman(destinationCardVendors)) {
            throw new NeDesignerException(String.format(
                    "RAMAN must be configured at both ends of segment %s -> %s.", source, destination));
        }
    }

    static boolean containsRaman(List<String> cardVendors) {
        if (cardVendors == null) {
            return false;
        }
        return cardVendors.stream().anyMatch(cardVendor -> {
            String vendorType = cardVendor.contains(NeInfo.CARD_TYPE_VENDOR_SEPERATOR)
                    ? cardVendor.substring(0, cardVendor.indexOf(NeInfo.CARD_TYPE_VENDOR_SEPERATOR))
                    : cardVendor;
            return CARD_VENDOR_TYPE.equals(vendorType);
        });
    }

    public static boolean containsRaman(Set<String> cardVendors) {
        return cardVendors != null && containsRaman(new java.util.ArrayList<>(cardVendors));
    }

    public static int getSlot(boolean ramanOnLeft, boolean ramanOnRight, boolean leftSide) {
        if (ramanOnLeft && ramanOnRight && !leftSide) {
            return DUAL_EAST_SLOT;
        }
        return SINGLE_SLOT;
    }

    public static boolean isDualDirectionAmplifier(String cardType) {
        return "ILA_CL".equals(cardType) || "DGE_CL".equals(cardType);
    }

    /** Select the actual station chain, rather than all cabling capabilities in the model. */
    public static Map<String, List<ExternalLinkTo>> selectStationLinks(
            Map<String, List<ExternalLinkTo>> relations, String sourceType, String destinationType)
            throws NeDesignerException {
        if ("RAMAN_CL".equals(sourceType)) {
            return selectPorts(relations, "SIG", destinationType,
                    isDualDirectionAmplifier(destinationType) ? WEST_PORT : "LINE");
        }
        if ("RAMAN_CL".equals(destinationType)) {
            return selectPorts(relations, isDualDirectionAmplifier(sourceType) ? EAST_PORT : "LINE",
                    destinationType, "SIG");
        }
        return relations;
    }

    public static Map<String, List<ExternalLinkTo>> selectSpanLinks(
            Map<String, List<ExternalLinkTo>> relations, String sourceType, String destinationType)
            throws NeDesignerException {
        if ("RAMAN_CL".equals(sourceType) && "RAMAN_CL".equals(destinationType)) {
            return selectPorts(relations, "LINE", destinationType, "LINE");
        }
        return relations;
    }

    private static Map<String, List<ExternalLinkTo>> selectPorts(
            Map<String, List<ExternalLinkTo>> relations, String sourcePort,
            String destinationType, String destinationPort) throws NeDesignerException {
        List<ExternalLinkTo> candidates = relations == null ? null : relations.get(sourcePort);
        List<ExternalLinkTo> selected = candidates == null ? Collections.emptyList()
                : candidates.stream().filter(to -> destinationType.equals(to.getCardType())
                        && destinationPort.equals(to.getPort())).collect(Collectors.toList());
        if (selected.isEmpty()) {
            throw new NeDesignerException("Missing Raman link relation: " + sourcePort + " -> "
                    + destinationType + ":" + destinationPort);
        }
        Map<String, List<ExternalLinkTo>> result = new LinkedHashMap<>();
        result.put(sourcePort, selected);
        return result;
    }
}
