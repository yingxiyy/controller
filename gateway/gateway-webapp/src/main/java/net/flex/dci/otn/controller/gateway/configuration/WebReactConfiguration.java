package net.flex.dci.otn.controller.gateway.configuration;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.TimeUnit;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.gateway.filter.NettyWriteResponseFilter;
import org.springframework.cloud.gateway.filter.WebClientWriteResponseFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.client.reactive.ReactorResourceFactory;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.client.WebSocketClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.WebsocketClientSpec;

/**
 * add for web cros configuration
 *
 * @version 1.0
 * @date 2022/1/24 10:35
 */
@Configuration
public class WebReactConfiguration {

    private static final int MAX_FRAME_PAYLOAD_LENGTH = 1048576;

    @Bean
    @LoadBalanced
    public WebClient webClient() {
        return WebClient.builder().exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(configurer -> configurer
                                .defaultCodecs()
                                .maxInMemorySize(10 * 1024 * 1024))
                        .build())
                .build();
    }

    @Bean
    @Order(value = NettyWriteResponseFilter.WRITE_RESPONSE_FILTER_ORDER - 1)
    public WebClientWriteResponseFilter webClientWriteResponseFilter() {
        return new WebClientWriteResponseFilter();
    }

    @Bean
    public ReactorResourceFactory reactorResourceFactory() {
        ReactorResourceFactory factory = new ReactorResourceFactory();
        factory.setUseGlobalResources(false);
        return factory;
    }


    /**
     * maxFramePayloadLength – Maximum allowable frame payload length. Setting this value to your
     * application's requirement may reduce denial of service attacks using long data frames.
     *
     * @param factory
     * @return
     */
    @Bean
    public HttpClient gatewayHttpClient(ReactorResourceFactory factory) {
        return HttpClient.create(factory.getConnectionProvider())
                .option(ChannelOption.SO_KEEPALIVE, true)
                .doOnConnected(conn -> conn.addHandlerLast(
                                new IdleStateHandler(0, 0, 30, TimeUnit.SECONDS))
                        .addHandlerLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void userEventTriggered(ChannelHandlerContext ctx, Object evt)
                                    throws Exception {
                                if (evt instanceof IdleStateEvent) {
                                    WebSocketFrame ping = new PingWebSocketFrame(
                                            ctx.alloc().buffer(1).writeByte(1));
                                    ctx.writeAndFlush(ping);
                                } else {
                                    super.userEventTriggered(ctx, evt);
                                }
                            }
                        })
                ).option(ChannelOption.SO_REUSEADDR, true);

    }


    @Bean
    public WebSocketClient webSocketClient(HttpClient gatewayHttpClient) {
        return new ReactorNettyWebSocketClient(gatewayHttpClient,
                WebsocketClientSpec.builder().maxFramePayloadLength(MAX_FRAME_PAYLOAD_LENGTH));
    }
}
