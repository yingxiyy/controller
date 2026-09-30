package net.flex.dci.otn.controller.gateway.common.configuration;


import java.util.stream.Collectors;
import net.flex.dci.otn.controller.gateway.common.exceptions.advice.GateWayExceptionHandler;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.web.ErrorProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.web.reactive.result.view.ViewResolver;


@Configuration
@EnableConfigurationProperties({ServerProperties.class, WebProperties.class})
public class ExceptionConfig {

    private final ServerProperties serverProperties;


    public ExceptionConfig(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    @Bean
    public ErrorProperties errorProperties() {
        return new ErrorProperties();
    }


    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public ErrorWebExceptionHandler errorWebExceptionHandler(ErrorAttributes errorAttributes,
            org.springframework.boot.autoconfigure.web.ResourceProperties resourceProperties,
            WebProperties webProperties, ObjectProvider<ViewResolver> viewResolvers,
            ServerCodecConfigurer serverCodecConfigurer, ApplicationContext applicationContext) {
        DefaultErrorWebExceptionHandler exceptionHandler = new GateWayExceptionHandler(
                errorAttributes,
                resourceProperties.hasBeenCustomized() ? resourceProperties
                        : webProperties.getResources(),
                this.serverProperties.getError(), applicationContext);
        exceptionHandler.setViewResolvers(
                viewResolvers.orderedStream().collect(Collectors.toList()));
        exceptionHandler.setMessageWriters(serverCodecConfigurer.getWriters());
        exceptionHandler.setMessageReaders(serverCodecConfigurer.getReaders());
        return exceptionHandler;
    }
}

