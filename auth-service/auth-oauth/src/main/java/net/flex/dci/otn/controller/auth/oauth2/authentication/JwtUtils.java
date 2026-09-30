package net.flex.dci.otn.controller.auth.oauth2.authentication;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 2022/4/20 16:48
 */
public class JwtUtils {

    private JwtUtils() {

    }

    public static JWSHeader.Builder headers() {
        return new JWSHeader.Builder(JWSAlgorithm.RS256);
    }

    public static JWTClaimsSet.Builder accessTokenClaims(RegisteredClient registeredClient,
            String issuer, String subject, Set<String> authorizedScopes) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(registeredClient.getTokenSettings()
                .getAccessTokenTimeToLive());
        JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder();
        if (StringUtils.hasText(issuer)) {
            claimsBuilder.issuer(issuer);
        }
        claimsBuilder
                .subject(subject)
                .audience(Collections.singletonList(registeredClient.getClientId()))
                .issueTime(new Date(issuedAt.toEpochMilli()))
                .expirationTime(new Date(expiresAt.toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .notBeforeTime(new Date(issuedAt.toEpochMilli()));
        if (!CollectionUtils.isEmpty(authorizedScopes)) {
            claimsBuilder.claim(OAuth2ParameterNames.SCOPE, authorizedScopes);
        }
        // @formatter:on

        return claimsBuilder;
    }
}
