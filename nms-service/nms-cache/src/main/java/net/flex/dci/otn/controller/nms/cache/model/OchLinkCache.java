package net.flex.dci.otn.controller.nms.cache.model;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/3/24 17:34
 */
@Data
@Builder
@AllArgsConstructor
public class OchLinkCache extends BaseCache implements Serializable {

    private String ochLinkId;

    private String friendName;

    private String planeName;

    private String sourceSiteId;

    private String destSiteId;

    private String sourceNodeId;

    private String destNodeId;

    private String sourceTpId;

    private String destTpId;

    private List<String> supportPhyLinkIds;

    private String supportSiteLinkId;

    private List<String> supportTunnelIds;

}
