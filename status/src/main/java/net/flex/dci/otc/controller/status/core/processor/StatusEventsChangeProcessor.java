package net.flex.dci.otc.controller.status.core.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.StatusChangeObjectType;
import net.flex.dci.otc.common.model.ne.StatusChangeEvent;
import net.flex.dci.otc.controller.status.core.IStateProcessor;
import net.flex.dci.otc.controller.status.core.handler.StateChangeChainHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @version 1.0
 * @date 8/26/2025 2:04 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class StatusEventsChangeProcessor implements IStateProcessor {

    private final StateChangeChainHandler stateChangeChainHandler;

    @Override
    public void processStatusEvent(StatusChangeEvent statusChangeEvent) {
        log.info("process status event change ,event is:{}", statusChangeEvent);
        StatusChangeObjectType objectType = statusChangeEvent.getStatusChangeObjectType();
        String objectId = statusChangeEvent.getObjectId();
        log.debug("status event change object type is :{} and objectId:{}", objectType, objectId);
        if (!StringUtils.hasText(statusChangeEvent.getAdminStatus())
                && !StringUtils.hasText(statusChangeEvent.getOperStatus())) {
            log.warn("meaningless status events,do nothing");
            return;
        }
        stateChangeChainHandler.processStatusEventChange(statusChangeEvent);
    }
}
