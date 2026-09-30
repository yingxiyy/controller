package net.flex.dci.otc.controller.ne.manager.core.kafka.handler.impl;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.NeConnStatus.CONNECTED_STATE;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.NeConnStatus.CONN_STATUS;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.NeConnStatus.DISCONNECT_STATE;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.NeConnStatus.SYNCHRONIZE_STATE;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.core.kafka.handler.NeChangeHandler;
import net.flex.dci.otc.controller.ne.manager.core.kafka.model.ElementChange;
import net.flex.dci.otc.controller.ne.manager.core.kafka.model.NeState;
import net.flex.dci.otc.controller.ne.manager.core.kafka.service.impl.NeOutOfControlService;
import net.flex.dci.otc.controller.ne.manager.core.kafka.service.impl.NeSynchronizeService;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 2022/3/28 15:26
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NeStateChangeHandler implements NeChangeHandler {

    private final NeOutOfControlService neOutOfControlService;

    private final NeSynchronizeService neSynchronizedService;

    @Override
    public void handleStateChange(ElementChange elementChange) {
        NeState state = elementChange.getNeState();
        log.info("handle the ");
        if (state == null) {
            log.warn("there no state change do nothing");
            return;
        }
        log.info("start to handle the ne state change ,state is :{}", state);
        String neId = state.getNodeId();
        String operationState = state.getOperationalState();
        if (!StringUtils.hasText(operationState)) {
            log.warn("nonsense message,discard it and do nothing");
            return;
        }
        if (operationState.equals(DISCONNECT_STATE)) {
            neOutOfControlService.handleStateChange(neId, false);
        } else if (operationState.equals(SYNCHRONIZE_STATE)) {
            neSynchronizedService.handleStateChange(neId, false);
        } else if (operationState.equals(CONNECTED_STATE)) {
            neSynchronizedService.startSynchronizingNe(neId);
        }

    }

    @Override
    public String msgType() {
        return CONN_STATUS;
    }

    @Override
    public void handleStateChanges(List<ElementChange> elementChanges) {
        if (elementChanges == null || elementChanges.isEmpty()) {
            return;
        }

        Map<String, ElementChange> latestMap = new LinkedHashMap<>();
        for (ElementChange change : elementChanges) {
            NeState state = change.getNeState();
            if (state == null || !StringUtils.hasText(state.getNodeId())) {
                continue;
            }
            latestMap.put(state.getNodeId(), change);
        }

        log.info("NeStateChangeHandler batch: {} -> {} after dedup",
                elementChanges.size(), latestMap.size());
        for (ElementChange change : latestMap.values()) {
            try {
                handleStateChange(change);
            } catch (Exception e) {
                log.error("Failed to process state change for ne:{}",
                        change.getNeState().getNodeId(), e);
            }
        }
    }


}
