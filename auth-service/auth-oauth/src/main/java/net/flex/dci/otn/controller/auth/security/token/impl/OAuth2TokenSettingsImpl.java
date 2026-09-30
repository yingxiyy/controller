package net.flex.dci.otn.controller.auth.security.token.impl;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.auth.properties.OAuth2TokenProperties;
import net.flex.dci.otn.controller.auth.security.token.OAuth2TokenSettings;
import org.springframework.security.oauth2.server.authorization.config.TokenSettings;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 2022/4/24 23:13
 */
@Component
@Slf4j
public class OAuth2TokenSettingsImpl implements OAuth2TokenSettings {

    private final long refreshTokenTime;

    private final long accessTokenTime;

    private final String refreshTokenTimeUnit;

    private final String accessTokenTimeUnit;


    public OAuth2TokenSettingsImpl(OAuth2TokenProperties oAuth2TokenProperties) {
        this.refreshTokenTime = oAuth2TokenProperties.getRefreshTokenTime();
        this.accessTokenTime = oAuth2TokenProperties.getAccessTokenTime();
        this.refreshTokenTimeUnit = oAuth2TokenProperties.getRefreshTokenTimeUnit();
        this.accessTokenTimeUnit = oAuth2TokenProperties.getAccessTokenTimeUnit();
    }

    @Override
    public TokenSettings getTokenSettings() {
        Duration accessTokenDuration = setTokenTime(accessTokenTimeUnit, accessTokenTime, 10);
        Duration refreshTokenDuration = setTokenTime(refreshTokenTimeUnit, refreshTokenTime, 120);
        TokenSettings.Builder tokenSettingsBuilder = TokenSettings.builder()
                .accessTokenTimeToLive(accessTokenDuration)
                .refreshTokenTimeToLive(refreshTokenDuration);
        TokenSettings tokenSettings = tokenSettingsBuilder.build();
        return tokenSettings;
    }

    private Duration setTokenTime(String tokenTimeUnit, long tokenTime, long durationMinutes) {
        Duration duration = Duration.ofMinutes(durationMinutes);
        if (StringUtils.hasText(tokenTimeUnit)) {
            switch (tokenTimeUnit.toUpperCase()) {
                case "M":
                case "MINUTE":
                case "MINUTES":
                    duration = Duration.ofMinutes(tokenTime);
                    break;
                case "H":
                case "HOUR":
                case "HOURS":
                    duration = Duration.ofHours(tokenTime);
                    break;
                case "D":
                case "DAY":
                case "DAYS":
                    duration = Duration.ofDays(tokenTime);
                    break;
                case "W":
                case "WEEK":
                case "WEEKS":
                    duration = Duration.of(tokenTime, ChronoUnit.WEEKS);
                    break;
            }
        }
        return duration;
    }
}
