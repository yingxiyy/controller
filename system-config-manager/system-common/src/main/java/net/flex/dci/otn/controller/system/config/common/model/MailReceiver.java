package net.flex.dci.otn.controller.system.config.common.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @version 1.0
 * @date 9/14/2023 11:29 AM
 */
@Data
public class MailReceiver implements Serializable {

    private List<String> mailAddresses;
}
