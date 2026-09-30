package net.flex.dci.otn.controller.nms.enums;

import lombok.Getter;

/**
 * @version 1.0
 * @date 2023/1/31 16:57
 */
@Getter
public enum AdditionalProperty {

    SOURCE_SITE_NAME("source-site-name"),
    DEST_SITE_NAME("dest-site-name"),
    SOURCE_NODE_NAME("source-node-name"),
    DEST_NODE_NAME("dest-node-name"),
    SOURCE_TP_NAME("source-tp-name"),
    DEST_TP_NAME("dest-tp-name"),
    ;

    private String name;

    AdditionalProperty(String name) {
        this.name = name;
    }

    public static AdditionalProperty getAdditionalProperty(String value) {
        for (AdditionalProperty additionalProperty : AdditionalProperty.values()) {
            if (additionalProperty.getName().equals(value)) {
                return additionalProperty;
            }
        }
        return null;
    }

    public void setName(String name) {
        this.name = name;
    }
}
