package net.flex.dci.otn.controller.gateway.filter;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import java.nio.charset.StandardCharsets;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.gateway.common.exceptions.AuthorizationException;
import net.flex.dci.otn.controller.gateway.common.model.RestResult;
import net.flex.dci.otn.controller.gateway.common.properties.Account;
import org.apache.sshd.common.util.Base64;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * basic auth method for the request
 *
 * @version 1.0
 * @date 2022/1/25 10:15
 */
@Deprecated
@Slf4j
public class AuthFilter implements GlobalFilter, Ordered {

    @Resource
    private Account account;

    public AuthFilter(Account account) {
        this.account = account;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        log.debug("user account authorization start");
        ServerHttpRequest request = exchange.getRequest();
        if (request.getMethod().equals(HttpMethod.GET)) {
            return chain.filter(exchange);
        }
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        ServerHttpResponse resp = exchange.getResponse();
        ServerWebExchangeUtils.addOriginalRequestUrl(exchange, exchange.getRequest().getURI());
        if (!StringUtils.hasText(token)) {
            //todo:return need auth
//            return authError(resp, "authentication required");
//            throw new AuthorizationException("authentication required!");
            throw new CommonException(CommonExceptionType.AUTHORIZATION_ERROR,
                    "authentication required!");
        } else {
            //todo: oauth jwt to check the token
            try {
                checkToken(token);
                return chain.filter(exchange);
            } catch (Exception ex) {
                log.error("auth failed :{}", ex.getMessage());
//                return authError(resp, ex.getMessage());
                throw new CommonException(CommonExceptionType.AUTHORIZATION_ERROR, ex.getMessage());
            }
        }

    }

    private void checkToken(String token) {
        String auth = account.getAccount() + ":" + account.getPassword();
        byte[] encodedAuth = Base64.encodeBase64(auth.getBytes(StandardCharsets.US_ASCII));
        String authHeader = "Basic " + new String(encodedAuth);
        if (!token.equals(authHeader)) {
            throw new AuthorizationException(
                    "Authentication failed. account or password is wrong please try again");
        }
    }

    /**
     * auth error
     *
     * @param resp
     * @return
     */
    private Mono<Void> authError(ServerHttpResponse resp, String msg) {
        resp.setStatusCode(HttpStatus.UNAUTHORIZED);
        resp.getHeaders().add("content-type", "application/json;charset=UTF-8");
        RestResult restResult = new RestResult(HttpStatus.UNAUTHORIZED.value(), null, msg);
        String returnStr = JSON.toJSONString(restResult, SerializerFeature.IgnoreNonFieldGetter);
        DataBuffer buffer = resp.bufferFactory().wrap(returnStr.getBytes(StandardCharsets.UTF_8));
        return resp.writeWith(Flux.just(buffer));
    }

    @Override
    public int getOrder() {
        return -19;
    }
}
