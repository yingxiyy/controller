package net.flex.dci.otn.controller.cli.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.cache.redis.core.RedisCacheOperation;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zkclient4boot.util.ConfLoader;
import net.flex.dci.otn.topology.cache.model.clisession.CLiSessionCache;
import net.flex.dci.otn.topology.cache.utils.DciCacheUtils;
import org.springframework.stereotype.Component;

/**
 *
 * @version 1.0
 * @date 9/18/2025 11:24 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CliSessionCacheHandlerImpl implements CliSessionCacheHandler {

    private final RedisCacheOperation redisCacheOperation;

    @Override
    public void recordCache(String sessionId) {
        log.debug("record current cli session id with direct instance");
        InstanceDetails currentInstance = ConfLoader.buildDetails();
        String instanceId = currentInstance.getId();
        log.debug("add current session Id:{} reference instance :{}", sessionId, instanceId);
        CLiSessionCache cLiSessionCache = CLiSessionCache.builder().sessionId(sessionId)
                .cliServiceId(instanceId).build();
        String cliSessionKey = DciCacheUtils.generateCliSessionCacheKey(sessionId, instanceId);
        redisCacheOperation.set(cliSessionKey, cLiSessionCache);
    }

    @Override
    public void handleInstanceRemove(InstanceDetails detail) {
        log.debug("current cli instance is down instance is:{}", detail.getId());
        String instanceId = detail.getId();
        String instanceIdGeneralKey = DciCacheUtils.getCliSessionCachePatternByInstance(instanceId);
        redisCacheOperation.deleteKeyPattern(instanceIdGeneralKey);
    }

    @Override
    public void removeCache(String sessionId) {
        log.debug("session id down remove cache :{}", sessionId);
        String cliSessionPattern = DciCacheUtils.generateCliSessionCacheKeyRegex(sessionId);
        redisCacheOperation.deleteKeyPattern(cliSessionPattern);
    }
}
