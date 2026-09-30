package net.flex.dci.otn.controller.nms.cache.manager;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otn.controller.nms.cache.model.PhyNodeCache;
import net.flex.dci.otn.controller.nms.cache.model.SiteCache;
import net.flex.dci.otn.controller.nms.cache.model.SiteLinkCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/23 16:36
 */
@Component
@Slf4j
@AllArgsConstructor
public class SiteLinkCacheManager extends AbstractCacheManager<SiteLinkCache> {

    private final SiteLinkDao siteLinkDao;

    private final PhyNodeCacheManager phyNodeCacheManager;


    private final SiteCacheManager siteCacheManager;

    @Override
    public SiteLinkCache getValue(String id) throws ExecutionException, InterruptedException {
        log.debug("get site node detail info cache, phyNode id is :{}", id);

        String sign_key = id + SIGN_KEY;

        String sign = cacheOperation.get(sign_key);

        SiteLinkCache siteLinkCache = cacheOperation.getObject(id,
                SiteLinkCache.class);
        if (siteLinkCache == null) {
            return refreshCache(id);
        } else if (sign == null) {
            refreshCache(id);
            return siteLinkCache;
        } else {
            return siteLinkCache;
        }
    }

    @Override
    protected SiteLinkCache refreshCache(String id)
            throws ExecutionException, InterruptedException {
        log.info("start to refresh site link cache ,link id is {}", id);
        Callable<SiteLinkCache> refreshSiteLinkCacheTask =
                () -> {
                    SiteLinkCache siteLinkCache = null;
                    if (cacheOperation.tryLock(generateLockKey(id), TimeUnit.SECONDS, 30, 30)) {
                        log.debug("refresh the site link additional properties");
                        Link siteLink = siteLinkDao.getSiteLinkById(id);
                        Site site = siteLink.getAugmentation(Link1.class).getSite();

                        String siteLinkName = site.getFriendlyName();
                        String plane = site.getPlaneName();
                        String sourceTp = siteLink.getSource().getSourceTp().getValue();
                        String destTp = siteLink.getDestination().getDestTp().getValue();
                        SiteCache sourceSite = siteCacheManager.getValue(
                                siteLink.getSource().getSourceNode().getValue());
                        SiteCache destSite = siteCacheManager.getValue(
                                siteLink.getDestination().getDestNode().getValue());
                        PhyNodeCache sourceNode = phyNodeCacheManager.getValue(
                                PhysicalTpIdNamingRule.getNodeId(
                                        sourceTp));
                        PhyNodeCache destNode = phyNodeCacheManager.getValue(
                                PhysicalTpIdNamingRule.getNodeId(destTp
                                ));

                        siteLinkCache = SiteLinkCache.builder().siteLinkId(id)
                                .siteLinkFriendlyName(siteLinkName)
                                .plane(plane)
                                .destNodeId(destNode.getRefSiteName())
                                .destSiteId(destSite.getSiteNodeId())
                                .destSiteName(destSite.getSiteFriendlyName())
                                .destNodeName(destNode.getPhyNodeName())
                                .sourceSiteId(sourceSite.getSiteNodeId())
                                .sourceSiteName(sourceSite.getSiteFriendlyName())
                                .sourceNodeId(sourceNode.getPhyNodeId())
                                .sourceSiteId(sourceSite.getSiteNodeId())
                                .sourceTpId(sourceTp)
                                .destTpId(destTp)
                                .build();
                    }

                    return siteLinkCache;
                };
        return refreshAndGetCache(id, refreshSiteLinkCacheTask);
    }
}
