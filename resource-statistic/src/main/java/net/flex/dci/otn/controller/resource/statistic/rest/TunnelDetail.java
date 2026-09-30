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
public class TunnelDetail implements Serializable {

    private String tunnelId;

    private String tunnelName;

    private String serviceType;
    
    private String protectionType;

    private String subnet;

    private String lineSideRate;

    private String clientSideRate;

    private String protectionLevel;

    private String centreFrequency;

    private String sourceSiteName;

    private String sourceNeName;

    private String sourceLineTpName;

    private String sourceClientTpName;

    private String destSiteName;

    private String destNeName;

    private String destLineTpName;

    private String destClientTpName;

    private String creationTime;

    private String activationTime;


}
