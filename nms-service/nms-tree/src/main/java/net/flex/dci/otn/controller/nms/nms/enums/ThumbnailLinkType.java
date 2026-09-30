package net.flex.dci.otn.controller.nms.nms.enums;

/**
 * @version 1.0
 * @date 2022/12/7 17:03
 */
public enum ThumbnailLinkType {

    NORMAL(2),
    OMSP(4),
    OTSP(6),
    OTS(-1);


    private Integer typeValue;

    ThumbnailLinkType(Integer typeValue) {
        this.typeValue = typeValue;
    }

    public static ThumbnailLinkType getThumbnailLinkType(Integer typeValue) {
        if (typeValue != null) {
            for (ThumbnailLinkType linkType : ThumbnailLinkType.values()) {
                if (linkType.getTypeValue().equals(typeValue)) {
                    return linkType;
                }
            }
        }
        return null;
    }


    public Integer getTypeValue() {
        return typeValue;
    }

    public void setTypeValue(Integer typeValue) {
        this.typeValue = typeValue;
    }
}
