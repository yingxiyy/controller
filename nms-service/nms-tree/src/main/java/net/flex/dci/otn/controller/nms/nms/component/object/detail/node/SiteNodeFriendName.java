package net.flex.dci.otn.controller.nms.nms.component.object.detail.node;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otn.topology.cache.manager.DciTopologyCacheManager;
import net.flex.dci.otn.topology.cache.model.SiteCache;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/11/2023 5:01 PM
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SiteNodeFriendName implements NeFriendName {

    private final DciTopologyCacheManager dciTopologyCacheManager;

    @Override
    public String getNodeName(String neId) {
        log.debug("start to get the site node friend name the ne id is:{}", neId);
//        Node siteNode = siteNodeDao.getSiteNodeById(neId);
//        if (null == siteNode) {
//            log.error("failed to find the site node,the site node id is:{}", neId);
//        }
//        Site siteNodePhysical = siteNode.getAugmentation(
//                Node1.class).getSite();
        SiteCache siteCache = dciTopologyCacheManager.getValue(neId, SiteCache.class);
        String friendName = siteCache.getFriendlyName();
        return friendName;
    }

    @Override
    public String supportTopologyId() {
        return TopoNameConstants.Site_Topo_Key;
    }
}
