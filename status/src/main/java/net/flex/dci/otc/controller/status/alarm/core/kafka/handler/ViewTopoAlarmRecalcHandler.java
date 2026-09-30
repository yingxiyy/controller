package net.flex.dci.otc.controller.status.alarm.core.kafka.handler;

import net.flex.dci.otc.common.model.view.ViewTopoAlarmRecalcMsg;

/**
 * 2026/2/22
 *
 * @author musa
 * @version 1.0
 **/
public interface ViewTopoAlarmRecalcHandler {

    void recalculateViewTopo(ViewTopoAlarmRecalcMsg topoAlarmRecalcMsg);
}
