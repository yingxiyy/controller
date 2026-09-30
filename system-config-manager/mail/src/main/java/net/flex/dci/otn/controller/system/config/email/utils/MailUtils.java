/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.email.utils;


import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.mdoel.mail.MailServerConfiguration;
import net.flex.dci.otn.controller.system.config.common.model.ToopAlarm;
import net.flex.dci.otn.controller.system.config.common.utils.i18n.I18nAlarmUtils;
import net.flex.dci.otn.controller.system.config.common.utils.i18n.I18nMailUtils;
import net.flex.dci.otn.controller.system.config.email.dto.MailSenderConfigData;
import net.flex.dci.otn.controller.system.config.translator.AlarmInfoTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class MailUtils {


    @Autowired
    private AlarmInfoTranslator alarmInfoTranslator;

    public static String getHtml(ToopAlarm alarm) {
        return null;
    }

    public static MailSenderConfigData mailConfigurationData2Ui(
            MailServerConfiguration mailServerConfiguration) {
        MailSenderConfigData mailSenderConfigData = MailSenderConfigData.builder()
                .enableSSL(mailServerConfiguration.getEnableSSL())
                .password(mailServerConfiguration.getPassword())
                .user(mailServerConfiguration.getUser())
                .smtpPort(mailServerConfiguration.getSmtpPort())
                .smtpHost(mailServerConfiguration.getSmtpHost())
                .build();
        return mailSenderConfigData;
    }

    public static MailServerConfiguration mailConfigurationData2Db(
            MailSenderConfigData mailSenderConfigData) {
        MailServerConfiguration mailServerConfiguration = MailServerConfiguration.builder()
                .enableSSL(mailSenderConfigData.getEnableSSL())
                .password(mailSenderConfigData.getPassword())
                .user(mailSenderConfigData.getUser())
                .smtpHost(mailSenderConfigData.getSmtpHost())
                .smtpPort(mailSenderConfigData.getSmtpPort())
                .build();
        return mailServerConfiguration;
    }

    public String getSubject(ToopAlarm alarm) {
        ToopAlarm translateAlarm = alarmInfoTranslator.translateAlarm(alarm);
        String alarmSubjectFormat = I18nMailUtils.getAlarmEmailSubject();
        return String.format(alarmSubjectFormat, translateAlarm.getToopKey(),
                I18nAlarmUtils.getMessage(alarm.getSeverity().toUpperCase()),
                I18nAlarmUtils.getIsClear(alarm.getIsClear()));
    }

    public String getMailTestContent() {
        return TextMailGenerator.getTestMail();
    }

    public String getAlarmContent(ToopAlarm alarm) {
        ToopAlarm translateAlarm = alarmInfoTranslator.translateAlarm(alarm);
        return TextMailGenerator.getAlarmMail(translateAlarm);
    }

}
