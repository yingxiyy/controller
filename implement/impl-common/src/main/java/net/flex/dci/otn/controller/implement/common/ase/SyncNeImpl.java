package net.flex.dci.otn.controller.implement.common.ase;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.controller.implement.common.impl.StepResult;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RemoveResourceOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.FailObj;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.Object;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CommunicationStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class SyncNeImpl {
    private final NeManagerRpc neMgr = SpringBeanFinder.getBean(NeManagerRpc.class);
    private final PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);

    public SyncNeImpl() {}

    public void sync(Map<String, List<CrossConnectionAttributes>> cfgXcMap, Map<String, Node> ocmNodeMap) {
        // Own the pool per sync call so failure or interruption cannot leave idle workers behind.
        ExecutorService executor = Executors.newFixedThreadPool(10);
        try {
            sync(cfgXcMap, ocmNodeMap, executor);
        } finally {
            // Preserve already submitted work; shutdownNow would interrupt device operations.
            executor.shutdown();
        }
    }

    private void sync(Map<String, List<CrossConnectionAttributes>> cfgXcMap, Map<String, Node> ocmNodeMap,
            ExecutorService executor) {
        List<Node> opNodes = new ArrayList<>();
        for (String nodeId : cfgXcMap.keySet()) {
            Node opNode = phyNodeDao.getOpPhyNodeById(nodeId);
            if (opNode == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "some node hasn't managed by adapter");
            }
            opNodes.add(opNode);
        }

        List<Future<Void>> futures = new ArrayList<>();
        for (Node opNode : opNodes) {
            Future<Void> future = executor.submit(() -> {
                String nodeId = opNode.getNodeId().getValue();
                log.info("==== Start sync for node [{}] ====", nodeId);
                syncAction(opNode, cfgXcMap.get(nodeId), ocmNodeMap.get(nodeId));
                log.info("==== Finished sync for node [{}] ====", nodeId);
                return null;
            });
            futures.add(future);
        }

        // 等待所有任务完成
        for (Future<Void> f : futures) {
            try {
                f.get();
                log.debug("rebuild done");
            } catch (ExecutionException e) {
                throw new RuntimeException(e.getCause());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("SyncRebuildAction interrupted", e);
            }
        }

    }

    /**
     * 同步单个网元，找出差异，下发/删除，再更新OCM
     */
    private void syncAction(Node opNode, List<CrossConnectionAttributes> cfgXcs, Node ocmNode) {
        syncNe(opNode);

        Physical opNodeAttr = opNode.getAugmentation(Node1.class).getPhysical();
        List<CrossConnections> opXcs = opNodeAttr.getCrossConnections();

        log.info("Comparing XC for node [{}] (IP={}) | configXC={}, deviceXC={}",
                opNode.getNodeId().getValue(), opNodeAttr.getIp(),
                cfgXcs != null ? cfgXcs.size() : 0,
                opXcs != null ? opXcs.size() : 0);

        List<CrossConnectionAttributes> needWrite2Ne = new ArrayList<>();
        List<CrossConnectionAttributes> needRemoveFromNe = new ArrayList<>();
        compareXc(cfgXcs, opXcs, needWrite2Ne, needRemoveFromNe);

        if (!needWrite2Ne.isEmpty()) {
            log.info("→ Need to INSERT {} XC on node [{}]: {}", needWrite2Ne.size(), opNodeAttr.getIp(),
                    needWrite2Ne.stream().map(x -> x.getCrossConnectionId().getValue()).collect(Collectors.toList()));
            writeInfo2Ne(buildNodeWithXc(opNode, needWrite2Ne));
        }

        if (!needRemoveFromNe.isEmpty()) {
            log.info("→ Need to REMOVE {} XC from node [{}]: {}", needRemoveFromNe.size(), opNodeAttr.getIp(),
                    needRemoveFromNe.stream().map(x -> x.getCrossConnectionId().getValue()).collect(Collectors.toList()));
            removeInfoFromNe(buildNodeWithXc(opNode, needRemoveFromNe));
        }

        if (!needWrite2Ne.isEmpty() || !needRemoveFromNe.isEmpty()) {
            Physical ocmNodeAttr = ocmNode.getAugmentation(Node1.class).getPhysical();
            log.debug("Updating OCM on node {}: {}", opNodeAttr.getIp(), ocmNodeAttr.getOCMGripGroups());
            Node ne = new NodeBuilder()
                    .setNodeId(opNode.getNodeId())
                    .setKey(opNode.getKey())
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder()
                                    .setIp(opNodeAttr.getIp())
                                    .setFriendlyName(opNodeAttr.getFriendlyName())
                                    .setNodeType(opNodeAttr.getNodeType())
                                    .setOCMGripGroups(ocmNodeAttr.getOCMGripGroups())
                                    .build())
                            .build())
                    .build();
            writeInfo2Ne(ne);
        } else {
            log.info("Node [{}] (IP={}) is already synchronized, no action needed.", opNodeAttr.getFriendlyName(), opNodeAttr.getIp());
        }
    }

    private Node buildNodeWithXc(Node node, List<CrossConnectionAttributes> xcs) {
        List<CrossConnections> xcList = xcs.stream().map(x -> new CrossConnectionsBuilder(x).build()).collect(Collectors.toList());
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        return new NodeBuilder()
                .setNodeId(node.getNodeId())
                .setKey(node.getKey())
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder()
                                .setIp(nodeAttr.getIp())
                                .setFriendlyName(nodeAttr.getFriendlyName())
                                .setNodeType(nodeAttr.getNodeType())
                                .setCrossConnections(xcList)
                                .build())
                        .build())
                .build();
    }

    /**
     * 对比 config vs op XC，找出差异
     */
    private void compareXc(List<CrossConnectionAttributes> cfgXcs, List<CrossConnections> opXcs,
                           List<CrossConnectionAttributes> needWrite2Ne, List<CrossConnectionAttributes> needRemoveFromNe) {

        log.debug("Begin XC comparison: cfgXcCount={}, opXcCount={}",
                cfgXcs != null ? cfgXcs.size() : 0, opXcs != null ? opXcs.size() : 0);

        for (CrossConnectionAttributes cfgXc : cfgXcs) {
            if (cfgXc.getWssChannel() == null) continue;

            // 跳过 Down 或 Allocate 状态的 XC
            if (cfgXc.getAdminState() == AdminStatus.Down || cfgXc.getImplementState() == ImplementState.Allocate) {
                log.trace("Skip XC [{}] (state=Down/Allocate)", cfgXc.getCrossConnectionId().getValue());
                continue;
            }

            String cfgXcId = cfgXc.getCrossConnectionId().getValue();
            CrossConnectionAttributes opXc = opXcs.stream()
                    .filter(x -> x.getCrossConnectionId().getValue().equals(cfgXcId))
                    .findAny()
                    .orElse(null);

            if (opXc == null) {
                log.debug("XC [{}] missing on device, will add.", cfgXcId);
                needWrite2Ne.add(cfgXc);
                continue;
            }

            if (opXc.getAdminState() == AdminStatus.Down || opXc.getImplementState() == ImplementState.Allocate) {
                log.debug("XC [{}] on device inactive (Down/Allocate), will re-add.", cfgXcId);
                needWrite2Ne.add(cfgXc);
                continue;
            }

            String exists = PropertyTool.getValue(opXc.getProperties(), "existsOnNe");
            if (exists == null || exists.equalsIgnoreCase("false")) {
                log.debug("XC [{}] existsOnNe=false (merge-only), will add to device.", cfgXcId);
                needWrite2Ne.add(cfgXc);
            } else {
                log.trace("XC [{}] already valid on device (existsOnNe=true).", cfgXcId);
            }
        }

        // 删除多余的 XC
        for (CrossConnections opXc : opXcs) {
            if (opXc.getWssChannel() == null) continue;

            String exists = PropertyTool.getValue(opXc.getProperties(), "existsOnNe");
            if (exists == null || exists.equalsIgnoreCase("false")) {
                log.trace("XC [{}] skip remove (merge-only, not on device).", opXc.getCrossConnectionId().getValue());
                continue;
            }

            String opXcId = opXc.getCrossConnectionId().getValue();
            CrossConnectionAttributes cfgXc = cfgXcs.stream()
                    .filter(x -> x.getCrossConnectionId().getValue().equals(opXcId))
                    .findAny()
                    .orElse(null);

            if (cfgXc == null) {
                log.debug("XC [{}] found on device but not in config, will remove.", opXcId);
                needRemoveFromNe.add(opXc);
            }
        }

        log.info("XC compare result: needWrite={}, needRemove={}", needWrite2Ne.size(), needRemoveFromNe.size());
    }

    private void syncNe(Node node) {
        String nodeId = node.getNodeId().getValue();
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

        log.info("Syncing node [{}] (IP={})...", nodeId, nodeAttr.getIp());
        neMgr.uploadNe(nodeId);

        int retries = 3;
        boolean success = false;
        for (int i = 0; i < retries; i++) {
            try {
                TimeUnit.SECONDS.sleep(30);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Thread interrupted for node " + nodeAttr.getIp(), e);
            }

            node = phyNodeDao.getOpPhyNodeById(nodeId);
            nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            log.trace("Node [{}] sync status = {}", nodeAttr.getIp(), nodeAttr.getCommunicationStatus());
            if (nodeAttr.getCommunicationStatus().equals(CommunicationStatusType.SyncFinished)) {
                success = true;
                break;
            }
        }

        if (!success) {
            throw new RuntimeException(String.format("Sync failed for node: %s", nodeAttr.getIp()));
        }
        log.info("Sync finished for node [{}] (IP={})", nodeId, nodeAttr.getIp());
    }

    public void removeInfoFromNe(Node node) {
        String nodeId = node.getNodeId().getValue();
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        log.info("Removing XC from node [{}] (IP={})", nodeId, nodeAttr.getIp());

        try {
            RemoveResourceOutput output = neMgr.removeResource(node);
            extractFailObj(nodeAttr.getIp(), new StepResult(nodeId), output.getFailObj());
        } catch (CommonException e) {
            throw new CommonException(CommonExceptionType.DEVICE_ERROR, "write ne error", e);
        }
    }

    public void writeInfo2Ne(Node node) {
        String nodeId = node.getNodeId().getValue();
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        log.info("Writing XC to node [{}] (IP={})", nodeId, nodeAttr.getIp());

        try {
            ConfigNeOutput output = neMgr.configNe(node);
            extractFailObj(nodeAttr.getIp(), new StepResult(nodeId), output.getFailObj());
        } catch (CommonException e) {
            throw new CommonException(CommonExceptionType.DEVICE_ERROR, "write ne error", e);
        }
    }

    private void extractFailObj(String ip, StepResult result, FailObj failObj) throws CommonException {
        if (failObj != null && failObj.getObject() != null && !failObj.getObject().isEmpty()) {
            for (Object obj : failObj.getObject()) {
                if (obj.getMessageInfo() != null && !obj.getMessageInfo().isEmpty()) {
                    log.error("Device [{}] reported error: {}", ip, obj.getMessageInfo());
                    throw new CommonException(CommonExceptionType.DEVICE_ERROR, obj.getMessageInfo());
                }
            }
        }
    }
}
