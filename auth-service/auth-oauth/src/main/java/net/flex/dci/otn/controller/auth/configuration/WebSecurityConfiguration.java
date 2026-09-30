package net.flex.dci.otn.controller.auth.configuration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * @version 1.0
 * @date 2022/4/15 13:39
 */
@EnableWebSecurity
@Slf4j
//@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class WebSecurityConfiguration {


    private final UserDetailsService userDetailsService;


    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManagerBean(
            AuthenticationManagerBuilder authenticationManagerBuilder) throws Exception {
        authenticationManagerBuilder.userDetailsService(userDetailsService)
                .passwordEncoder(passwordEncoder()).and().eraseCredentials(true);

        return authenticationManagerBuilder.build();
    }

    //    @Override
//    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
//        auth.userDetailsService(userDetailsService).passwordEncoder(passwordEncoder());
//    }
//
//    @Override
//    public void configure(HttpSecurity httpSecurity) throws Exception {
//        httpSecurity.csrf().disable();
//        httpSecurity.antMatcher("/oauth2/**")
//                .authorizeRequests()
//                .antMatchers("/oauth2/**").permitAll()
//                .antMatchers("/rsa/publickey").permitAll()
//                .and().csrf().disable();
////
////        httpSecurity
////                .csrf().disable()
////                .anonymous().disable()
////                .authorizeRequests()
////                .antMatchers("/api-docs/**").permitAll();
////        httpSecurity.csrf().disable().authorizeRequests().requestMatchers(EndpointRequest)
//
//    }
}
