//package net.flex.dci.otn.controller.auth.configuration;
//
//import javax.annotation.Resource;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otn.controller.auth.properties.JwtProperties;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
//import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
//import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
//import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
//import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
//import org.springframework.security.oauth2.provider.token.TokenStore;
//import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
//
///**
// * @version 1.0
// * @date 2022/4/16 19:32
// */
//
////@Configuration
//@Slf4j
//@EnableAuthorizationServer
//public class Oauth2ServerConfiguration extends AuthorizationServerConfigurerAdapter {
//
//    @Resource
//    AuthenticationManager authenticationManager;
//
//    @Resource
//    UserDetailsService userDetailsService;
//
//    @Resource
//    TokenStore jwtTokenStore;
//
//    @Resource
//    JwtAccessTokenConverter jwtAccessTokenConverter;
//
//    @Resource
//    JwtProperties jwtProperties;
//
//
//    @Override
//    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
//        String clientId = "client_id";
//        String clientSecret = "123";
//        clients.inMemory()
//
//                .withClient(clientId)
//
//                .authorizedGrantTypes("password", "refresh_token")
//
//                .accessTokenValiditySeconds(
//                        Math.toIntExact(jwtProperties.getTokenValidityInSeconds()))
//                .refreshTokenValiditySeconds(60 * 60 * 2)
//                .resourceIds("rid")
//
//                .scopes("all")
//
//                .secret(new BCryptPasswordEncoder().encode(clientSecret));
//    }
//
//    @Override
//    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {
//        endpoints.tokenStore(jwtTokenStore)
//                .authenticationManager(authenticationManager)
//                .userDetailsService(userDetailsService)
//                .accessTokenConverter(jwtAccessTokenConverter);
//    }
//
//    @Override
//    public void configure(AuthorizationServerSecurityConfigurer security) throws Exception {
//
//        security.allowFormAuthenticationForClients();
//    }
//
//}
