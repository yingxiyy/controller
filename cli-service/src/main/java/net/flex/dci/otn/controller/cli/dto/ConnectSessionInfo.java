package net.flex.dci.otn.controller.cli.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 *
 * 2025/9/16
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ConnectSessionInfo implements Serializable {

    private String sessionInfo;

}
