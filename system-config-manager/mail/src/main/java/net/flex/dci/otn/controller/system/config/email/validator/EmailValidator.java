package net.flex.dci.otn.controller.system.config.email.validator;

/**
 * @version 1.0
 * @date 10/11/2023 1:53 PM
 */
public interface EmailValidator {

    boolean validEmailAddress(String emailAddress, String domain);

}
