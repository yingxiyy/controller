package net.flex.dci.otn.controller.auth.oauth2.handler;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.http.converter.OAuth2ErrorHttpMessageConverter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * @version 1.0
 * @date 2022/9/28 15:20
 */
@Slf4j
public class OAuth2CustomAuthErrorHandler implements AuthenticationFailureHandler {

    private final HttpMessageConverter<OAuth2Error> errorHttpResponseConverter;

    public OAuth2CustomAuthErrorHandler() {
        this.errorHttpResponseConverter = new OAuth2ErrorHttpMessageConverter();
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
            HttpServletResponse response, AuthenticationException e)
            throws IOException, ServletException {
        log.debug("on authentication failure ");
        if (e instanceof OAuth2AuthenticationException) {
            OAuth2Error error = ((OAuth2AuthenticationException) e).getError();
            ServletServerHttpResponse httpResponse = new ServletServerHttpResponse(response);
            httpResponse.setStatusCode(HttpStatus.BAD_REQUEST);
            this.errorHttpResponseConverter.write(error, (MediaType) null, httpResponse);
        }
    }
}
