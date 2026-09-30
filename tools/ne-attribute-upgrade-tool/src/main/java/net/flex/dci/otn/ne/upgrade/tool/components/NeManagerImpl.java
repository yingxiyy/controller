package net.flex.dci.otn.ne.upgrade.tool.components;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.rpc.client.rpcs.AdapterRpc;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TelemetryDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConnectNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveNeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CommunicationStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2026/4/8
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class NeManagerImpl implements NeManager {

    private final PhyNodeDao phyNodeDao;

    private final AdapterRpc adapterRpc;

    private final AdapterDao adapterDao;

    private final TelemetryDao telemetryDao;


    @Override
    public void unSupervisionNe(String neId) throws ExecutionException, InterruptedException {
        log.info("unSupervisionNe:{}", neId);
        Adapter adapter = adapterDao.getAdapterByNeId(
                neId);
        if (adapter == null) {
            log.warn("current ne is not managed by adapter,skip");
            return;
        }
        RemoveNeOutput unRegisterOutput = adapterRpc.unregisterNe(
                adapter, neId, true);
        if (unRegisterOutput.getReturnCode().equals(RpcResultType.Success)) {
            adapterDao.removeRegisteredNeId(adapter.getName().getValue(), neId);
            log.info("success to unSupervision the ne:{}", neId);
            updateNodeUnSupervisionState(neId);
        } else {
            log.warn("unSupervision failed the neId is:{}", neId);
        }
    }


    @Override
    public void supervisionNe(Node node) throws ExecutionException, InterruptedException {
        String neId = node.getNodeId().getValue();
        log.info("start to supervision ne:{}", neId);
        phyNodeDao.updateConfigPhyNodeSupervisionState(neId,
                SupervisionStatusType.Monitoring);
        List<Adapter> adapters = adapterDao.getAdapters();
        if (CollectionUtils.isEmpty(adapters)) {
            log.error("supervision ne:{} failed, no available adapters", neId);
            return;
        }
        Node configNode = phyNodeDao.getConfigPhyNodeById(neId);
        int randomIndex = ThreadLocalRandom.current().nextInt(adapters.size());
        Adapter randomAdapter = adapters.get(randomIndex);
        log.info("supervision ne:{} use random adapter:{}", neId, randomAdapter.getName());
        ConnectNeOutput connectNeOutput = adapterRpc.connectNe(randomAdapter, configNode);
        if (connectNeOutput.getReturnCode() == RpcResultType.Success) {
            log.info("success to supervision the ne:{}", neId);
            adapterDao.addRegisteredNeId(randomAdapter.getName().getValue(), neId);
            adapterRpc.syncNeData(randomAdapter, neId);
        } else {
            log.warn("failed to supervision the ne:{}", neId);
        }

    }


    public void updateNodeUnSupervisionState(String neId) {
        log.info("change the ne:{} to unSupervised", neId);
        phyNodeDao.updateConfigPhyNodeSupervisionState(neId,
                SupervisionStatusType.Unmonitored);
        phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId,
                CommunicationStatusType.Broken);
    }
}
