package net.flex.dci.otn.controller.cli.component;

import net.flex.dci.otc.zk.common.entity.InstanceDetails;

/**
 *
 * @version 1.0
 * @date 9/18/2025 11:23 AM
 */
public interface CliSessionCacheHandler {

    void recordCache(String sessionId);

    void handleInstanceRemove(InstanceDetails detail);

    void removeCache(String sessionId);
}
