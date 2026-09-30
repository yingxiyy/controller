package net.flex.dci.otn.controller.auth.security.oauth.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.auth.oauth2.registered.client.OAuth2RegisteredClient;
import net.flex.dci.otn.controller.auth.security.oauth.OAuth2RegisteredClientService;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/4/24 17:18
 */
@Slf4j
@Service
public class OAuth2RegisteredClientServiceImpl implements OAuth2RegisteredClientService {

    private final List<OAuth2RegisteredClient> inMemoryOAuth2RegisteredClients;

    public OAuth2RegisteredClientServiceImpl(List<OAuth2RegisteredClient> oAuth2RegisteredClients) {
        this.inMemoryOAuth2RegisteredClients = oAuth2RegisteredClients;
    }

    @Override
    public List<RegisteredClient> getOAuth2RegisteredClient() {
        List<RegisteredClient> registeredClients = Collections.emptyList();
        if (!CollectionUtils.isEmpty(inMemoryOAuth2RegisteredClients)) {
            registeredClients = inMemoryOAuth2RegisteredClients.stream()
                    .map(oAuth2RegisteredClient -> oAuth2RegisteredClient.getRegisteredClient())
                    .collect(Collectors.toList());
        }
        return registeredClients;
    }
}
