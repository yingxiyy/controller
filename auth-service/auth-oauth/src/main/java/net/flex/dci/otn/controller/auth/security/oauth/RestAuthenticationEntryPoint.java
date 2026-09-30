//package net.flex.dci.otn.controller.auth.security.oauth;
//
//import java.io.IOException;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otc.common.exception.CommonException;
//import net.flex.dci.otc.common.exception.CommonExceptionType;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.web.AuthenticationEntryPoint;
//import org.springframework.stereotype.Component;
//
///**
// * @version 1.0
// * @date 2022/4/18 13:33
// */
//@Component
//@Slf4j
//public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
//
//    @Override
//    public void commence(HttpServletRequest httpServletRequest,
//            HttpServletResponse httpServletResponse, AuthenticationException e)
//            throws IOException, ServletException {
//        throw new CommonException(CommonExceptionType.AUTHORIZATION_ERROR, "unauthenticated!");
//    }
//}
