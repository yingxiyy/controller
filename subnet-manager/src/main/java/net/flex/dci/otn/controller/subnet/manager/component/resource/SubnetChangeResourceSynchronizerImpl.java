package net.flex.dci.otn.controller.subnet.manager.component.resource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.dao.ViewLinkDao;
import net.flex.dci.otc.mongo.dao.ViewNodeDao;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 2026/2/14
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class SubnetChangeResourceSynchronizerImpl implements SubnetChangeResourceSynchronizer {

    private final ViewNodeDao viewNodeDao;
    private final ViewLinkDao viewLinkDao;
    private final PhyNodeDao phyNodeDao;
    private final PhyLinkDao phyLinkDao;
    private final SiteLinkDao siteLinkDao;
    private final OchLinkDao ochLinkDao;
    private final TunnelDao tunnelDao;

    @Async("subnetManagerExecutor")
    @Override
    public void synchronizeUpdateSubnetName(String subnetId, String oldSubnetName,
            String newSubnetName,
            String operator) {
        log.info(
                "synchronize update subnetName start ,subnetId:{} oldSubnetName:{} newSubnetName:{} operator:{}",
                subnetId, oldSubnetName, newSubnetName, operator);
        try {
            synchronizeResourceSubnetName(subnetId, oldSubnetName, newSubnetName);
            log.info("[Subnet Change - Resource Sync] Execution Completed! subnetId:{}", subnetId);
        } catch (Exception ex) {
            log.error("[Subnet Change - Resource Sync] Execution failed！subnetId:{}", subnetId, ex);
        }
    }

    private void synchronizeResourceSubnetName(String subnetId, String oldSubnetName,
            String newSubnetName) {
        log.debug(
                "subnet name update synchronize resource for subnet id:{} oldSubnetName:{} newSubnetName:{}",
                subnetId, oldSubnetName, newSubnetName);
        long viewNodeCount = viewNodeDao.updateViewNodeSubnetNameBySubnetId(subnetId,
                newSubnetName);
        log.debug("update view node count:{}", viewNodeCount);
        long viewLinkCount = viewLinkDao.updateViewLinkSubnetNameBySubnetId(subnetId,
                newSubnetName);
        log.debug("update view link count:{}", viewLinkCount);
        long phyNodeCount = phyNodeDao.updatePhyNodeSubnetNameBySubnetId(subnetId, newSubnetName);
        log.debug("update phyNode count:{}", phyNodeCount);
        long phyLinkCount = phyLinkDao.updatePhyLinkSubnetNameBySubnetId(subnetId, newSubnetName);
        log.debug("update phyLink count:{}", phyLinkCount);
        long siteLinkCount = siteLinkDao.updateSiteLinkSubnetNameBySubnetId(subnetId,
                newSubnetName);
        log.debug("update siteLink count:{}", siteLinkCount);
        long ochLinkCount = ochLinkDao.updateOchLinkSubnetNameBySubnetId(subnetId, newSubnetName);
        log.debug("update ochLink count:{}", ochLinkCount);
        long tunnelCount = tunnelDao.updateSubnetNameBySubnetId(subnetId, newSubnetName);
        log.debug("update tunnel count:{}", tunnelCount);
    }
}
