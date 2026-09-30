package net.flex.dci.otn.controller.nms.cache.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/3/20 10:17
 */
@Data
@Builder
@AllArgsConstructor
public class PhyNodeCache extends BaseCache implements Serializable {

    private String phyNodeId;

    private String phyNodeName;

    private String neType; //OPC/TPC

    private String adapterName;

    private String refSiteId;

    private String refSiteName;

    private String collectorName;

    private String softwareVersion;

    private SiteLinkCache siteLinkCaches;


    private String domainName;

    private String refSiteLink;

    private String refTelemetryName;


}
