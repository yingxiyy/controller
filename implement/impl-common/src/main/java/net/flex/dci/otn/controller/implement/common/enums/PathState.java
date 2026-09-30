package net.flex.dci.otn.controller.implement.common.enums;

import lombok.Getter;

/**
 *
 * 2025/9/15
 *
 * @author musa
 * @version 1.0
 **/
@Getter
public enum PathState {
    LOP("Lockout of Protection at Path. It means the current path is lockout from Protection. Not allowed to select this path by any command and automatic switch"),
    FS("Force switch to this path"),
    MS("Manual switch to this path"),
    SF("Signal Failure at this path"),
    SD("Signal degradation at this path"),
    WTC("This path is waiting to check state. This state comes from SF/SD by digital diagnosis. After service switching to Another path, the current path state, will downgrade to this level. Operator could confirm the signal status of this path by other probe and manually clear this state to normal"),
    WTR("This path is in waiting to restore state. In revertive operation, after the clearing of an SF or SD on a primary path, maintains a normal traffic signal as selected from the alternate path until a Wait-to-Restore timer expires. If the timer expires prior to any other event or command, the state will be changed to no request (NR)"),
    NR("No request. Signal is normal at this path");

    private final String description;

    PathState(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
