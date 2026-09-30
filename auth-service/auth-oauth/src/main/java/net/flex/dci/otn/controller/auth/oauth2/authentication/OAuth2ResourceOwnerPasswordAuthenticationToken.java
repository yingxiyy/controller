package net.flex.dci.otn.controller.auth.oauth2.authentication;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.Version;
import org.springframework.util.Assert;

/**
 * @version 1.0
 * @date 2022/4/20 11:28
 */
public class OAuth2ResourceOwnerPasswordAuthenticationToken extends AbstractAuthenticationToken {

    private static final long serialVersionUID;

    static {
        serialVersionUID = Version.SERIAL_VERSION_UID;
    }

    private final AuthorizationGrantType authorizationGrantType;
    private final Authentication clientPrincipal;
    private final Set<String> scopes;
    private final Map<String, Object> additionalParameters;

    public OAuth2ResourceOwnerPasswordAuthenticationToken(
            AuthorizationGrantType authorizationGrantType,
            Authentication clientPrincipal, @Nullable Set<String> scopes,
            @Nullable Map<String, Object> additionalParameters) {
        super(Collections.emptyList());
        Assert.notNull(authorizationGrantType, "authorizationGrantType cannot be null");
        Assert.notNull(clientPrincipal, "clientPrincipal cannot be null");
        this.authorizationGrantType = authorizationGrantType;
        this.clientPrincipal = clientPrincipal;
        this.scopes = Collections.unmodifiableSet(
                scopes != null ? new HashSet<>(scopes) : Collections.emptySet());
        this.additionalParameters = Collections.unmodifiableMap(
                additionalParameters != null ? new HashMap<>(additionalParameters)
                        : Collections.emptyMap());

    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Object getPrincipal() {
        return this.clientPrincipal;
    }

    public AuthorizationGrantType getAuthorizationGrantType() {
        return this.authorizationGrantType;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    public Map<String, Object> getAdditionalParameters() {
        return additionalParameters;
    }
}
