package net.flex.dci.otn.controller.system.config.email.component;

import net.flex.dci.otn.controller.system.config.common.model.MailReceiver;

import java.util.List;

/**
 * @version 1.0
 * @date 9/14/2023 1:04 PM
 */

public interface AlarmNotifierValidator {

    void validateMailAddress(List<String> addresses);

    void validateMaileReceiver(MailReceiver mailReceiver);
}
