package net.flex.dci.otn.controller.apsswitchlog.component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.ProtectType;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otn.controller.apsswitchlog.enums.ResourceType;
import net.flex.dci.otn.controller.utils.Constants;
import net.flex.dci.otn.topology.cache.manager.TopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.SiteLinkCache;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 11/17/2025 11:12 AM
 */
@Component
@Slf4j
public class SiteLinkResourceResolver extends AbstractResourceResolver {


    protected SiteLinkResourceResolver(
            TopologyCacheManager topologyCacheManager) {
        super(topologyCacheManager);
    }

    @Override
    public List<String> resolve(String resourceId) {
        log.debug("start to resolve current site link:{}", resourceId);
        SiteLinkCache siteLinkCache = topologyCacheManager.getValue(resourceId);
        if (siteLinkCache.getProtectionType().equals(ProtectType.UNPROTECTED)) {
            return Collections.singletonList(Constants.EMPTY_TAG);
        }
        String sourceXcId = siteLinkCache.getSourceApsXCId();
        String destXcId = siteLinkCache.getDestinationApsXCId();
        String sourceXcRefNeId = PhysicalXcIdNamingRule.getNodeId(sourceXcId);
        String destXcRefNeId = PhysicalXcIdNamingRule.getNodeId(destXcId);
        return Arrays.asList(sourceXcRefNeId, destXcRefNeId);

    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.siteLink;
    }
}
