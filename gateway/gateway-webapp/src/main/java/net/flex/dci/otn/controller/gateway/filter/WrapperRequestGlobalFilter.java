package net.flex.dci.otn.controller.gateway.filter;

import io.netty.buffer.ByteBufAllocator;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.gateway.valuemap.GatewayContext;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * cache post put patch request body for lifecycle log
 *
 * @version 1.0
 * @date 2022/2/10 13:40
 */
@Slf4j
public class WrapperRequestGlobalFilter implements GlobalFilter, Ordered {

    private static final String CONTENT_TYPE_JSON = "application/json";

    private final DataBufferFactory dataBufferFactory = new NettyDataBufferFactory(
            ByteBufAllocator.DEFAULT);
    private static final List<HttpMessageReader<?>> messageReaders =
            HandlerStrategies.withDefaults().messageReaders();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().pathWithinApplication().value();
        GatewayContext gatewayContext = new GatewayContext();
        gatewayContext.setPath(path);
        exchange.getAttributes().put(GatewayContext.CACHE_GATEWAY_CONTEXT, gatewayContext);
        HttpHeaders headers = request.getHeaders();
        MediaType contentType = headers.getContentType();
        log.debug("start to log the request ");
        log.info("http method:{},Url:{}", request.getMethod(), request.getURI().getRawPath());

        //todo:get user id
//        AtomicReference<String> reqStr = new AtomicReference<>();
//        String contentType = request.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (null != contentType && requiresBody(Objects.requireNonNull(request.getMethod()))) {
//            ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(request) {
//
//                @Override
//                public Flux<DataBuffer> getBody() {
//                    Flux<DataBuffer> body = super.getBody();
//                    InputStreamHolder holder = new InputStreamHolder();
//                    body.subscribe(buffer -> holder.inputStream = buffer.asInputStream());
//                    if (null != holder.inputStream) {
//                        String bodyStr = JSON.toJSONString(holder.inputStream);
//                        DataBuffer dataBuffer = dataBufferFactory.allocateBuffer();
//                        reqStr.set(bodyStr);
//                        String json = bodyStr;
//                        log.info("request http body is:{}", json);
//                        dataBuffer.write(json.getBytes(StandardCharsets.UTF_8));
//                        return Flux.just(dataBuffer);
//                    } else {
//                        return super.getBody();
//                    }
//                }
//            };
//            log.info("request body is :{}", reqStr.get());
//            return chain.filter(exchange.mutate().request(decorator).build());
            Mono<Void> voidMono = null;
            if (contentType.equals(
                    MediaType.APPLICATION_JSON) || contentType.equals(
                    MediaType.APPLICATION_JSON_UTF8)) {
                voidMono = readBody(exchange, chain, gatewayContext);
            } else if (contentType.equals(MediaType.APPLICATION_FORM_URLENCODED)) {
                voidMono = readFormData(exchange, chain, gatewayContext);
            }
            if (voidMono != null) {
                return voidMono;
            }
            return chain.filter(exchange);
        }
        return chain.filter(exchange);
    }

    /**
     * readFormData
     *
     * @param exchange
     * @param chain
     * @param gatewayContext
     * @return
     */
    private Mono<Void> readFormData(ServerWebExchange exchange, GatewayFilterChain chain,
            GatewayContext gatewayContext) {
        final ServerHttpRequest request = exchange.getRequest();
        HttpHeaders headers = request.getHeaders();
        return exchange.getFormData().doOnNext(multiValueMap -> {
            gatewayContext.setFormData(multiValueMap);
            log.info("request body x-www-form-urlencoded:{}", multiValueMap);
        }).then(Mono.defer(() -> {
            Charset charset = headers.getContentType().getCharset();
            charset = charset == null ? StandardCharsets.UTF_8 : charset;
            String charsetName = charset.name();
            MultiValueMap<String, String> formData = gatewayContext.getFormData();
            if (null == formData || formData.isEmpty()) {
                return chain.filter(exchange);
            }
            StringBuilder formDataBodyBuilder = new StringBuilder();
            String entryKey;
            List<String> entryValue;
            try {
                for (Map.Entry<String, List<String>> entry : formData.entrySet()) {
                    entryKey = entry.getKey();
                    entryValue = entry.getValue();
                    for (String value : entryValue) {
                        formDataBodyBuilder.append(entryKey).append("=")
                                .append(URLEncoder.encode(value, charsetName)).append("&");
                    }

                }
            } catch (UnsupportedEncodingException e) {
                log.error("failed to encode the data :{}", e);
            }

            String formDataBodyString = "";
            if (formDataBodyBuilder.length() > 0) {
                formDataBodyString = formDataBodyBuilder.substring(0,
                        formDataBodyBuilder.length() - 1);
            }

            byte[] bodyBytes = formDataBodyString.getBytes(charset);
            int contentLength = bodyBytes.length;
            ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(request) {

                @Override
                public HttpHeaders getHeaders() {
                    HttpHeaders httpHeaders = new HttpHeaders();
                    httpHeaders.putAll(super.getHeaders());
                    if (contentLength > 0) {
                        httpHeaders.setContentLength(contentLength);
                    } else {
                        httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                    }
                    return httpHeaders;
                }

                @Override
                public Flux<DataBuffer> getBody() {
                    return DataBufferUtils.read(new ByteArrayResource(bodyBytes),
                            new NettyDataBufferFactory(ByteBufAllocator.DEFAULT), contentLength);
                }
            };

            ServerWebExchange mutateExchange = exchange.mutate().request(decorator).build();
            return chain.filter(mutateExchange);
        }));
    }

    /**
     * test for read body data
     *
     * @param exchange
     * @param chain
     * @param gatewayContext
     * @return
     */
    private Mono<Void> readBody(ServerWebExchange exchange, GatewayFilterChain chain,
            GatewayContext gatewayContext) {
        return DataBufferUtils.join(exchange.getRequest().getBody()).flatMap(dataBuffer -> {
            byte[] bytes = new byte[dataBuffer.readableByteCount()];
            dataBuffer.read(bytes);
            DataBufferUtils.release(dataBuffer);
            Flux<DataBuffer> cachedFlux = Flux.defer(() -> {
                DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                DataBufferUtils.retain(buffer);
                return Mono.just(buffer);
            });
            ServerHttpRequest mutateRequest = new ServerHttpRequestDecorator(
                    exchange.getRequest()) {
                @Override
                public Flux<DataBuffer> getBody() {
                    return cachedFlux;
                }
            };
            ServerWebExchange mutatedExchange = exchange.mutate().request(mutateRequest).build();
            return ServerRequest.create(mutatedExchange, messageReaders)
                    .bodyToMono(String.class)
                    .doOnNext(objectValue -> {
                        log.info("request body is {}", objectValue);
                        log.info("--------------------end ------------------------------------");
                        gatewayContext.setCacheReqBody(objectValue);
                    }).then(chain.filter(mutatedExchange));
        });
    }

    @Override
    public int getOrder() {
        return -17;
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
