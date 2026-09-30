package net.flex.dci.otc.controller.ne.manager.monitor.handler;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.enums.MapperType;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 6/9/2025 4:41 PM
 */
@Component
@Slf4j
public class MapperEventHandlerImpl implements MapperEventHandler {

    private final Map<MapperType, AbstractMapperChangeEventHandler> eventHandlerMap;

    public MapperEventHandlerImpl(List<AbstractMapperChangeEventHandler> eventHandlerList) {
        eventHandlerMap = eventHandlerList.stream().collect(
                Collectors.toMap(AbstractMapperChangeEventHandler::mapperType,
                        eventHandler -> eventHandler));
    }

    @Override
    public void handleMapperAdd(MapperType mapperType, InstanceDetails instance) {
        log.debug("handle mapperType:{} mapper add,instance is:{}", mapperType, instance);
        eventHandlerMap.getOrDefault(mapperType, new DefaultMapperChangeEventHandler())
                .handleNodeAdded(instance);
    }

    @Override
    public void handleMapperRemove(MapperType mapperType, InstanceDetails instance) {
        log.debug("handle mapperType:{} mapper remove ,instance is:{}", mapperType, instance);
        eventHandlerMap.getOrDefault(mapperType, new DefaultMapperChangeEventHandler())
                .handleNodeRemove(instance);
    }
}
