package net.flex.dci.otn.controller.gateway.authorization.filter;

import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.gateway.authorization.configuration.IgnoreUrlConfiguration;
import net.flex.dci.otn.controller.gateway.common.utils.AuthConstants;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * @version 1.0
 * @date 2022/4/18 15:18
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class IgnoreUrlsRemoveJwtFilter implements WebFilter {

    private final IgnoreUrlConfiguration ignoreUrlConfiguration;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        URI uri = request.getURI();
        PathMatcher pathMatcher = new AntPathMatcher();
        List<String> ignoreUrls = ignoreUrlConfiguration.getUrls();
        for (String ignoreUrl : ignoreUrls) {
            if (pathMatcher.match(ignoreUrl, uri.getPath())) {
                request = request.mutate().header(AuthConstants.JWT_TOKEN_HEADER, "").build();
                exchange = exchange.mutate().request(request).build();
                return chain.filter(exchange);
            }
        }

        return chain.filter(exchange);
    }
}
