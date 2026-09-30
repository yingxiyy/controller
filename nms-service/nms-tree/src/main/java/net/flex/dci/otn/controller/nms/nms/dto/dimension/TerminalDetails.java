package net.flex.dci.otn.controller.nms.nms.dto.dimension;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/4/7
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class TerminalDetails implements Serializable {

    private String siteId;

    private String siteName;

    private String neId;

    private String neName;

    private String neSubType;

    private String equipmentId;

    private String portId;

    private String portName;


}
