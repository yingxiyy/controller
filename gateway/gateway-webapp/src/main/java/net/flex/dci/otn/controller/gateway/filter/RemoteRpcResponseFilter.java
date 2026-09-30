package net.flex.dci.otn.controller.gateway.filter;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.CLIENT_RESPONSE_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.isAlreadyRouted;

import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyExtractors;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @version 1.0
 * @date 2022/1/26 13:26
 */
@Component
@Slf4j
public class RemoteRpcResponseFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.debug("start to handle the request for the remote rpc response");
        if (isAlreadyRouted(exchange)) {
            return Mono.defer(() -> {
                ClientResponse clientResponse = exchange.getAttribute(CLIENT_RESPONSE_ATTR);
                if (clientResponse == null) {
                    return Mono.empty();
                }
                log.trace("web client write response filter start");
                ServerHttpResponse response = exchange.getResponse();
                exchange.getResponse().getHeaders().entrySet().stream()
                        .filter(kv -> (kv.getValue() != null && kv.getValue().size() > 1))
                        .filter(kv -> (kv.getKey().equals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)
                                || kv.getKey()
                                .equals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS) || kv.getKey()
                                .equals(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)))
                        .forEach(kv ->
                        {
                            kv.setValue(new ArrayList<String>() {{
                                add(kv.getValue().get(0));
                            }});
                        });
                //todo: record response for lifecycle
                return response.writeWith(clientResponse.body(BodyExtractors.toDataBuffers()))
                        .log("web client response");
            });
        }
        return chain.filter(exchange);

    }


    @Override
    public int getOrder() {
        return 4;
    }
}
