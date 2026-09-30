package net.flex.dci.otn.controller.nms.nms.handler.impl.resource.alarm;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.nms.enums.NMSResourceType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.LocateResourcesByAlarmOutput;
import org.springframework.stereotype.Component;

/**
 * 2025/8/4
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class AlarmResourceLocatorImpl implements AlarmResourceLocator {

    private final Map<NMSResourceType, AbstractAlarmResourceLocator> locatorMap;

    public AlarmResourceLocatorImpl(List<AbstractAlarmResourceLocator> alarmResourceLocators) {
        locatorMap = alarmResourceLocators.stream()
                .collect(Collectors.toMap(AbstractAlarmResourceLocator::locateResourceType,
                        Function.identity()));
    }

    @Override
    public LocateResourcesByAlarmOutput locateAlarmResource(String resourceId,
            NMSResourceType nmsResourceType) {
        log.debug("get resource locate resources resourceId is:{} resourceType:{}", resourceId,
                nmsResourceType);
        LocateResourcesByAlarmOutput locateResourcesByAlarmOutput = locatorMap.get(nmsResourceType)
                .getLocateByAlarmResourceId(resourceId);
        return locateResourcesByAlarmOutput;
    }
}
