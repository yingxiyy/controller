package net.flex.dci.otc.controller.otdr.manager;

import net.flex.dci.otc.common.model.TaskInfoMessage;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.otdr.monitors.grouping.OtdrMonitors;

/**
 * @version 1.0
 * @date 2022/8/30 11:33
 */
public interface OtdrManager {

    OtdrMonitors getOtdrMonitors(String nodeId, String refCardId);

    StartOtdrOutput startOtdr(StartOtdrInput startOtdrInput, TaskInfoMessage taskInfoMessage);


}
