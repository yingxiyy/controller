package net.flex.dci.otn.controller.nms.cache.manager;

import static net.flex.dci.otc.common.util.YangConstants.SITE_TOPO_KEY;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.cache.redis.utils.RedisLockUtil;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otn.controller.nms.cache.model.PhyLinkCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/24 15:52
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PhyLinkCacheManager extends AbstractCacheManager<PhyLinkCache> {

    private final PhyLinkDao phyLinkDao;

    @Override
    public PhyLinkCache getValue(String id) throws ExecutionException, InterruptedException {
        log.debug("start to get phy link cache ,phy link id is:{}", id);
        String signKey = id + SIGN_KEY;
        PhyLinkCache phyLinkCache = cacheOperation.getObject(id, PhyLinkCache.class);
        String sign = cacheOperation.get(signKey);
        if (phyLinkCache == null) {
            return refreshCache(id);
        } else {
            if (null == sign) {
                refreshCache(id);
            }
            return phyLinkCache;
        }
    }

    @Override
    protected PhyLinkCache refreshCache(String id) throws ExecutionException, InterruptedException {
        log.debug("reload data to cache for phy link :{}", id);
        Callable<PhyLinkCache> task = () -> {
            PhyLinkCache phyLinkCache = null;
            if (RedisLockUtil.tryLock(generateLockKey(id), TimeUnit.SECONDS, 30, 30)) {
                log.debug("start to reload phy link to cache ,phy link id is :{}", id);
                Link phyLink = phyLinkDao.getPhyLinkById(id);

                Destination destination = phyLink.getDestination();
                Source source = phyLink.getSource();

                Physical physical = phyLink.getAugmentation(
                        Link1.class).getPhysical();

                String phyLinkFriendlyName = physical.getFriendlyName();
                List<String> refSiteLinkIds = getSiteLinkIds(physical);

                phyLinkCache = PhyLinkCache.builder().phyLinkId(id)
                        .phyLinkFriendlyName(phyLinkFriendlyName)
                        .destNodeId(destination.getDestNode().getValue())
                        .sourceNodeId(source.getSourceNode().getValue())
                        .destTpId(destination.getDestTp().getValue())
                        .siteLinkIds(refSiteLinkIds)
                        .build();
                cacheOperation.set(id + SIGN_KEY, 1, EXPIRE_TIME_OUT);
                cacheOperation.set(id, phyLinkCache, 2 * EXPIRE_TIME_OUT);
            }
            return phyLinkCache;
        };
        return refreshAndGetCache(id, task);
    }

    private List<String> getSiteLinkIds(Physical physical) {
        List<SupportedLink> supportLink = physical.getSupportedLink();
        List<String> supportSiteLinkIds = supportLink.stream()
                .filter(supportedLink -> supportedLink.getTopologyRef().getValue()
                        .equals(SITE_TOPO_KEY))
                .map(supportedLink -> supportedLink.getLinkRef().getValue())
                .collect(Collectors.toList());
        return supportSiteLinkIds;
    }
}
