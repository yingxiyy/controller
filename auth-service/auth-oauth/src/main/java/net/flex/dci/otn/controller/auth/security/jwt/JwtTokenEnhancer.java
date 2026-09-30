//package net.flex.dci.otn.controller.auth.security.jwt;
//
//import java.util.HashMap;
//import java.util.Map;
//import net.flex.dci.otn.controller.auth.dto.UserDto;
//import net.flex.dci.otn.controller.auth.utils.Constants;
//import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
//import org.springframework.security.oauth2.common.OAuth2AccessToken;
//import org.springframework.security.oauth2.provider.OAuth2Authentication;
//import org.springframework.security.oauth2.provider.token.TokenEnhancer;
//
///**
// * @version 1.0
// * @date 2022/4/18 13:28
// */
//public class JwtTokenEnhancer implements TokenEnhancer {
//
//    @Override
//    public OAuth2AccessToken enhance(OAuth2AccessToken oAuth2AccessToken,
//            OAuth2Authentication oAuth2Authentication) {
//        UserDto userDto = (UserDto) oAuth2Authentication.getPrincipal();
//        Map<String, Object> info = new HashMap<>();
//        info.put(Constants.USER_ID, userDto.getId());
//        info.put(Constants.USER_NAME, userDto.getUsername());
//        ((DefaultOAuth2AccessToken) oAuth2AccessToken).setAdditionalInformation(info);
//        return oAuth2AccessToken;
//    }
//}
