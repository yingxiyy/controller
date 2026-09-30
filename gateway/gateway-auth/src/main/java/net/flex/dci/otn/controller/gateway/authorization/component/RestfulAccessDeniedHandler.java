package net.flex.dci.otn.controller.gateway.authorization.component;

import com.alibaba.fastjson.JSON;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.gateway.common.model.ApiErrorResponse;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.server.authorization.ServerAccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * @version 1.0
 * @date 2022/4/18 15:29
 */
@Component
@Slf4j
public class RestfulAccessDeniedHandler implements ServerAccessDeniedHandler {

    @Override
    public Mono<Void> handle(ServerWebExchange serverWebExchange, AccessDeniedException e) {
        log.debug("handle restful access denied ");
        ServerHttpResponse response = serverWebExchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        ApiErrorResponse apiError = ApiErrorResponse.builder()
                .status(CommonExceptionType.NO_PERMISSION.getHttpStatus().value())
                .error_code(CommonExceptionType.NO_PERMISSION.getValue())
                .message(e.getMessage())
                .build();
        String body = JSON.toJSONString(apiError);
        HttpHeaders httpHeaders = response.getHeaders();
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }
}
