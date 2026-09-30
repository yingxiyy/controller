package net.flex.dci.otn.controller.sftp.manager.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 *
 * 2025/9/22
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class FtpServerDto implements Serializable {
    
    private String id;

    private String name;

    private String type;

    private String user;

    private String password;

    private String address;

    private int port;
}
