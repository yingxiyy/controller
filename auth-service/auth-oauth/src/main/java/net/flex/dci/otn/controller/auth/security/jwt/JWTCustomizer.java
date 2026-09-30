package net.flex.dci.otn.controller.auth.security.jwt;

import net.flex.dci.otn.controller.auth.oauth2.authentication.JWTEncodingContext;

/**
 * @version 1.0
 * @date 2022/4/21 13:18
 */
public interface JWTCustomizer {

    void customizeToken(JWTEncodingContext jwtEncodingContext);
}
