package net.flex.dci.otn.controller.nms.cache.manager;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.Constants;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import net.flex.dci.otn.controller.nms.cache.model.TerminationPointCache;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/20 11:34
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TerminationPointCacheManager extends AbstractCacheManager<TerminationPointCache> {


    private final TerminationPointDao terminationPointDao;


    @Override
    public TerminationPointCache getValue(String tpId)
            throws ExecutionException, InterruptedException {
        log.debug("get termination point cache ,tpId is:{}", tpId);
        String sign_key = tpId + SIGN_KEY;

        String sign = cacheOperation.get(sign_key);

        TerminationPointCache terminationPointCache =
                cacheOperation.getObject(tpId,
                        TerminationPointCache.class);
        if (terminationPointCache == null) {
            return refreshCache(tpId);
        } else {
            if (sign == null) {
                refreshCache(tpId);
            }
            return terminationPointCache;
        }

    }

    /**
     * refresh sign key background
     *
     * @param tpId
     */
    @Override
    protected TerminationPointCache refreshCache(String tpId)
            throws ExecutionException, InterruptedException {

        Callable<TerminationPointCache> task = () -> {
            log.debug("background refresh the cache for the termination point");
            if (cacheOperation.tryLock(generateLockKey(tpId), TimeUnit.SECONDS, 30, 30)) {
                cacheOperation.set(tpId + SIGN_KEY, 1, EXPIRE_TIME_OUT);
                String[] ids = tpId.split(Constants.POUND);
                String nodeId = ids[0] + Constants.POUND + ids[1];
                TerminationPoint tp = terminationPointDao.getConfigPhyTpByNeIdAndTpId(
                        nodeId, tpId);
                TerminationPointCache tpCache = TerminationPointCache.getFromTerminationPoint(
                        tpId,
                        tp);
                cacheOperation.set(tpId, tpCache, 2 * EXPIRE_TIME_OUT);

                log.debug("finish to refresh it");
                return tpCache;
            }
            return null;
        };
        return refreshAndGetCache(tpId, task);
    }
}
