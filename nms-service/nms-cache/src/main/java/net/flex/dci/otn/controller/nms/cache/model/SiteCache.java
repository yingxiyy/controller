package net.flex.dci.otn.controller.nms.cache.model;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/3/20 10:15
 */
@Data
@Builder
@AllArgsConstructor
public class SiteCache extends BaseCache implements Serializable {

    private String siteNodeId;

    private String siteFriendlyName;

}
