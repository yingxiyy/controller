package net.flex.dci.otn.controller.nms.cache;

import static net.flex.dci.otn.topology.cache.utils.DciCacheConstants.ROUTE_STRUCT;

import com.alibaba.fastjson.JSON;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.cache.redis.core.RedisCacheOperation;
import net.flex.dci.otn.controller.nms.nms.dto.RouteSequenceDto;
import org.springframework.stereotype.Component;

/**
 * 2026/7/5
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class NmsCacheManager {

    private final RedisCacheOperation redisCacheOperation;

    public RouteSequenceDto getConnectionRouteSequence(String connectionId) {
        log.debug("get connection route sequence the connectionId:{}", connectionId);
        String cacheKey = generateRouteSequenceKey(connectionId);
        RouteSequenceDto routeSequenceDto = redisCacheOperation.getObject(cacheKey,
                RouteSequenceDto.class);
        return routeSequenceDto;

    }


    private String generateRouteSequenceKey(String connectionId) {
        return ROUTE_STRUCT + connectionId;
    }

    public RouteSequenceDto getOchLinkRoute(String linkId) {
        log.debug("get och link route sequence the och link id is:{}", linkId);
        String cacheKey = generateRouteSequenceKey(linkId);
        String routeSequenceJson = redisCacheOperation.get(cacheKey);
        RouteSequenceDto routeSequenceDto = JSON.parseObject(routeSequenceJson,
                RouteSequenceDto.class);
        return routeSequenceDto;
    }

    public void setOchLinkRoute(String linkId, RouteSequenceDto linkRouteSequence) {
        String cacheKey = generateRouteSequenceKey(linkId);
        String json = JSON.toJSONString(linkRouteSequence);
        redisCacheOperation.set(cacheKey, json, 7, TimeUnit.DAYS);
    }
}
