package net.flex.dci.otn.controller.gateway.filter;

import static net.flex.dci.otn.controller.gateway.common.utils.AuthConstants.USER_ID;
import static net.flex.dci.otn.controller.gateway.common.utils.AuthConstants.USER_NAME;

import com.nimbusds.jose.JWSObject;
import java.net.URI;
import java.text.ParseException;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.gateway.common.utils.AuthConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @version 1.0
 * @date 2022/4/18 10:44
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {


    @Override
    public int getOrder() {
        return -20;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.info("start to handle the web security");

        ServerHttpRequest serverHttpRequest = exchange.getRequest();
        URI uri = exchange.getRequest().getURI();
        log.debug("the http request is:{},{}", uri.getPath(), serverHttpRequest.getHeaders());
        String token = serverHttpRequest.getHeaders().getFirst(AuthConstants.JWT_TOKEN_HEADER);
        if (Objects.isNull(token)) {
            token = serverHttpRequest.getQueryParams().getFirst(AuthConstants.JWT_TOKEN_HEADER);
        }

        if (StringUtils.isBlank(token)) {
            log.debug("token is null!");
            return chain.filter(exchange);
        }

        try {
            String realToken = token.replace(AuthConstants.JWT_TOKEN_PREFIX, "");
            String userId = getUserInfo(realToken, USER_ID);
            String username = getUserInfo(realToken, USER_NAME);
            ServerHttpRequest request = serverHttpRequest.mutate()
                    .headers(httpHeaders -> {
                        httpHeaders.remove(AuthConstants.JWT_TOKEN_HEADER);
                        httpHeaders.add(AuthConstants.USER_TOKEN_HEADER, userId);
                        httpHeaders.add(AuthConstants.USER_NAME_TOKEN_HEADER, username);
                        httpHeaders.add(AuthConstants.USER_WHO_TOKEN_HEADER, username);
                    })
                    .build();

            exchange = exchange.mutate().request(request).build();
            return chain.filter(exchange);
        } catch (ParseException e) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the access token is invalided!");
        }


    }


    private String getUserInfo(String realToken, String key) throws ParseException {
        JWSObject jwsObject = JWSObject.parse(realToken);
        Map<String, Object> jsonMap = jwsObject.getPayload().toJSONObject();
        Object result = jsonMap.get(key);
        return String.valueOf(result);
    }
}
