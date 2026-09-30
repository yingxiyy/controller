package net.flex.dci.otc.controller.ne.manager.core.kafka.service;

/**
 * @version 1.0
 * @date 2022/3/28 15:20
 */
public interface NeSynchronized extends BaseStateChange {

    void startSynchronizingNe(String neId);

}
