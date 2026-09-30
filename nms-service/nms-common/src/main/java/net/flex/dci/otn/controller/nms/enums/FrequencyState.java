package net.flex.dci.otn.controller.nms.enums;

/**
 * @version 1.0
 * @date 2022/6/24 13:11
 */
public enum FrequencyState {
    BUSY("busy"),
    Free("free");

    private String state;

    FrequencyState(String state) {
        this.state = state;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}
