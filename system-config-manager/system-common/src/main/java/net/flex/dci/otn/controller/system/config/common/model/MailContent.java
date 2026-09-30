package net.flex.dci.otn.controller.system.config.common.model;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 8/29/2023 11:08 AM
 */
@Data
@Builder
public class MailContent implements Serializable {

    private String subject;

    private String content;
}
