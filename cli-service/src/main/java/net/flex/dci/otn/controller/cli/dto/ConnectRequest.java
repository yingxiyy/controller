package net.flex.dci.otn.controller.cli.dto;

import java.io.Serializable;
import lombok.Data;

/**
 *
 * @version 1.0
 * @date 9/17/2025 1:11 PM
 */
@Data
public class ConnectRequest implements Serializable {

    private String host;

    private int port = 22;

    private String username;

    private String password;
}
