package net.flex.dci.otn.controller.apsswitchlog.enums;

import lombok.Getter;

/**
 * @version 1.0
 * @date 2022/5/5 10:43
 */
@Getter
public enum OrderElement {

    /*
    id("id"),
    resourceName("resourceName"),
    resourceType("resourceType"),
    endTime("endTime"),
    actionType("actionType"),
    who("who"),
    actionTime("actionTime");
    */
    id("id"),

    neId("neId"),

    logId("logId"),

    activePath("activePath"),

    activeIndex("activeIndex"),

    apsModuleName("apsModuleName"),


    apsMode("aps_mode"), // 需补充实际枚举类

    triggerType("triggerType"),


    startTime("start_time"), // 纳秒级时间戳

    endTime("end_time"),

    duration("duration"),

    text("log_text"),

    createTimestamp("createTimestamp"),
    ;
    private final String elementName;

    //
    OrderElement(String elementName) {
        this.elementName = elementName;
    }

    public static OrderElement fromElementName(String elementName) {
        for (OrderElement orderElement : OrderElement.values()) {
            if (orderElement.elementName.equals(elementName)) {
                return orderElement;
            }
        }
        throw new IllegalArgumentException(
                String.format("Unsupported order element %s", elementName));
    }
}
