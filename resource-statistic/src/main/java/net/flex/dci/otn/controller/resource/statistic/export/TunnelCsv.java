package net.flex.dci.otn.controller.resource.statistic.export;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * 2025/11/2
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
public class TunnelCsv implements Serializable {

    private String tunnelId;

    private String subnet;

    private String tunnelName;

    private String serviceType;

    private String lineSideRate;

    private String clientSideRate;

    private String protectionLevel;

    private String protectionType;

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
