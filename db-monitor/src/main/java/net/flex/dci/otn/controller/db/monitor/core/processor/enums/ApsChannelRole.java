package net.flex.dci.otn.controller.db.monitor.core.processor.enums;

/**
 *
 * @version 1.0
 * @date 8/28/2025 2:18 PM
 */
public enum ApsChannelRole {
    WORKING("primary"),
    PROTECTION("secondary"),
    TERTIARY("third");

    private final String code;

    ApsChannelRole(String code) {
        this.code = code;
    }

    public static ApsChannelRole fromCode(String code) {
        for (ApsChannelRole role : values()) {
            if (role.code.equalsIgnoreCase(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("unknown aps channel role:" + code);
    }

}
