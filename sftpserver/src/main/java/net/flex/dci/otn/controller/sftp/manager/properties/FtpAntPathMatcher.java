package net.flex.dci.otn.controller.sftp.manager.properties;

import lombok.Data;

import java.io.Serializable;

/**
 * @version 1.0
 * @date 6/7/2023 3:10 PM
 */

@Data
public class FtpAntPathMatcher implements Serializable {

    private String path;

    private String pathVariable;
}
