/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.tunnel.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import net.flex.dci.otn.controller.implement.common.enums.BusinessLinkType;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.opList.OpType;
import net.flex.dci.otn.controller.implement.common.opList.TypeUtils;
import net.flex.dci.otn.controller.implement.common.service.MyExecutor;
import net.flex.dci.otn.controller.implement.common.utils.ActionNotificationMessage;
import net.flex.dci.otn.controller.implement.common.utils.BindingThirdLegScope;
import net.flex.dci.otn.controller.implement.common.utils.NeManagementChecker;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelSyncInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelSyncInputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelSyncOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelSyncOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.attributes.SupportingLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 目的是复用段分组，这样不同复用段的业务可以同时下发 然后由于TunnelImplementor里面有zkResourceLock, 同样复用段资源的tunnel，多于两个进去必然会锁一个，
 * 这样在TunnelImplementor中提取释放光层后，两个线程再次竞争，如果是同一个ochLink, 那就还是锁，提取释放光层就没有意义了， 所以需要放9个线程进去，
 * 然后需要有不同光层，这样提前释放光层资源后， 新的ochLink 才可以提前下发 （因为电层下发慢） 因为现在一个ochLink 最多8个Tunnel, 所以放9个进去必然达到上面的目的
 */
@Slf4j
@Component
public class TunnelImplementSync {

    private static final int GROUP_MAX_PARALLEL = 9;
    private static final String BINDING_3_RD_LEG_RESTORE_TUNNEL = "binding3rdLegRestoreTunnel";

    @Autowired
    private ImplConfig implConfig;

    @Autowired
    private NeManagementChecker neManagementChecker;

    @Autowired
    private MyExecutor myExecutor;

    @Autowired
    private TunnelDao tunnelDao;

    // group scheduler states
    private final ConcurrentHashMap<String, GroupState> groupById = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> siteLinkToGroupId = new ConcurrentHashMap<>();
    private final Deque<String> readyGroups = new ArrayDeque<>();
    private final Set<String> readyGroupSet = new HashSet<>();
    private final Set<String> processingTunnelIds = new HashSet<>();
    private final Set<String> bindingThirdLegOwnerOchLinks = new HashSet<>();
    private final ReentrantLock registryLock = new ReentrantLock();
    private final AtomicLong groupIdSeq = new AtomicLong(0);
    private final AtomicLong batchIdSeq = new AtomicLong(0);


    //===================================================
    //
    // init env
    //
    //===================================================
    public UpdateTunnelSyncOutput start(UpdateTunnelSyncInput input, String who)
            throws CommonException {
        ChangedObject cache = new ChangedObject();

        OpType opType = checkBasicParam(input);
        ActionType actionType =
                ImplementState.Implement.equals(input.getImplementState()) ?
                        ActionType.implement : ActionType.deimplement;
        List<String> tunnelIds = uniqueTunnelIds(input);
        List<TunnelTask> tasks = new ArrayList<>();
        List<String> notificationNames = new ArrayList<>();
        List<String> skippedSameStateTunnels = new ArrayList<>();
        List<String> registeredOwnerOchLinks = new ArrayList<>();
        String batchId = nextBatchId();
        boolean enqueueStarted = false;

        registryLock.lock();
        try {
            for (String tunnelId : tunnelIds) {
                Tunnel stateCheckedTunnel = getTunnelForStateCheck(cache, tunnelId);
                if (isAlreadyTargetState(cache, stateCheckedTunnel,
                        input.getImplementState())) {
                    // Batch update should skip no-op tunnels instead of aborting the whole request.
                    skippedSameStateTunnels.add(stateCheckedTunnel.getFriendlyName());
                    log.info("skip tunnel {} because current implementState is already {}",
                            stateCheckedTunnel.getFriendlyName(), input.getImplementState());
                    continue;
                }

                CheckedTunnel checkedTunnel = checkParam(cache, input, tunnelId, opType);
                Tunnel tunnel = checkedTunnel.tunnel;
                notificationNames.add(tunnel.getFriendlyName());

                EnqueuePlan plan = buildEnqueuePlan(cache, input, tunnel,
                        checkedTunnel.siteLinks, actionType, batchId);
                if (plan.skipCurrent) {
                    log.info(
                            "skip duplicate binding third leg restore tunnel {}, owner is already queued or running on OCH {}",
                            tunnel.getFriendlyName(), plan.ochLinkId);
                }
                if (plan.ownerTaskAdded) {
                    registeredOwnerOchLinks.add(plan.ochLinkId);
                }
                tasks.addAll(plan.tasks);
            }

            sortBindingThirdLegOwners(tasks);
            validateBindingDependentsHaveOwner(tasks);
            validateNoQueuedNormalTasks(tasks);
            enqueueStarted = true;
            tasks.forEach(task -> enqueueTaskLocked(task, actionType, who));
        } catch (RuntimeException e) {
            if (!enqueueStarted) {
                // Roll back owners registered during planning; they have no queued task to clear them.
                registeredOwnerOchLinks.forEach(bindingThirdLegOwnerOchLinks::remove);
            }
            throw e;
        } finally {
            registryLock.unlock();
        }

        if (!skippedSameStateTunnels.isEmpty()) {
            skippedSameStateTunnels.forEach(name -> ActionNotificationMessage
                .sendLinkImplementMethodDoneNotification(BusinessLinkType.TUNNEL, name,
                    actionType));
        }

        if (!tasks.isEmpty()) {
            notificationNames.forEach(name -> ActionNotificationMessage
                    .sendLinkImplementMethodStartNotification(BusinessLinkType.TUNNEL, name,
                            actionType));
            dispatchReadyTasks();
            return new UpdateTunnelSyncOutputBuilder()
                    .setReturnCode(RpcResultType.AcceptAndStartAsync)
                    .setReturnMessage("start async processing")
                    .build();
        }

        return new UpdateTunnelSyncOutputBuilder()
                .setReturnCode(RpcResultType.Success)
                .setReturnMessage(buildNoTaskMessage(skippedSameStateTunnels))
                .build();
    }

    private Tunnel getTunnelForStateCheck(ChangedObject cache, String tunnelId) {
        Tunnel tunnel = cache.getChangedTunnel(tunnelId);
        if (tunnel == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find required tunnel " + tunnelId);
        }
        return tunnel;
    }

    private boolean isAlreadyTargetState(ChangedObject cache, Tunnel tunnel,
            ImplementState targetState) {
        if (!tunnel.getImplementState().equals(targetState)) {
            return false;
        }
        if (!ImplementState.Implement.equals(targetState)) {
            return true;
        }

        // A completed device flow may fail during third-leg finalization after
        // the tunnel reaches Implement. Keep it eligible until both markers are cleared.
        return !isBindingThirdLegRestoreTunnel(tunnel)
                && !isBindingThirdLegOch(cache, ochLinkId(tunnel));
    }

    private String buildNoTaskMessage(List<String> skippedSameStateTunnels) {
        if (!skippedSameStateTunnels.isEmpty()) {
            return "all requested tunnels are already target-state";
        }
        return "binding third leg owner is already processing";
    }

    private EnqueuePlan buildEnqueuePlan(ChangedObject cache, UpdateTunnelSyncInput input, Tunnel tunnel,
            List<String> siteLinks, ActionType actionType, String batchId) {
        String ochLinkId = ochLinkId(tunnel);
        EnqueuePlan plan = new EnqueuePlan(ochLinkId);
        boolean implement = ActionType.implement.equals(actionType);
        boolean bindingThirdLegOch = false;
        boolean currentIsRestoreTunnel = false;
        List<Tunnel> restoreTunnels = Collections.emptyList();

        if (implement) {
            bindingThirdLegOch = isBindingThirdLegOch(cache, ochLinkId);
            List<Tunnel> sameOchTunnels = tunnelDao.getAllTunnelsUnderOchLink(
                    Arrays.asList(ochLinkId));
            restoreTunnels = sameOchTunnels.stream()
                    .filter(this::isBindingThirdLegRestoreTunnel)
                    // Bind-third-leg is an OCH-level action; OCH source TP decides owner order.
                    .sorted(Comparator.comparing(this::bindingOwnerSortKey))
                    .collect(Collectors.toList());
            // OCH marker and restore-tunnel markers are written and cleared as one third-leg transaction.
            validateBindingThirdLegMarkers(bindingThirdLegOch, restoreTunnels, ochLinkId);
            currentIsRestoreTunnel = bindingThirdLegOch && isBindingThirdLegRestoreTunnel(tunnel);
            Tunnel owner = restoreTunnels.isEmpty() ? null : restoreTunnels.get(0);

            if (!restoreTunnels.isEmpty() && !isBindingOwnerRegistered(ochLinkId)
                    && tryRegisterBindingOwner(ochLinkId)) {
                addBindingOwnerTask(plan, input, siteLinks, ochLinkId, owner,
                        restoreTunnels, batchId);
            }

            if (currentIsRestoreTunnel) {
                if (owner == null || !owner.getTunnelId().getValue()
                        .equals(tunnel.getTunnelId().getValue())) {
                    plan.skipCurrent = true;
                    return plan;
                }
                if (!plan.ownerTaskAdded) {
                    plan.skipCurrent = true;
                    return plan;
                }
                return plan;
            }
        }

        // update-tunnel-sync only accepts implement/deimplement. Implement needs
        // one same-OCH owner before followers run electric-only; deimplement stays
        // same-OCH serial because only the last tunnel may release OCH resources.
        plan.tasks.add(new TunnelTask(input, new LifeCycleSevice(), siteLinks, ochLinkId,
                true,
                currentIsRestoreTunnel, tunnel, batchId, Collections.emptyList(),
                bindingThirdLegOch
                        && ImplementState.Allocate.equals(tunnel.getImplementState())));
        return plan;
    }

    private void addBindingOwnerTask(EnqueuePlan plan, UpdateTunnelSyncInput input,
            List<String> siteLinks, String ochLinkId, Tunnel owner,
            List<Tunnel> restoreTunnels, String batchId) {
        UpdateTunnelSyncInput ownerInput = new UpdateTunnelSyncInputBuilder(input)
                .setTunnelId(Collections.singletonList(owner.getTunnelId().getValue()))
                .build();
        List<BindingThirdLegCompletionTask> completionTasks =
                buildBindingThirdLegCompletionTasks(owner, restoreTunnels);
        // Allocate tunnels under the same OCH must wait until this restore owner
        // clears binding3rdLeg; otherwise they may parse the stale third-leg route.
        plan.tasks.add(new TunnelTask(ownerInput, new LifeCycleSevice(),
                siteLinks, ochLinkId, true, true, owner, batchId,
                completionTasks, false));
        plan.ownerTaskAdded = true;
    }

    private List<BindingThirdLegCompletionTask> buildBindingThirdLegCompletionTasks(Tunnel owner,
            List<Tunnel> restoreTunnels) {
        List<BindingThirdLegCompletionTask> completionTasks = new ArrayList<>();
        for (Tunnel restoreTunnel : restoreTunnels) {
            if (restoreTunnel.getTunnelId().equals(owner.getTunnelId())) {
                continue;
            }
            completionTasks.add(new BindingThirdLegCompletionTask(restoreTunnel));
        }
        return completionTasks;
    }

    private String bindingOwnerSortKey(Tunnel tunnel) {
        return OchLinkIdNamingRule.getTpAId(ochLinkId(tunnel));
    }

    private boolean tryRegisterBindingOwner(String ochLinkId) {
        registryLock.lock();
        try {
            return bindingThirdLegOwnerOchLinks.add(ochLinkId);
        } finally {
            registryLock.unlock();
        }
    }

    private boolean isBindingOwnerRegistered(String ochLinkId) {
        registryLock.lock();
        try {
            return bindingThirdLegOwnerOchLinks.contains(ochLinkId);
        } finally {
            registryLock.unlock();
        }
    }

    private boolean isBindingThirdLegRestoreTunnel(Tunnel tunnel) {
        return tunnel != null && PropertyTool.existProperty(tunnel.getProperties(),
                BINDING_3_RD_LEG_RESTORE_TUNNEL);
    }

    private void validateBindingThirdLegMarkers(boolean bindingThirdLegOch,
            List<Tunnel> restoreTunnels, String ochLinkId) {
        boolean hasRestoreTunnelMarker = !restoreTunnels.isEmpty();
        if (bindingThirdLegOch == hasRestoreTunnelMarker) {
            return;
        }
        String msg = bindingThirdLegOch
                ? "OCH has binding3rdLeg but no restore tunnel marker: "
                : "restore tunnel marker exists but OCH has no binding3rdLeg: ";
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                msg + ochLinkId);
    }

    private boolean isBindingThirdLegOch(ChangedObject cache, String ochLinkId) {
        Link ochLink = cache.getChangedOchLink(ochLinkId);
        if (ochLink == null) {
            return false;
        }
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1 ochAug =
                ochLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class);
        if (ochAug == null) {
            return false;
        }
        Och ochLinkAttr = ochAug.getOch();
        return ochLinkAttr != null && BindingThirdLegScope.isBindingThirdLeg(ochLinkAttr);
    }

    private void sortBindingThirdLegOwners(List<TunnelTask> tasks) {
        List<TunnelTask> owners = tasks.stream()
                .filter(task -> task.bindingThirdLegOwner)
                .sorted(Comparator.comparing(task -> task.ochLinkSourceTp))
                .collect(Collectors.toList());
        if (owners.size() < 2) {
            return;
        }
        // Only owner slots are reordered; normal tunnel tasks keep the original batch order.
        Iterator<TunnelTask> ownerIterator = owners.iterator();
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).bindingThirdLegOwner) {
                tasks.set(i, ownerIterator.next());
            }
        }
    }

    private void validateBindingDependentsHaveOwner(List<TunnelTask> tasks) {
        for (TunnelTask task : tasks) {
            if (!task.preserveAllocateOnOwnerFailure) {
                continue;
            }
            boolean ownerInThisBatch = tasks.stream().anyMatch(candidate ->
                    candidate.bindingThirdLegOwner
                            && candidate.batchId.equals(task.batchId)
                            && candidate.ochLinkId.equals(task.ochLinkId)
                            && !processingTunnelIds.contains(candidate.tunnelId));
            if (!ownerInThisBatch) {
                // Do not let an Allocate follower from another REST request become
                // the retry owner after the real third-leg owner finishes or fails.
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "binding third leg owner is already processing: "
                                + task.friendlyName);
            }
        }
    }

    private void validateNoQueuedNormalTasks(List<TunnelTask> tasks) {
        for (TunnelTask task : tasks) {
            if (!processingTunnelIds.contains(task.tunnelId)) {
                continue;
            }
            if (task.bindingThirdLegOwner) {
                log.info("binding third leg owner tunnel {} is already queued or processing",
                        task.tunnelId);
                continue;
            }
            String msg = "tunnel is already queued or processing: " + task.tunnelId;
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }
    }

    private void enqueueTaskLocked(TunnelTask task, ActionType actionType, String who) {
        if (processingTunnelIds.contains(task.tunnelId)) {
            if (task.bindingThirdLegOwner) {
                log.info("binding third leg owner tunnel {} is already queued or processing",
                        task.tunnelId);
                return;
            }
            String msg = "tunnel is already queued or processing: " + task.tunnelId;
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, msg);
        }
        // Keep topology implement-state unchanged while waiting in the batch queue.
        // TunnelImplementor owns the Do/Deimplementing transition after it has
        // the original OCH/tunnel state needed for owner and route decisions.
        task.lifeCycleService.logStartLinkImpl(task.tunnelId,
                TaskInfoMessage.ResourceType.tunnel,
                task.friendlyName,
                actionType,
                who, null);
        for (BindingThirdLegCompletionTask completionTask : task.bindingThirdLegCompletionTasks) {
            completionTask.lifeCycleService.logStartLinkImpl(
                    completionTask.tunnelId,
                    TaskInfoMessage.ResourceType.tunnel,
                    completionTask.friendlyName, actionType, who, null);
        }
        processingTunnelIds.add(task.tunnelId);

        GroupState root = resolveOrMergeGroupLocked(task.siteLinks);
        root.queue.offerLast(task);
        markGroupReadyLocked(root.groupId);
    }

    private void dispatchReadyTasks() {
        List<DispatchTask> dispatchList;
        registryLock.lock();
        try {
            dispatchList = takeDispatchableTasksLocked();
        } finally {
            registryLock.unlock();
        }
        submitDispatches(dispatchList);
    }

    private GroupState resolveOrMergeGroupLocked(List<String> siteLinks) {
        Set<String> hitRoots = new HashSet<>();
        for (String siteLink : siteLinks) {
            String groupId = siteLinkToGroupId.get(siteLink);
            if (groupId == null) {
                continue;
            }
            GroupState root = findRootGroupLocked(groupId);
            if (root != null) {
                hitRoots.add(root.groupId);
            }
        }

        GroupState root;
        if (hitRoots.isEmpty()) {
            root = new GroupState(nextGroupId());
            groupById.put(root.groupId, root);
        } else {
            List<String> sortedRoots = new ArrayList<>(hitRoots);
            Collections.sort(sortedRoots);
            root = groupById.get(sortedRoots.get(0));
            for (int i = 1; i < sortedRoots.size(); i++) {
                GroupState oldRoot = groupById.get(sortedRoots.get(i));
                if (oldRoot == null || oldRoot.rootGroupId != null || oldRoot.groupId.equals(
                        root.groupId)) {
                    continue;
                }
                mergeGroupsLocked(root, oldRoot);
            }
        }

        for (String siteLink : siteLinks) {
            root.siteLinks.add(siteLink);
            siteLinkToGroupId.put(siteLink, root.groupId);
        }
        return root;
    }

    private void mergeGroupsLocked(GroupState root, GroupState oldRoot) {
        root.siteLinks.addAll(oldRoot.siteLinks);
        root.queue.addAll(oldRoot.queue);
        oldRoot.queue.clear();
        root.running += oldRoot.running;
        root.runningOchLinks.addAll(oldRoot.runningOchLinks);
        oldRoot.runningOchLinks.clear();
        root.completedImplementOchLinkBatch.addAll(oldRoot.completedImplementOchLinkBatch);
        root.failedImplementOchLinkBatch.putAll(oldRoot.failedImplementOchLinkBatch);
        oldRoot.completedImplementOchLinkBatch.clear();
        oldRoot.failedImplementOchLinkBatch.clear();
        oldRoot.rootGroupId = root.groupId;

        for (String siteLink : oldRoot.siteLinks) {
            siteLinkToGroupId.put(siteLink, root.groupId);
        }

        readyGroupSet.remove(oldRoot.groupId);
        readyGroups.removeIf(gid -> gid.equals(oldRoot.groupId));
        if (oldRoot.running == 0) {
            groupById.remove(oldRoot.groupId, oldRoot);
        }
        if (!root.queue.isEmpty() && root.running < GROUP_MAX_PARALLEL) {
            markGroupReadyLocked(root.groupId);
        }
    }

    private GroupState findRootGroupLocked(String groupId) {
        GroupState group = groupById.get(groupId);
        if (group == null) {
            return null;
        }
        while (group.rootGroupId != null) {
            GroupState parent = groupById.get(group.rootGroupId);
            if (parent == null) {
                break;
            }
            group = parent;
        }
        return group;
    }

    private List<DispatchTask> takeDispatchableTasksLocked() {
        List<DispatchTask> dispatchList = new ArrayList<>();
        while (!readyGroups.isEmpty()) {
            String groupId = readyGroups.pollFirst();
            readyGroupSet.remove(groupId);

            GroupState group = groupById.get(groupId);
            if (group == null || group.rootGroupId != null) {
                continue;
            }
            if (group.running >= GROUP_MAX_PARALLEL || group.queue.isEmpty()) {
                continue;
            }

            TunnelTask task = pollDispatchableTaskLocked(group);
            if (task == null) {
                cleanupRootGroupIfIdleLocked(group);
                continue;
            }
            markOchImplementOwnerLocked(group, task);
            markOchOpticalHandledByOwnerLocked(group, task);
            group.running++;
            markOchRunningLocked(group, task);
            dispatchList.add(new DispatchTask(group.groupId, task));

            if (!group.queue.isEmpty() && group.running < GROUP_MAX_PARALLEL) {
                markGroupReadyLocked(group.groupId);
            }
        }
        return dispatchList;
    }

    private void markGroupReadyLocked(String groupId) {
        GroupState group = groupById.get(groupId);
        if (group == null || group.rootGroupId != null) {
            return;
        }
        if (group.queue.isEmpty() || group.running >= GROUP_MAX_PARALLEL) {
            return;
        }
        if (readyGroupSet.add(groupId)) {
            readyGroups.offerLast(groupId);
        }
    }

    private TunnelTask pollDispatchableTaskLocked(GroupState group) {
        Iterator<TunnelTask> iterator = group.queue.iterator();
        while (iterator.hasNext()) {
            TunnelTask task = iterator.next();
            String ownerFailure = ochOwnerFailure(group, task);
            if (ownerFailure != null) {
                iterator.remove();
                finishSkippedTaskLocked(task, ownerFailure);
                continue;
            }
            // Same-OCH implement tasks stay serialized until one owner has
            // persisted the complete OCH common stage.
            if (requiresOchSerial(group, task) && group.runningOchLinks.contains(task.ochLinkId)) {
                continue;
            }
            iterator.remove();
            return task;
        }
        return null;
    }

    private String ochOwnerFailure(GroupState group, TunnelTask task) {
        if (!ImplementState.Implement.equals(task.targetState)) {
            return null;
        }
        return group.failedImplementOchLinkBatch.get(ochBatchKey(task));
    }

    private void finishSkippedTaskLocked(TunnelTask task, String reason) {
        processingTunnelIds.remove(task.tunnelId);
        if (task.bindingThirdLegOwner) {
            bindingThirdLegOwnerOchLinks.remove(task.ochLinkId);
        }
        if (!task.preserveAllocateOnOwnerFailure) {
            // The private stage never ran. Keep the batch retryable and make the
            // follower state consistent with the failed OCH common stage.
            tunnelDao.updateTunnelImplementState(task.tunnelId,
                    ImplementState.PartialImplement,
                    AdminStatus.Up);
        }
        task.lifeCycleService.logEndLinkImpl(reason);
        finishBindingThirdLegCompletionTasks(task, reason);
    }

    private void markOchRunningLocked(GroupState group, TunnelTask task) {
        if (requiresOchSerial(group, task)) {
            group.runningOchLinks.add(task.ochLinkId);
            task.ochSerialRunningMarked = true;
        }
    }

    private void unmarkOchRunningLocked(GroupState group, TunnelTask task) {
        if (task.ochSerialRunningMarked) {
            group.runningOchLinks.remove(task.ochLinkId);
            task.ochSerialRunningMarked = false;
        }
    }

    private boolean requiresOchSerial(GroupState group, TunnelTask task) {
        if (!task.serializeByOch) {
            return false;
        }
        if (!ImplementState.Implement.equals(task.targetState)) {
            return true;
        }
        return !group.completedImplementOchLinkBatch.contains(ochBatchKey(task));
    }

    private void markOchImplementOwnerLocked(GroupState group, TunnelTask task) {
        if (ImplementState.Implement.equals(task.targetState)
                && requiresOchSerial(group, task)) {
            task.ochImplementOwner = true;
        }
    }

    private void markOchOpticalHandledByOwnerLocked(GroupState group, TunnelTask task) {
        if (ImplementState.Implement.equals(task.targetState)
                && !task.ochImplementOwner
                && group.completedImplementOchLinkBatch.contains(ochBatchKey(task))) {
            // The same-batch owner has completed the whole OCH common stage.
            task.skipOchOpticalInCurrentBatch = true;
        }
    }

    private void submitDispatches(List<DispatchTask> dispatchList) {
        for (DispatchTask dispatch : dispatchList) {
            try {
                myExecutor.lazyDo(() -> executeTask(dispatch));
            } catch (RuntimeException e) {
                log.error("Failed to submit tunnel {} in group {}, return it to queue.",
                        dispatch.task.tunnelId, dispatch.groupId, e);
                returnDispatchToQueue(dispatch);
                // One executor submit failure must not block other ready groups in this batch.
                continue;
            }
        }
    }

    private void returnDispatchToQueue(DispatchTask dispatch) {
        registryLock.lock();
        try {
            GroupState group = groupById.get(dispatch.groupId);
            if (group == null) {
                log.error("Cannot return tunnel {} to queue, group {} no longer exists.",
                        dispatch.task.tunnelId, dispatch.groupId);
                processingTunnelIds.remove(dispatch.task.tunnelId);
                if (dispatch.task.bindingThirdLegOwner) {
                    bindingThirdLegOwnerOchLinks.remove(dispatch.task.ochLinkId);
                }
                return;
            }

            if (group.rootGroupId == null) {
                group.running = Math.max(0, group.running - 1);
                unmarkOchRunningLocked(group, dispatch.task);
                group.queue.offerFirst(dispatch.task);
                markGroupReadyLocked(group.groupId);
                return;
            }

            group.running = Math.max(0, group.running - 1);
            GroupState root = findRootGroupLocked(group.groupId);
            if (root == null) {
                log.error("Cannot return tunnel {} to queue, root group of {} no longer exists.",
                        dispatch.task.tunnelId, dispatch.groupId);
                processingTunnelIds.remove(dispatch.task.tunnelId);
                if (dispatch.task.bindingThirdLegOwner) {
                    bindingThirdLegOwnerOchLinks.remove(dispatch.task.ochLinkId);
                }
                return;
            }
            root.running = Math.max(0, root.running - 1);
            unmarkOchRunningLocked(root, dispatch.task);
            root.queue.offerFirst(dispatch.task);
            markGroupReadyLocked(root.groupId);
        } finally {
            registryLock.unlock();
        }
    }

    private void executeTask(DispatchTask dispatch) {
        TunnelTask task = dispatch.task;
        TunnelImplementor implementor = null;
        try {
            log.debug("Processing tunnel {} in group {} siteLinks {}", task.tunnelId,
                    dispatch.groupId, task.siteLinks);

            implConfig.setTunnelForceDeimplement(
                    task.force);

            implementor = new TunnelImplementor(
                    task.tunnelId,
                    task.targetState,
                    task.lifeCycleService,
                    task.skipOchOpticalInCurrentBatch,
                    task.ochImplementOwner ? () -> onOchCommonStageReady(dispatch) : null);
            implementor.startSyncAction();
        } catch (Exception e) {
            log.error("Error processing tunnel {} in group {}", task.tunnelId, dispatch.groupId, e);
        } finally {
            task.success = task.lifeCycleService.getTaskInfoMessage() != null
                    && Boolean.TRUE.equals(
                    task.lifeCycleService.getTaskInfoMessage().getSuccessfully());
            task.ochCommonStageReadyForFollowers = implementor != null
                    && implementor.isOchCommonStageReadyForFollowers();
            finishBindingThirdLegCompletionTasks(task, task.success ? null : taskErrorReason(task));
            onTaskFinished(dispatch);
        }
    }

    private String taskErrorReason(TunnelTask task) {
        if (task.lifeCycleService.getTaskInfoMessage() == null) {
            return "internal error before task result was created";
        }
        return task.lifeCycleService.getTaskInfoMessage().getErrorReason();
    }

    private void finishBindingThirdLegCompletionTasks(TunnelTask task, String errorMsg) {
        for (BindingThirdLegCompletionTask completionTask : task.bindingThirdLegCompletionTasks) {
            completionTask.lifeCycleService.logEndLinkImpl(errorMsg);
        }
    }

    private void onTaskFinished(DispatchTask dispatch) {
        List<DispatchTask> dispatchList;
        registryLock.lock();
        try {
            TunnelTask task = dispatch.task;
            processingTunnelIds.remove(task.tunnelId);
            if (task.bindingThirdLegOwner) {
                bindingThirdLegOwnerOchLinks.remove(task.ochLinkId);
            }

            GroupState group = groupById.get(dispatch.groupId);
            if (group != null) {
                if (group.rootGroupId == null) {
                    updateOchOwnerResultLocked(group, task);
                    group.running = Math.max(0, group.running - 1);
                    unmarkOchRunningLocked(group, task);
                    if (!group.queue.isEmpty() && group.running < GROUP_MAX_PARALLEL) {
                        markGroupReadyLocked(group.groupId);
                    }
                    cleanupRootGroupIfIdleLocked(group);
                } else {
                    group.running = Math.max(0, group.running - 1);
                    GroupState root = groupById.get(group.rootGroupId);
                    if (root != null) {
                        updateOchOwnerResultLocked(root, task);
                        root.running = Math.max(0, root.running - 1);
                        unmarkOchRunningLocked(root, task);
                        if (!root.queue.isEmpty() && root.running < GROUP_MAX_PARALLEL) {
                            markGroupReadyLocked(root.groupId);
                        }
                        cleanupRootGroupIfIdleLocked(root);
                    }
                    if (group.running == 0 && group.queue.isEmpty()) {
                        groupById.remove(group.groupId, group);
                    }
                }
            }

            dispatchList = takeDispatchableTasksLocked();
        } finally {
            registryLock.unlock();
        }
        submitDispatches(dispatchList);
    }

    private void onOchCommonStageReady(DispatchTask dispatch) {
        List<DispatchTask> dispatchList;
        registryLock.lock();
        try {
            TunnelTask task = dispatch.task;
            task.ochCommonStageReadyForFollowers = true;
            GroupState group = findRootGroupLocked(dispatch.groupId);
            if (group == null) {
                return;
            }
            // The OCH is durable as Implement. Followers can now enter their
            // tunnel-private stages while the owner continues its own private work.
            group.completedImplementOchLinkBatch.add(ochBatchKey(task));
            if (!group.queue.isEmpty() && group.running < GROUP_MAX_PARALLEL) {
                markGroupReadyLocked(group.groupId);
            }
            dispatchList = takeDispatchableTasksLocked();
        } finally {
            registryLock.unlock();
        }
        submitDispatches(dispatchList);
    }

    private void updateOchOwnerResultLocked(GroupState group, TunnelTask task) {
        if (!task.ochImplementOwner) {
            return;
        }
        if (task.success || task.ochCommonStageReadyForFollowers) {
            group.completedImplementOchLinkBatch.add(ochBatchKey(task));
        } else {
            String reason = taskErrorReason(task);
            group.failedImplementOchLinkBatch.put(ochBatchKey(task),
                    reason == null ? "OCH owner tunnel failed" : reason);
        }
    }

    private String ochBatchKey(TunnelTask task) {
        return task.ochLinkId + "@" + task.batchId;
    }

    private void cleanupRootGroupIfIdleLocked(GroupState root) {
        if (root.rootGroupId != null) {
            return;
        }
        if (root.running != 0 || !root.queue.isEmpty()) {
            return;
        }
        boolean hasActiveAlias = groupById.values().stream()
                .anyMatch(g -> root.groupId.equals(g.rootGroupId) && g.running > 0);
        if (hasActiveAlias) {
            return;
        }

        for (String siteLink : root.siteLinks) {
            siteLinkToGroupId.compute(siteLink, (k, v) -> root.groupId.equals(v) ? null : v);
        }
        readyGroupSet.remove(root.groupId);
        readyGroups.removeIf(gid -> gid.equals(root.groupId));
        groupById.remove(root.groupId, root);
    }

    private String nextGroupId() {
        return "sg-" + groupIdSeq.incrementAndGet();
    }

    private String nextBatchId() {
        return "batch-" + batchIdSeq.incrementAndGet();
    }

    private String ochLinkId(Tunnel tunnel) {
        if (tunnel.getSupportingLink() != null && !tunnel.getSupportingLink().isEmpty()) {
            return tunnel.getSupportingLink().get(0).getLinkRef().getValue();
        }
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                String.format("the tunnel %s is not supported by ochLink",
                        tunnel.getFriendlyName()));
    }

    private String ochLinkIds(Tunnel tunnel) {
        List<String> ochLinkIds = tunnel.getSupportingLink().stream()
                .map(sl -> sl.getLinkRef().getValue()).collect(Collectors.toList());
        if (!ochLinkIds.isEmpty()) {
            Collections.sort(ochLinkIds);
            return String.join(",", ochLinkIds);
        }
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                String.format("the tunnel %s is not supported by ochLink",
                        tunnel.getFriendlyName()));
    }

    private List<String> resolveSiteLinks(ChangedObject cache, Tunnel tunnel) {
        if (tunnel.getSupportingLink() == null || tunnel.getSupportingLink().isEmpty()) {
            return Collections.emptyList();
        }

        List<String> ochLinkIds = tunnel.getSupportingLink().stream()
                .filter(Objects::nonNull)
                .map(SupportingLink::getLinkRef)
                .filter(Objects::nonNull)
                .map(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri::getValue)
                .collect(Collectors.toList());

        List<String> siteLinkIds = new ArrayList<>();
        ochLinkIds.forEach(ochLinkId -> {
            Link ochLink = cache.getChangedOchLink(ochLinkId);
            if (ochLink == null || ochLink.getSupportingLink() == null) {
                return;
            }

            siteLinkIds.addAll(
                    ochLink.getSupportingLink().stream()
                            .filter(Objects::nonNull)
                            .map(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink::getLinkRef)
                            .filter(Objects::nonNull)
                            .map(org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri::getValue)
                            .filter(Objects::nonNull)
                            .filter(SiteLinkIdNamingRule::isSiteLink)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())
            );
        });

        // 去重并排序，确保锁获取顺序稳定，避免死锁
        return siteLinkIds.stream()
                .distinct()
                .sorted()
                .collect(Collectors.toList());
    }

    /**
     * @param input
     * @return which siteLink will be operated
     * @throws CommonException
     */
    private OpType checkBasicParam(UpdateTunnelSyncInput input) {
        if (input.getImplementState() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "implementState is mandatory");
        }

        OpType opType = TypeUtils.fromImplementState(input.getImplementState());
        if (opType == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "implement-state must be allocate or implement");
        }

        if (input.getTunnelId() == null || input.getTunnelId().isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "tunnelId is mandatory");
        }
        if (input.getImplementState().equals(ImplementState.Implement) && input.isForce() != null
                && input.isForce()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "only support force deImplement");
        }
        return opType;
    }

    private List<String> uniqueTunnelIds(UpdateTunnelSyncInput input) {
        return new ArrayList<>(new LinkedHashSet<>(input.getTunnelId()));
    }

    private CheckedTunnel checkParam(ChangedObject changedObject, UpdateTunnelSyncInput input,
            String tunnelId, OpType opType) throws CommonException {
        Tunnel tunnel = changedObject.getChangedTunnel(tunnelId);
        if (tunnel == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find required tunnel " + tunnelId);
        }

        ImplConfig implConfig = SpringBeanFinder.getBean(ImplConfig.class);
        if (implConfig.getYangModel().equals(NeYangModel.ByteDance)
                || implConfig.getYangModel().equals(NeYangModel.Chassis20)) {
            checkingSiteLinkState(tunnel, opType);
        }

        if (opType.equals(OpType.IMPLEMENT)) {
            RouteInfo rInfo = new RouteInfo();
            rInfo.parse(tunnel.getExplictRoute().getRoute());

            neManagementChecker.checking(rInfo.getNodeIdList());
        }

        List<String> siteLinks = resolveSiteLinks(changedObject, tunnel);
        if (siteLinks.isEmpty()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("the tunnel %s is not supported by siteLink",
                            tunnel.getFriendlyName()));
        }
        return new CheckedTunnel(tunnel, siteLinks);
    }

    private void checkingSiteLinkState(Tunnel tunnel, OpType opType) {
        ChangedObject changedObject = new ChangedObject();
        if (tunnel.getSupportingLink() == null || tunnel.getSupportingLink().isEmpty()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "dataBase error");
        }

        SupportingLink ochLinkId = tunnel.getSupportingLink().get(0);
        if (ochLinkId == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "dataBase error");
        }

        Link ochLink = changedObject.getChangedOchLink(ochLinkId.getLinkRef().getValue());
        if (ochLink == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "dataBase error");
        }

        if (opType.equals(OpType.IMPLEMENT)) {
            boolean oneAseInjected = false;
            boolean allAseInjected = true;
            for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink sl : ochLink.getSupportingLink()) {
                String linkId = sl.getLinkRef().getValue();
                if (SiteLinkIdNamingRule.isSiteLink(linkId)) {
                    Link siteLink = changedObject.getChangedSiteLink(linkId);
                    if (siteLink == null) {
                        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                                "dataBase error");
                    }
                    if (siteLink.getAugmentation(Link1.class).getSite().getImplementState()
                            .equals(ImplementState.Allocate)) {
                        throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                                "the service related OMS link must be implemented at first");
                    }
                    Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
                    if (siteLinkAttr.getDummyLink() != null && !siteLinkAttr.getDummyLink()
                            .isEmpty()) {
                        oneAseInjected = true;
                        allAseInjected = allAseInjected;
                    } else {
                        allAseInjected = false;
                    }
                }
            }
            if (!oneAseInjected || (oneAseInjected && allAseInjected)) {
                //good condition
            } else {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "the tunnel based siteLink must be all ASE injected");
            }
        }
    }

    private static class TunnelTask {

        // 只保留执行所需标量，避免每个任务持有整个批量请求。
        private final ImplementState targetState;
        private final boolean force;
        private LifeCycleSevice lifeCycleService;
        private final List<String> siteLinks;
        private final String tunnelId;
        private final String ochLinkId;
        private final String ochLinkSourceTp;
        private final String batchId;
        private final boolean serializeByOch;
        private final boolean bindingThirdLegOwner;
        // 排队期间仅保留标识和通知所需名称，不持有包含完整路由的 Tunnel。
        private final String friendlyName;
        private final List<BindingThirdLegCompletionTask> bindingThirdLegCompletionTasks;
        private final boolean preserveAllocateOnOwnerFailure;
        private boolean success = false;
        private boolean ochCommonStageReadyForFollowers = false;
        private boolean ochImplementOwner = false;
        private boolean ochSerialRunningMarked = false;
        private boolean skipOchOpticalInCurrentBatch = false;

        private TunnelTask(UpdateTunnelSyncInput input, LifeCycleSevice lifeCycleService,
                List<String> siteLinks, String ochLinkId, boolean serializeByOch,
                boolean bindingThirdLegOwner, Tunnel tunnel, String batchId) {
            this(input, lifeCycleService, siteLinks, ochLinkId, serializeByOch,
                    bindingThirdLegOwner, tunnel, batchId, Collections.emptyList(), false);
        }

        private TunnelTask(UpdateTunnelSyncInput input, LifeCycleSevice lifeCycleService,
                List<String> siteLinks, String ochLinkId, boolean serializeByOch,
                boolean bindingThirdLegOwner, Tunnel tunnel,
                String batchId,
                List<BindingThirdLegCompletionTask> bindingThirdLegCompletionTasks,
                boolean preserveAllocateOnOwnerFailure) {
            this.targetState = input.getImplementState();
            this.force = Boolean.TRUE.equals(input.isForce());
            this.lifeCycleService = lifeCycleService;
            this.siteLinks = siteLinks;
            this.tunnelId = tunnel.getTunnelId().getValue();
            this.ochLinkId = ochLinkId;
            this.ochLinkSourceTp = OchLinkIdNamingRule.getTpAId(ochLinkId);
            this.batchId = batchId;
            this.serializeByOch = serializeByOch;
            this.bindingThirdLegOwner = bindingThirdLegOwner;
            this.friendlyName = tunnel.getFriendlyName();
            this.bindingThirdLegCompletionTasks =
                    bindingThirdLegCompletionTasks == null
                            ? Collections.emptyList()
                            : bindingThirdLegCompletionTasks;
            this.preserveAllocateOnOwnerFailure = preserveAllocateOnOwnerFailure;
        }
    }

    private static class BindingThirdLegCompletionTask {

        // 加腿完成通知同样只需要 ID 和名称，不能让队列保留完整业务对象。
        private final String tunnelId;
        private final String friendlyName;
        private final LifeCycleSevice lifeCycleService = new LifeCycleSevice();

        private BindingThirdLegCompletionTask(Tunnel tunnel) {
            this.tunnelId = tunnel.getTunnelId().getValue();
            this.friendlyName = tunnel.getFriendlyName();
        }
    }

    private static class CheckedTunnel {

        private final Tunnel tunnel;
        private final List<String> siteLinks;

        private CheckedTunnel(Tunnel tunnel, List<String> siteLinks) {
            this.tunnel = tunnel;
            this.siteLinks = siteLinks;
        }
    }

    private static class EnqueuePlan {

        private final String ochLinkId;
        private final List<TunnelTask> tasks = new ArrayList<>();
        private boolean ownerTaskAdded = false;
        private boolean skipCurrent = false;

        private EnqueuePlan(String ochLinkId) {
            this.ochLinkId = ochLinkId;
        }
    }

    private static class DispatchTask {

        private final String groupId;
        private final TunnelTask task;

        private DispatchTask(String groupId, TunnelTask task) {
            this.groupId = groupId;
            this.task = task;
        }
    }

    private static class GroupState {

        private final String groupId;
        private final Set<String> siteLinks = new HashSet<>();
        private final Deque<TunnelTask> queue = new ArrayDeque<>();
        private final Set<String> runningOchLinks = new HashSet<>();
        private final Set<String> completedImplementOchLinkBatch = ConcurrentHashMap.newKeySet();
        private final ConcurrentHashMap<String, String> failedImplementOchLinkBatch =
                new ConcurrentHashMap<>();
        private int running = 0;
        // null means root group; non-null means this group has been merged into rootGroupId.
        private String rootGroupId = null;

        private GroupState(String groupId) {
            this.groupId = groupId;
        }
    }
}
