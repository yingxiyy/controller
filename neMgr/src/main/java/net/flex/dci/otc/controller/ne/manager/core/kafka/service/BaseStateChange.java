package net.flex.dci.otc.controller.ne.manager.core.kafka.service;

/**
 * @version 1.0
 * @date 2022/3/28 15:22
 */
public interface BaseStateChange {

    void handleStateChange(String neId, boolean mutable) throws Exception;
}
