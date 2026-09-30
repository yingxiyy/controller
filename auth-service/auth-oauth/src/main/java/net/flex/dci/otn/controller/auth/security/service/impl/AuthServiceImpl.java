//package net.flex.dci.otn.controller.auth.security.service.impl;
//
//import java.util.concurrent.TimeUnit;
//import javax.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otc.cache.redis.core.RedisCacheOperation;
//import net.flex.dci.otn.controller.auth.dto.LoginDto;
//import net.flex.dci.otn.controller.auth.properties.JwtProperties;
//import net.flex.dci.otn.controller.auth.security.jwt.utils.JwtTokenUtil;
//import net.flex.dci.otn.controller.auth.security.service.AuthService;
//import net.flex.dci.otn.controller.auth.utils.Constants;
//import org.springframework.security.authentication.AuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.core.userdetails.UserDetails;
//import org.springframework.security.core.userdetails.UserDetailsService;
//import org.springframework.stereotype.Service;
//
///**
// * @version 1.0
// * @date 2022/4/17 21:30
// */
//@Service
//@Slf4j
//@RequiredArgsConstructor
//public class AuthServiceImpl implements AuthService {
//
//    private final AuthenticationManager authenticationManager;
//
//    private final UserDetailsService userDetailsService;
//
//    private final JwtTokenUtil jwtTokenUtil;
//
//    private final RedisCacheOperation cacheOperation;
//
//    private final JwtProperties jwtProperties;
//
//    @Override
//
//    public String login(LoginDto loginDto) {
//        String username = loginDto.getUsername();
//        String password = loginDto.getPassword();
//        log.debug("start to user:{} login", username);
//        UsernamePasswordAuthenticationToken upToken = new UsernamePasswordAuthenticationToken(
//                username, password);
//        final Authentication authentication = authenticationManager.authenticate(upToken);
//        SecurityContextHolder.getContext().setAuthentication(authentication);
//        final UserDetails userDetails = userDetailsService.loadUserByUsername(username);
//        final String token = jwtTokenUtil.generateToken(userDetails);
//        cacheOperation.set(token, authentication,
//                Math.toIntExact(jwtProperties.getTokenValidityInSeconds()), TimeUnit.SECONDS);
//        return token;
//    }
//
//    @Override
//    public void logout(HttpServletRequest httpServletRequest) {
//        log.debug("start to logout");
//        String authentication = httpServletRequest.getHeader(Constants.AUTHENTICATION_HEADER_NAME);
//        String token = authentication.replace(Constants.TOKEN_PREFIX, "");
//        cacheOperation.deleteKey(token);
//    }
//}
