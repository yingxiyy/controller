/*
 * Copyright (c) 2019 Network Flex Any Comp. and others.
 */
package net.flex.dci.otn.controller.implement.tunnel.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.tools.lock.AbstractResourceLock;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionBidir1To2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BatchRemoveLegInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BatchRemoveLegOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BatchRemoveLegOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.stereotype.Service;

/** Owns the public remove-leg workflow, including device work and final resource cleanup. */
@Service
@Slf4j
@RequiredArgsConstructor
public class ProtectionLegRemovalService {

    private static final AtomicLong GROUP_ID = new AtomicLong(System.currentTimeMillis());

    private final TunnelDao tunnelDao;
    private final ProtectionLegResourceManager removeLegResources;
    private final Set<String> runningRemoveLegOchLinks = ConcurrentHashMap.newKeySet();

    public BatchRemoveLegOutput remove(BatchRemoveLegInput input, String who) {
        LinkedHashMap<String, Tunnel> owners = buildOwners(input == null ? null : input.getTunnelId());
        List<String> errors = new ArrayList<>();
        List<String> alreadyProcessing = new ArrayList<>();

        for (Tunnel owner : owners.values()) {
            try {
                String processingMessage = executeOwner(owner, who);
                if (processingMessage != null) {
                    alreadyProcessing.add(processingMessage);
                }
            } catch (Exception e) {
                // OCH groups are independent; one failure must not hide later task results.
                errors.add(owner.getFriendlyName() + ": " + messageOf(e));
            }
        }

        if (!errors.isEmpty()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "batch remove protection leg failed: " + String.join("; ", errors));
        }
        return new BatchRemoveLegOutputBuilder()
                .setReturnCode(RpcResultType.Success)
                .setReturnMessage(buildSuccessMessage(alreadyProcessing))
                .build();
    }

    LinkedHashMap<String, Tunnel> buildOwners(List<String> tunnelIds) {
        if (tunnelIds == null || tunnelIds.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "batch remove-leg tunnel-id list is empty");
        }

        LinkedHashSet<String> errors = new LinkedHashSet<>();
        LinkedHashSet<String> uniqueIds = new LinkedHashSet<>(tunnelIds);
        if (uniqueIds.size() != tunnelIds.size()) {
            errors.add("batch remove-leg tunnel-id list contains duplicate tunnel: "
                    + findDuplicates(tunnelIds).stream()
                    .map(this::getTunnelDisplayName)
                    .collect(Collectors.joining(", ")));
        }

        LinkedHashMap<String, Tunnel> owners = new LinkedHashMap<>();
        for (String tunnelId : uniqueIds) {
            if (isBlank(tunnelId)) {
                errors.add("Tunnel id is empty");
                continue;
            }
            Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
            if (tunnel == null) {
                errors.add("Tunnel does not exist: " + tunnelId);
                continue;
            }
            try {
                validateTunnel(tunnel);
                String ochLinkId = removeLegResources.getOchLinkByTunnel(tunnel)
                        .getLinkId().getValue();
                if (!owners.containsKey(ochLinkId)) {
                    // Validate every OCH before the first owner changes persistent state.
                    if (!removeLegResources.isBatchThirdLegAlreadyRemoved(tunnel)) {
                        removeLegResources.validateRemoveProtectionLeg(tunnel);
                    }
                    owners.put(ochLinkId, tunnel);
                }
            } catch (Exception e) {
                errors.add(tunnel.getFriendlyName() + ": " + messageOf(e));
            }
        }

        if (!errors.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.join("; ", errors));
        }
        return owners;
    }

    private String executeOwner(Tunnel owner, String who) {
        String ochLinkId = removeLegResources.getOchLinkByTunnel(owner).getLinkId().getValue();
        if (!runningRemoveLegOchLinks.add(ochLinkId)) {
            // 同一个 OCH 的 remove-leg 已经在本进程执行，后续入口不再重复拿同一批 ZK 资源锁。
            log.info("skip duplicate remove-leg owner {}, OCH {} is already processing",
                    owner.getFriendlyName(), ochLinkId);
            return owner.getFriendlyName() + " is already processing remove protection leg";
        }
        TaskInfoMessage root = new TaskInfoMessage(who, TaskInfoMessage.ResourceType.tunnel,
                TaskInfoMessage.ActionType.bind, "");
        root.setGroupId(nextGroupId());
        root.setRoot(true);
        root.setResourceId(owner.getTunnelId().getValue());
        root.setResourceName(owner.getFriendlyName());
        List<TaskInfoMessage> taskInfos = new ArrayList<>();
        taskInfos.add(root);
        AtomicReference<TaskInfoMessage> deviceTask = new AtomicReference<>();
        AbstractResourceLock locker = null;
        boolean locked = false;

        try {
            // One lock covers marker persistence, device removal and final resource cleanup.
            locker = removeLegResources.newRemoveProtectionLegLocker(owner);
            locker.getLock();
            locked = true;
            // The request may have waited behind another remove-leg owner; use the
            // latest tunnel/OCH state before deciding whether device/DB work is still needed.
            owner = reloadOwner(owner);
            if (!removeLegResources.isBatchThirdLegAlreadyRemoved(owner)) {
                boolean deviceRemovalRequired = removeLegResources.prepareProtectionLegRemoval(owner);
                if (deviceRemovalRequired) {
                    deimplementOwner(owner, root, deviceTask);
                }
                // Implemented OCH trims only after device success; Allocate OCH has no device phase.
                TaskInfoMessage resourceTask = removeLegResources.removeProtectionLegResources(owner, root);
                taskInfos.add(resourceTask);
                if (!Boolean.TRUE.equals(resourceTask.getSuccessfully())) {
                    // Keep the failed cleanup child in task center before propagating the owner failure.
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "remove protection leg resource cleanup failed: "
                                    + resourceTask.getErrorReason());
                }
            }
            root.setSuccessfully(true);
            root.setEndTime(System.currentTimeMillis());
            taskInfos.addAll(copyTasksForSameOch(owner,
                    ownerTaskTemplates(taskInfos, deviceTask.get()), true, null));
        } catch (Exception e) {
            root.setSuccessfully(false);
            root.setErrorReason(messageOf(e));
            root.setEndTime(System.currentTimeMillis());
            // Every tunnel under the OCH exposes the owner's exact failure result.
            taskInfos.addAll(copyTasksForSameOch(owner,
                    ownerTaskTemplates(taskInfos, deviceTask.get()),
                    false, root.getErrorReason()));
            if (e instanceof CommonException) {
                throw (CommonException) e;
            }
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "remove protection leg failed: " + messageOf(e), e);
        } finally {
            try {
                // Publish every owner/non-owner result before releasing the OCH remove-leg lock.
                taskInfos.forEach(TaskInfoMessager::sendMessage);
            } finally {
                if (locked && locker != null) {
                    locker.unlock();
                }
                runningRemoveLegOchLinks.remove(ochLinkId);
            }
        }
        return null;
    }

    private String buildSuccessMessage(List<String> alreadyProcessing) {
        if (alreadyProcessing.isEmpty()) {
            return "Batch remove protection leg successfully.";
        }
        return "Batch remove protection leg accepted, already processing: "
                + String.join("; ", alreadyProcessing);
    }

    private Tunnel reloadOwner(Tunnel owner) {
        Tunnel latest = tunnelDao.getTunnelById(owner.getTunnelId().getValue());
        if (latest == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Tunnel does not exist: " + owner.getTunnelId().getValue());
        }
        return latest;
    }

    private void deimplementOwner(Tunnel owner, TaskInfoMessage root,
                                  AtomicReference<TaskInfoMessage> deviceTask) {
        LifeCycleSevice lifeService = new LifeCycleSevice();
        lifeService.logStartLinkImpl(owner.getTunnelId().getValue(),
                TaskInfoMessage.ResourceType.tunnel, owner.getFriendlyName(),
                TaskInfoMessage.ActionType.deimplement, root.getWho(), root.getGroupId());
        new ProtectionLegDeimplementor(owner.getTunnelId().getValue(), lifeService).startSyncAction();

        TaskInfoMessage result = lifeService.getTaskInfoMessage();
        // LifeCycleSevice publishes the owner task itself; retain it only as a template
        // so every non-owner receives the same device child without duplicating the owner record.
        deviceTask.set(result);
        if (result == null || !Boolean.TRUE.equals(result.getSuccessfully())) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "remove protection leg failed: "
                            + (result == null ? "unknown device failure" : result.getErrorReason()));
        }
    }

    private List<TaskInfoMessage> ownerTaskTemplates(List<TaskInfoMessage> taskInfos,
                                                      TaskInfoMessage deviceTask) {
        List<TaskInfoMessage> templates = new ArrayList<>(taskInfos);
        if (deviceTask != null) {
            templates.add(deviceTask);
        }
        return templates;
    }

    private List<TaskInfoMessage> copyTasksForSameOch(Tunnel owner,
                                                       List<TaskInfoMessage> sourceTasks,
                                                       boolean successfully,
                                                       String errorReason) {
        String ownerId = owner.getTunnelId().getValue();
        String ochLinkId = removeLegResources.getOchLinkByTunnel(owner).getLinkId().getValue();
        return tunnelDao.getTunnelNameAndIdUnderOchLink(Arrays.asList(ochLinkId)).stream()
                .filter(tunnel -> tunnel != null && tunnel.getId() != null)
                .filter(tunnel -> !ownerId.equals(tunnel.getId()))
                .flatMap(tunnel -> {
                    long groupId = nextGroupId();
                    return sourceTasks.stream().map(source -> copyTask(source,
                            tunnel.getId(), tunnel.getFriendlyName(), groupId,
                            successfully, errorReason));
                })
                .collect(Collectors.toList());
    }

    TaskInfoMessage copyTask(TaskInfoMessage source, String tunnelId,
                             String tunnelName, long groupId,
                             boolean successfully, String errorReason) {
        TaskInfoMessage copy = new TaskInfoMessage(source);
        copy.setId(null);
        copy.setGroupId(groupId);
        copy.setRoot(source.getRoot());
        copy.setObjectId(source.getObjectId());
        copy.setObjectType(source.getObjectType());
        copy.setScanResultId(source.getScanResultId());
        copy.setResourceId(source.getResourceId());
        copy.setResourceName(source.getResourceName());
        boolean rootTask = source.getRoot() == null || source.getRoot();
        boolean tunnelDeviceTask = TaskInfoMessage.ResourceType.tunnel.equals(source.getResourceType())
                && TaskInfoMessage.ActionType.deimplement.equals(source.getActionType());
        if (rootTask || tunnelDeviceTask) {
            // Each mirrored tunnel owns its root and tunnel-scoped device child.
            copy.setResourceId(tunnelId);
            copy.setResourceName(tunnelName);
        }
        // Root shows the owner summary; child tasks keep their exact execution result.
        copy.setSuccessfully(rootTask ? successfully : source.getSuccessfully());
        copy.setErrorReason(rootTask ? errorReason : source.getErrorReason());
        return copy;
    }

    private void validateTunnel(Tunnel tunnel) {
        if (!ProtectionBidir1To2.class.equals(tunnel.getProtectionType())) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "invalid tunnel, only support 1:3 remove-leg, but is: "
                            + (tunnel.getProtectionType() == null ? "null"
                            : tunnel.getProtectionType().getSimpleName()));
        }
    }

    private Set<String> findDuplicates(List<String> values) {
        Set<String> seen = new HashSet<>();
        Set<String> duplicates = new LinkedHashSet<>();
        for (String value : values) {
            if (!seen.add(value)) {
                duplicates.add(value);
            }
        }
        return duplicates;
    }

    private String getTunnelDisplayName(String tunnelId) {
        if (isBlank(tunnelId)) {
            return "";
        }
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        return tunnel == null || isBlank(tunnel.getFriendlyName())
                ? tunnelId : tunnel.getFriendlyName();
    }

    private static long nextGroupId() {
        return GROUP_ID.incrementAndGet();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static String messageOf(Exception e) {
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }
}
