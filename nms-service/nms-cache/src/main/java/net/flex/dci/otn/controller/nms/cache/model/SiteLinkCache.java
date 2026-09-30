package net.flex.dci.otn.controller.nms.cache.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/3/20 10:18
 */
@Builder
@Data
@AllArgsConstructor
public class SiteLinkCache extends BaseCache implements Serializable {

    private String siteLinkId;

    private String siteLinkFriendlyName;

    private String plane;

    private String sourceSiteId;

    private String destSiteId;

    private String sourceSiteName;

    private String destSiteName;

    private String sourceTpId;

    private String tpName;

    private String destTpId;

    private String destNodeName;

    private String sourceNodeId;

    private String sourceNodeName;

    private String destNodeId;

}
