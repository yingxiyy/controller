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
public class SiteLinkDetails implements Serializable {

    private String siteLinkId;

    private String friendlyName;

    private String subnetId;

    private String subnetName;

    private String sourceSiteId;

    private String sourceSiteName;

    private String destSiteId;

    private String destSiteName;

    private String sourceNeId;

    private String sourceNeName;

    private String destNeId;

    private String destNeName;

    private String sourceTpId;

    private String sourceTpName;

    private String destTpId;

    private String destTpName;


}
