package net.flex.dci.otn.controller.apsswitchlog.component;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.topology.cache.manager.TopologyCacheManager;

/**
 *
 * @version 1.0
 * @date 11/17/2025 11:32 AM
 */
@Slf4j
public abstract class AbstractResourceResolver implements ResourceResolver {

    protected final TopologyCacheManager topologyCacheManager;

    protected AbstractResourceResolver(TopologyCacheManager topologyCacheManager) {
        this.topologyCacheManager = topologyCacheManager;
    }
}
