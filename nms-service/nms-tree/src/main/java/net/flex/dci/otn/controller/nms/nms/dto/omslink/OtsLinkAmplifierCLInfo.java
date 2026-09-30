package net.flex.dci.otn.controller.nms.nms.dto.omslink;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 *
 * 2025/8/11
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class OtsLinkAmplifierCLInfo implements Serializable {

    private String tpId;

    private String tpName;

    private String nodeId;

    private String neName;

    private String ip;

    private String siteId;

    private String siteName;

    private DirectionAmplifierInfo amplifierC;

    private DirectionAmplifierInfo amplifierL;

}
