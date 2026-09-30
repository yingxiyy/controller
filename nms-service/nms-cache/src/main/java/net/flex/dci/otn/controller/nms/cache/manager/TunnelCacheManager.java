package net.flex.dci.otn.controller.nms.cache.manager;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.cache.redis.utils.RedisLockUtil;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otn.controller.nms.cache.model.TunnelCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.base.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.base.attributes.SourceTp;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/24 16:34
 */
@Slf4j
@Component
@AllArgsConstructor
public class TunnelCacheManager extends AbstractCacheManager<TunnelCache> {

    private final TunnelDao tunnelDao;

    @Override
    public TunnelCache getValue(String id) throws ExecutionException, InterruptedException {
        log.debug("get tunnel cache info for the id :{}", id);
        String sign = cacheOperation.get(generateSignKey(id));
        TunnelCache tunnelCache = cacheOperation.getObject(id, TunnelCache.class);
        if (null == tunnelCache) {
            return refreshCache(id);
        } else {
            if (null == sign) {
                refreshCache(id);
            }
            return tunnelCache;
        }
    }

    @Override
    protected TunnelCache refreshCache(String id) throws ExecutionException, InterruptedException {
        log.debug("reload info from tunnel,tunnel id is :{}", id);
        Callable<TunnelCache> callable = () -> {
            TunnelCache tunnelCache = null;
            if (RedisLockUtil.tryLock(generateLockKey(id), 30, 30)) {
                log.debug("reload tunnel detail info :{}", id);
                Tunnel tunnel = tunnelDao.getTunnelById(id);
                String tunnelName = tunnel.getFriendlyName();
                List<SourceTp> sourceTp = tunnel.getSourceTp();
                List<DestinationTp> destTp = tunnel.getDestinationTp();
                String supportOchLinkId = tunnel.getSupportingLink().get(0).getLinkRef().getValue();
                tunnelCache = TunnelCache.builder().tunnelId(id).friendlyName(tunnelName)
                        .destTpId(destTp.get(0).getTpRef().getValue())
                        .sourceTpId(sourceTp.get(0).getTpRef().getValue())
                        .supportOchLinkId(supportOchLinkId).build();
            }
            return tunnelCache;
        };
        return refreshAndGetCache(id, callable);
    }
}
