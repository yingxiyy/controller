package net.flex.dci.otn.controller.gateway.filter;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.isAlreadyRouted;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setAlreadyRouted;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otn.controller.gateway.common.model.RestResult;
import net.flex.dci.otn.controller.gateway.dispatch.MicroserviceSelector;
import net.flex.dci.otn.controller.gateway.dispatch.model.ServerInstance;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @version 1.0
 * @date 2022/1/25 13:05
 */
@Component
@Slf4j
public class RemoteRpcRequestFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;

//    private OperationsService operationsService;

    public RemoteRpcRequestFilter(WebClient webClient) {
        this.webClient = webClient;
    }

    @SneakyThrows
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.debug("start to handle remote rpc request");
        URI requestUrl = exchange.getRequiredAttribute(GATEWAY_REQUEST_URL_ATTR);
        String scheme = requestUrl.getScheme();
        if (isAlreadyRouted(exchange) || (!scheme.equals("http") && !scheme.equals("https"))) {
            return chain.filter(exchange);
        }

        setAlreadyRouted(exchange);
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();
        //todo: select one module for the request
//        URI uri = new DefaultUriBuilderFactory().builder()
//                .port(18009)
//                .path(requestUrl.getPath())
//                .host("127.0.0.1")
//                .scheme(requestUrl.getScheme())
//                .build();
        URI uri = wrapperRedirectURL(exchange.getRequest().getURI(), scheme);
        RequestBodySpec bodySpec = this.webClient.method(method).uri(uri)
                .headers(httpHeaders -> {
                    httpHeaders.addAll(request.getHeaders());
                    httpHeaders.remove(HttpHeaders.HOST);
                });
        RequestHeadersSpec<?> headersSpec;
        if (requiresBody(method)) {
            headersSpec = bodySpec.body(BodyInserters.fromDataBuffers(request.getBody()));
        } else {
            headersSpec = bodySpec;
        }
//        RequestBodySpec bodySpec = (RequestBodySpec)((RequestBodySpec)this.webClient.method(method).uri(requestUrl)).headers((httpHeaders) -> {
//            httpHeaders.addAll(filteredHeaders);
//            if (!preserveHost) {
//                httpHeaders.remove("Host");
//            }
//
//        });

        return headersSpec.exchange().flatMap((res) -> {
            HttpHeaders upstream = new HttpHeaders();
            res.headers().asHttpHeaders()
                    .forEach((name, values) -> {
                        // 跳过所有 transfer-encoding，无论大小写
                        if (name.equalsIgnoreCase(HttpHeaders.TRANSFER_ENCODING)) {
                            return;
                        }
                        // 其它头正常搬运（按需去重）
                        List<String> deduped = values.size() > 1
                                ? values.stream().distinct().collect(Collectors.toList())
                                : values;
                        upstream.put(name, deduped);
                    });
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().putAll(upstream);
            response.setStatusCode(res.statusCode());
            exchange.getAttributes().put(ServerWebExchangeUtils.CLIENT_RESPONSE_ATTR, res);
            return chain.filter(exchange);
        });

//        RequestPath requestPath = exchange.getRequest().getPath();
//        String path = requestPath.value();
//        String requestUrl = exchange.getRequest().getURI().getRawPath();
//        ServerHttpResponse resp = exchange.getResponse();
////        try {   //do something
////            String result = operationsService.executeRequest(exchange.getRequest(),
////                    exchange.getResponse());
////            return okResult(exchange.getResponse(), result);
////        } catch (Exception ex) {
////            return rpcError(resp, ex.getMessage());
////        }
//        return chain.filter(exchange);
    }

    private URI wrapperRedirectURL(URI uri, String schema) throws Exception {
        String path = uri.getPath();
        ServerInstance serverInstance = MicroserviceSelector.selectByPath(path);
        String ipAddress = serverInstance.getIp();
        //to support ipv6 address redirect
        String host = CommonUtil.formateIpAddress(ipAddress);
        URI resolveUri = new DefaultUriBuilderFactory().builder()
                .port(serverInstance.getPort())
                .path(path)
                .query(uri.getQuery())
                .host(host)
                .scheme(schema)
                .build();
        return resolveUri;
    }

    private Mono<Void> okResult(ServerHttpResponse resp, Object result) {
        resp.setStatusCode(HttpStatus.OK);
        resp.getHeaders().add("content-type", "application/json;charset=UTF-8");
//        RestResult restResult = new RestResult(HttpStatus.UNAUTHORIZED.value(), null, msg);
        String returnStr = (String) result;
        DataBuffer buffer = resp.bufferFactory().wrap(returnStr.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Flux.just(buffer));
    }


    @Override
    public int getOrder() {
        return 3;
    }


    private Mono<Void> rpcError(ServerHttpResponse resp, String msg) {
        resp.setStatusCode(HttpStatus.NOT_ACCEPTABLE);
        resp.getHeaders().add("content-type", "application/json;charset=UTF-8");
        RestResult restResult = new RestResult(HttpStatus.NOT_ACCEPTABLE.value(), null,
                msg);
        String returnStr = JSON.toJSONString(restResult, SerializerFeature.IgnoreNonFieldGetter);
        DataBuffer buffer = resp.bufferFactory().wrap(returnStr.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Flux.just(buffer));
    }


    private boolean requiresBody(HttpMethod method) {
        switch (method) {
            case PUT:
            case POST:
            case PATCH:
                return true;
            default:
                return false;
        }
    }
}
