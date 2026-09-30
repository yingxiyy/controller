package net.flex.dci.otn.controller.resource.statistic.rest;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/4/11
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class SiteLinkDetail implements Serializable {

    private String siteLinkId;

    private String siteLinkName;

    private String subnet;

    private String sourceSiteId;

    private String destSiteId;

    private String sourceSiteName;

    private String destSiteName;

    private String sourceNeId;

    private String sourceNeName;

    private String sourceTpId;

    private String sourceTpName;

    private String destNeId;

    private String destNeName;

    private String destTpId;

    private String destTpName;

    private String msModel;

    private String protectionType;

    private String bandwidth;

    private String demandSource;

    private String creationTime;

    private String activationTime;

    private String implementState;
}
