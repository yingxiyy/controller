/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.nbi.impl;

import static net.flex.dci.otn.controller.implement.common.utils.CommonUtils.convert;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.implement.common.dto.ContactNeResult;
import net.flex.dci.otn.controller.implement.common.enums.SetResultCode;
import net.flex.dci.otn.controller.implement.common.nbi.impl.BaseImpl;
import net.flex.dci.otn.controller.implement.physical.component.ne.attribute.NeAttributeUpdateStrategy;
import net.flex.dci.otn.controller.implement.physical.component.ne.attribute.NodePhysicalUpdateTaskMessageHandler;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateNodeInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateNodeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateNodeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.update.node.input.Nodes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class NePhysical extends BaseImpl {

    private final PhyNodeDao phyNodeDao;
    private final NeManagerRpc neManagerRpc;
    private final List<NeAttributeUpdateStrategy> neAttributeUpdateStrategies;

    private final NodePhysicalUpdateTaskMessageHandler nodePhysicalUpdateTaskMessageHandler;

    private final int BATCH_SIZE = 10;

    @Autowired
    @Qualifier("updateExecutor")
    private Executor updateNodePhysicalExecutor;

    public NePhysical(PhyNodeDao phyNodeDao, NeManagerRpc neManagerRpc,
            List<NeAttributeUpdateStrategy> neAttributeUpdateStrategies,
            NodePhysicalUpdateTaskMessageHandler nodePhysicalUpdateTaskMessageHandler) {
        this.phyNodeDao = phyNodeDao;
        this.neManagerRpc = neManagerRpc;
        this.neAttributeUpdateStrategies = neAttributeUpdateStrategies;

        this.nodePhysicalUpdateTaskMessageHandler = nodePhysicalUpdateTaskMessageHandler;
    }

    @Override
    public UpdateNodeOutput updateNe(UpdateNodeInput input, TaskInfoMessage taskInfoMessage) {

        if (input.getNodes() == null || input.getNodes().isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Failed to Config phy node because node list is empty");
        }

        try {
//            lockResource(input.getNodes(), locker);
            updatePhysical(input, taskInfoMessage);
        } catch (Exception e) {
            log.error("update node error", e);
            throw e;
        }

        UpdateNodeOutputBuilder outBuilder = new UpdateNodeOutputBuilder();
        outBuilder.setReturnCode(RpcResultType.Success);
        return outBuilder.build();
    }

    private void lockResource(List<Nodes> nodes, ZkResourceLock locker) throws CommonException {
        for (Nodes node : nodes) {
            locker.addResource(node.getNodeId().getValue());
        }
        locker.getLock();
    }

    private void updatePhysical(UpdateNodeInput input, TaskInfoMessage taskInfoMessage)
            throws CommonException {
        ZkResourceLock locker = new ZkResourceLock();
        try {
            lockResource(input.getNodes(), locker);
            List<Nodes> changeNodeList = input.getNodes();
            log.info("start to update the ne physical, node count: {}", changeNodeList.size());
            int changeNodeSize = changeNodeList.size();
            if (changeNodeSize > 1) {
                TaskInfoMessage rootTaskInfoMessage = nodePhysicalUpdateTaskMessageHandler.generateBatchUpdateTaskInfo(
                        changeNodeList, taskInfoMessage);
                executeBatchUpdate(changeNodeList, rootTaskInfoMessage);
            } else if (changeNodeSize == 1) {
                processSingleNode(changeNodeList.get(0), taskInfoMessage);
            }
        } finally {
            locker.unlock();
        }
        log.info("finish to update all ne physical");
    }

    private void executeBatchUpdate(List<Nodes> changeNodeList, TaskInfoMessage taskInfoMessage) {
        log.info("batch update mode, using AsynchronousExecutor, total nodes: {}",
                changeNodeList.size());

        try {
            nodePhysicalUpdateTaskMessageHandler.startLogBatchUpdate(taskInfoMessage);
            validateDuplicateInBatch(changeNodeList);
            List<List<Nodes>> batches = Lists.partition(changeNodeList, BATCH_SIZE);
            log.info("batch update the divided into {} batches,{} nodes per batch", batches.size(),
                    BATCH_SIZE);
            List<String> successNodes = Lists.newArrayList();
            List<String> failedNodes = Lists.newArrayList();
            for (int i = 0; i < batches.size(); i++) {
                List<Nodes> batch = batches.get(i);
                log.info("processing batch {}/{}, size: {}", i + 1, batches.size(), batch.size());
                processBatch(batch, taskInfoMessage, successNodes, failedNodes);
                log.info("batch {}/{} completed, accumulated success: {}, failed: {}",
                        i + 1, batches.size(), successNodes.size(), failedNodes.size());
            }

            int total = changeNodeList.size();
            log.info("batch update completed. success: {}/{}, failed: {}/{}",
                    successNodes.size(), total, failedNodes.size(), total);

            if (!successNodes.isEmpty()) {
                log.info("success nodes: {}", successNodes);
            }
            if (!failedNodes.isEmpty()) {
                log.error("failed nodes: {}", failedNodes);
            }
            String result = String.format("batch update finished. success: %d, failed: %d",
                    successNodes.size(), failedNodes.size());

            nodePhysicalUpdateTaskMessageHandler.endLogBatchUpdate(result, taskInfoMessage,
                    failedNodes.size());

        } catch (Exception e) {
            log.error("batch update failed", e);
            String errorResult = String.format("batch update failed: %s", e.getMessage());
            nodePhysicalUpdateTaskMessageHandler.endLogFailedBatchUpdate(errorResult,
                    taskInfoMessage);
        }

    }

    private void processBatch(List<Nodes> batch, TaskInfoMessage taskInfoMessage,
            List<String> successNodes, List<String> failedNodes) {
        log.debug("process batch method to execute the update node physical info");
        List<CompletableFuture<NodeProcessResult>> futures = batch.stream()
                .map(changeNode -> CompletableFuture.supplyAsync(
                        () -> processSingleNode(changeNode, taskInfoMessage),
                        updateNodePhysicalExecutor
                ))
                .collect(Collectors.toList());

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
        );

        CompletableFuture<List<NodeProcessResult>> allResultsFuture = allFutures.thenApply(v ->
                futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList())
        );
        List<NodeProcessResult> results = allResultsFuture.join();
        log.info("batch update all node process results:{} ", results);
        for (NodeProcessResult result : results) {
            if (result.isSuccess()) {
                successNodes.add(result.getNodeId());
            } else {
                failedNodes.add(result.getNodeId());
                log.error("Node {} failed, errors: {}", result.getNodeId(),
                        result.getErrorMessage());
            }
        }

    }

    private void validateDuplicateInBatch(List<Nodes> changeNodeList) throws CommonException {
        log.info("validate duplicate in batch size:{}", changeNodeList.size());
        Map<String, List<String>> friendlyNameToNodes = new HashMap<>();
        Map<String, List<String>> ipToNodes = new HashMap<>();

        for (Nodes node : changeNodeList) {
            String nodeId = node.getNodeId().getValue();
            Physical physical = node.getPhysical();

            if (physical != null) {
                if (StringUtils.hasText(physical.getFriendlyName())) {
                    String friendlyName = physical.getFriendlyName();
                    friendlyNameToNodes.computeIfAbsent(friendlyName, k -> new ArrayList<>())
                            .add(nodeId);
                }

                if (StringUtils.hasText(physical.getIp())) {
                    String ip = physical.getIp();
                    ipToNodes.computeIfAbsent(ip, k -> new ArrayList<>()).add(nodeId);
                }
            }
        }

        List<String> duplicateFriendlyNames = friendlyNameToNodes.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> String.format("%s(nodes: %d)", entry.getKey(),
                        entry.getValue().size()))
                .collect(Collectors.toList());

        List<String> duplicateIps = ipToNodes.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .map(entry -> String.format("%s(nodes: %d)", entry.getKey(),
                        entry.getValue().size()))
                .collect(Collectors.toList());

        if (!duplicateFriendlyNames.isEmpty() || !duplicateIps.isEmpty()) {
            StringBuilder errorMsg = new StringBuilder("Batch update validation failed:");
            if (!duplicateFriendlyNames.isEmpty()) {
                errorMsg.append(" Duplicate friendlyNames: ").append(duplicateFriendlyNames);
            }
            if (!duplicateIps.isEmpty()) {
                errorMsg.append(" Duplicate IPs: ").append(duplicateIps);
            }
            log.error(errorMsg.toString());
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, errorMsg.toString());
        }

        log.info("Batch update validation passed. No duplicates found.");
    }

    private NodeProcessResult processSingleNode(Nodes changeNode, TaskInfoMessage taskInfoMessage) {
        String nodeId = changeNode.getNodeId().getValue();
        log.info("Start to Config phy node {}", nodeId);

        List<ContactNeResult> results = Lists.newArrayList();

        for (NeAttributeUpdateStrategy neAttributeUpdateStrategy : neAttributeUpdateStrategies) {
            if (neAttributeUpdateStrategy.supports(changeNode)) {
                try {
                    ContactNeResult result = neAttributeUpdateStrategy.execute(changeNode,
                            taskInfoMessage);
                    results.add(result);
                    if (result.getCode() != SetResultCode.SUCCESS) {
                        log.error("Strategy {} failed for node: {}, error: {}",
                                neAttributeUpdateStrategy.getClass().getSimpleName(),
                                nodeId, result.getMessage());
                    }
                } catch (Exception e) {
                    String strategyName = neAttributeUpdateStrategy.getClass().getSimpleName();
                    log.error("Strategy {} exception for node: {}", strategyName, nodeId, e);
                    results.add(ContactNeResult.builder()
                            .code(SetResultCode.FAILED)
                            .message(strategyName + ":" + e.getMessage())
                            .build());
                }
            }
        }

        log.info("Finish to Config phy node {}", nodeId);

        // 检查所有结果，只要有失败的就算失败
        List<ContactNeResult> failedResults = results.stream()
                .filter(r -> r.getCode() != SetResultCode.SUCCESS)
                .collect(Collectors.toList());

        if (failedResults.isEmpty()) {
            return NodeProcessResult.success(nodeId);
        } else {
            String errorMsg = failedResults.stream()
                    .map(r -> r.getMessage())
                    .collect(Collectors.joining("; "));
            return NodeProcessResult.failure(nodeId, errorMsg);
        }
    }

    /**
     * 节点处理结果
     */
    private static class NodeProcessResult {

        private final String nodeId;
        private final boolean success;
        private final String errorMessage;

        private NodeProcessResult(String nodeId, boolean success, String errorMessage) {
            this.nodeId = nodeId;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static NodeProcessResult success(String nodeId) {
            return new NodeProcessResult(nodeId, true, null);
        }

        public static NodeProcessResult failure(String nodeId, String errorMessage) {
            return new NodeProcessResult(nodeId, false, errorMessage);
        }

        public String getNodeId() {
            return nodeId;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    private void validateChangeNodeFrindlyName(Nodes node) {
        validateChangeNodeCommon(node);

        Physical phy = node.getPhysical();
        validateFriendlyName(phy, node.getNodeId().getValue());
    }

    private void validateChangeNodeLoginInfo(Nodes node) {
        validateChangeNodeCommon(node);

        Physical phy = node.getPhysical();
        validateIpPort(phy, node.getNodeId().getValue());
    }

    private void validateChangeNodeCommon(Nodes node) {
        if (node.getNodeId() == null || node.getNodeId().getValue() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Failed to Config phy node because node id is null");
        }

        Physical phy = node.getPhysical();
        if (phy == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Failed to Config phy node without detail info");
        }

        boolean existedNode = phyNodeDao.existsCfgNode(node.getNodeId().getValue());
        if (!existedNode) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find node " + node.getNodeId().getValue());
        }
    }

    private void validateFriendlyName(Physical phy, String neId) {
        log.debug("validate friendly name");
        if (phy.getFriendlyName() == null) {
            return;
        }
        String friendlyName = phy.getFriendlyName();
        if (!StringUtils.hasText(friendlyName)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "ne's friendly-name cannot be empty");
        }
        if (friendlyName.contains(" ")) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "ne's friendly-name cannot contain spaces");
        }

        if (friendlyName.length() > Constant.NodefriendlyNameLength) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "ne's friendly-name should be smaller than " + Constant.NodefriendlyNameLength
                            + " characters");
        }
//        List<Node> nodes = phyNodeDao.getConfigNodeWithFriendlyName(friendlyName);
//        if (!nodes.isEmpty()) {
//            if (nodes.size() > 1) {
//                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                        String.format("find duplicate friendlyName on other node %s",
//                                nodes.stream()
//                                        .map(x -> x.getAugmentation(Node1.class).getPhysical().getFriendlyName())
//                                        .collect(Collectors.toList())));
//            }
//            if (!nodes.get(0).getNodeId().getValue().equals(neId)) {
//                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                        String.format(
//                                "duplicated friendlyName %s on other ne", friendlyName));
//            }
//        }
    }

    private void validateIpPort(Physical phy, String neId) {
        log.debug("validate ip and port modify ");
        if (phy.getIp() == null) {
            return;
        }
        Physical nePhysical = phyNodeDao.getConfigPhysicalByNode(neId);
        if (phy.getPort() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("NE: %s port is mandatory ", nePhysical.getFriendlyName()));
        }
        String ip = nePhysical.getIp() == null ? null : nePhysical.getIp();
        Integer sPort = nePhysical.getPort() == null ? null : nePhysical.getPort().getValue();
        if (ip != null && sPort != null &&
                phy.getIp().equals(ip) &&
                phy.getPort().getValue().equals(sPort)) {
            return;
        }
        if (nePhysical.getSupervisionStatus() == SupervisionStatusType.Monitoring
                && nePhysical.getIp() != null) {
            String errorMessage = "current ne:%s(%s) is supervised,should be unsupervised before modifying ip";
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format(errorMessage, nePhysical.getFriendlyName(),
                            neId));
        }

        if (!isValidIp(ip)) {
            String errorMessage = String.format("the ip %s is not valid", ip);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, errorMessage);
        }
        List<Node> nodes = phyNodeDao.getConfigNodeWithIpPort(phy.getIp(), phy.getPort());
        if (!nodes.isEmpty()) {
            if (nodes.size() > 1) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        String.format("find duplicate ip on other node %s",
                                nodes.stream()
                                        .map(x -> x.getAugmentation(Node1.class).getPhysical()
                                                .getFriendlyName())
                                        .collect(Collectors.toList())));
            }
            if (!nodes.get(0).getNodeId().getValue().equals(neId)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        String.format(
                                "Failed to Config phy node:%s because ip %s and port %s exists on other node",
                                nePhysical.getFriendlyName(),
                                phy.getIp(),
                                phy.getPort().getValue()));
            }
        }
    }

    private boolean isValidIp(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        return isValidIpv4(ip) || isValidIpv6(ip);
    }

    private boolean isValidIpv4(String ip) {
        String ipv4Pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";
        return ip.matches(ipv4Pattern);
    }

    private boolean isValidIpv6(String ip) {
        String ipv6Pattern = "^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$|" +
                "^(?:[0-9a-fA-F]{1,4}:){1,7}:$|" +
                "^(?:[0-9a-fA-F]{1,4}:){1,6}:[0-9a-fA-F]{1,4}$|" +
                "^(?:[0-9a-fA-F]{1,4}:){1,5}(?::[0-9a-fA-F]{1,4}){1,2}$|" +
                "^(?:[0-9a-fA-F]{1,4}:){1,4}(?::[0-9a-fA-F]{1,4}){1,3}$|" +
                "^(?:[0-9a-fA-F]{1,4}:){1,3}(?::[0-9a-fA-F]{1,4}){1,4}$|" +
                "^(?:[0-9a-fA-F]{1,4}:){1,2}(?::[0-9a-fA-F]{1,4}){1,5}$|" +
                "^[0-9a-fA-F]{1,4}:(?::[0-9a-fA-F]{1,4}){1,6}$|" +
                "^:(?::[0-9a-fA-F]{1,4}){1,7}$|" +
                "^::$|" +
                "^::1$|" +
                "^(?:[0-9a-fA-F]{1,4}:){1,4}:(?:[0-9]{1,3}\\.){3}[0-9]{1,3}$";
        return ip.matches(ipv6Pattern);
    }

    private Node newDbNode(Node dbNode, String friendlyName) {
        try {
            String key = "hostName";
            //find out the key in oldProp, and replace it
            PropertyBuilder pb = new PropertyBuilder()
                    .setKey(new PropertyKey(key))
                    .setName(key)
                    .setValue(friendlyName);

            Node1 oldNode1 = dbNode.getAugmentation(Node1.class);
            Physical physical = oldNode1.getPhysical();
            List<Property> proList = new ArrayList<>();
            if (physical.getProperties() != null) {
                proList = oldNode1.getPhysical().getProperties().getProperty();
                proList.removeIf(prop -> prop.getName().equals(key));
            }
            proList.add(pb.build());
            Node1 phyNode = new Node1Builder(oldNode1)
                    .setPhysical(new PhysicalBuilder(oldNode1.getPhysical())
                            .setProperties(new PropertiesBuilder()
                                    .setProperty(proList)
                                    .build())
                            .setFriendlyName(friendlyName)
                            .build())
                    .build();

            Node rst = new NodeBuilder(dbNode)
                    .addAugmentation(Node1.class, phyNode)
                    .build();
            return rst;
        } catch (Exception ex) {
            log.error("failed to update node,{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.DEVICE_ERROR, ex.getMessage(), ex);
        }

    }

    public Node updateFriendlyName(Node dbNode, String friendlyName) {
        String key = "hostName";
        String value = friendlyName;

        Physical physical = dbNode.getAugmentation(Node1.class).getPhysical();
        Properties newProp = updateProperties(physical.getProperties(), key, value);

        return new NodeBuilder(dbNode)
                .addAugmentation(Node1.class,
                        new Node1Builder()
                                .setPhysical(new PhysicalBuilder(physical)
                                        .setFriendlyName(friendlyName)
                                        .setProperties(newProp)
                                        .build())
                                .build())
                .build();
    }

    private Properties updateProperties(Properties properties, String key, String value) {
        List<Property> proList = new ArrayList<>();
        if (properties != null) {
            proList = properties.getProperty();
            proList.removeIf(prop -> prop.getName().equals(key));
        }

        PropertyBuilder pb = new PropertyBuilder()
                .setKey(new PropertyKey(key))
                .setName(key)
                .setValue(value);
        proList.add(pb.build());

        return new PropertiesBuilder().setProperty(proList).build();
    }

    /**
     * ����޸���loginInfo, ����true
     *
     * @param node
     * @param configNode
     * @throws CommonException
     */
    private void updateLoginInfo(Nodes node, Node configNode) throws CommonException {
        log.debug("change ne login info:{}", node);
        Physical physical = configNode.getAugmentation(Node1.class).getPhysical();

        String ip = null;
        PortNumber port = null;
        String user = null;
        String passwd = null;
        if (node.getPhysical().getIp() != null) {
            ip = node.getPhysical().getIp();
        }
        if (node.getPhysical().getPort() != null) {
            port = node.getPhysical().getPort();
        }
        if (node.getPhysical().getLoginName() != null) {
            user = node.getPhysical().getLoginName();
        }
        if (node.getPhysical().getLoginPasswd() != null) {
            passwd = node.getPhysical().getLoginPasswd();
        }

        if (ip != null || port != null || user != null || passwd != null) {
            Node newNode = new NodeBuilder()
                    .setNodeId(configNode.getNodeId())
                    .addAugmentation(Node1.class,
                            new Node1Builder()
                                    .setPhysical(new PhysicalBuilder(physical)
                                            .setIp(ip == null ? physical.getIp() : ip.toLowerCase())
                                            .setPort(port == null ? physical.getPort() : port)
                                            .setLoginName(
                                                    user == null ? physical.getLoginName() : user)
                                            .setLoginPasswd(
                                                    passwd == null ? physical.getLoginPasswd()
                                                            : passwd)
                                            .setAdminState(AdminStatus.Up)
                                            .build())
                                    .build())
                    .build();
            //todo: set current phy node supervision type
            log.debug("update current phy node:{} supervision state to monitoring",
                    configNode.getNodeId());
            phyNodeDao.updateConfigPhyNodeSupervisionState(configNode.getNodeId().getValue(),
                    SupervisionStatusType.Monitoring);
            phyNodeDao.updatePhyNodeLoginInfo(newNode);
        }
    }

//    private Node checkParam(Nodes node) {
//        //check validation of inputing node's parameter
//        if (node.getNodeId() == null || node.getNodeId().getValue() == null) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "Failed to Config phy node because node id is null");
//        }
//
//        Physical phy = node.getPhysical();
//        if (phy == null) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "Failed to Config phy node without detail info");
//        }
//
//        Node mongoNode = phyNodeDao.getConfigPhyNodeById(node.getNodeId().getValue());
//        if (mongoNode == null) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "cannot find node " + node.getNodeId().getValue());
//        }
//
//        checkIpPort(phy, mongoNode);
//        checkFriendlyName(phy);
//        return mongoNode;
//    }

    private void checkFriendlyName(Physical phy) {
        if (phy.getFriendlyName() == null) {
            return;
        }
        if (phy.getFriendlyName().isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "ne's friendly-name cannot be empty");
        }

        String friendlyName = phy.getFriendlyName();
        if (friendlyName.length() > Constant.NodefriendlyNameLength) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "ne's friendly-name should be smaller than " + Constant.NodefriendlyNameLength
                            + " characters");
        }

        if (phyNodeDao.existsNeFriendlyName(friendlyName)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Duplicate friendlyName");
        }

    }

    private void checkIpPort(Physical phy, Node mongoNode) {
        Node1 node1 = mongoNode.getAugmentation(Node1.class);
        if (phy.getIp() == null) {
            return;
        }
        if (phy.getPort() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "NE port is mandatory" + node1.getPhysical().getFriendlyName());
        }

        if (node1 != null) {
            String sIp = null;
            Integer sPort = null;
            if (node1.getPhysical() != null) {
                String ip = node1.getPhysical().getIp();
                if (ip != null) {
                    sIp = ip;
                }
                if (node1.getPhysical().getPort() != null) {
                    sPort = node1.getPhysical().getPort().getValue();
                }

                if (sIp != null && sPort != null &&
                        phy.getIp().equals(sIp) &&
                        phy.getPort().getValue().equals(sPort)) {
                    return;
                }
            }
        }

        if (phyNodeDao.existsIpPort(phy.getIp(), phy.getPort())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Duplicate IP/Port with Node ");
        }
    }

    private void checkDuplicateLoginInfo(Nodes node) throws CommonException {
        List<Node> dbNodes = phyNodeDao.listPhyNodes();
        for (Node dbNode : dbNodes) {
            Node1 phyNode = dbNode.getAugmentation(Node1.class);
            if (phyNode != null && phyNode.getPhysical() != null) {
                Physical phy = phyNode.getPhysical();
                if (phy.getIp() != null && phy.getPort() != null
                        && node.getPhysical().getIp() != null
                        && node.getPhysical().getPort() != null) {
                    if (phy.getIp()
                            .equals(node.getPhysical().getIp())
                            && phy.getPort().getValue().intValue() == node
                            .getPhysical().getPort().getValue().intValue()
                            && !(dbNode.getNodeId().equals(node.getNodeId()))) {
                        throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                                String.format(
                                        "Failed to Config phy node because ip %s and port %s exists on other node",
                                        phy.getIp(),
                                        phy.getPort().getValue()));
                    }
                }
            }
        }
    }

    /**
     * as required the NE's friendlyName will be write to NE as hostName. hostName stored at
     * "properties": { "property": [ { "name": "hostName", "value":
     * "CQ-TH-ODC-TMP-TMP-IOPC4-G-01-1616379022426" } ] }
     *
     * @param node
     * @param dbCfgNode
     * @throws CommonException
     */
    private ContactNeResult updateFriendlyName(Nodes node, Node dbCfgNode) throws CommonException {
        String friendlyName = node.getPhysical().getFriendlyName();
        log.debug("start update node{} friendlyName{}", node.getNodeId().getValue(),
                friendlyName);
        Node newNode = updateFriendlyName(dbCfgNode, friendlyName);
        phyNodeDao.saveConfigPhyNode(newNode);
        //updateOpTree
//        Node dbOpNode = phyNodeDao.getOpPhyNodeById(node.getNodeId().getValue());
        if (phyNodeDao.existsOpNode(node.getNodeId().getValue())) {
            //send to ne
            log.info("config ne:{} friendly name :{}", node.getNodeId(), friendlyName);
            ConfigNeOutput configOutput = neManagerRpc.configNe(
                    updateFriendlyNameInput(dbCfgNode, friendlyName));
            log.info("finish to config ne:{} friendly name :{},result is:{}", node.getNodeId(),
                    friendlyName, configOutput);
            if (configOutput.getFailObj() != null) {
                String failedMessage = convert(configOutput.getFailObj());
                return ContactNeResult.builder().code(SetResultCode.FAILED).message(failedMessage)
                        .build();
            }
//            newNode = updateFriendlyName(dbOpNode, friendlyName);
//            phyNodeDao.saveOpPhyNode(newNode);
        }
        return ContactNeResult.builder().code(SetResultCode.SUCCESS).build();
    }

    private Node updateFriendlyNameInput(Node dbOpNode, String friendlyName) {
        String key = "hostName";
        PropertyBuilder pb = new PropertyBuilder()
                .setKey(new PropertyKey(key))
                .setName(key)
                .setValue(friendlyName);

        Node newNode = new NodeBuilder()
                .setKey(dbOpNode.getKey())
                .setNodeId(dbOpNode.getNodeId())
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder()
                                .setProperties(new PropertiesBuilder().setProperty(
                                        Collections.singletonList(pb.build())).build())
                                .build())
                        .build())
                .build();
        return newNode;
    }

    private ConfigNeInput configNeInput(Node node) {
        List<TerminationPoint> confTps = new ArrayList<TerminationPoint>();
        if (node.getTerminationPoint() != null && !node.getTerminationPoint().isEmpty()) {
            for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint tp : node
                    .getTerminationPoint()) {
                TerminationPoint confTp = new TerminationPointBuilder()
                        .setKey(new TerminationPointKey(
                                new TpId(tp.getTpId())))
                        .setPhysical(tp.getAugmentation(TerminationPoint1.class).getPhysical())
                        .setTpId(new TpId(tp.getTpId())).build();

                confTps.add(confTp);
            }
        }
        ConfigNeInput input = new ConfigNeInputBuilder()
                .setNodeId(node.getNodeId())
                .setPhysical(node.getAugmentation(Node1.class).getPhysical())
                .setTerminationPoint(confTps)
                .build();
        return input;
    }
}

