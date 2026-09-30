package net.flex.dci.otn.controller.system.config.email.component.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.MailRecipientDao;
import net.flex.dci.otn.controller.system.config.common.model.MailReceiver;
import net.flex.dci.otn.controller.system.config.email.component.AlarmNotifierValidator;
import net.flex.dci.otn.controller.system.config.email.service.MailConfigureService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @version 1.0
 * @date 9/14/2023 1:07 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AlarmNotifierValidatorImpl implements AlarmNotifierValidator {

    private static final String MAIL_REGEX = "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$";

    private final MailRecipientDao mailRecipientDao;

    private final MailConfigureService mailConfigureService;


    @Override
    public void validateMailAddress(List<String> addresses) {
        log.debug("validate the mail receiver ");
        List<String> invalidMailAddress = addresses.stream().filter(address -> !validateEmailAddress(address)).collect(Collectors.toList());
        if (!invalidMailAddress.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format("invalid mail address format,the address is: %s", invalidMailAddress));
        }
    }

    @Override
    public void validateMaileReceiver(MailReceiver mailReceiver) {
        log.debug("validate the mail receiver");
        String mailSenderAddress = mailConfigureService.getMailSenderAddress();
        if (mailSenderAddress == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "mail server should be configured!");
        }
        List<String> invalidMailAddress = mailReceiver.getMailAddresses().stream().filter(address -> !validateEmailAddress(address)).collect(Collectors.toList());
        if (!invalidMailAddress.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format("invalid mail address format,the address is: %s", invalidMailAddress));
        }
        List<String> notInDbMailAddress = mailReceiver.getMailAddresses().stream().filter(address -> !mailRecipientDao.existTheRecipient(address)).collect(Collectors.toList());
        if (!notInDbMailAddress.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format("absent addresses in server,the address is: %s", notInDbMailAddress));
        }
    }


    private boolean validateEmailAddress(String mailAddress) {
        log.debug("validate the mail address:{}", mailAddress);
        Pattern pattern = Pattern.compile(MAIL_REGEX);
        Matcher matcher = pattern.matcher(mailAddress);
        return matcher.find();
    }


}
