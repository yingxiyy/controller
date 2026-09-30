package net.flex.dci.otc.controller.ne.manager.components.notification;

/**
 * @version 1.0
 * @date 8/14/2025 11:58 AM
 */
public interface NeManagerEventNotification {

    void notifyNeManageFailed(Exception exception);

    void notifyNeResourceManageFailed(Exception exception);

    void notifyNeDataMergeSuccess(String neId);


    void broadcastNeSyncing(String neId, String neName);

    void notifyRegisterNeSuccess(String neId);

    void notifyUnregisterNeSuccess(String neId);
}
