/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.email.service;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.MailConfigDao;
import net.flex.dci.otc.mongo.dao.MailRecipientDao;
import net.flex.dci.otc.mongo.mdoel.mail.MailRecipient;
import net.flex.dci.otc.mongo.mdoel.mail.MailServerConfiguration;
import net.flex.dci.otn.controller.system.config.email.component.AlarmNotifierValidator;
import net.flex.dci.otn.controller.system.config.email.dto.MailConfigModel;
import net.flex.dci.otn.controller.system.config.email.dto.MailSenderConfigData;
import net.flex.dci.otn.controller.system.config.email.utils.MailUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MailConfigureService {

    private static final String CONFIG_FILE = "mail.json";

    private MailConfigModel mailConfigModel = MailConfigModel.builder().build();

    @Autowired
    private MailSenderService mailSenderService;

    @Autowired
    private MailRecipientDao mailRecipientDao;

    @Autowired
    private MailConfigDao mailConfigDao;

    @Autowired
    private AlarmNotifierValidator alarmNotifierValidator;

//    @PostConstruct
//
//    private void loadConfig() {
//        log.info("Begin to load mail config.");
//        try {
//            File configFile = getConfigFile();
//            if (configFile == null) {
//                log.warn("Loading config failed,because {} not found.", CONFIG_FILE);
//                return;
//            }
//            ObjectMapper mapper = new ObjectMapper();
//            mailConfigModel = mapper.readValue(configFile, MailConfigModel.class);
//
//        } catch (Exception e) {
//            log.warn("Default setting hasn't found or content is an invalid JSON : {}", CONFIG_FILE,
//                    e);
//        }
//
//    }
//
//    private File getConfigFile() {
//        File configFile = new File(CONFIG_FILE);
//        if ((configFile == null) || (!configFile.exists())) {
//            log.warn("Mail config  file {} not found, return.", CONFIG_FILE);
//            return null;
//        }
//        return configFile;
//    }
//
//    @PreDestroy
//    private void saveConfig() {
//        log.info("Begin to save mail to config file: {}", CONFIG_FILE);
//        try {
//            File configFile = getConfigFile();
//            if (configFile == null) {
////                configFile = new File("src/main/resources/" + configFileName);
//                configFile = new File(CONFIG_FILE);
//                Files.touch(configFile);
//            }
//            ObjectMapper mapper = new ObjectMapper();
//            mapper.writeValue(configFile, mailConfigModel);
//
//        } catch (Exception e) {
//            log.error("Failed to save mail recipients to {}", CONFIG_FILE, e);
//        }
//    }


    public List<String> getRecipients() {
//        if (mailConfigModel != null && mailConfigModel.getMailRecipients() != null) {
//            return mailConfigModel.getMailRecipients();
//        }
//        return Collections.EMPTY_LIST;
        log.debug("get all mail recipients");
        List<MailRecipient> mailRecipients = mailRecipientDao.listAllMailRecipients();
        //todo : just only get the mail recipient
        List<String> mails = mailRecipients.stream().map(MailRecipient::getMailRecipient).collect(
                Collectors.toList());
        return mails;
    }


    public void updateRecipients(List<String> recipients) {
        log.info("Update mail recipients: ", recipients);
//        mailConfigModel.setMailRecipients(recipients);
        alarmNotifierValidator.validateMailAddress(recipients);
        List<MailRecipient> mailRecipients = recipients.stream()
                .map(recipient -> MailRecipient.builder().mailRecipient(recipient)
                        .createTime(System.currentTimeMillis()).build()).collect(
                        Collectors.toList());
        mailRecipientDao.dropAllRecipient();
        mailRecipientDao.batchSaveMailRecipients(mailRecipients);
    }

    public MailSenderConfigData getMailSenderConfigData() {
//        return mailConfigModel.getMailSenderConfigData();
        log.debug("start to load the mail server configure");
        MailServerConfiguration mailServerConfiguration = mailConfigDao.getMailServerConfiguration();
        if (mailServerConfiguration == null) {
            return null;
        }
        MailSenderConfigData mailSenderConfigData = MailUtils.mailConfigurationData2Ui(
                mailServerConfiguration);
        return mailSenderConfigData;
    }

    public void updateMailSenderConfigData(MailSenderConfigData mailSenderConfigData)
            throws GeneralSecurityException {
        log.info("Update the mail configure as: {}", mailSenderConfigData);
        MailServerConfiguration mailServerConfiguration = MailUtils.mailConfigurationData2Db(
                mailSenderConfigData);
        mailConfigDao.updateMailServerConfiguration(mailServerConfiguration);
//        this.mailConfigModel.setMailSenderConfigData(mailSenderConfigData);
        mailSenderService.updateMailSender(mailSenderConfigData);
    }

    public String getMailSenderAddress() {
        MailSenderConfigData mailSenderConfigData = getMailSenderConfigData();
        if (mailSenderConfigData == null) {
            return null;
        }
        return mailSenderConfigData.getUser();
    }
}
