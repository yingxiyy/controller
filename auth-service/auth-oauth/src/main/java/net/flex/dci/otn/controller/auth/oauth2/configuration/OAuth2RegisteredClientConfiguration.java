package net.flex.dci.otn.controller.auth.oauth2.configuration;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.auth.security.oauth.OAuth2RegisteredClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * @version 1.0
 * @date 2022/4/24 17:10
 */
@Configuration
@Slf4j
@PropertySource("classpath:oauth2-registered-client.properties")
public class OAuth2RegisteredClientConfiguration {

    @Autowired
    private OAuth2RegisteredClientService oAuth2RegisteredClientService;

    @Bean
    public RegisteredClientRepository registeredClientRepository() {
        log.debug("init registered Client Repository");
        List<RegisteredClient> registeredClients = oAuth2RegisteredClientService.getOAuth2RegisteredClient();
//        RegisteredClientRepository registeredClientRepository = new InMemoryRegisteredClientRepository();
//        for (RegisteredClient registeredClient : registeredClients) {
//            registeredClientRepository.save(registeredClient);
//        }
        return new InMemoryRegisteredClientRepository(registeredClients);
    }
}
