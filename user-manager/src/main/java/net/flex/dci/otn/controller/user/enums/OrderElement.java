package net.flex.dci.otn.controller.user.enums;

/**
 * @version 1.0
 * @date 2022/4/19 15:18
 */
public enum OrderElement {
    id("id"),
    createTime("create-time"),
    username("username"),
    role("role");


    private String elementName;

    OrderElement(String elementName) {
        this.elementName = elementName;
    }

    public String getElementName() {
        return elementName;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }
}
