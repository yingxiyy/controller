package net.flex.dci.otn.controller.app.monitor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otn.controller.app.monitor.alertService.SystemMetricAlertService;
import net.flex.dci.otn.controller.app.monitor.service.FullStackMonitorService;
import org.springframework.stereotype.Component;

/**
 *
 * 2025/11/28
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class FullStackMonitorServiceImpl implements FullStackMonitorService {

    private final SystemMetricAlertService systemMetricAlertService;

    @Override
    public void checkSystemMetric(InstanceDetails instanceDetails) {
        log.debug("check system metric and generate system metric alarm alert");
        systemMetricAlertService.checkSystemMetricAlert(instanceDetails);
    }
}
