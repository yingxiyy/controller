package net.flex.dci.otn.controller.apsswitchlog.component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.apsswitchlog.enums.ResourceType;
import org.springframework.stereotype.Component;

/**
 *
 * @version 1.0
 * @date 11/17/2025 11:13 AM
 */
@Component
@Slf4j
public class ResourceLookupResolver {

    private Map<ResourceType, ResourceResolver> resolverMap;

    public ResourceLookupResolver(List<ResourceResolver> resolverList) {
        resolverMap = resolverList.stream().collect(Collectors.toMap(ResourceResolver::resourceType,
                Function.identity()));
    }

    public List<String> resolveResource(ResourceType resourceType, String resourceId) {
        log.debug("resolve resource type:{} resourceId:{}", resourceType, resourceId);
        return resolverMap.get(resourceType).resolve(resourceId);
    }
}
