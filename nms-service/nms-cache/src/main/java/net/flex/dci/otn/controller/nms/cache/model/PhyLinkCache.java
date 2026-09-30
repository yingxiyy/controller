package net.flex.dci.otn.controller.nms.cache.model;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/3/20 10:26
 */
@Data
@Builder
@AllArgsConstructor
public class PhyLinkCache extends BaseCache implements Serializable {

    private String phyLinkId;

    private String phyLinkFriendlyName;

    private String sourceNodeId;

    private String destNodeId;

    private String sourceSiteId;

    private String destSiteId;

    private String destTpId;

    private String sourceTpId;

    private List<String> siteLinkIds;


}
