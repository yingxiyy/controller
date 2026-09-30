package net.flex.dci.otc.controller.ne.manager.components.communicateState;

/**
 * 2026/8/10
 *
 * @author musa
 * @version 1.0
 **/
public interface CommunicateStateUpdater {

    void syncing(String neId);

    void synced(String neId);

    void syncFailed(String neId, Throwable t);

    void syncFailed(String neId);

    void notManaged(String neId);

    void broken(String nodeId);

    void loginFail(String neId);
}
