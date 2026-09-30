package net.flex.dci.otn.controller.implement.physical.component.ne.attribute;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/4/14
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class NodeInfo implements Serializable {

    private String neId;

    private String friendlyName;

    private String ip;

    private Integer port;

    private String loginName;

    private String loginPwd;

}
