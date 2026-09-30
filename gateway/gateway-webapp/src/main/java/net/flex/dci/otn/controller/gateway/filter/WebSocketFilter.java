package net.flex.dci.otn.controller.gateway.filter;

import static org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter.filterRequest;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_REQUEST_URL_ATTR;
import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.setAlreadyRouted;
import static org.springframework.util.StringUtils.commaDelimitedListToStringArray;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otn.controller.gateway.common.utils.Constants;
import net.flex.dci.otn.controller.gateway.dispatch.MicroserviceSelector;
import net.flex.dci.otn.controller.gateway.dispatch.model.ServerInstance;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.headers.HttpHeadersFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import org.springframework.web.reactive.socket.server.WebSocketService;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.DefaultUriBuilderFactory;
import reactor.core.publisher.Mono;

/**
 * @version 1.0
 * @date 2022/1/25 10:17
 */
@Component
@Slf4j
public class WebSocketFilter implements GlobalFilter, Ordered {


    public static final String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";


    private final WebSocketClient webSocketClient;


    private final WebSocketService webSocketService;


    private final ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider;


    private volatile List<HttpHeadersFilter> headersFilters;

    @Autowired
    public WebSocketFilter(WebSocketClient webSocketClient, WebSocketService webSocketService,
            ObjectProvider<List<HttpHeadersFilter>> headersFiltersProvider) {
        this.webSocketClient = webSocketClient;
        this.webSocketService = webSocketService;
        this.headersFiltersProvider = headersFiltersProvider;
    }

    static void changeSchemaIfIsWebSocketUpgrade(ServerWebExchange exchange) {
        String schema = exchange.getRequest().getURI().getScheme();
        String upgrade = exchange.getRequest().getHeaders().getUpgrade();
        if ("WebSocket".equalsIgnoreCase(upgrade) && ("http".equals(schema) || "https".equals(
                schema))) {
            if (exchange.getAttributes().get(GATEWAY_REQUEST_URL_ATTR) != null) {
                log.debug("websocket is already redirected do nothing");
                return;
            }
            String wsSchema = convertHttp2Ws(schema);
            //todo:change the request uri pending method
            URI wsRequestUrl = wrapperRedirectUrl(exchange.getRequest().getURI(),
                    wsSchema);
//            URI uri = new DefaultUriBuilderFactory().builder()
//                    .port(8885)
//                    .path(exchange.getRequest().getPath().value())
//                    .host("10.242.111.29")
//                    .scheme(schema)
//                    .build();

//            URI wsRequestUrl = UriComponentsBuilder.fromUri(redirectUrl).scheme(wsSchema)
//                    .build().toUri();
            exchange.getAttributes().put(GATEWAY_REQUEST_URL_ATTR, wsRequestUrl);

            log.debug("changeSchemeTo:[" + wsRequestUrl + "]");
        }
    }

    /**
     * return redirect url
     *
     * @param schema
     * @return
     */
    private static URI wrapperRedirectUrl(URI uri, String schema) {
        String path = uri.getPath();
        ServerInstance serverInstance = MicroserviceSelector.selectByPath(path);
        try {
            String host = CommonUtil.formateIpAddress(serverInstance.getIp());
            URI resolveUri = new DefaultUriBuilderFactory().builder()
                    .port(serverInstance.getPort())
                    .path(URLDecoder.decode(path, "UTF-8"))
                    .host(host)
                    .query(uri.getQuery())
                    .scheme(schema)
                    .build();
            return resolveUri;
        } catch (UnsupportedEncodingException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }
    }

    static String convertHttp2Ws(String schema) {
        schema = schema.toLowerCase();
        return "http".equals(schema) ? "ws" : "https".equals(schema) ? "wss" : schema;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.debug("start to handle the web socket filter ");
        changeSchemaIfIsWebSocketUpgrade(exchange);
        String schema = exchange.getRequest().getURI().getScheme();
        String upgrade = exchange.getRequest().getHeaders().getUpgrade();
        if (!(Constants.WEBSOCKET.equalsIgnoreCase(upgrade) && (Constants.HTTP.equals(schema)
                || Constants.HTTPS.equals(
                schema)))) {
            return chain.filter(exchange);
        }
//        if ((!"webso".equals(schema) && !"wss".equals(schema))) {
//            return chain.filter(exchange);
//        }
        setAlreadyRouted(exchange);
        HttpHeaders headers = exchange.getRequest().getHeaders();
        HttpHeaders filtered = filterRequest(getHeadersFilters(), exchange);

        List<String> protocols = headers.get(SEC_WEBSOCKET_PROTOCOL);
        if (protocols != null) {
            protocols = headers.get(SEC_WEBSOCKET_PROTOCOL).stream().flatMap(
                            header -> Arrays.stream(commaDelimitedListToStringArray(header)))
                    .map(String::trim).collect(Collectors.toList());
        }
        URI requestUrl = exchange.getRequiredAttribute(GATEWAY_REQUEST_URL_ATTR);
        return this.webSocketService.handleRequest(exchange,
                new ProxyWebSocketHandler(
                        requestUrl, this.webSocketClient, filtered, protocols));
    }

    private List<HttpHeadersFilter> getHeadersFilters() {
        if (this.headersFilters == null) {
            this.headersFilters = this.headersFiltersProvider
                    .getIfAvailable(ArrayList::new);

            headersFilters.add((headers, exchange) -> {
                HttpHeaders filtered = new HttpHeaders();
                headers.entrySet().stream()
                        .filter(entry -> !entry.getKey().toLowerCase()
                                .startsWith("sec-websocket"))
                        .forEach(header -> filtered.addAll(header.getKey(),
                                header.getValue()));
                return filtered;
            });
        }

        return this.headersFilters;
    }

    @Override
    public int getOrder() {
        return -21;
    }


    private static class ProxyWebSocketHandler implements WebSocketHandler {

        private final WebSocketClient webSocketClient;
        private final URI url;

        private final HttpHeaders headers;

        private final List<String> subProtocols;

        public ProxyWebSocketHandler(URI url, WebSocketClient webSocketClient, HttpHeaders headers,
                List<String> subProtocols) {
            this.url = url;
            this.webSocketClient = webSocketClient;
            this.headers = headers;
            this.subProtocols = subProtocols;
        }

        @Override
        public Mono<Void> handle(WebSocketSession session) {
            log.info("proxy WebSocket handler for the session:{}", session);
            return webSocketClient.execute(url, this.headers, new WebSocketHandler() {
                @Override
                public Mono<Void> handle(WebSocketSession proxySession) {
                    Mono<Void> proxySessionSend = proxySession.send(session.receive().doOnNext(
                            WebSocketMessage::retain)).log("proxySession", Level.FINE);
                    Mono<Void> serverSessionSend = session.send(
                                    proxySession.receive().doOnNext(WebSocketMessage::retain))
                            .log("Session", Level.FINE);
                    return Mono.when(proxySessionSend, serverSessionSend).then();
                }
            });
        }
    }
}
