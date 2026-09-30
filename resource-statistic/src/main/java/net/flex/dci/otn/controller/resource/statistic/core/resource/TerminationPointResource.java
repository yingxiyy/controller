package net.flex.dci.otn.controller.resource.statistic.core.resource;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 2026/1/30
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class TerminationPointResource extends AbstractResource {


    protected TerminationPointResource(SiteLinkDao siteLinkDao, TunnelDao tunnelDao,
            PhyNodeDao phyNodeDao, EquipmentsDao equipmentsDao,
            SubNetTreeNodeDao subNetTreeNodeDao) {
        super(siteLinkDao, tunnelDao, phyNodeDao, equipmentsDao, subNetTreeNodeDao);
    }

    public List<String> extractConnectionRelativeTpIds(String siteLinkId, String tunnelId) {
        log.debug("resolve termination point from connect siteLink:{} tunnelId:{} ",
                siteLinkId, tunnelId);
        if (!StringUtils.hasText(siteLinkId) && !StringUtils.hasText(tunnelId)) {
            log.debug("filter from connect condition is null,do nothing");
            return new ArrayList<>();
        }
        List<String> tpIds = new ArrayList<>();
        if (StringUtils.hasText(siteLinkId)) {
            //get tps from siteLink
            List<String> siteLinkRefTps = resolveTpsFromSiteLink(siteLinkId);
            tpIds.addAll(siteLinkRefTps);
        }
        if (StringUtils.hasText(tunnelId)) {
            List<String> tunnelRefTps = resolveTpsFromTunnel(tunnelId);
            tpIds.addAll(tunnelRefTps);
        }

        return tpIds;
    }

    private List<String> resolveTpsFromTunnel(String tunnelId) {
        return new ArrayList<>();
    }

    private List<String> resolveTpsFromSiteLink(String siteLinkId) {
        return new ArrayList<>();
    }
}
