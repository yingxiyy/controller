package net.flex.dci.otn.controller.resource.statistic.service;

import java.util.List;
import net.flex.dci.otn.controller.resource.statistic.dto.NodeInfo;
import net.flex.dci.otn.controller.resource.statistic.dto.SiteInfo;
import net.flex.dci.otn.controller.resource.statistic.dto.SiteLinkInfo;

/**
 * @version 1.0
 * @date 11/3/2025 1:45 PM
 */
public interface CascadeService {


    List<SiteInfo> retrieveSiteByNetwork(String network);

    List<NodeInfo> getSiteSubPhyNodeBySite(String siteId, String id);

    List<SiteLinkInfo> getRefSiteLinkInfoBySubnet(String subnetId);
}
