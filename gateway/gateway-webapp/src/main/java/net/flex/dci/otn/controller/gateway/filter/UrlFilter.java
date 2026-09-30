package net.flex.dci.otn.controller.gateway.filter;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;

import java.net.URI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

/**
 * decorate exchange attribute
 *
 * @version 1.0
 * @date 2022/1/26 12:17
 */

@Component
@Slf4j
public class UrlFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //erich the exchange attribute
        String scheme = exchange.getRequest().getURI().getScheme();

        //todo:change the request uri pending method
        URI uri = exchange.getRequest().getURI();

        URI requestUrl = UriComponentsBuilder.fromUri(uri).scheme(scheme)
                .build().toUri();
        exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, requestUrl);

        log.debug("enrich the attribute :[" + requestUrl + "]");
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -20;
    }
}
