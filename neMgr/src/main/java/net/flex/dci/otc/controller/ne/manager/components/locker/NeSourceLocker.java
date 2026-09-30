package net.flex.dci.otc.controller.ne.manager.components.locker;

import net.flex.dci.otc.zkclient4boot.lock.DciDistributeLock;

/**
 * 2025/7/27
 *
 * @author musa
 * @version 1.0
 **/
public interface NeSourceLocker {

    DciDistributeLock getResourceLock(String neId);

}
