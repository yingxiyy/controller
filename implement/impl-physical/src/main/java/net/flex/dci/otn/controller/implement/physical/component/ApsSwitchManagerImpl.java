package net.flex.dci.otn.controller.implement.physical.component;

import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.EXCEPTION_REASON_PREFIX;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.FINISH;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.PATH_ALREADY_SET;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.dto.batch.TunnelRelateOchLinkDto;
import net.flex.dci.otn.controller.implement.common.dto.RestoreResult;
import net.flex.dci.otn.controller.implement.common.dto.SwitchResult;
import net.flex.dci.otn.controller.implement.common.enums.ApsMember;
import net.flex.dci.otn.controller.implement.common.enums.CustomApsPath;
import net.flex.dci.otn.controller.implement.common.enums.SetResultCode;
import net.flex.dci.otn.controller.implement.physical.component.aps.ApsCommandHandler;
import net.flex.dci.otn.controller.implement.physical.component.aps.ApsTaskMessageHandler;
import net.flex.dci.otn.controller.implement.physical.util.ApsSwitchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RestoreApsPathInput.TargetApsMember;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.batch.aps._switch.input.ApsSwitch;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 2025/8/12
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class ApsSwitchManagerImpl implements ApsSwitchManager {

    private static final int QUEUE_CAPACITY = 1000;


    private static final Executor ASYNC_EXECUTOR = new ThreadPoolExecutor(
            5, // 核心线程数
            10, // 最大线程数
            60L, TimeUnit.SECONDS, // 空闲线程存活时间
            new LinkedBlockingQueue<>(QUEUE_CAPACITY), // 有界队列（容量1000）
            new ThreadFactoryBuilder()
                    .setNameFormat("aps-switch-executor-%d")
                    .setDaemon(true)
                    .build(),
            new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
    );


    private static final Executor APS_EXECUTOR = new ThreadPoolExecutor(
            10, 20, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(50),
            new ThreadFactoryBuilder().setNameFormat("aps-cross-executor-%d").build(),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    private final TunnelDao tunnelDao;

    private final OchLinkDao ochLinkDao;

    private final CrossConnectionsDao crossConnectionsDao;

    private final ApsCommandHandler apsCommandHandler;

    private final ApsTaskMessageHandler apsTaskMessageHandler;

    @Value("${batch.aps.switch.max-concurrent:20}")
    private int maxConcurrentTasks;

    @Override
    public void batchTunnelApsSwitch(List<String> tunnelIds, ApsSwitch apsSwitch,
            ApsPath targetApsPath,
            TaskInfoMessage rootTaskInfo) {
        log.debug(
                "batch tunnel aps switch,the tunnelIds :{} target Aps Path:{} apsSwitch configuration:{}",
                tunnelIds, targetApsPath, apsSwitch);

        if (tunnelIds == null || tunnelIds.isEmpty()) {
            log.warn("empty tunnelIds, skip batch aps switch");
            return;
        }

        apsTaskMessageHandler.logStartBatchApsSwitch(rootTaskInfo);
        monitorThreadQueueStatus();
        Map<String, List<String>> ochGroupToTunnels = getBatchApsOchTunnelGroups(tunnelIds);
        List<List<String>> batches = Lists.partition(new ArrayList<>(ochGroupToTunnels.keySet()),
                maxConcurrentTasks);
        //        List<List<String>> batches = Lists.partition(tunnelIds, maxConcurrentTasks);
        log.info("batch tunnel aps switch, total tunnels: {}, och links: {}, batches: {}",
                tunnelIds.size(), ochGroupToTunnels.size(), batches.size());
        List<SwitchResult> allResults = new ArrayList<>();
        for (int i = 0; i < batches.size(); i++) {
            List<String> batch = batches.get(i);

            log.info("processing batch {}/{}, och links in batch: {}", i + 1, batches.size(), batch.size());

            Map<String, List<String>> ochRefTunnels = batch.stream()
                    .collect(Collectors.toMap(
                            key -> key,
                            ochGroupToTunnels::get
                    ));

            List<SwitchResult> batchResults = processBatch(ochRefTunnels, targetApsPath,
                    apsSwitch, rootTaskInfo);
            allResults.addAll(batchResults);

            log.info("batch {}/{} completed, results: {}", i + 1, batches.size(),
                    batchResults.size());
        }

        log.debug("batch aps switch result is:{} ", allResults);
        apsTaskMessageHandler.logFishedBatchApsSwitch(rootTaskInfo, allResults);
    }

    private List<SwitchResult> processBatch(Map<String, List<String>> ochGroupToTunnels,
            ApsPath targetApsPath, ApsSwitch apsSwitch, TaskInfoMessage rootTaskInfo) {
        List<CompletableFuture<Map<String, SwitchResult>>> switchFutures = ochGroupToTunnels.entrySet()
                .stream().map(entry -> CompletableFuture.supplyAsync(
                        () -> {
                            String ochLinkId = entry.getKey();
                            List<String> groupTunnelIds = entry.getValue();
                            log.debug("tunnel:{} based on och link :{} single aps switch",
                                    groupTunnelIds, ochLinkId);
                            SwitchResult switchResult = executeSingleApsSwitch(ochLinkId,
                                    targetApsPath, apsSwitch, rootTaskInfo, groupTunnelIds);
                            Map<String, SwitchResult> results = new HashMap<>();
                            for (String tunnelId : groupTunnelIds) {
                                results.put(tunnelId, SwitchResult.builder()
                                        .code(switchResult.getCode())
                                        .tunnelId(tunnelId)
                                        .build());
                            }

                            return results;
                        },
                        ASYNC_EXECUTOR
                ))
                .collect(Collectors.toList());

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                switchFutures.toArray(new CompletableFuture[0])
        );
        CompletableFuture<List<SwitchResult>> allResultsFuture = allFutures.thenApply(
                v -> switchFutures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(map -> map.values().stream())
                        .collect(Collectors.toList()));

        return allResultsFuture.join();
    }


    @Override
    public SwitchResult executeApsSwitch(String neId, String apsName, String apsCrossConnectionId,
            ApsPath targetPath, TaskInfoMessage taskInfo) {
        log.debug("execute aps switch command neId:{} apsName:{} apsPath:{}", neId, apsName,
                targetPath);
        CustomApsPath customApsPath = CustomApsPath.fromApsPath(targetPath);
        CrossConnections apsCrossConnection = crossConnectionsDao.getXCById(apsCrossConnectionId);
        return apsCommandHandler.executeCommand(neId, apsCrossConnection, apsName, customApsPath,
                taskInfo);
    }

    @Override
    public RestoreResult executeRestore(String neId, String apsName, String apsCrossConnectionId,
            TargetApsMember restoreMember, TaskInfoMessage restoreTaskInfo) {
        log.debug("execute restore aps path command through ne:{} apsName:{} member:{}", neId,
                apsName, restoreMember);
        ApsMember apsMember = ApsMember.fromTargetApsMember(restoreMember);
        CrossConnections apsCrossConnection = crossConnectionsDao.getXCById(apsCrossConnectionId);
        return apsCommandHandler.executeRestoreCommand(neId, apsCrossConnection, apsName, apsMember,
                restoreTaskInfo);
    }


    /**
     * monitor thread queue status
     */
    private void monitorThreadQueueStatus() {
        if (ASYNC_EXECUTOR instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) ASYNC_EXECUTOR;
            int queueSize = executor.getQueue().size();
            int activeCount = executor.getActiveCount();
            if (queueSize > QUEUE_CAPACITY * 0.8) {
                log.warn("Thread pool queue is near capacity:{}/{}", queueSize, QUEUE_CAPACITY);
            }
            log.debug("aps Thread pool status: Active={},Queue={},PoolSize={}", activeCount,
                    queueSize, executor.getPoolSize());
        }
        if (APS_EXECUTOR instanceof ThreadPoolExecutor) {
            ThreadPoolExecutor executor = (ThreadPoolExecutor) APS_EXECUTOR;
            int queueSize = executor.getQueue().size();
            int activeCount = executor.getActiveCount();
            if (queueSize > QUEUE_CAPACITY * 0.8) {
                log.warn("aps executor Thread pool queue is near capacity:{}/{}", queueSize,
                        QUEUE_CAPACITY);
            }
            log.debug("aps executor Thread pool status: Active={},Queue={},PoolSize={}",
                    activeCount,
                    queueSize, executor.getPoolSize());
        }
    }

    private SwitchResult executeSingleApsSwitch(String ochLinkId, ApsPath targetApsPath,
            ApsSwitch apsSwitch, TaskInfoMessage rootTaskInfo, List<String> associatedTunnelIds) {
        log.debug(
                "execute single och aps switch :{} and aps switch command:{} associatedTunnelIds:{}",
                ochLinkId,
                apsSwitch, associatedTunnelIds);
        try {
            Link ochLink = ochLinkDao.getOchLinkByLinkId(ochLinkId);
            List<CrossConnections> apsCrossConnections = getApsCrossConnections(ochLink);
            String username = rootTaskInfo.getWho();
            Long groupId = rootTaskInfo.getGroupId();
            List<Tunnel> associateTunnels = tunnelDao.listAllTunnelByIds(associatedTunnelIds);
            //aps switch cmd send
            List<CompletableFuture<List<SwitchResult>>> apsSwitchFutures = apsCrossConnections.stream()
                    .map(apsCrossConnection -> CompletableFuture.supplyAsync(
                                    () -> sendApsSwitchCommand2Ne(ochLink, apsCrossConnection,
                                            targetApsPath,
                                            apsSwitch, username, groupId, associateTunnels),
                                    APS_EXECUTOR)
                            .exceptionally(ex -> {
                                // 异常处理：记录日志并返回空结果或错误结果
                                log.error("Failed to send APS switch command", ex);
                                return Collections.emptyList(); // 或者返回包含错误信息的SwitchResult
                            }))
                    .collect(Collectors.toList());
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                    apsSwitchFutures.toArray(new CompletableFuture[0]));
            CompletableFuture<List<SwitchResult>> allResultsFuture = allFutures.thenApply(
                    v -> apsSwitchFutures.stream().map(CompletableFuture::join)
                            .flatMap(List::stream)
                            .collect(
                                    Collectors.toList()));
            List<SwitchResult> switchResults = allResultsFuture.join();
            log.debug("switch results:{}", switchResults);
            long failedCount = switchResults.stream()
                    .filter(switchResult -> switchResult.getCode().equals(SetResultCode.FAILED))
                    .count();
            SetResultCode switchResultCode =
                    failedCount > 0L ? SetResultCode.FAILED : SetResultCode.SUCCESS;
            return SwitchResult.builder().code(switchResultCode).build();
        } catch (Exception ex) {
            log.error("failed to execute ochLink:{} single aps switch,the reason is:{}", ochLinkId,
                    ex.getMessage(), ex);
            return SwitchResult.builder().code(SetResultCode.FAILED).ochLinkId(ochLinkId)
                    .message(ex.getMessage()).build();
        }
    }

    /**
     * @param apsCrossConnection
     * @param apsSwitch
     */
    private List<SwitchResult> sendApsSwitchCommand2Ne(Link ochLink,
            CrossConnections apsCrossConnection,
            ApsPath targetPath,
            ApsSwitch apsSwitch, String username, Long groupId, List<Tunnel> associatedTunnels) {
        log.debug("execute the aps switch command to och link :{}", ochLink.getLinkId().getValue());
        String apsCrossConnectionId = apsCrossConnection.getCrossConnectionId().getValue();
        String apsName = apsCrossConnection.getAps().getName();
        String refNeId = apsCrossConnection.getNodeRef().getValue();
        log.debug("send aps switch command to ne:{},the aps cross connection:{} ref ochLinkId :{}",
                refNeId, apsCrossConnectionId, ochLink.getLinkId().getValue());
        //todo：two step switch configuration set
        List<SwitchResult> switchResults = new LinkedList<>();
        if (targetPath != null) {
            SwitchResult switchResult = apsTargetPathSet(ochLink, apsCrossConnection, username,
                    groupId, targetPath, associatedTunnels);
            switchResults.add(switchResult);
        }
        if (apsSwitch != null && apsSwitch.getAps() != null) {
            SwitchResult configSwitchResult = configApsSwitch(username, apsName, ochLink,
                    apsCrossConnection, refNeId, groupId, apsSwitch, associatedTunnels);
            switchResults.add(configSwitchResult);
        }
        log.debug("current apsSwitchCrossConnection :{} switch Command switchResult is:{}",
                apsCrossConnectionId, switchResults);
        return switchResults;

    }

    private SwitchResult configApsSwitch(String username, String apsName,
            Link ochLink, CrossConnections apsCrossConnection, String refNeId, Long groupId,
            ApsSwitch apsSwitch, List<Tunnel> associatedTunnels) {
        String ochLinkId = ochLink.getLinkId().getValue();
        List<String> associatedTunnelIds = associatedTunnels.stream().map(Tunnel::getTunnelId)
                .map(Uri::getValue).collect(
                        Collectors.toList());
        String apsCrossConnectionId = apsCrossConnection.getCrossConnectionId().getValue();
        log.debug("start to config och link:{} the aps switch:{} ref ne:{} attribute :{}",
                ochLinkId, apsCrossConnectionId, refNeId, apsSwitch);
        //to set aps switch configuration to the apsCrossConnection
        ApsSwitch switchConfig = ApsSwitchUtils.compareAndGetApsSwitchConfig(apsSwitch,
                apsCrossConnection.getAps());
        if (switchConfig == null) {
            associatedTunnels.forEach(
                    tunnel -> apsTaskMessageHandler.logApsConfigSubTask(tunnel, ochLinkId,
                            username, apsCrossConnectionId, refNeId, groupId, apsName,
                            apsSwitch, SetResultCode.SUCCESS,
                            "Configuration already up-to-date"));
            return SwitchResult.builder()
                    .code(SetResultCode.SUCCESS)
                    .message("Configuration already up-to-date")
                    .ochLinkId(ochLinkId)
                    .associatedTunnelIds(associatedTunnelIds)
                    .build();
        }
        //only send the config result
        List<TaskInfoMessage> subTasks = associatedTunnels
                .stream()
                .map(tunnel -> apsTaskMessageHandler.logStartApsConfigSubTask(tunnel,
                        ochLink.getLinkId().getValue(), username, apsCrossConnectionId, apsName,
                        refNeId,
                        groupId, switchConfig)).collect(Collectors.toList());
        try {
            SwitchResult configResult = apsCommandHandler.configAps(refNeId, apsCrossConnectionId,
                    apsName, switchConfig);
            subTasks.forEach(subTask -> apsTaskMessageHandler.logFinishApsConfigSubTask(
                    subTask,
                    configResult.getCode(), configResult.getMessage()));
            return SwitchResult.builder()
                    .code(configResult.getCode())
                    .message(configResult.getMessage())
                    .ochLinkId(ochLinkId)
                    .associatedTunnelIds(associatedTunnelIds)
                    .build();
        } catch (Exception ex) {
            log.error("failed to config aps target path,the reason is:{}", ex.getMessage(), ex);
            subTasks.forEach(subTask -> apsTaskMessageHandler.logFinishApsConfigSubTask(
                    subTask, SetResultCode.FAILED, EXCEPTION_REASON_PREFIX + ex.getMessage()));
            return SwitchResult.builder()
                    .code(SetResultCode.FAILED)
                    .message(ex.getMessage())
                    .ochLinkId(ochLinkId)
                    .associatedTunnelIds(associatedTunnelIds)
                    .build();
        }

    }

    /***
     * asp target path set to och link
     * @param ochLink
     * @param apsCrossConnection
     * @param username
     * @param groupId
     * @param targetPath
     * @param associatedTunnels
     * @return
     */
    private SwitchResult apsTargetPathSet(Link ochLink, CrossConnections apsCrossConnection,
            String username, Long groupId, ApsPath targetPath, List<Tunnel> associatedTunnels) {
        String refNeId = apsCrossConnection.getNodeRef().getValue();
        String apsCrossConnectionId = apsCrossConnection.getCrossConnectionId().getValue();
        String apsName = apsCrossConnection.getAps().getName();
        String ochLinkId = ochLink.getLinkId().getValue();
        List<String> associatedTunnelIds = associatedTunnels.stream().map(Tunnel::getTunnelId)
                .map(Uri::getValue).collect(
                        Collectors.toList());
        log.debug(
                "send aps switch to target path command to ne:{},the aps cross connection:{} ref ochLink:{} ref tunnel :{} targetPath",
                refNeId, apsCrossConnectionId, ochLinkId, associatedTunnelIds);
        ApsPath currentApsControlPath = apsCrossConnection.getAps().getForceToPort();
        if (targetPath == currentApsControlPath) {
            log.warn(
                    "set target path is same as aps crossConnection:{} current apsControlPath:{},do nothing!",
                    apsCrossConnectionId, currentApsControlPath);
            for (Tunnel tunnel : associatedTunnels) {
                apsTaskMessageHandler.logApsSwitchControlSubTask(
                        tunnel, ochLinkId, apsCrossConnection, username, groupId, targetPath,
                        SetResultCode.SUCCESS, PATH_ALREADY_SET);
            }
            return SwitchResult.builder().code(SetResultCode.SUCCESS).message(FINISH)
                    .ochLinkId(ochLinkId)
                    .associatedTunnelIds(associatedTunnelIds)
                    .build();
        }
        List<TaskInfoMessage> subTaskInfos = associatedTunnels.stream()
                .map(tunnel -> apsTaskMessageHandler.logStartApsSwitchControlSubTask(
                        tunnel, ochLinkId, apsCrossConnection, username, groupId, targetPath))
                .collect(Collectors.toList());
        try {
            //to execute aps switch command
            CustomApsPath customApsPath = CustomApsPath.fromApsPath(targetPath);
            SwitchResult switchResult = apsCommandHandler.executeCommand(refNeId,
                    apsCrossConnection, apsName, customApsPath,
                    null);
            subTaskInfos.forEach(subTaskInfo ->
                    apsTaskMessageHandler.logFinishApsSwitchControlSubTask(
                            subTaskInfo,
                            switchResult.getCode(),
                            switchResult.getMessage())
            );

            return SwitchResult.builder()
                    .code(switchResult.getCode())
                    .message(switchResult.getMessage())
                    .ochLinkId(ochLinkId)
                    .associatedTunnelIds(associatedTunnelIds)
                    .build();
        } catch (Exception ex) {
            log.error("failed to set aps target path,the reason is:{}", ex.getMessage(), ex);
            subTaskInfos.forEach(subTaskInfo ->
                    apsTaskMessageHandler.logFinishApsSwitchControlSubTask(
                            subTaskInfo,
                            SetResultCode.FAILED,
                            EXCEPTION_REASON_PREFIX + ex.getMessage())
            );
            return SwitchResult.builder()
                    .code(SetResultCode.FAILED)
                    .message(ex.getMessage())
                    .ochLinkId(ochLinkId)
                    .associatedTunnelIds(associatedTunnelIds)
                    .build();
        }
    }

    private List<CrossConnections> getApsCrossConnections(Link ochLink) {
        log.debug("get aps cross connections by the och link id:{}", ochLink.getLinkId());
        Och ochLinkPhysical = ochLink.getAugmentation(Link1.class).getOch();
        Class<? extends ProtectionType> protectType = ochLinkPhysical.getProtectionType();
        log.debug("current och link :{} protection Type is:{}", ochLink.getLinkId(),
                protectType.getCanonicalName());
        List<Route> linkRoutes = ochLinkPhysical.getExplictRoute().getRoute();
        //assemble current the link only have one route;
        Route linkRoute = linkRoutes.get(0);
        //only in primary route
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> crossConnections = linkRoute.getPrimary()
                .getCrossConnections();
        List<String> apsCrossConnections = crossConnections.stream()
                .filter(crossConnection -> crossConnection.getAps() != null)
                .map(CrossConnectionAttributes::getCrossConnectionId)
                .map(Uri::getValue)
                .collect(
                        Collectors.toList());
        log.debug("the aps cross connection id is:{}", apsCrossConnections);
        List<CrossConnections> realApsCrossConnections = apsCrossConnections.stream()
                .map(crossConnectionsDao::getXCById).collect(Collectors.toList());
        return realApsCrossConnections;
    }


    private Map<String, List<String>> getBatchApsOchTunnelGroups(List<String> tunnelIds) {
        log.debug("dividing the tunnel:{} group by ochLink", tunnelIds);
        Map<String, List<String>> ochGroupToTunnels = new HashMap<>();
        List<TunnelRelateOchLinkDto> tunnelRelateOchLinkDtos = tunnelDao.retrieveAllTunnelRelateOchLinkByTunnelIds(
                tunnelIds);

        for (TunnelRelateOchLinkDto tunnelRelateOchLinkDto : tunnelRelateOchLinkDtos) {
            String tunnelId = tunnelRelateOchLinkDto.getTunnelId();
            String ochLinkId = tunnelRelateOchLinkDto.getOchLinkId();
            ochGroupToTunnels.computeIfAbsent(ochLinkId, k -> new ArrayList<>()).add(tunnelId);
        }

        return ochGroupToTunnels;
    }
}
