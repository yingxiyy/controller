/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.sftp.manager.configuration;

import net.flex.dci.otn.controller.sftp.manager.interceptor.FtpServerNetConfInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/9/8 14:33
 */
@Configuration
public class SftpWebConfig extends WebMvcConfigurationSupport {

    @Autowired
    private FtpServerNetConfInterceptor ftpServerNetConfInterceptor;

    @Override
    protected void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ftpServerNetConfInterceptor)
                .addPathPatterns("/restconf/config/ftp-server:**", "/restconf/config/ftp-server:ftp-servers/**");

    }
}
