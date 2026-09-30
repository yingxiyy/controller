package net.flex.dci.otn.controller.auth.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/4/18 16:34
 */
@Data
@Builder
public class Oauth2TokenDto implements Serializable {

    private String accessToken;

    private String refreshToken;

    private String tokenHeader;

    private int expiresIn;

    private String role;

}
