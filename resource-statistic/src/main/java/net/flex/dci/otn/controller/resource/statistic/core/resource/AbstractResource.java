package net.flex.dci.otn.controller.resource.statistic.core.resource;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;

/**
 * 2026/1/30
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public abstract class AbstractResource {

    protected final SiteLinkDao siteLinkDao;

    protected final TunnelDao tunnelDao;

    protected final PhyNodeDao phyNodeDao;

    protected final EquipmentsDao equipmentsDao;

    protected final SubNetTreeNodeDao subNetTreeNodeDao;

    protected AbstractResource(SiteLinkDao siteLinkDao, TunnelDao tunnelDao, PhyNodeDao phyNodeDao,
            EquipmentsDao equipmentsDao, SubNetTreeNodeDao subNetTreeNodeDao) {
        this.siteLinkDao = siteLinkDao;
        this.tunnelDao = tunnelDao;
        this.phyNodeDao = phyNodeDao;
        this.equipmentsDao = equipmentsDao;
        this.subNetTreeNodeDao = subNetTreeNodeDao;
    }


    protected List<String> getSubNetDescendantIds(List<String> planeId) {
        List<SubNetTreeNode> subNetTreeNodes = subNetTreeNodeDao.getAllDescendants(planeId);
        return subNetTreeNodes.stream().map(SubNetTreeNode::getSubNetId)
                .collect(Collectors.toList());
    }
}
