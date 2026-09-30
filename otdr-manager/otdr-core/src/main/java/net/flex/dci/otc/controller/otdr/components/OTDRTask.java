package net.flex.dci.otc.controller.otdr.components;

import net.flex.dci.otc.common.model.TaskInfoMessage;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.StartOtdrInput;

/**
 * @version 1.0
 * @date 2022/8/30 15:25
 */
public interface OTDRTask {

    String startOtdr(StartOtdrInput startOtdrInput, TaskInfoMessage taskInfoMessage);
}
