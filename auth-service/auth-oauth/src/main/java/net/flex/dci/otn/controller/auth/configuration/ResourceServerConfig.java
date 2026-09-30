//package net.flex.dci.otn.controller.auth.configuration;
//
//import lombok.RequiredArgsConstructor;
//import net.flex.dci.otn.controller.auth.security.oauth.RestAccessDeniedHandler;
//import net.flex.dci.otn.controller.auth.security.oauth.RestAuthenticationEntryPoint;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
//import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
//import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
//import org.springframework.security.oauth2.provider.error.OAuth2AccessDeniedHandler;
//
///**
// * @version 1.0
// * @date 2022/4/18 13:31
// */
////@Configuration
//@EnableResourceServer
//@RequiredArgsConstructor
//public class ResourceServerConfig extends ResourceServerConfigurerAdapter {
//
//    private static final String RESOURCE_ID = "rid";
//
//    private final RestAccessDeniedHandler restAccessDeniedHandler;
//
//    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
//
//    @Override
//    public void configure(HttpSecurity http) throws Exception {
//        http.authorizeRequests().antMatchers("/admin/**").hasAnyRole("admin", "Admin");
//        http.anonymous().disable()
//                .csrf().disable()
//                .authorizeRequests()
//                .antMatchers("/users/**").access("hasRole('ADMIN')")
//                .and().exceptionHandling().accessDeniedHandler(new OAuth2AccessDeniedHandler());
//
//        http.exceptionHandling().accessDeniedHandler(restAccessDeniedHandler);
//    }
//
//
//    @Override
//    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
//        resources.resourceId(RESOURCE_ID).stateless(false)
//                .authenticationEntryPoint(restAuthenticationEntryPoint);
//    }
//}
