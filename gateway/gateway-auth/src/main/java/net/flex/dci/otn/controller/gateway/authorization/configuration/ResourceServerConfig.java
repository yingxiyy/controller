package net.flex.dci.otn.controller.gateway.authorization.configuration;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zkclient4boot.refactor.utils.DciInstancesUtils;
import net.flex.dci.otn.controller.gateway.authorization.AuthorizationManager;
import net.flex.dci.otn.controller.gateway.authorization.component.RestAuthenticationEntryPoint;
import net.flex.dci.otn.controller.gateway.authorization.component.RestfulAccessDeniedHandler;
import net.flex.dci.otn.controller.gateway.authorization.filter.IgnoreUrlsRemoveJwtFilter;
import net.flex.dci.otn.controller.gateway.common.utils.AuthConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;

/**
 * @version 1.0
 * @date 2022/4/18 9:59
 */
@RequiredArgsConstructor
@EnableWebFluxSecurity
@Slf4j
public class ResourceServerConfig {

    private final IgnoreUrlConfiguration ignoreUrlConfig;

    private final IgnoreUrlsRemoveJwtFilter ignoreUrlsRemoveJwtFilter;

    private final AuthorizationManager authorizationManager;

    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;

    private final RestfulAccessDeniedHandler restfulAccessDeniedHandler;

    //    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        List<InstanceDetails> instances = DciInstancesUtils.getStateInstancesByModuleName(
                AuthConstants.JWT_PRODUCER);
        if (CollectionUtils.isEmpty(instances)) {
            throw new IllegalArgumentException("No auth-service instance found");
        }
        int size = instances.size();
        Random r = new Random();
        int index = r.nextInt(size);
        InstanceDetails producer = instances.get(index);
        String host = CommonUtil.formateIpAddress(producer.getMyIp());
        String jwkSetUri =
                "http://" + host + ":" + producer.getPort() + "/rsa/publicKey";
        log.debug("to get publicKey from url:{}", jwkSetUri);
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }


    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        AtomicReference<ReactiveJwtDecoder> cache = new AtomicReference<>();
        AtomicReference<Long> lastRefreshTime = new AtomicReference<>(0L);
        long refreshIntervalMillis = 5 * 60 * 1000; // 5分钟刷新一次
        http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt
                        .jwtDecoder(token -> {
                            long now = System.currentTimeMillis();
                            ReactiveJwtDecoder delegate = cache.get();
                            if (delegate == null
                                    || now - lastRefreshTime.get() > refreshIntervalMillis) {
                                synchronized (cache) {
                                    delegate = cache.get();
                                    if (delegate == null || now - lastRefreshTime.get()
                                            > refreshIntervalMillis) {
                                        delegate = reactiveJwtDecoder();
                                        cache.set(delegate);
                                        lastRefreshTime.set(now);
                                    }
                                }
                            }
                            return delegate.decode(token);
                        })
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                )
                .authenticationEntryPoint(restAuthenticationEntryPoint)
        );
        http.addFilterBefore(ignoreUrlsRemoveJwtFilter, SecurityWebFiltersOrder.AUTHENTICATION);

        http.authorizeExchange()
                .pathMatchers(ignoreUrlConfig.getUrls().toArray(new String[0])).permitAll()
                .anyExchange().access(authorizationManager)
                .and()
                .exceptionHandling()
                .accessDeniedHandler(restfulAccessDeniedHandler)
                .authenticationEntryPoint(restAuthenticationEntryPoint)
                .and()
                .csrf().disable();
        return http.build();
    }


    @Bean
    public Converter<Jwt, ? extends Mono<? extends AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        jwtGrantedAuthoritiesConverter.setAuthorityPrefix(AuthConstants.JWT_AUTHORITY_PREFIX);
        jwtGrantedAuthoritiesConverter.setAuthoritiesClaimName(AuthConstants.AUTHORITY_CLAIM_NAME);
        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(
                jwtGrantedAuthoritiesConverter);
        return new ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter);

    }


}
