/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.email.service;


import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otn.controller.system.config.common.enums.I18nMailCode;
import net.flex.dci.otn.controller.system.config.common.exception.AlarmMailException;
import net.flex.dci.otn.controller.system.config.common.model.MailContent;
import net.flex.dci.otn.controller.system.config.common.model.MailReceiver;
import net.flex.dci.otn.controller.system.config.common.model.ToopAlarm;
import net.flex.dci.otn.controller.system.config.common.utils.AsynchronousExecutor;
import net.flex.dci.otn.controller.system.config.common.utils.i18n.I18nMailUtils;
import net.flex.dci.otn.controller.system.config.email.component.AlarmNotifierValidator;
import net.flex.dci.otn.controller.system.config.email.utils.MailUtils;
import net.flex.dci.otn.controller.system.config.email.validator.impl.EmailValidatorImpl;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MailImplService {

//    @Autowired
//    private JavaMailSender emailSender;

    @Autowired
    private AlarmNotifierValidator alarmNotifierValidator;

    @Autowired
    private MailSenderService mailSenderService;
    /* @Autowired
     private SimpleMailMessage template;*/
    @Autowired
    private MailConfigureService mailConfigureService;

    @Autowired
    private MailUtils mailUtils;

    @Autowired
    private EmailValidatorImpl emailValidator;

   /* private static final String NOREPLY_ADDRESS = "noreply@dciworld.com";
    @Value("${mail.sender: noreply@dciworld.com}")
    private String sender;*/
/*
    @Autowired
    private SpringTemplateEngine thymeleafTemplateEngine;

    @Autowired
    private FreeMarkerConfigurer freemarkerConfigurer;*/

/*    @Value("classpath:/mail-logo.png")
    private Resource resourceFile;*/


    public Boolean sendSimpleMessage(String to, String subject, String text) {
        try {
            log.info("start to send message to {}", to);
            JavaMailSender emailSender = mailSenderService.getEmailSender();
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(mailConfigureService.getMailSenderAddress());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, true);

//            try {
//                emailSender = mailSenderService.getEmailSender();
//            } catch (AlarmMailException | GeneralSecurityException e) {
//                log.error(
//                        "Failed to send mail:{}, because failed to get a mailSender by SMTP configure: {}",
//                        message, mailConfigureService.getMailSenderConfigData(), e);
//                return false;
//            }
            emailSender.send(message);
            log.debug("Send mail:{},success", message);
            return true;
        } catch (MailException | AlarmMailException | GeneralSecurityException |
                 MessagingException exception) {
            log.error("Failed to send mail: {}", exception.getMessage(), exception);
            return false;
        }
    }

    public void send(ToopAlarm alarm) {
//        String text=MailUtils.getHtml(alarm);
        String text = mailUtils.getAlarmContent(alarm);
        String subject = mailUtils.getSubject(alarm);
        for (String to : mailConfigureService.getRecipients()) {

            sendSimpleMessage(to, subject, text);
        }

    }

/*    public void test(String to) {
        StringBuilder example = new StringBuilder();
        example.append("级别:\t紧急\n");
        example.append("原因:\t Remote_Fault;Remote Fault\n");
        example.append("分组:\tOTU_Port_Failure\n");
        example.append("源:\t重庆-云祥(CQ-YX)#CQ-YX-TMP-TMP-PSIM-D-02-1636955289169#T2X2C4-1-1#T2X2C4-1-1-C2\n");
        example.append("设备产生时间：2021-11-25T14:50:34.000+08:00\n");
        sendSimpleMessage(to, "新告警【紧急】", example.toString());

    }*/

    public void test() {
        log.debug("start to send the check mail  to the recipients");
        AsynchronousExecutor.execute(() -> {
            log.info("start to send check mail to the recipients");
            mailConfigureService.getRecipients().forEach(recipient -> {
                sendSimpleMessage(recipient, "DCI测试邮件", mailUtils.getMailTestContent());
            });
        });
    }

    public void check() {
        String to = mailConfigureService.getMailSenderAddress();
        if (to == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "测试邮件发送失败，因为邮件发送用户名未配置。");
        }

        AsynchronousExecutor.execute(() -> {
            log.info("start to send check mail to the recipients");
            sendCheckMail(to);
        });
    }

    public void sendAlarms(List<ToopAlarm> alarms) {
        log.info("start to send alarms mail to recipients");
        log.debug("start to send alarms mail to recipients");
        List<MailContent> transferAlarms = alarms.stream()
                .map(alarm -> MailContent.builder().subject(mailUtils.getSubject(alarm))
                        .content(mailUtils.getAlarmContent(alarm)).build())
                .collect(Collectors.toList());
        List<String> recipients = mailConfigureService.getRecipients();
        AsynchronousExecutor.execute(() -> {
            log.debug("send alarm email start");
            recipients.forEach(recipient -> {
                log.debug("send alarm email to the recipient :{}", recipient);
                log.info("send alarm email to the recipient :{}", recipient);
                for (MailContent mailContent : transferAlarms) {
                    sendSimpleMessage(recipient, mailContent.getSubject(),
                            mailContent.getContent());
                }
            });
        });

    }

    /**
     * send test mail to the mail receiver
     *
     * @param mailReceiver
     */
    public void sendCheckMail(MailReceiver mailReceiver) {
        log.debug("start to send check mail to the mail receiver,mail address is:{}", mailReceiver);
        alarmNotifierValidator.validateMaileReceiver(mailReceiver);
        AsynchronousExecutor.execute(() -> {
            log.info("start to send check mail to the recipients");
            mailReceiver.getMailAddresses().forEach(this::sendCheckMail);
        });
    }

    private void sendCheckMail(String recipient) {
        boolean validResult = emailValidator.validEmailAddress(recipient, mailConfigureService.getMailSenderAddress());
        if (validResult) {
            sendSimpleMessage(recipient, I18nMailUtils.getMessage(I18nMailCode.checkMailSubject.name()), mailUtils.getMailTestContent());
            String message = String.format(I18nMailUtils.getSuccessCheckEmailFormatter(), recipient);
            BroadcastMessager.publishKafkaMessage(BroadcastMessage.builder().title(BroadCastConstant.SEND_CHECK_EMAIL).message(message).error(false).build());
        } else {
            //notification to do set reachable status false
            String message = String.format(I18nMailUtils.getFailedCheckEmailFormatter(), recipient);
            BroadcastMessager.publishKafkaMessage(BroadcastMessage.builder().title(BroadCastConstant.SEND_CHECK_EMAIL).error(true).message(message).build());
        }
    }


}
