package net.flex.dci.otn.controller.nms.cache.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/3/24 13:28
 */
@Data
@Builder
@AllArgsConstructor
public class TunnelCache extends BaseCache implements Serializable {

    private String tunnelId;

    private String friendlyName;

    private String sourceSiteId;

    private String destSiteId;

    private String sourceNodeId;

    private String destNodeId;

    private String sourceTpId;

    private String destTpId;

    private String siteLinkId;

    private String supportOchLinkId;

}
