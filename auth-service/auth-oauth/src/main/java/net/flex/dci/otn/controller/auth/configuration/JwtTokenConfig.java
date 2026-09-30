//package net.flex.dci.otn.controller.auth.configuration;
//
//import java.security.KeyPair;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otn.controller.auth.security.jwt.JwtTokenEnhancer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.security.oauth2.provider.token.TokenEnhancer;
//import org.springframework.security.oauth2.provider.token.TokenStore;
//import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
//import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
//import org.springframework.security.rsa.crypto.KeyStoreKeyFactory;
//
///**
// * @version 1.0
// * @date 2022/4/18 13:25
// */
////@Configuration
//@Slf4j
//public class JwtTokenConfig {
//
//    @Bean
//    public TokenStore jwtTokenStore() {
//        return new JwtTokenStore(jwtAccessTokenConverter());
//    }
//
//    @Bean
//    public JwtAccessTokenConverter jwtAccessTokenConverter() {
//        JwtAccessTokenConverter accessTokenConverter = new JwtAccessTokenConverter();
//        accessTokenConverter.setSigningKey("dci_plus");
//
//        accessTokenConverter.setKeyPair(keyPair());
//        return accessTokenConverter;
//
//    }
//
//    @Bean
//    public TokenEnhancer jwtTokenEnhancer() {
//        return new JwtTokenEnhancer();
//    }
//
//
//    @Bean
//    public KeyPair keyPair() {
//        KeyStoreKeyFactory keyStoreKeyFactory = new KeyStoreKeyFactory(
//                new ClassPathResource("jwt.jks"),
//                "dciflex".toCharArray());
//        return keyStoreKeyFactory.getKeyPair("jwt", "flexdci".toCharArray());
//    }
//}
