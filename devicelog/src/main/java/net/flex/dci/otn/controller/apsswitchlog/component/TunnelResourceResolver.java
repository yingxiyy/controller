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
import net.flex.dci.otn.topology.cache.model.TunnelCache;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 11/17/2025 11:12 AM
 */
@Component
@Slf4j
public class TunnelResourceResolver extends AbstractResourceResolver {


    protected TunnelResourceResolver(
            TopologyCacheManager topologyCacheManager) {
        super(topologyCacheManager);
    }

    @Override
    public List<String> resolve(String resourceId) {
        log.debug("get tunnel resource for aps,the tunnel Id:{}", resourceId);
        TunnelCache tunnelCache = topologyCacheManager.getValue(resourceId);
        if (tunnelCache.getProtectionType().equals(ProtectType.UNPROTECTED)) {
            return Collections.singletonList(Constants.EMPTY_TAG);
        }
        String sourceApsXCId = tunnelCache.getSourceApsXCId();
        String destApsXcId = tunnelCache.getDestinationApsXCId();
        String sourceApsXcIdRefNeId = PhysicalXcIdNamingRule.getNodeId(sourceApsXCId);
        String destApsXcIdRefNeId = PhysicalXcIdNamingRule.getNodeId(destApsXcId);
        return Arrays.asList(sourceApsXcIdRefNeId, destApsXcIdRefNeId);
    }

    @Override
    public ResourceType resourceType() {
        return ResourceType.tunnel;
    }
}
