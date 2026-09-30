/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.tools.lifecycle.configuration;

import net.flex.dci.otn.controller.tools.lifecycle.LifecycleDispatcher;
import net.flex.dci.otn.controller.tools.lifecycle.interceptor.HttpMethodInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewFilter;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.web.context.request.WebRequestInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/16 11:13
 */

@Configuration(
        proxyBeanMethods = false
)
@ConditionalOnWebApplication(
        type = Type.SERVLET
)
@ConditionalOnClass({WebMvcConfigurer.class, HttpMethodInterceptor.class,
        OpenEntityManagerInViewInterceptor.class,
        OpenEntityManagerInViewFilter.class})
public class LifecycleInterceptorConfiguration {

//    @Bean
//    public LifecycleDispatcher lifecycleDispatcher() {
//        return new LifecycleDispatcher();
//    }

    @Bean
    public HttpMethodInterceptor httpMethodInterceptor(WebRequestInterceptor webRequestInterceptor,
            LifecycleDispatcher lifecycleDispatcher) {
        return new HttpMethodInterceptor(webRequestInterceptor, lifecycleDispatcher);
    }

    @Bean
    public WebMvcConfigurer httpMethodInterceptorWebMvcConfigurer(
            HttpMethodInterceptor httpMethodInterceptor) {
        return new WebMvcConfigurer() {
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(httpMethodInterceptor);
            }
        };
    }
}
