package net.flex.dci.otn.controller.taskinfo.enums;

/**
 * @version 1.0
 * @date 2022/5/5 10:43
 */
public enum OrderElement {

    id("id"),
    resourceName("resourceName"),
    resourceType("resourceType"),
    endTime("endTime"),
    actionType("actionType"),
    who("who"),
    actionTime("actionTime");

    private String elementName;

    OrderElement(String elementName) {
        this.elementName = elementName;
    }
}
