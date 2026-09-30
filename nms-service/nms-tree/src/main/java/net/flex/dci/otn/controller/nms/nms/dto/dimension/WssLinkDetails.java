package net.flex.dci.otn.controller.nms.nms.dto.dimension;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/4/6
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class WssLinkDetails implements Serializable {

    private String wssLinkId;

    private String wssName;

    private String sourceSiteId;

    private String destSiteId;

    private String sourceNeId;

    private String sourceNeName;

    private String sourceTpId;

    private String sourceTpName;

    private String destNeId;

    private String destNeName;

    private String destTpId;

    private String destTpName;

}
