package net.flex.dci.otn.controller.nms.cache.manager;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.cache.redis.utils.RedisLockUtil;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otn.controller.nms.cache.model.SiteCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.springframework.stereotype.Component;

/**
 * siteid->site node friendly name
 *
 * @version 1.0
 * @date 2022/3/20 11:03
 */
@Component
@Slf4j
@AllArgsConstructor
public class SiteCacheManager extends AbstractCacheManager<SiteCache> {


    private final SiteNodeDao siteNodeDao;

    @Override
    public SiteCache getValue(String id) throws ExecutionException, InterruptedException {
        log.debug("get site node detail info cache, phyNode id is :{}", id);

        String sign_key = id + SIGN_KEY;

        String sign = cacheOperation.get(sign_key);

        SiteCache siteCache = cacheOperation.getObject(id,
                SiteCache.class);
        if (siteCache == null) {
            return refreshCache(id);
        } else if (sign == null) {
            refreshCache(id);
            return siteCache;
        } else {
            return siteCache;
        }
    }

    @Override
    protected SiteCache refreshCache(String id) throws ExecutionException, InterruptedException {
        log.debug("start to get or refresh the phy node cache ,id :{}", id);
        Callable<SiteCache> siteCacheTask = () -> {
            if (RedisLockUtil.tryLock(generateLockKey(id), TimeUnit.SECONDS, 30, 30)) {
                log.debug("start to get ref siteNode :{}", id);
                Node siteNode = siteNodeDao.getSiteNodeById(id);
                Node1 siteNodePhysical = siteNode.getAugmentation(Node1.class);
                Site site = siteNodePhysical.getSite();

                SiteCache siteCache = SiteCache.builder().siteNodeId(id)
                        .siteFriendlyName(site.getFriendlyName()).build();
                cacheOperation.set(id + SIGN_KEY, 1, EXPIRE_TIME_OUT);
                cacheOperation.set(id, siteNode, 2 * EXPIRE_TIME_OUT);
                return siteCache;
            }
            return null;
        };
        return refreshAndGetCache(id, siteCacheTask);
    }
}
