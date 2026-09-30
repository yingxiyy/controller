package net.flex.dci.otn.controller.allocate.common;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;

@Slf4j
public class OpNodeMerger {

    private final PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
    private final NeManagerRpc neMgr = SpringBeanFinder.getBean(NeManagerRpc.class);


    public void merge(String nodeId) throws CommonException {
        log.debug("merge tp OP node {}", nodeId);
        if (!phyNodeDao.nodeExistIp(nodeId) || !phyNodeDao.existsOpNode(nodeId)) {
            return;
        }

        try {
            neMgr.mergeNe(new NodeId(nodeId));
        } catch (Exception e) {
            log.error("merge ne error", e);
        }
    }
}
