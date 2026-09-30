package net.flex.dci.otn.controller.auth.security.jwt.impl;

import static net.flex.dci.otn.controller.auth.utils.Constants.USER_ID;

import com.nimbusds.jwt.JWTClaimsSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.auth.dto.UserDto;
import net.flex.dci.otn.controller.auth.oauth2.authentication.JWTEncodingContext;
import net.flex.dci.otn.controller.auth.security.jwt.JWTCustomizer;
import net.flex.dci.otn.controller.auth.utils.Constants;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/4/21 13:20
 */
@Slf4j
@Component
public class JWTCustomizerServiceImpl implements JWTCustomizer {

    @Override
    public void customizeToken(JWTEncodingContext context) {
        AbstractAuthenticationToken token = null;

        Authentication authenticataion = SecurityContextHolder.getContext().getAuthentication();

        if (authenticataion instanceof OAuth2ClientAuthenticationToken) {
            token = (OAuth2ClientAuthenticationToken) authenticataion;
        }
        if (token != null) {
            if (token.isAuthenticated() && OAuth2TokenType.ACCESS_TOKEN.equals(
                    context.getTokenType())) {
                Authentication authentication = context.getPrincipal();
                if (authentication != null) {
                    if (authentication instanceof UsernamePasswordAuthenticationToken) {
                        UserDto userPrincipal = (UserDto) authentication.getPrincipal();
                        Long userId = userPrincipal.getId();
                        Set<String> authorities = userPrincipal.getAuthorities().stream()
                                .map(GrantedAuthority::getAuthority)
                                .collect(Collectors.toSet());
                        Map<String, Object> userAttributes = new HashMap<>();
                        userAttributes.put(USER_ID, userId);
                        //add custom properties
                        JWTClaimsSet.Builder JWTClaimSetBuilder = context.getClaims();
                        if (!CollectionUtils.isEmpty(authorities)) {
                            JWTClaimSetBuilder.claim(Constants.AUTHORITIES, authorities);
                        }

                        //add custom properties
                        userAttributes.forEach(JWTClaimSetBuilder::claim);
                    }
                }
            }
        }

    }
}
