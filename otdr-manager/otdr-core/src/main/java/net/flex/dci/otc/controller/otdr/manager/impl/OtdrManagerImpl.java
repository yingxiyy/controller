package net.flex.dci.otc.controller.otdr.manager.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.controller.otdr.components.OTDRMonitor;
import net.flex.dci.otc.controller.otdr.components.OTDRTask;
import net.flex.dci.otc.controller.otdr.manager.OtdrManager;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.otdr.monitors.grouping.OtdrMonitors;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/8/30 11:34
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OtdrManagerImpl implements OtdrManager {

    private final OTDRMonitor otdrMonitor;

    private final OTDRTask otdrTask;

    @Override
    public OtdrMonitors getOtdrMonitors(String nodeId, String refCardId) {
        return otdrMonitor.getMonitors(nodeId, refCardId);
    }

    @Override
    public StartOtdrOutput startOtdr(StartOtdrInput startOtdrInput,
            TaskInfoMessage taskInfoMessage) {
        String resultId = otdrTask.startOtdr(startOtdrInput, taskInfoMessage);
        log.info("start otdr ne result id is:{}", resultId);
        return new StartOtdrOutputBuilder().setResultId(resultId).build();
    }
}
