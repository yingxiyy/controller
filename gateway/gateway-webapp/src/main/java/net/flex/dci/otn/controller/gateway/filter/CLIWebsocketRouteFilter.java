package net.flex.dci.otn.controller.gateway.filter;

import static net.flex.dci.otn.controller.gateway.common.utils.Constants.TARGET_INSTANCE_HEADER;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.messaging.simp.SimpMessageHeaderAccessor.SESSION_ID_HEADER;

import com.google.gson.Gson;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.cache.redis.core.RedisCacheOperation;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zkclient4boot.refactor.utils.DciInstancesUtils;
import net.flex.dci.otn.topology.cache.model.clisession.CLiSessionCache;
import net.flex.dci.otn.topology.cache.utils.DciCacheUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 *
 * @version 1.0
 * @date 9/18/2025 4:27 PM
 */
@Component
@Slf4j
public class CLIWebsocketRouteFilter implements GlobalFilter, Ordered {

    @Autowired
    private RedisCacheOperation redisCacheOperation;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        if (!isWebsocketConnectionRequest(path, exchange)) {
            return chain.filter(exchange);
        }
        String sessionId = extractSessionId(request);
        if (null == sessionId) {
            return sendErrorResponse(exchange, "MISSING_SESSION", "Session ID is required",
                    HttpStatus.BAD_REQUEST);
        }
        String instanceId = getInstanceFromSessionId(sessionId);
        if (null == instanceId) {
            return sendErrorResponse(exchange, "SESSION_EXPIRED", "Session not found or expired",
                    HttpStatus.BAD_REQUEST);
        }
        InstanceDetails targetInstance = getTargetInstance(instanceId);
        if (null == targetInstance) {
            removeInstanceRefCliSessionCache(instanceId);
            return sendErrorResponse(exchange, "ROUTING_ERROR", "Failed to build target URI",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        URI targetUri = buildTargetUri(request.getURI(), targetInstance);
        if (targetUri == null) {
            //remove current instance ref session id cache
            return sendErrorResponse(exchange, "ROUTING_ERROR", "Failed to build target URI",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ServerHttpRequest modifiedRequest = request.mutate()
                .uri(targetUri)
                .header(TARGET_INSTANCE_HEADER, instanceId)
                .header(SESSION_ID_HEADER, sessionId)
                .build();
        ServerWebExchange redirectExchange = exchange.mutate().request(modifiedRequest).build();
        redirectExchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, targetUri);
        log.info("Routing WebSocket request for session {} to instance {}", sessionId, instanceId);
        return chain.filter(redirectExchange);
    }

    private void removeInstanceRefCliSessionCache(String instanceId) {
        log.debug("remove the instance :{} connected cli session cache", instanceId);
        String instanceIdCachePattern = DciCacheUtils.getCliSessionCachePatternByInstance(
                instanceId);
        redisCacheOperation.deleteKeyPattern(instanceIdCachePattern);
    }

    private InstanceDetails getTargetInstance(String instanceId) {
        log.debug("get target instance by the instance id:{}", instanceId);
        InstanceDetails instance = DciInstancesUtils.getInstancesByInstanceId(instanceId);
        return instance;
    }

    private URI buildTargetUri(URI originalUri, InstanceDetails targetInstance) {
        try {
            // 解析实例ID
            String scheme = originalUri.getScheme();
            String host = CommonUtil.formateIpAddress(targetInstance.getMyIp());
            int port = targetInstance.getPort();
            // 构建新的URI
            return new URI(
                    scheme, // WebSocket协议
                    originalUri.getUserInfo(),
                    host,
                    port,
                    originalUri.getPath(),
                    originalUri.getQuery(),
                    originalUri.getFragment()
            );
        } catch (Exception e) {
            log.error("Failed to build target URI for instance: {}", targetInstance.getId(), e);
            return null;
        }
    }


    private String getInstanceFromSessionId(String sessionId) {
        log.debug("get session connection cli instance by session Id:{}", sessionId);
        try {
            String sessionIdKeyPattern = DciCacheUtils.generateCliSessionCacheKeyRegex(sessionId);
            Set<String> sessionIdKeys = redisCacheOperation.keys(sessionIdKeyPattern);
            if (sessionIdKeys.isEmpty()) {
                log.warn("No keys found for sessionId: {}", sessionId);
                return null;
            }

            if (sessionIdKeys.size() > 1) {
                log.error("Multiple keys found for sessionId: {}, keys: {}", sessionId,
                        sessionIdKeys);
                return null;
            }

            String sessionCacheKey = sessionIdKeys.iterator().next();
            CLiSessionCache cLiSessionCache = redisCacheOperation.getObject(sessionCacheKey,
                    CLiSessionCache.class);
            return cLiSessionCache == null ? null : cLiSessionCache.getCliServiceId();
        } catch (Exception e) {
            log.error("Failed to get instance ID from Redis for session: {}", sessionId, e);
            return null;
        }
    }


    private String extractSessionId(ServerHttpRequest request) {
        String sessionId = request.getQueryParams().getFirst("sessionId");
        return sessionId;
    }

    private boolean isWebsocketConnectionRequest(String path, ServerWebExchange exchange) {
        return (path.startsWith("/cli/") || path.contains("/ws")) && requestHasWebsocketHeaders(
                exchange);
    }

    private boolean requestHasWebsocketHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest();
        return request.getHeaders().containsKey("Upgrade") && "websocket".equalsIgnoreCase(
                request.getHeaders().getFirst("Upgrade"));
    }

    private Mono<Void> sendErrorResponse(ServerWebExchange exchange, String code,
            String message, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> error = new HashMap<>();
        error.put("status", httpStatus.value());
        error.put("error_code", code);
        error.put("message", message);
        Gson gson = new Gson();
        String errorJson = gson.toJson(error);
        byte[] bytes = errorJson.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -22;
    }
}
