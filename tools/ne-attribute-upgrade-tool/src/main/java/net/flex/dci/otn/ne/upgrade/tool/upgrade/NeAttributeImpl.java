package net.flex.dci.otn.ne.upgrade.tool.upgrade;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.base.page.PageResult;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TelemetryDao;
import net.flex.dci.otn.ne.upgrade.tool.components.NeManager;
import net.flex.dci.otn.ne.upgrade.tool.components.NeSubTypeUpgrader;
import net.flex.dci.otn.ne.upgrade.tool.loader.NeIdListReader;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.manager.TelemetryServer;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.springframework.stereotype.Component;

/**
 * 2026/3/13
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class NeAttributeImpl implements NeAttribute {

    private static final int BATCH_SIZE = 200;

    private final PhyNodeDao phyNodeDao;

    private final NeManager neManager;

    private final NeIdListReader neIdListReader;

    private final TelemetryDao telemetryDao;

    private final Map<NodeType, NeSubTypeUpgrader> neSubTypeMap;

    public NeAttributeImpl(List<NeSubTypeUpgrader> neSubTypeUpgraders, PhyNodeDao phyNodeDao,
            NeManager neManager, NeIdListReader neIdListReader, TelemetryDao telemetryDao) {
        this.phyNodeDao = phyNodeDao;
        this.neSubTypeMap = neSubTypeUpgraders.stream().collect(HashMap::new,
                (map, neSubTypeUpgrade) -> map.put(neSubTypeUpgrade.generalType(),
                        neSubTypeUpgrade), HashMap::putAll);
        this.neManager = neManager;
        this.neIdListReader = neIdListReader;
        this.telemetryDao = telemetryDao;
    }


    @Override
    public void upgradeNeSubType() {
        log.info("start to upgrade ne subType");
        long currentTimestamp = System.currentTimeMillis();
        long currentNodeCount = phyNodeDao.totalPhyNode();
        int page = 1;
        long totalPage = (currentNodeCount + BATCH_SIZE - 1) / BATCH_SIZE;
        while (page <= totalPage) {
            try {
                log.info("update ne subType batch num:{}", page);
                PageResult<Node> batchNode = phyNodeDao.listConfigPhyNodesPaged(page, BATCH_SIZE);
                List<Node> currentNodes = batchNode.getList();
                upgradeNeSubTypeAttr(currentNodes);
                page++;
            } catch (Exception ex) {
                log.error("failed to upgrade current ne sub type,the reason is:{}",
                        ex.getMessage());
            }
        }
        double cost = (System.currentTimeMillis() - currentTimestamp) / 1000d;
        log.info("finish upgrade current ne subType,cost :{}s", cost);

    }

    @Override
    public void upgradeNeIp() {
        log.info("start to upgrade ne ip");
        long currentTimestamp = System.currentTimeMillis();
        long currentNodeCount = phyNodeDao.totalPhyNode();
        int page = 1;
        long totalPage = (currentNodeCount + BATCH_SIZE - 1) / BATCH_SIZE;
        AtomicLong count = new AtomicLong(0);
        while (page <= totalPage) {
            try {
                log.info("update ne ip batch num:{}", page);
                PageResult<Node> batchNode = phyNodeDao.listConfigPhyNodesPaged(page, BATCH_SIZE);
                List<Node> currentNodes = batchNode.getList();
                upgradeNeIpAttr(currentNodes, count);
                page++;
            } catch (Exception ex) {
                log.error("failed to upgrade current ne ip,the reason is:{}",
                        ex.getMessage());
            }
        }
        double cost = (System.currentTimeMillis() - currentTimestamp) / 1000d;
        log.info("finish upgrade current:{} ne ip,cost :{}s", count.get(), cost);
    }

    @Override
    public void removeTelemetryRecord() {
        log.info("remove telemetry record");
        log.info("load ne id list reader");
        List<String> neIds = neIdListReader.getNeIdList();
        log.info("load neIds size:{}", neIds.size());
        List<TelemetryServer> telemetryServers = telemetryDao.listTelemetryServers();
        TelemetryServer telemetryServer = telemetryServers.get(0);
        for (String neId : neIds) {
            log.info("remove register phy node:{}", neId);
            telemetryDao.removeRegisterPhyNode(telemetryServer.getName().getValue(), neId);
        }
    }

    /**
     * upgrade neIp attr
     *
     * @param currentNodes
     */
    private void upgradeNeIpAttr(List<Node> currentNodes, AtomicLong count) {
        log.debug("upgrade ne ip attribute,node size:{}", currentNodes.size());
        List<Node> upperIpNodes = currentNodes.stream()
                .filter(node -> {
                    String ipAddress = node.getAugmentation(Node1.class).getPhysical().getIp();
                    String opIpAddress = phyNodeDao.getOpPhyNodeById(node.getNodeId().getValue())
                            .getAugmentation(Node1.class).getPhysical().getIp();
                    return ipAddress != null && (!ipAddress.equals(ipAddress.toLowerCase())
                            || !opIpAddress.equals(ipAddress.toLowerCase()));
                })
                .collect(Collectors.toList());
        upperIpNodes.forEach(node -> {
            try {
                String neId = node.getNodeId().getValue();
                String ip = node.getAugmentation(Node1.class).getPhysical().getIp();
                log.info("upgrade the ne:{} ip:{}", neId, ip);
                count.incrementAndGet();
                String lowerCaseIp = ip.toLowerCase();
                //update ne ip
                neManager.unSupervisionNe(neId);
                phyNodeDao.updatePhyNodeLoginIp(neId, lowerCaseIp);

                //stop supervision
                neManager.supervisionNe(node);
                //start supervision
            } catch (Exception e) {
                log.error("failed to upgrade ne:{} ip: {}",
                        node.getNodeId().getValue(), e.getMessage(), e);
            }
        });
    }


    /**
     * upgrade NeSubType attr
     *
     * @param currentNodes
     */
    private void upgradeNeSubTypeAttr(List<Node> currentNodes) {
        log.debug("upgrade NeSubType attribute ,node size:{}", currentNodes.size());
        Map<NodeType, List<Node>> nodeTypeListMap = currentNodes.stream()
                .collect(Collectors.groupingBy(node -> node.getAugmentation(
                        Node1.class).getPhysical().getNodeType()));
        nodeTypeListMap.forEach((NodeType nodeType, List<Node> nodes) -> {
            Optional.ofNullable(neSubTypeMap.get(nodeType))
                    .ifPresent(processor -> processor.upgradeNeSubType(nodes));
        });
    }
}
