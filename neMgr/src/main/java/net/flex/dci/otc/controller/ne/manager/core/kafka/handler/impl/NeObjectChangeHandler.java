package net.flex.dci.otc.controller.ne.manager.core.kafka.handler.impl;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.CARD;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.CROSS_CONNECTION;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.LINK;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.OBJ_CHANGE;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.controller.ne.manager.core.kafka.handler.NeChangeHandler;
import net.flex.dci.otc.controller.ne.manager.core.kafka.model.ElementChange;
import net.flex.dci.otc.controller.ne.manager.core.kafka.model.ObjectChange;
import net.flex.dci.otc.controller.ne.manager.core.kafka.service.impl.NeSynchronizeService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 2022/4/27 16:33
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NeObjectChangeHandler implements NeChangeHandler {

    private final NeSynchronizeService neSynchronizedService;

    @Override
    public void handleStateChange(ElementChange elementChange) {
        if (elementChange.getObjectChange() == null) {
            return;
        }
        log.info("start to handle the state change for the element");
        ObjectChange objectChange = elementChange.getObjectChange();
        log.info("ne change for object change the type is :{}", objectChange.getType());
        log.info("ne change for object change the action is :{}", objectChange.getAction());
        log.info("ne change for object change the objectId is :{}", objectChange.getObjectId());
        String objectId = objectChange.getObjectId();
        String type = objectChange.getType();
        String neId = getNeIdByElement(objectId, type);
        if (neId != null) {
            neSynchronizedService.handleStateChange(neId, true);
        }
    }

    @Override
    public String msgType() {
        return OBJ_CHANGE;
    }

    @Override
    public void handleStateChanges(List<ElementChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return;
        }
        Map<String, ElementChange> latestMap = new LinkedHashMap<>();
        for (ElementChange change : changes) {
            ObjectChange objectChange = change.getObjectChange();
            if (objectChange == null || !StringUtils.hasText(objectChange.getObjectId())) {
                continue;
            }
            String objectId = objectChange.getObjectId();
            String type = objectChange.getType();
            String neId = getNeIdByElement(objectId, type);
            latestMap.put(neId, change);
        }

        log.info("NeObjectChangeHandler batch: {} -> {} after dedup",
                changes.size(), latestMap.size());
        for (ElementChange change : latestMap.values()) {
            try {
                handleStateChange(change);
            } catch (Exception e) {
                log.error("Failed to process object change for ne:{}",
                        change.getNeState().getNodeId(), e);
            }
        }
    }

    private String getNeIdByElement(String objectId, String type) {
        String neId = objectId;
        if (type.equals(CROSS_CONNECTION)) {
            neId = PhysicalXcIdNamingRule.getNodeId(objectId);
        } else if (type.equals(LINK)) {
            neId = PhysicalLinkIdNamingRule.getNodeAId(objectId); //inner link src and dest same
        } else if (type.equals(CARD)) {
            neId = PhysicalEqpIdNamingRule.getNodeId(objectId);
        }
        return neId;
    }
}
