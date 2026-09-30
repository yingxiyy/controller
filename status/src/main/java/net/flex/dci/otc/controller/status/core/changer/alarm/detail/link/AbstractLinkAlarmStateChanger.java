package net.flex.dci.otc.controller.status.core.changer.alarm.detail.link;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.changer.IStateChanger;
import net.flex.dci.otc.controller.status.core.evaluate.ViewLinkAlarmStateEvaluate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version 1.0
 * @date 8/22/2023 2:30 PM
 */

@Slf4j
public abstract class AbstractLinkAlarmStateChanger<T> implements
        IStateChanger<T> {

    @Autowired
    protected ViewLinkAlarmStateEvaluate viewLinkAlarmStateEvaluate;

}
