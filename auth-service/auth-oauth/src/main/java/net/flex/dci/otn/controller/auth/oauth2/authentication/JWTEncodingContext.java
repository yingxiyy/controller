package net.flex.dci.otn.controller.auth.oauth2.authentication;

import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenContext;
import org.springframework.util.Assert;

/**
 * @version 1.0
 * @date 2022/4/21 10:10
 */
public class JWTEncodingContext implements OAuth2TokenContext {

    private final Map<Object, Object> context;

    private JWTEncodingContext(Map<Object, Object> context) {
        this.context = Collections.unmodifiableMap(new HashMap(context));
    }

    @Nullable
    public <V> V get(Object key) {
        return this.hasKey(key) ? (V) this.context.get(key) : null;
    }

    @Override
    public boolean hasKey(Object key) {
        Assert.notNull(key, "key cannot be null");
        return this.context.containsKey(key);
    }


    public JWSHeader.Builder getHeaders() {
        return (JWSHeader.Builder) this.get(JWSHeader.Builder.class);
    }

    public JWTClaimsSet.Builder getClaims() {
        return (JWTClaimsSet.Builder) this.get(
                JWTClaimsSet.Builder.class);
    }

    public static JWTEncodingContext.Builder with(
            JWSHeader.Builder headersBuilder,
            JWTClaimsSet.Builder claimsBuilder) {
        return new JWTEncodingContext.Builder(headersBuilder, claimsBuilder);
    }

    public static final class Builder extends
            AbstractBuilder<JWTEncodingContext, JWTEncodingContext.Builder> {

        private Builder(JWSHeader.Builder headersBuilder,
                JWTClaimsSet.Builder claimsBuilder) {
            Assert.notNull(headersBuilder, "headersBuilder cannot be null");
            Assert.notNull(claimsBuilder, "claimsBuilder cannot be null");
            this.put(JWSHeader.Builder.class,
                    headersBuilder);
            this.put(JWTClaimsSet.Builder.class,
                    claimsBuilder);
        }


        public JWTEncodingContext build() {
            return new JWTEncodingContext(this.getContext());
        }
    }
}
