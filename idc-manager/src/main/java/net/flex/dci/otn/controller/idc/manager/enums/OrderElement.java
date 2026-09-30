package net.flex.dci.otn.controller.idc.manager.enums;

/**
 * @version 1.0
 * @date 2022/1/26 14:43
 */
public enum OrderElement {
    CITY("city"),
    COUNTRY("country"),
    REGION("region"),
    PROVINCE("province"),
    SITE("site"),
    DISTRICT("district");

    private String name;

    OrderElement(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
