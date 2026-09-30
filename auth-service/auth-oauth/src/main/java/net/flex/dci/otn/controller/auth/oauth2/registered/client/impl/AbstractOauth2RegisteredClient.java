package net.flex.dci.otn.controller.auth.oauth2.registered.client.impl;

import static net.flex.dci.otn.controller.auth.utils.Constants.OAUTH2_REGISTERED_CLIENT;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.auth.oauth2.registered.client.OAuth2RegisteredClient;
import net.flex.dci.otn.controller.auth.security.token.OAuth2TokenSettings;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.server.authorization.config.TokenSettings;

/**
 * @version 1.0
 * @date 2022/4/24 23:07
 */

@Slf4j
public abstract class AbstractOauth2RegisteredClient implements OAuth2RegisteredClient {

    protected final Environment environment;

    protected final OAuth2TokenSettings oAuth2TokenSettings;

    protected AbstractOauth2RegisteredClient(Environment env,
            OAuth2TokenSettings oAuth2TokenSettings) {
        this.environment = env;
        this.oAuth2TokenSettings = oAuth2TokenSettings;
    }

    protected String getClientProperty(String client, String property) {
        log.debug("in getClient property");
        String propertyName = String.format("%s.%s.%s", OAUTH2_REGISTERED_CLIENT, client, property);
        String propertyValue = environment.getProperty(propertyName);
        return propertyValue;
    }


    protected TokenSettings getTokenSettings() {
        return oAuth2TokenSettings.getTokenSettings();
    }
}
