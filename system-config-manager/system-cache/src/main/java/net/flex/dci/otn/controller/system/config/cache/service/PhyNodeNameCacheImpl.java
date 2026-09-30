package net.flex.dci.otn.controller.system.config.cache.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.system.config.cache.model.PhyNodeRefName;
import net.flex.dci.otn.controller.system.config.cache.util.EhcacheUtil;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.BasicCache;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/14 18:23
 */
@Component
@Slf4j
@AllArgsConstructor
public class PhyNodeNameCacheImpl implements PhyNodeNameCache {

    private static final String CACHE_NAME = "phyNodeCache";

    private final DciTopologyCacheManager cacheManager;

    @Override
    public String getFriendlyName(String id) {
        PhyNodeRefName nodeRefName = (PhyNodeRefName) EhcacheUtil.getObject(CACHE_NAME, id);
        if (nodeRefName == null) {
            nodeRefName = _getFriendlyName(id);
        }
        return nodeRefName.getFriendlyName();
    }

    private PhyNodeRefName _getFriendlyName(String id) {
        log.info("get the nodeId :{} from controller", id);
        BasicCache cache = cacheManager.getValue(id);
        if (cache != null) {
            String friendlyName = cache.getFriendlyName();
            PhyNodeRefName phyNodeRefName = PhyNodeRefName.builder().id(id)
                    .friendlyName(friendlyName).build();
            EhcacheUtil.put(CACHE_NAME, id, phyNodeRefName);
            return phyNodeRefName;
        }
        return new PhyNodeRefName();
    }
}
