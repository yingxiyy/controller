package net.flex.dci.otn.controller.resource.statistic.enums;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * 2026/7/17
 *
 * @author musa
 * @version 1.0
 **/
@Getter
@Slf4j
public enum TunnelProtectionType {

    UNPROTECTED("ProtectionUnprotected", "unprotected"),

    BIDIR_1_TO_1("ProtectionBidir1To1", "1:2"),


    BIDIR_1_TO_2("ProtectionBidir1To2", "1:3");


    private String rawValue;

    private String displayName;

    TunnelProtectionType(String rawValue, String displayName) {
        this.rawValue = rawValue;
        this.displayName = displayName;
    }

    public static TunnelProtectionType fromRawValue(String rawValue) {
        for (TunnelProtectionType protectionType : TunnelProtectionType.values()) {
            if (protectionType.getRawValue().equals(rawValue)) {
                return protectionType;
            }
        }
        return TunnelProtectionType.UNPROTECTED;
    }

    public static String getDisplayLabel(String protectionSimpleName, String legRequired) {
        log.debug("the protection simple name:{} legRequired:{}", protectionSimpleName,
                legRequired);
        TunnelProtectionType tunnelProtectionType = fromRawValue(protectionSimpleName);
        String baseDisplay = tunnelProtectionType.getDisplayName();
        if ("true".equalsIgnoreCase(legRequired)) {
            switch (tunnelProtectionType) {
                case BIDIR_1_TO_2:
                    baseDisplay += "(2)";
                    break;
                case BIDIR_1_TO_1:
                    baseDisplay += "(1)";
                    break;
                case UNPROTECTED:
                default:
                    break;
            }
        }
        return baseDisplay;
    }
}
