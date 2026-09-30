package net.flex.dci.otc.controller.ne.manager.core.kafka.handler;

import java.util.List;
import net.flex.dci.otc.controller.ne.manager.core.kafka.model.ElementChange;

/**
 * @version 1.0
 * @date 2022/4/27 16:10
 */
public interface NeChangeHandler {

    void handleStateChange(ElementChange elementChange);

    String msgType();

    void handleStateChanges(List<ElementChange> changes);
}
