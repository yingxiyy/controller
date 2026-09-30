package net.flex.dci.otn.controller.auth.oauth2.authentication;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimNames;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPair;
import java.security.Principal;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.auth.dto.UserDto;
import net.flex.dci.otn.controller.auth.utils.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.core.OAuth2TokenType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenCustomizer;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.config.ProviderSettings;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/4/20 15:33
 */
@Slf4j
public class OAuth2ResourceOwnerPasswordAuthenticationProvider implements AuthenticationProvider {

    private static final StringKeyGenerator DEFAULT_REFRESH_TOKEN_GENERATOR = new Base64StringKeyGenerator(
            Base64.getUrlEncoder().withoutPadding(), 96);


    private final AuthenticationManager authenticationManager;

    private final OAuth2AuthorizationService authorizationService;

    private final KeyPair keyPair;

    private OAuth2TokenCustomizer<JWTEncodingContext> jwtCustomizer = (context) -> {
    };

    private Supplier<String> refreshTokenGenerator = DEFAULT_REFRESH_TOKEN_GENERATOR::generateKey;

    private ProviderSettings providerSettings;

    public OAuth2ResourceOwnerPasswordAuthenticationProvider(
            AuthenticationManager authenticationManager,
            OAuth2AuthorizationService authorizationService,
            KeyPair keyPair) {
        Assert.notNull(authorizationService, "authorizationService cannot be null");
        this.authenticationManager = authenticationManager;
        this.authorizationService = authorizationService;
        this.keyPair = keyPair;
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {
        log.debug("authentication");
        OAuth2ResourceOwnerPasswordAuthenticationToken resouceOwnerPasswordAuthentication = (OAuth2ResourceOwnerPasswordAuthenticationToken) authentication;
        OAuth2ClientAuthenticationToken clientPrincipal = getAuthenticatedClientElseThrowInvalidClient(
                resouceOwnerPasswordAuthentication);
        RegisteredClient registeredClient = clientPrincipal.getRegisteredClient();
        if (!registeredClient.getAuthorizationGrantTypes()
                .contains(AuthorizationGrantType.PASSWORD)) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNAUTHORIZED_CLIENT);
        }
        Map<String, Object> additionalParams = resouceOwnerPasswordAuthentication.getAdditionalParameters();
        String username = (String) additionalParams.get(OAuth2ParameterNames.USERNAME);
        String password = (String) additionalParams.get(OAuth2ParameterNames.PASSWORD);
        username = new String(Base64.getDecoder().decode(username));
        password = new String(Base64.getDecoder().decode(password));
        try {
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                    username, password);
            log.debug("got username password authentication token="
                    + usernamePasswordAuthenticationToken);
            Authentication userAuthentication = authenticationManager.authenticate(
                    usernamePasswordAuthenticationToken);
            Set<String> authorizedScopes = registeredClient.getScopes();
            if (!CollectionUtils.isEmpty(resouceOwnerPasswordAuthentication.getScopes())) {
                Set<String> unauthorizedScopes = resouceOwnerPasswordAuthentication.getScopes()
                        .stream()
                        .filter(requestedScope -> !registeredClient.getScopes()
                                .contains(requestedScope))
                        .collect(Collectors.toSet());
                if (!CollectionUtils.isEmpty(unauthorizedScopes)) {
                    throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_SCOPE);
                }

                authorizedScopes = new LinkedHashSet<>(
                        resouceOwnerPasswordAuthentication.getScopes());
            }

            String issuer =
                    this.providerSettings != null ? this.providerSettings.getIssuer() : null;
            JWSHeader.Builder headersBuilder = JwtUtils.headers();
            JWTClaimsSet.Builder claimSetBuilder = JwtUtils.accessTokenClaims(registeredClient,
                    issuer, userAuthentication.getName(), authorizedScopes);

//            //HEADER BUILDER
            JWTEncodingContext context = JWTEncodingContext.with(headersBuilder, claimSetBuilder)
                    .registeredClient(registeredClient)
                    .principal(userAuthentication)
                    .authorizedScopes(authorizedScopes)
                    .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                    .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                    .authorizationGrant(resouceOwnerPasswordAuthentication)
                    .build();
            this.jwtCustomizer.customize(context);
            JWTClaimsSet claims = context.getClaims().build();

            JWSSigner signer = new RSASSASigner(this.keyPair.getPrivate());
            SignedJWT jwtAccessToken = new SignedJWT(
                    headersBuilder.build(), claimSetBuilder.build());
            jwtAccessToken.sign(signer);
            authorizedScopes = (Set<String>) claims.getClaim(OAuth2ParameterNames.SCOPE);
            OAuth2AccessToken accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    jwtAccessToken.serialize(),
                    claims.getIssueTime().toInstant(),
                    claims.getExpirationTime().toInstant(),
                    authorizedScopes);
            OAuth2RefreshToken refreshToken = null;
            if (registeredClient.getAuthorizationGrantTypes()
                    .contains(AuthorizationGrantType.REFRESH_TOKEN)) {
                refreshToken = generateRefreshToken(
                        registeredClient.getTokenSettings().getRefreshTokenTimeToLive());
            }

            OAuth2Authorization.Builder
                    authorizationBuilder = OAuth2Authorization.withRegisteredClient(
                            registeredClient)
                    .principalName(userAuthentication.getName())
                    .authorizationGrantType(AuthorizationGrantType.PASSWORD)
                    .token(accessToken,
                            (metadata) ->
                            {
                                try {
                                    metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME,
                                            jwtAccessToken.getJWTClaimsSet().getClaims());
                                } catch (ParseException e) {
                                    log.error("failed to parse the jwt claims");
                                }

                            })
                    .attribute(OAuth2Authorization.AUTHORIZED_SCOPE_ATTRIBUTE_NAME,
                            authorizedScopes)
                    .attribute(Principal.class.getName(), usernamePasswordAuthenticationToken);
            this.authorizationService.save(authorizationBuilder.build());

            if (refreshToken != null) {
                authorizationBuilder.refreshToken(refreshToken);
            }

            Map<String, Object> tokenAdditionalParameters = new HashMap<>();
            claims.getClaims().forEach((key, value) -> {
                if (!key.equals(OAuth2ParameterNames.SCOPE) &&
                        !key.equals(JWTClaimNames.ISSUED_AT) &&
                        !key.equals(JWTClaimNames.EXPIRATION_TIME) &&
                        !key.equals(JWTClaimNames.NOT_BEFORE) &&
                        !key.equals(Constants.AUTHORITIES)) {
                    tokenAdditionalParameters.put(key, value);
                }
            });
            UserDto userDto = (UserDto) userAuthentication.getPrincipal();

            tokenAdditionalParameters.put("roles", userDto.getRoles());
            log.debug("returning OAuth2AccessTokenAuthenticationToken");

            return new OAuth2AccessTokenAuthenticationToken(registeredClient,
                    clientPrincipal, accessToken, refreshToken, tokenAdditionalParameters);
        } catch (Exception ex) {
            log.error("problem in authenticate", ex);
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR, ex.getMessage(), null),
                    ex);
        }

    }


    @Override
    public boolean supports(Class<?> authentication) {
        boolean supports = OAuth2ResourceOwnerPasswordAuthenticationToken.class.isAssignableFrom(
                authentication);
        log.debug("supports authentication=" + authentication + " returning " + supports);
        return supports;
    }

    /**
     * generateRefresh token
     *
     * @param refreshTokenTimeToLive
     * @return
     */
    private OAuth2RefreshToken generateRefreshToken(Duration refreshTokenTimeToLive) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(refreshTokenTimeToLive);
        return new OAuth2RefreshToken(this.refreshTokenGenerator.get(), issuedAt, expiresAt);
    }

    private OAuth2ClientAuthenticationToken getAuthenticatedClientElseThrowInvalidClient(
            Authentication authentication) {

        OAuth2ClientAuthenticationToken clientPrincipal = null;

        if (OAuth2ClientAuthenticationToken.class.isAssignableFrom(
                authentication.getPrincipal().getClass())) {
            clientPrincipal = (OAuth2ClientAuthenticationToken) authentication.getPrincipal();
        }

        if (clientPrincipal != null && clientPrincipal.isAuthenticated()) {
            return clientPrincipal;
        }

        throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
    }

    @Autowired
    public void setProviderSettings(ProviderSettings providerSettings) {
        this.providerSettings = providerSettings;
    }

    @Autowired
    public void setJwtCustomizer(OAuth2TokenCustomizer<JWTEncodingContext> jwtCustomizer) {
        this.jwtCustomizer = jwtCustomizer;
    }
}
