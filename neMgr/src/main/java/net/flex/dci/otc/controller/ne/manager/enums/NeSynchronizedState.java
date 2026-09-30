package net.flex.dci.otc.controller.ne.manager.enums;

import lombok.extern.slf4j.Slf4j;

/**
 * 2026/4/19
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public enum NeSynchronizedState {
    SYNCING,
    NOT_SYNCED,
    SYNC_SUCCESS,
    SYNC_FAILED;


    public static NeSynchronizedState safeValueOf(String state) {
        if (state == null) {
            log.debug("Null sync state, defaulting to NOT_SYNCED");
            return NOT_SYNCED;
        }

        String trimmedState = state.trim();
        if (trimmedState.isEmpty()) {
            log.debug("Blank sync state, defaulting to NOT_SYNCED");
            return NOT_SYNCED;
        }

        if ("null".equalsIgnoreCase(trimmedState)) {
            log.debug("Literal 'null' sync state, defaulting to NOT_SYNCED");
            return NOT_SYNCED;
        }

        try {
            return NeSynchronizedState.valueOf(state.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid sync state: '{}', defaulting to NOT_SYNCED", state);
            return NOT_SYNCED;
        }
    }
}
