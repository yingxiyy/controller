package net.flex.dci.otc.controller.ne.manager.core.kafka.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.core.kafka.handler.NeChangeHandler;
import net.flex.dci.otc.controller.ne.manager.core.kafka.model.ElementChange;
import net.flex.dci.otc.controller.ne.manager.core.kafka.service.NeChangeRegisteredHandlerService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/4/27 16:21
 */
@Slf4j
@Service
public class NeChangeRegisteredHandlerServiceImpl implements NeChangeRegisteredHandlerService {

    private final List<NeChangeHandler> neChangeHandlers;


    private final Map<String, NeChangeHandler> neChangeHandlerMap;

    public NeChangeRegisteredHandlerServiceImpl(List<NeChangeHandler> neChangeHandlers) {
        this.neChangeHandlers = neChangeHandlers;
        this.neChangeHandlerMap = neChangeHandlers.stream()
                .collect(Collectors.toMap(
                        NeChangeHandler::msgType,  // key: msgType
                        handler -> handler,         // value: handler本身
                        (existing, replacement) -> { // 重复key处理
                            log.warn("Duplicate msgType {} found, using first instance",
                                    existing.msgType());
                            return existing;
                        },
                        ConcurrentHashMap::new      // 指定Map类型
                ));

        log.info("Successfully registered {} NeChangeHandler instances", neChangeHandlerMap.size());
    }

    @Override
    public List<NeChangeHandler> getNeChangeHandler() {
        List<NeChangeHandler> handlers = Collections.emptyList();
        if (!CollectionUtils.isEmpty(neChangeHandlers)) {
            handlers = this.neChangeHandlers;
        }
        return handlers;
    }

    @Override
    public void processElementChange(ElementChange elementChange) {
        try {
            log.info("current process element change is:{}", elementChange);
            String msgType = elementChange.getMsgType();
            neChangeHandlerMap.get(msgType).handleStateChange(elementChange);
        } catch (Exception ex) {
            log.error("failed to process element change:{}", ex.getMessage(), ex);
        }
    }

    @Override
    public void batchMessage(List<ElementChange> elementChanges) {
        if (CollectionUtils.isEmpty(elementChanges)) {
            return;
        }

        log.info("Batch processing {} messages", elementChanges.size());

        List<ElementChange> dedupChanges = dedupByNeIdAndMsgType(elementChanges);
        log.info("After dedup: {} messages", dedupChanges.size());

        Map<String, List<ElementChange>> grouped = dedupChanges.stream()
                .collect(Collectors.groupingBy(ElementChange::getMsgType));

        for (Map.Entry<String, List<ElementChange>> entry : grouped.entrySet()) {
            String msgType = entry.getKey();
            List<ElementChange> changes = entry.getValue();

            NeChangeHandler handler = neChangeHandlerMap.get(msgType);
            if (handler == null) {
                log.warn("No handler found for msgType: {}", msgType);
                continue;
            }

            try {
                log.info("Processing {} messages for msgType: {}", changes.size(), msgType);

                try {
                    handler.handleStateChanges(changes);
                } catch (Exception e) {
                    log.error("Failed to handle process nestate message for : {}", changes.size(),
                            e);
                }

            } catch (Exception e) {
                log.error("Failed to process batch for msgType: {}", msgType, e);
            }
        }
    }


    private List<ElementChange> dedupByNeIdAndMsgType(List<ElementChange> changes) {
        Map<String, ElementChange> latestMap = new java.util.LinkedHashMap<>();

        for (ElementChange change : changes) {
            if (change.getNeState() == null || change.getMsgType() == null) {
                continue;
            }

            String neId = change.getNeState().getNodeId();
            String msgType = change.getMsgType();
            String key = neId + ":" + msgType; // 复合 key

            latestMap.put(key, change);
        }

        return new java.util.ArrayList<>(latestMap.values());
    }
}
