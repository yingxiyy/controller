/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.email.service;

import com.sun.mail.util.MailSSLSocketFactory;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.system.config.common.exception.AlarmMailException;
import net.flex.dci.otn.controller.system.config.email.dto.MailSenderConfigData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.Properties;

@Service
@Slf4j
public class MailSenderService {

    private JavaMailSenderImpl mailSender;


    @Autowired
    private MailConfigureService mailConfigureService;

    public JavaMailSender getEmailSender() throws AlarmMailException, GeneralSecurityException {
        if (mailSender == null) {
            MailSenderConfigData mailSenderConfigData = mailConfigureService.getMailSenderConfigData();
            if (mailSenderConfigData == null) {
                String msg = "Failed to get mail sender, because SMTP not configured.";
                log.error(msg);
                throw new AlarmMailException(msg);
            }
            mailSender = new JavaMailSenderImpl();
            configMailSender(mailSenderConfigData);
//            props.put("mail.debug", "true");
        }
        return mailSender;
    }

    private void configMailSender(MailSenderConfigData mailSenderConfigData)
            throws GeneralSecurityException {
        mailSender.setHost(mailSenderConfigData.getSmtpHost());
        mailSender.setPort(mailSenderConfigData.getSmtpPort());
        mailSender.setUsername(mailSenderConfigData.getUser());
        mailSender.setPassword(mailSenderConfigData.getPassword());

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        if (mailSenderConfigData.getEnableSSL()) {
            MailSSLSocketFactory sf = new MailSSLSocketFactory();
            sf.setTrustAllHosts(true);
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.ssl.socketFactory", sf);
        } else {
            props.put("mail.smtp.starttls.enable", "false");
        }

        props.put("mail.smtp.connectiontimeout", mailSenderConfigData.getTimeOut());
        props.put("mail.smtp.timeout", mailSenderConfigData.getTimeOut());
        props.put("mail.smtp.writetimeout", mailSenderConfigData.getTimeOut());

        if (mailSenderConfigData.isEnableDebug()) {
            props.put("mail.debug", "true");
        } else {
            props.put("mail.debug", "false");
        }
        if (mailSenderConfigData.getProxyHost() != null
                && mailSenderConfigData.getProxyPort() != null) {
            props.setProperty("mail.smtp.proxy.host", mailSenderConfigData.getProxyHost());
            props.setProperty("mail.smtp.proxy.port", mailSenderConfigData.getProxyPort());
        } else {
            props.remove("mail.smtp.proxy.host");
            props.remove("mail.smtp.proxy.port");
        }

    }

    public void updateMailSender(MailSenderConfigData mailSenderConfigData)
            throws GeneralSecurityException {
        if (mailSender == null) {
            mailSender = new JavaMailSenderImpl();
        }
        configMailSender(mailSenderConfigData);
    }
}
