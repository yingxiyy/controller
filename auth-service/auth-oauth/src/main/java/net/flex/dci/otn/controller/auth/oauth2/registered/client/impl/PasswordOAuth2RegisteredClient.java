package net.flex.dci.otn.controller.auth.oauth2.registered.client.impl;

import static net.flex.dci.otn.controller.auth.utils.Constants.ID;
import static net.flex.dci.otn.controller.auth.utils.Constants.NAME;
import static net.flex.dci.otn.controller.auth.utils.Constants.SCOPE;
import static net.flex.dci.otn.controller.auth.utils.Constants.SECRET;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.auth.security.token.OAuth2TokenSettings;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.config.TokenSettings;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/24 22:49
 */
@Component
@Slf4j
public class PasswordOAuth2RegisteredClient extends AbstractOauth2RegisteredClient {

    private final static String PASSWORD_CLIENT = "password";

    protected PasswordOAuth2RegisteredClient(Environment env,
            OAuth2TokenSettings oAuth2TokenSettings) {
        super(env, oAuth2TokenSettings);
    }

    @Override
    public RegisteredClient getRegisteredClient() {
        log.debug("In PasswordOAuth2RegisteredClient.getRegisteredClient()");

        String passwordClientId = getClientProperty(PASSWORD_CLIENT, ID);
        String passwordClientName = getClientProperty(PASSWORD_CLIENT, NAME);
        String passwordClientSecret = getClientProperty(PASSWORD_CLIENT, SECRET);
        String scope = getClientProperty(PASSWORD_CLIENT, SCOPE);
        TokenSettings tokenSettings = getTokenSettings();
        RegisteredClient passwordRegisteredClient = RegisteredClient.withId("1")
                .clientId(passwordClientId)
                .clientName(passwordClientName)
                .clientSecret(new BCryptPasswordEncoder().encode(passwordClientSecret))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .tokenSettings(tokenSettings)
                .scope(scope)
                .build();
        return passwordRegisteredClient;
    }
}
