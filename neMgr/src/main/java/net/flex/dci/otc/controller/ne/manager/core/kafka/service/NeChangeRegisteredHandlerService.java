package net.flex.dci.otc.controller.ne.manager.core.kafka.service;

import java.util.List;
import net.flex.dci.otc.controller.ne.manager.core.kafka.handler.NeChangeHandler;
import net.flex.dci.otc.controller.ne.manager.core.kafka.model.ElementChange;

/**
 * @version 1.0
 * @date 2022/4/27 16:18
 */
public interface NeChangeRegisteredHandlerService {

    List<NeChangeHandler> getNeChangeHandler();

    void processElementChange(ElementChange elementChange);

    void batchMessage(List<ElementChange> elementChanges);
}
