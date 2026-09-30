package net.flex.dci.otn.controller.auth.security.oauth;

import java.util.List;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * @version 1.0
 * @date 2022/4/24 17:16
 */
public interface OAuth2RegisteredClientService {

    List<RegisteredClient> getOAuth2RegisteredClient();
}
