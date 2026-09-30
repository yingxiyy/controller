package net.flex.dci.otn.controller.auth.security.token;

import org.springframework.security.oauth2.server.authorization.config.TokenSettings;

/**
 * @version 1.0
 * @date 2022/4/24 23:11
 */
public interface OAuth2TokenSettings {

    TokenSettings getTokenSettings();
}
