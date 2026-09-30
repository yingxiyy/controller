package net.flex.dci.otn.controller.gateway.authorization;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.cache.redis.core.RedisCacheOperation;
import net.flex.dci.otc.common.constants.AuthConstant;
import net.flex.dci.otn.controller.gateway.authorization.configuration.IgnoreUrlConfiguration;
import net.flex.dci.otn.controller.gateway.common.utils.AuthConstants;
import net.flex.dci.otn.controller.gateway.common.utils.Constants;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.server.authorization.AuthorizationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import reactor.core.publisher.Mono;

/**
 * authorization manager for the right
 *
 * @version 1.0
 * @date 2022/4/16 11:44
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AuthorizationManager implements ReactiveAuthorizationManager<AuthorizationContext> {

    private final RedisCacheOperation redisCacheOperation;

    private final IgnoreUrlConfiguration ignoreUrlConfiguration;

    private final Gson gson;


    @Override
    public Mono<AuthorizationDecision> check(
            Mono<Authentication> mono,
            AuthorizationContext authorizationContext) {
        URI uri = authorizationContext.getExchange().getRequest().getURI();
        String path = uri.getPath();
        String method = authorizationContext.getExchange().getRequest().getMethodValue();
        PathMatcher pathMatcher = new AntPathMatcher();
        ServerHttpRequest request = authorizationContext.getExchange()
                .getRequest();
        String schema = authorizationContext.getExchange().getRequest().getURI().getScheme();
        String upgrade = authorizationContext.getExchange().getRequest().getHeaders().getUpgrade();
        if ((Constants.WEBSOCKET.equalsIgnoreCase(upgrade) && (Constants.HTTP.equals(schema)
                || Constants.HTTPS.equals(
                schema)))) {
            return Mono.just(new AuthorizationDecision(true));
        }
        List<String> ignoreUrls = ignoreUrlConfiguration.getUrls();
        for (String ignoreUrl : ignoreUrls) {
            if (pathMatcher.match(ignoreUrl, uri.getPath())) {
                return Mono.just(new AuthorizationDecision(true));
            }
        }

        if (request.getMethod() == HttpMethod.OPTIONS) {
            return Mono.just(new AuthorizationDecision(true));
        }

        List<String> authorities = getRefAuthority(path, method);

//        return Mono.just(new AuthorizationDecision(false));
//        Object obj = redisTemplate.opsForHash().get(RedisConstant.RESOURCE_ROLES_MAP, uri.getPath());
//        List<String> authorities = Convert.toList(String.class,obj);
        authorities = authorities.stream().map(i -> i = AuthConstants.JWT_AUTHORITY_PREFIX + i)
                .collect(
                        Collectors.toList());

        return mono
                .filter(Authentication::isAuthenticated)
                .flatMapIterable(Authentication::getAuthorities)
                .map(GrantedAuthority::getAuthority)
                .any(authorities::contains)
                .map(AuthorizationDecision::new)
                .defaultIfEmpty(new AuthorizationDecision(false));

    }

    private List<String> getRefAuthority(String path, String method) {
        List<String> authorities = new ArrayList<>();
        authorities = getDirectPathAuthorities(path, method);
        if (authorities.isEmpty()) {
            authorities = getAntPathAuthorities(path);
        }
        return authorities;
    }

    private List<String> getDirectPathAuthorities(String path, String method) {
        log.debug("get direct path authorities from path:{} and method:{}", path, method);
        List<String> authorities = new ArrayList<>();
//        String key = path + AuthConstant.AT + method.toUpperCase();
        Map<String, String> directAuthRoleMap = redisCacheOperation.getAll(
                AuthConstant.RESOURCE_ROLES_DIRECT_MAP_KEY);
        PathMatcher pathMatcher = new AntPathMatcher();
        for (Map.Entry<String, String> entry : directAuthRoleMap.entrySet()) {
            String pathKey = (String) entry.getKey();
            String[] parts = pathKey.split(AuthConstant.AT);
            String patternPath = parts[0];
            String patternMethod = parts[1];
            Set<String> authoritySet = gson.fromJson(entry.getValue(),
                    new TypeToken<HashSet<String>>() {
                    }.getType());
            if (pathMatcher.match(patternPath, path) && patternMethod.equalsIgnoreCase(method)) {
                authorities.addAll(authoritySet);
            }
        }
//        Set<String> authoritiSet = (Set<String>) redisCacheOperation.getValue(
//                AuthConstant.RESOURCE_ROLES_DIRECT_MAP_KEY, key);
//        if (authoritiSet != null) {
//            authorities.addAll(authoritiSet);
//        }
        return authorities;
    }

    private List<String> getAntPathAuthorities(String path) {
        List<String> authorities = new ArrayList<>();
        Map<String, String> rolesAntMap = redisCacheOperation.getAll(
                AuthConstant.RESOURCE_ROLES_ANT_MAP_KEY);
        Set<String> antRoleKeySet = rolesAntMap.keySet();
        PathMatcher pathMatcher = new AntPathMatcher();
        for (Object antRoleKey : antRoleKeySet) {
            String antPath = (String) antRoleKey;
            if (pathMatcher.match(antPath, path)) {
                Set<String> roleCodes = gson.fromJson(rolesAntMap.get(antRoleKey),
                        new TypeToken<HashSet<String>>() {
                        }.getType());
                authorities.addAll(roleCodes);
            }
        }
        return authorities;
    }
}
