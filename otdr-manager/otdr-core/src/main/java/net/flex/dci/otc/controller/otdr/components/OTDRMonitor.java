package net.flex.dci.otc.controller.otdr.components;

import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.otdr.monitors.grouping.OtdrMonitors;

/**
 * @version 1.0
 * @date 2022/8/30 11:42
 */
public interface OTDRMonitor {

    OtdrMonitors getMonitors(String nodeId, String refCardId);

}
