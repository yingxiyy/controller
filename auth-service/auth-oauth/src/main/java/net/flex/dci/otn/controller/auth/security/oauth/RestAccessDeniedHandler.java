//package net.flex.dci.otn.controller.auth.security.oauth;
//
//import java.io.IOException;
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otc.common.exception.CommonException;
//import net.flex.dci.otc.common.exception.CommonExceptionType;
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.security.web.access.AccessDeniedHandler;
//import org.springframework.stereotype.Component;
//
///**
// * @version 1.0
// * @date 2022/4/18 13:33
// */
//@Component
//@Slf4j
//public class RestAccessDeniedHandler implements AccessDeniedHandler {
//
//    @Override
//    public void handle(HttpServletRequest httpServletRequest,
//            HttpServletResponse httpServletResponse, AccessDeniedException e)
//            throws IOException, ServletException {
//        throw new CommonException(CommonExceptionType.NO_PERMISSION, "ACCESS FORBIDDEN");
//    }
//}
