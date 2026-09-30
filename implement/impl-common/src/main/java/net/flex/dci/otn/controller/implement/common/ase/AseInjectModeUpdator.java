package net.flex.dci.otn.controller.implement.common.ase;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import net.flex.dci.otn.controller.implement.common.impl.ImplActionType;
import net.flex.dci.otn.controller.implement.common.impl.StepResult;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.recorder.StepRecord;
import net.flex.dci.otn.controller.implement.common.utils.BindingThirdLegScope;
import net.flex.dci.otn.controller.implement.common.utils.CommonUtils;
import net.flex.dci.otn.controller.implement.common.utils.NoIpConfigWriteLogger;
import net.flex.dci.otn.controller.implement.common.utils.NoIpConfigWriteLogger.NoIpDeviceError;
import net.flex.dci.otn.controller.implement.common.utils.RouteExtractor;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.FailObj;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.Object;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannelBuilder;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
public class AseInjectModeUpdator {

    private final static String FORMAT_XC = "xc@%s@%s";
    private final static String STATUS_SUCCESS = "success";
    private final static String STATUS_FAILURE = "failure";
    private final static String STATUS_SKIPPED = "skipped";

    private final Gson gson = new Gson();

    private final ImplConfig implConfig = SpringBeanFinder.getBean(ImplConfig.class);
    private final NeManagerRpc neMgr = SpringBeanFinder.getBean(NeManagerRpc.class);


    private ChangedObject changedObject;
    private LifeCycleSevice lifeService;

    public final static String ASE_CONTROL_MODE_DISABLE = "ASE_DISABLED";
    public final static String ASE_CONTROL_MODE_ENABLE = "ASE_ENABLED";
    public final static String ASE_CONTROL_MODE_AUTO = "ASE_AUTO";
    public final static String ASE_CONTROL_MODE_MANUAL = "MANUAL";
    public final static String ASE_CONTROL_MODE = "ase-control-mode";


    public final static String MC_POWER_CONTROL_MODEL_APC = "APC";
    public final static String MC_POWER_CONTROL_MODEL_MANUAL = "MANUAL";
    public final static String MC_SRC_2_DST_POWER_CONTROL_MODEL = "source-to-dest-power-control-mode";
    public final static String MC_DST_2_SRC_POWER_CONTROL_MODEL = "dest-to-source-power-control-mode";


    public AseInjectModeUpdator(ChangedObject changedObject) {
        this.changedObject = changedObject;
    }


    public void setAseControlModel(String tunnelId, String ochLinkId, Long taskGroupId, String mode) {
        setAseControlModel(tunnelId, ochLinkId, taskGroupId, mode, Collections.emptyList());
    }

    public void setAseControlModel(String tunnelId, String ochLinkId, Long taskGroupId, String mode,
            List<String> limitedSiteLinkIds) {
        setAseControlModel(tunnelId, ochLinkId, taskGroupId, mode, limitedSiteLinkIds, null);
    }

    /**
     * Limits a protection-leg operation to the exact XCs persisted in its retry marker.
     * A null XC scope preserves the existing add-leg and normal tunnel behavior.
     */
    public void setAseControlModel(String tunnelId, String ochLinkId, Long taskGroupId, String mode,
            List<String> limitedSiteLinkIds, Collection<String> limitedXcIds) {
        Link ochLink = changedObject.getChangedOchLink(ochLinkId);
        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();

        String taskName = String.format("ASE Control Mode: %d-%d (%s)",
            ochLinkAttr.getLowerFrequency().getValue().longValue(),
            ochLinkAttr.getUpperFrequency().getValue().longValue(),
            mode);

        log.debug("change ase-control-mode to {}, {}", mode, taskName);

        List<CrossConnectionAttributes> xcList = RouteExtractor.extractorXc(ochLinkAttr.getExplictRoute().getRoute());
        // Third-leg binding only changes ASE mode on the newly inserted siteLink path.
        xcList = BindingThirdLegScope.fromSiteLinks(changedObject, limitedSiteLinkIds)
                .filterRouteXcs(xcList);
        xcList = filterByXcIds(xcList, limitedXcIds);

        if (mode.equals(AseInjectModeUpdator.ASE_CONTROL_MODE_AUTO)) {
            //only action on IRA
            xcList = xcList.stream()
                    .filter(xc -> xc.getWssChannel() != null)
                    .filter(xc -> !xc.getCrossConnectionId().getValue().contains("WEST"))
                    .collect(Collectors.toList());
        }

        buildLifeService(tunnelId, null, taskName, taskGroupId);

        Map<String, List<String>> groupedByNodeId = xcList.stream()
                .filter(x->x.getWssChannel() != null)
                .map(x->x.getCrossConnectionId().getValue())
                .collect(Collectors.groupingBy(PhysicalXcIdNamingRule::getNodeId));

        updataIRAXCAttribute(groupedByNodeId, ASE_CONTROL_MODE, mode);
    }

    static List<CrossConnectionAttributes> filterByXcIds(
            List<CrossConnectionAttributes> xcs, Collection<String> limitedXcIds) {
        if (limitedXcIds == null) {
            return xcs;
        }
        Set<String> exactXcIds = new HashSet<>(limitedXcIds);
        return xcs.stream()
                .filter(xc -> exactXcIds.contains(xc.getCrossConnectionId().getValue()))
                .collect(Collectors.toList());
    }


    public void updateMCSrc2DstPowerControlModel(Link ochLink, String mode) {
        if (ochLink == null) {
            return;
        }
        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();

        List<CrossConnectionAttributes> routeXcList = RouteExtractor.extractorXc(ochLinkAttr.getExplictRoute().getRoute());

        log.debug(String.format("Update MediaChannel Src2Dst PowerControlMode: %d-%d (%s)",
                ochLinkAttr.getLowerFrequency().getValue().longValue(),
                ochLinkAttr.getUpperFrequency().getValue().longValue(),
                mode));
        //ochLink的交叉有电层的APS XC， 光层IRA的和DEG的wss XC
        routeXcList.stream()
                .filter(rxc->rxc.getAmplifier() != null && !rxc.getCrossConnectionId().getValue().contains("WEST"))
                .forEach(rxc-> {
                    String nodeId = PhysicalXcIdNamingRule.getNodeId(rxc.getCrossConnectionId().getValue());
                    Node node = changedObject.getChangedPhyNode(nodeId);
                    Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

                    List<CrossConnections> newNodeXcList = nodeAttr.getCrossConnections().stream()
                            .map(nodeXC->{
                                if (rxc.getCrossConnectionId().getValue().equals(nodeXC.getCrossConnectionId().getValue())) {
                                    Properties newProp = PropertyTool.addProperty(nodeXC.getProperties(), MC_SRC_2_DST_POWER_CONTROL_MODEL, mode);
                                    return new CrossConnectionsBuilder(nodeXC)
                                            .setProperties(newProp)
                                            .build();
                                } else {
                                    return nodeXC;
                                }
                            }).collect(Collectors.toList());

                    Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder(nodeAttr)
                                    .setCrossConnections(newNodeXcList)
                                    .build())
                            .build())
                            .build();
                    changedObject.addChangedPhyNode(newNode);
        });
    }

    public void setMCSrc2DstPowerControlModel(Link ochLink, String mode) {
        log.debug("change media channel src2Dst power control model to {} on {}", mode, ochLink.getLinkId().getValue());
        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();

        List<CrossConnectionAttributes> xcList = RouteExtractor.extractorXc(ochLinkAttr.getExplictRoute().getRoute());

        try {
            Map<String, List<String>> groupedByNodeId = xcList.stream()
                    .filter(x->x.getWssChannel() != null)
                    .map(x->x.getCrossConnectionId().getValue())
                    .collect(Collectors.groupingBy(PhysicalXcIdNamingRule::getNodeId));

            updataIRAXCAttribute(groupedByNodeId, MC_SRC_2_DST_POWER_CONTROL_MODEL, mode);
        } catch (Exception e) {
            // Deleting ASE media-channel requires the power-control mode to be
            // switched to MANUAL first. Swallowing this error lets the delete
            // continue and the device rejects it while auto-control is running.
            log.error("set MC power controller error", e);
            throw new RuntimeException("set MC power controller error", e);
        }
    }

    private void updataIRAXCAttribute(Map<String, List<String>> groupedByNodeId, String attributeName, String mode) {
        // Scope workers to one update so this object can be reused without retaining an idle pool.
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        try {
            updataIRAXCAttribute(groupedByNodeId, attributeName, mode, executor);
        } finally {
            // Drain submitted device writes normally; do not interrupt them on caller failure.
            executor.shutdown();
        }
    }

    private void updataIRAXCAttribute(Map<String, List<String>> groupedByNodeId, String attributeName,
            String mode, ExecutorService executor) {
        //remove DGE related OCH XC, only update IRA related.
        log.debug("Set following wssXC to {} \n{}", mode, groupedByNodeId);

        List<CompletableFuture<Void>> futures = groupedByNodeId.keySet()
            .stream().map(nodeId-> CompletableFuture.runAsync(() -> {
                try {
                    Node node = buildWritingNodeFor(nodeId, groupedByNodeId.get(nodeId), attributeName, mode);
                    if (node == null) {
                        // The dummy OCH route may still contain WSS XC IDs that
                        // have already been removed or are no longer active in
                        // cfg/op data. In that case there is no XC to switch to
                        // MANUAL, so skip this node and let the real delete flow
                        // handle the remaining device-side result.
                        log.warn("skip {} update on node {}, no active target wss XC found: {}",
                                attributeName, nodeId, groupedByNodeId.get(nodeId));
                        return;
                    }
                    Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
                    if (nodeAttr == null || nodeAttr.getCrossConnections() == null ||
                            nodeAttr.getCrossConnections().isEmpty()) {
                        // Empty write nodes are treated as configuration errors
                        // so the caller stops before deleting the media-channel.
                        throw new IllegalStateException("no target wss XC to update on node " + nodeId);
                    }
                    if (nodeAttr.getIp() == null && !implConfig.isWriteWithoutIP()) {
                        log.error("cannot update wss XC on node without IP: {}", nodeId);
                        return;
                    }

                    StepResult result = write2Ne(node);
                    StepRecord stepRecord = new StepRecord(node.getNodeId().getValue(),
                            nodeAttr.getFriendlyName(),
                            nodeAttr.getIp() == null ? "" : nodeAttr.getIp());
                    boolean hasError = updateAfterXc(stepRecord, result, node, attributeName, mode);
                    if (lifeService != null) {
                        lifeService.logStatusChanged(stepRecord);
                    }
                    if (hasError) {
                        CommonException deviceError = result.getError().get(0).getException();
                        throw new RuntimeException(deviceError.getMessage(), deviceError);
                    }
                } catch (Exception e) {
                    log.error("error happen", e);
                    throw new CompletionException(e);
                }
            }, executor)).collect(Collectors.toList());

        // Wait for all and collect exceptions
        List<Throwable> errors = new ArrayList<>();
        for (CompletableFuture<Void> future : futures) {
            try {
                future.get();  // This will throw if the task failed
            } catch (ExecutionException ee) {
                errors.add(ee.getCause());  // Root cause from async task
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                errors.add(ie);
            }
        }
        String msg = null;
        if (!errors.isEmpty()) {
            msg = errors.get(0).getMessage();
            if (lifeService != null) {
                lifeService.logEndLinkImpl(msg);
            }
            throw new RuntimeException("Some async tasks failed", errors.get(0));
        } else {
            if (lifeService != null) {
                lifeService.logEndLinkImpl(msg);
            }
        }
    }

    private Node buildWritingNodeFor(String nodeId, List<String> xcIdList, final String attributeName,  String mode) {
        Set<String> targetIds = new HashSet<>(xcIdList);
        List<CrossConnections> finalXcList = new ArrayList<>();

        log.debug("xcIds {}", targetIds);
        Node opNode = null;

        // 1. 先查 OpDB
        if (finalXcList.size() < targetIds.size()) {
            opNode = changedObject.getChangedPhyOpNode(nodeId);
            if (opNode != null) {
                log.debug("checking in opNode");
                processNodeXc(opNode, targetIds, finalXcList, attributeName, mode);
            }
        }

        // 2. 如果还没找齐，再查 ConfigDB
        Node configNode = changedObject.getChangedPhyNode(nodeId);
        if (configNode != null) {
            log.debug("checking in cfgNode");
            processNodeXc(configNode, targetIds, finalXcList, attributeName, mode);
        }
        if (!targetIds.isEmpty()) {
            // Only active WSS XCs need a MANUAL write. Missing, non-WSS, or
            // inactive XCs are ignored here so stale dummy-OCH route data does
            // not block the business OCH implementation.
            log.warn("skip {} update for unmatched target wss XC {} on node {}",
                    attributeName, targetIds, nodeId);
        }
        if (finalXcList.isEmpty()) {
            return null;
        }

        Node baseNode = configNode != null ? configNode : opNode;
        Physical nodeAttr = baseNode.getAugmentation(Node1.class).getPhysical();
        return new NodeBuilder()
                .setNodeId(baseNode.getNodeId())
                .setKey(baseNode.getKey())
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder()
                                .setCrossConnections(finalXcList)
                                .setIp(nodeAttr.getIp())
                                .setFriendlyName(nodeAttr.getFriendlyName())
                                .setNodeType(nodeAttr.getNodeType())
                                .build())
                        .build())
                .build();
    }

    // 抽取出来的公共处理方法
    private void processNodeXc(Node node, Set<String> targetIds, List<CrossConnections> finalXcList, final String attributeName,  String mode) {
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        if (nodeAttr == null || nodeAttr.getCrossConnections() == null) return;

        Properties properties = PropertyTool.addProperty(null, attributeName, mode);

        for (CrossConnections xc : nodeAttr.getCrossConnections()) {
            String xcId = xc.getCrossConnectionId().getValue();

            if (targetIds.contains(xcId) && xc.getWssChannel() != null) {
                if ((xc.getImplementState() != null && xc.getImplementState().equals(ImplementState.Implement)) ||
                        (xc.getAdminState() != null && xc.getAdminState().equals(AdminStatus.Up))) {
                    Properties xcProperties = properties;

                    // ASE control mode is only supported on the OCH A/Z IRA endpoints.
                    // DGE middle-node wssChannel XCs still participate in prototype
                    // copy/restore and MC power control, but must not receive
                    // ase-control-mode writes.
                    if (attributeName.equals(ASE_CONTROL_MODE) && CommonUtils.onDGE(nodeAttr, xc)) {
                        log.debug("skip DGE XC for ASE control mode: {}", xcId);
                        targetIds.remove(xcId);
                        continue;
                    }

                    if (xcId.contains("WEST")) {
                        if (attributeName.equals(MC_SRC_2_DST_POWER_CONTROL_MODEL)) {
                            xcProperties = PropertyTool.addProperty(xcProperties, MC_DST_2_SRC_POWER_CONTROL_MODEL, mode);
                        } else if (attributeName.equals(ASE_CONTROL_MODE)) {
                            //DEG/ILA is not support ASE_CONTROL_MODE
                            log.debug("this is DEG/ILA XC, no ASE_CONTROL_MODE required");
                            targetIds.remove(xcId); //查到一个就移除一个
                            break;
                        }
                    }
                    CrossConnections builtXc = new CrossConnectionsBuilder()
                            .setCrossConnectionId(xc.getCrossConnectionId())
                            .setKey(xc.getKey())
                            .setDescription(xc.getDescription())
                            .setWssChannel(new WssChannelBuilder()
                                    .setLowerFrequency(xc.getWssChannel().getLowerFrequency())
                                    .setUpperFrequency(xc.getWssChannel().getUpperFrequency())
                                    .setProperties(xcProperties)
                                    .build())
                            .build();

                    finalXcList.add(builtXc);

                    log.debug("update the XC {}: {}", xcId, properties);
                    targetIds.remove(xcId); //查到一个就移除一个
                    // Batch dummy-OCH delete may switch several media-channels
                    // on the same NE before deleting them one by one. Continue
                    // scanning only while there are remaining target XCs.
                    if (targetIds.isEmpty()) {
                        break;
                    }
                }
            }
        }
    }

    private boolean updateAfterXc(StepRecord stepRecord, StepResult result, Node changedNode,
                                  String attributeName, String mode) {
        Physical changedNodeAttr = changedNode.getAugmentation(Node1.class).getPhysical();

        List<CrossConnections> successedXcList = new ArrayList<>();
        boolean errFound = false;
        List<StepResult.ErrorInfo> unhandledErrors = result.hasError()
                ? new ArrayList<>(result.getError()) : new ArrayList<>();
        List<CrossConnections> changedXcList = changedNodeAttr.getCrossConnections();

        Set<String> actionObjIds = changedXcList.stream()
                .map(xc -> xc.getCrossConnectionId().getValue())
                .collect(Collectors.toSet());
        boolean hasNodeLevelError = hasNodeLevelError(result, actionObjIds);
        if (hasNodeLevelError) {
            //has some error hasn't find related action obj, put on NE
            CrossConnections firstOne = changedXcList.get(0);
            String name = String.format(FORMAT_XC, firstOne.getCrossConnectionId().getValue(),
                    firstOne.getDescription());

            String errorMsg = unhandledErrors.get(0).getException().getMessage();
            stepRecord.updatePropertyWithError(name, STATUS_FAILURE, errorMsg);
            return true;
        }

        for (CrossConnections xc : changedXcList) {
            String name = String.format(FORMAT_XC, xc.getCrossConnectionId().getValue(),
                    xc.getDescription());

            boolean found = false;
            if (result.hasError()) {
                for (StepResult.ErrorInfo error : result.getError()) {
                    if (error.getObjId().equals(xc.getCrossConnectionId().getValue())) {
                        String errorMsg = error.getException().getMessage();
                        if (shouldSkipMissingMediaChannelOnManual(attributeName, mode, errorMsg)) {
                            // The MANUAL write is a pre-delete protection step.
                            // If the device no longer has this media-channel,
                            // there is nothing to switch and DB must not be
                            // updated as if MANUAL was successfully written.
                            log.warn("skip MANUAL power-control update because media-channel is absent on NE: {}",
                                    xc.getCrossConnectionId().getValue());
                        } else {
                            stepRecord.updatePropertyWithError(name, STATUS_FAILURE, errorMsg);
                            errFound = true;
                        }

                        found = true;
                        unhandledErrors.remove(error);
                        break;
                    }
                }
            }
            if (!found) {
                successedXcList.add(xc);
                stepRecord.updateProperty(name, STATUS_SUCCESS);
            }
        }

        updateDbNode(changedNode.getNodeId().getValue(), successedXcList);

        return errFound;
    }

    //有错误，但是和下发对象ID不匹配，就返回true,代表这个是网元级别错误
    private boolean hasNodeLevelError(StepResult result, Set<String> actionObjIds) {
        if (!result.hasError()) {
            return false;
        }
        for (StepResult.ErrorInfo error : result.getError()) {
            if (actionObjIds.contains(error.getObjId())) {
                // Object-level errors are already tied to the submitted action object.
                return false;
            }
        }
        return true;
    }

    private boolean shouldSkipMissingMediaChannelOnManual(String attributeName, String mode, String errorMsg) {
        return (MC_SRC_2_DST_POWER_CONTROL_MODEL.equals(attributeName)
                || MC_DST_2_SRC_POWER_CONTROL_MODEL.equals(attributeName))
                && errorMsg != null;
    }

    private void updateDbNode(String nodeId, List<CrossConnections> successedXcList) {
        Node dbNode = changedObject.getChangedPhyNode(nodeId);
        if (dbNode == null) {
            // Residual OP-only cleanup can still write MANUAL to the device.
            // Without a config node there is no DB XC to merge back.
            log.error("cannot update DB after ASE mode change, cfg node is missing: {}", nodeId);
            return;
        }
        Physical nodeAttr = dbNode.getAugmentation(Node1.class).getPhysical();

        List<CrossConnections> newXcList = nodeAttr.getCrossConnections().stream().map(xc -> {
            CrossConnections writtenXc = successedXcList.stream()
                    .filter(successedXc -> successedXc.getCrossConnectionId().getValue().equals(xc.getCrossConnectionId().getValue()))
                    .findAny().orElse(null);
            if (xc.getWssChannel() != null && writtenXc != null && writtenXc.getWssChannel() != null) {
                // Keep DB in sync with the exact attributes successfully written to NE.
                // This method handles both ase-control-mode and MC power-control writes;
                // forcing ASE_AUTO here would overwrite unrelated MC updates.
                Properties newProps = mergeProperties(xc.getWssChannel().getProperties(), writtenXc.getWssChannel().getProperties());
                return new CrossConnectionsBuilder(xc)
                        .setWssChannel(new WssChannelBuilder(xc.getWssChannel())
                                .setProperties(newProps)
                                .build())
                        .build();
            } else {
                return xc;
            }
        }).collect(Collectors.toList());

        Node newNode = new NodeBuilder(dbNode).addAugmentation(Node1.class,
                new Node1Builder()
                        .setPhysical(new PhysicalBuilder(nodeAttr).setCrossConnections(newXcList).build())
                        .build())
                .build();

        changedObject.addChangedPhyNode(newNode);
    }

    private Properties mergeProperties(Properties baseProperties, Properties writtenProperties) {
        Properties mergedProperties = baseProperties;
        if (writtenProperties == null || writtenProperties.getProperty() == null) {
            return mergedProperties;
        }

        for (Property property : writtenProperties.getProperty()) {
            mergedProperties = PropertyTool.addProperty(mergedProperties, property.getName(), property.getValue());
        }
        return mergedProperties;
    }

    private StepResult write2Ne(Node newNode) throws CommonException {
        StepResult result = new StepResult(newNode.getNodeId().getValue());
        log.debug(" write to NE: {}", newNode);

        Physical nodeAttr = newNode.getAugmentation(Node1.class).getPhysical();
        if (nodeAttr.getIp() == null && implConfig.isWriteWithoutIP()) {
            //for test, skip TPC NE, 没有IP也可以直接写成功
            addNoIpDeviceErrors(result,
                    NoIpConfigWriteLogger.logSkippedWrite(log, "ase-inject-mode-configNe", ImplActionType.Implement, newNode));
            log.info("no IP, return success directly");
            return result;
        }

        ConfigNeOutput output;
        try {
            output = neMgr.configNe(newNode);
        } catch (CommonException e) {
            log.error("write ne error", e);
            result.addError(newNode.getNodeId().getValue(), e);
            return result;
//            throw exception;
        }

        return extractFailObj(newNode, result, output.getFailObj());
    }

    private void addNoIpDeviceErrors(StepResult result, List<NoIpDeviceError> errors) {
        for (NoIpDeviceError error : errors) {
            result.addError(error.getObjectId(),
                    new CommonException(CommonExceptionType.DEVICE_ERROR, error.getMessage()));
        }
    }

    private StepResult extractFailObj(Node newNode, StepResult result, FailObj failObj) throws CommonException {
        Physical nodeAttr = newNode.getAugmentation(Node1.class).getPhysical();

        if (failObj != null && failObj.getObject() != null && failObj.getObject().size() > 0) {
            for (Object obj : failObj.getObject()) {
                if (obj.getMessageInfo() != null && !obj.getMessageInfo().isEmpty()) {
                    result.addError(obj.getObjectId(),
                            new CommonException(CommonExceptionType.DEVICE_ERROR,
                                    obj.getMessageInfo()));
                }
            }

            log.debug("configNe result has error: {} \n {}", nodeAttr.getIp(),
                    failObj);
            log.info("result of this action is: \n{}", result);
        } else {
            log.debug("write2Ne success {}", nodeAttr.getIp());
        }
        return result;
    }

    private void buildLifeService(String tunnelId, String ochLinkId, String taskName, Long taskGroupId) {
        lifeService = new LifeCycleSevice();
        lifeService.logStartLinkImpl(tunnelId != null ? tunnelId : ochLinkId,
                tunnelId != null ? TaskInfoMessage.ResourceType.tunnel : TaskInfoMessage.ResourceType.ochlink,
                taskName,
                TaskInfoMessage.ActionType.updateDevice,
                "", taskGroupId);
    }

    public void updateMCSrc2DstPowerControlModel(Map<String, List<String>> dummyXcMap, String siteLinkId, Long taskGroupId) {
        try {
            String mode = MC_POWER_CONTROL_MODEL_MANUAL;
            String taskName = String.format("ASE Control Mode: (%s)", mode);

            log.debug("change ase-control-mode to {}", mode);


            lifeService = new LifeCycleSevice();
            lifeService.logStartLinkImpl(siteLinkId,
                    TaskInfoMessage.ResourceType.ochlink,
                    taskName,
                    TaskInfoMessage.ActionType.updateDevice,
                    "", taskGroupId);

            updataIRAXCAttribute(dummyXcMap, MC_SRC_2_DST_POWER_CONTROL_MODEL, MC_POWER_CONTROL_MODEL_MANUAL);
        } catch (Exception e) {
            // SiteLink delete cleanup must not delete residual WSS XCs unless
            // their media-channel power control has been switched to MANUAL.
            log.error("set MC power controller error", e);
        }
    }
}
