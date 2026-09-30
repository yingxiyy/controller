package net.flex.dci.otc.controller.status.core;

import java.util.List;
import net.flex.dci.otc.common.model.alarm.CtrlAlarm;
import net.flex.dci.otc.common.model.ne.StatusChangeEvent;

/**
 * @version 1.0
 * @date 2022/4/2 16:22
 */
public interface IStateProcessor {

    default void process(String neId) {

    }

    default void processBatch(List<String> neIds) {

    }

    default void process(CtrlAlarm ctrlAlarm) {

    }

    default void processRemove(String neId) {

    }

    default void processStatusEvent(StatusChangeEvent statusChangeEvent) {

    }

    default void processRemoveBatch(List<String> removeNeIds) {

    }
}
