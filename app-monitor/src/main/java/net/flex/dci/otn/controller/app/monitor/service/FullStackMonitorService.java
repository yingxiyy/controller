package net.flex.dci.otn.controller.app.monitor.service;

import net.flex.dci.otc.zk.common.entity.InstanceDetails;

/**
 *
 * 2025/11/28
 *
 * @author musa
 * @version 1.0
 **/
public interface FullStackMonitorService {

    void checkSystemMetric(InstanceDetails updateInstanceDetail);
}
