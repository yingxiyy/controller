package net.flex.dci.otn.controller.sftp.manager.model;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @version 1.0
 * @date 6/7/2023 3:24 PM
 */
@Data
@Builder
public class AntPathInfo implements Serializable {

    private String path;

    private String pathVariable;

    private String pathVariableValue;
}
