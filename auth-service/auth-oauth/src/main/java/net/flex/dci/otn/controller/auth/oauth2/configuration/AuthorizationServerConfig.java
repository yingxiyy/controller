package net.flex.dci.otn.controller.auth.oauth2.configuration;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.auth.oauth2.authentication.JWTEncodingContext;
import net.flex.dci.otn.controller.auth.oauth2.authentication.OAuth2ResourceOwnerPasswordAuthenticationConverter;
import net.flex.dci.otn.controller.auth.oauth2.authentication.OAuth2ResourceOwnerPasswordAuthenticationProvider;
import net.flex.dci.otn.controller.auth.security.jwt.JWTCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.config.ProviderSettings;
import org.springframework.security.oauth2.server.authorization.web.authentication.DelegatingAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ClientCredentialsAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2RefreshTokenAuthenticationConverter;
import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.RequestMatcher;

/**
 * @version 1.0
 * @date 2022/4/20 10:29
 */
@EnableWebSecurity
@Configuration
@Slf4j
public class AuthorizationServerConfig {

    @Autowired
    private JWTCustomizer jwtCustomizer;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationSecurityFilterChain(HttpSecurity http)
            throws Exception {
        OAuth2AuthorizationServerConfigurer<HttpSecurity> auth2AuthorizationServerConfigurer = new OAuth2AuthorizationServerConfigurer<>();
        http.apply(auth2AuthorizationServerConfigurer.tokenEndpoint(
                (tokenEndpoint) -> tokenEndpoint.accessTokenRequestConverter(
                        new DelegatingAuthenticationConverter(Arrays.asList(
                                new OAuth2AuthorizationCodeAuthenticationConverter(),
                                new OAuth2RefreshTokenAuthenticationConverter(),
                                new OAuth2ClientCredentialsAuthenticationConverter(),
                                new OAuth2ResourceOwnerPasswordAuthenticationConverter()
                        ))
                )));
        RequestMatcher endPointsMatcher = auth2AuthorizationServerConfigurer.getEndpointsMatcher();
        http.requestMatcher(endPointsMatcher)
                .authorizeRequests(
                        authorizeRequests -> authorizeRequests.anyRequest().authenticated())
                .csrf().disable()

                .apply(auth2AuthorizationServerConfigurer);
        SecurityFilterChain securityFilterChain = http.formLogin(Customizer.withDefaults()).build();

        addCustomOAuth2ResourceOwnerPasswordAuthenticationProvider(http);
        return securityFilterChain;
    }


    @Bean
    public KeyPair keyPair() {
        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(
                new ClassPathResource("jwt.jks"),
                "dciflex".toCharArray());
        return keyStoreKeyFactory.getKeyPair("jwt", "flexdci".toCharArray());
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(KeyPair keyPair) {
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey).privateKey(privateKey)
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

//    @Bean
//    public RegisteredClientRepository registeredClientRepository() {
//        RegisteredClient client = RegisteredClient.withId("DCI")
//                .clientId("DCI")
//                .clientSecret(new BCryptPasswordEncoder().encode("DCIWORLD"))
//                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
//                .authorizationGrantTypes(authorizationGrantTypes -> {
//                    authorizationGrantTypes.add(AuthorizationGrantType.PASSWORD);
//                    authorizationGrantTypes.add(AuthorizationGrantType.REFRESH_TOKEN);
//                })
//                .scope("all")
//                .build();
//        return new InMemoryRegisteredClientRepository(client);
//    }

    @Bean
    public ProviderSettings providerSettings() {
        return ProviderSettings.builder().tokenEndpoint("/oauth2/token").build();
    }

    private void addCustomOAuth2ResourceOwnerPasswordAuthenticationProvider(HttpSecurity http) {
        AuthenticationManager authenticationManager = http.getSharedObject(
                AuthenticationManager.class);
        ProviderSettings providerSettings = http.getSharedObject(ProviderSettings.class);
//        http.authenticationProvider();
        OAuth2AuthorizationService authorizationService = http.getSharedObject(
                OAuth2AuthorizationService.class);

        OAuth2TokenCustomizer<JWTEncodingContext> jwtCustomizer = buildCustomizer();

        OAuth2ResourceOwnerPasswordAuthenticationProvider resourceOwnerPasswordAuthenticationProvider =
                new OAuth2ResourceOwnerPasswordAuthenticationProvider(authenticationManager,
                        authorizationService, keyPair());
        if (jwtCustomizer != null) {
            resourceOwnerPasswordAuthenticationProvider.setJwtCustomizer(jwtCustomizer);
        }
//
        resourceOwnerPasswordAuthenticationProvider.setProviderSettings(providerSettings);

        // This will add new authentication provider in the list of existing authentication providers.
        http.authenticationProvider(resourceOwnerPasswordAuthenticationProvider);
    }

    @Bean
    public OAuth2TokenCustomizer<JWTEncodingContext> buildCustomizer() {
        OAuth2TokenCustomizer<JWTEncodingContext> customizer = (context) -> {
            jwtCustomizer.customizeToken(context);
        };
        return customizer;
    }

}
