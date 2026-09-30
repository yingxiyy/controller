package net.flex.dci.otn.controller.nms;

import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.cache.manager.AbstractCacheManager;
import net.flex.dci.otn.controller.nms.cache.manager.OchLinkCacheManager;
import net.flex.dci.otn.controller.nms.cache.manager.PhyLinkCacheManager;
import net.flex.dci.otn.controller.nms.cache.manager.PhyNodeCacheManager;
import net.flex.dci.otn.controller.nms.cache.manager.SiteCacheManager;
import net.flex.dci.otn.controller.nms.cache.manager.SiteLinkCacheManager;
import net.flex.dci.otn.controller.nms.cache.manager.TerminationPointCacheManager;
import net.flex.dci.otn.controller.nms.cache.manager.TunnelCacheManager;
import net.flex.dci.otn.controller.nms.cache.model.BaseCache;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/22 13:14
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DciCacheManager extends AbstractCacheManager<BaseCache> {

    private final OchLinkCacheManager ochLinkCacheManager;

    private final PhyLinkCacheManager phyLinkCacheManager;

    private final SiteCacheManager siteCacheManager;

    private final SiteLinkCacheManager siteLinkCacheManager;

    private final TunnelCacheManager tunnelCacheManager;

    private final PhyNodeCacheManager phyNodeCacheManager;

    private final TerminationPointCacheManager terminationPointCacheManager;

    @Override
    public BaseCache getValue(String id) throws ExecutionException, InterruptedException {
        return super.getValue(id);
    }
}
