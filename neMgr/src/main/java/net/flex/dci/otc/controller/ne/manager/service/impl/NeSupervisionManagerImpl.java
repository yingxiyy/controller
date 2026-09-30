package net.flex.dci.otc.controller.ne.manager.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.ne.manager.cache.RegisterNeCache;
import net.flex.dci.otc.controller.ne.manager.components.balancer.AdapterBalancer;
import net.flex.dci.otc.controller.ne.manager.components.validator.InputValidator;
import net.flex.dci.otc.controller.ne.manager.core.service.NeService;
import net.flex.dci.otc.controller.ne.manager.dto.ManageNeSynchroInfo;
import net.flex.dci.otc.controller.ne.manager.service.NeManager;
import net.flex.dci.otc.controller.ne.manager.service.NeSupervisionManager;
import net.flex.dci.otc.controller.ne.manager.utils.AsynchronousExecutor;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dto.NodeInfoDto;
import net.flex.dci.otc.serialization.util.SerializeUtil;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.StartSuperviseNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.StartSuperviseNeOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.StopSuperviseNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.StopSuperviseNeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CommunicationStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 7/30/2023 4:27 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NeSupervisionManagerImpl implements NeSupervisionManager {

    private final Cache<String, Boolean> taskCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();

    private final RegisterNeCache registerNeCache;

    private static final Semaphore REGISTER_SEMAPHORE = new Semaphore(10);

    private static final Semaphore UNREGISTER_SEMAPHORE = new Semaphore(10);

    private static final ThreadPoolExecutor STOP_SUPERVISE_POOL =
            new ThreadPoolExecutor(
                    20,
                    20,
                    60L,
                    TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(2000),
                    new ThreadFactory() {
                        private final AtomicInteger cnt = new AtomicInteger(1);

                        @Override
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, "stop-supervise-" + cnt.getAndIncrement());
                            t.setDaemon(true);
                            return t;
                        }
                    },
                    new ThreadPoolExecutor.CallerRunsPolicy()
            );
    private static final ThreadPoolExecutor NE_REGISTER_EXECUTOR;

    static {
        NE_REGISTER_EXECUTOR = new ThreadPoolExecutor(
                10,
                10,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(2000),
                new ThreadFactory() {
                    private final AtomicInteger counter = new AtomicInteger(0);

                    @Override
                    public Thread newThread(Runnable r) {
                        Thread thread = new Thread(r, "ne-register-" + counter.incrementAndGet());
                        thread.setDaemon(true);
                        return thread;
                    }
                },
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    private final InputValidator inputValidator;
    private final AdapterBalancer adapterBalancer;
    private final NeManager neManager;
    private final NeService neService;
    private final PhyNodeDao phyNodeDao;

    @Override
    public String superviseNe(String input) {
        log.info("start to supervise the ne the input is:{}", input);
        try {
            StartSuperviseNeInput startSuperviseNeInput = SerializeUtil.parseRpcInput(input,
                    StartSuperviseNeInput.class);
            inputValidator.validateStartSuperviseNeInput(startSuperviseNeInput);

            List<String> neIds = startSuperviseNeInput.getNeIds().stream().map(Uri::getValue)
                    .collect(Collectors.toList());

            String taskKey = "supervise:" + String.join(",", neIds);
            if (taskCache.getIfPresent(taskKey) != null) {
                log.info("Duplicate supervise task detected, skip: {}", taskKey);
                StartSuperviseNeOutputBuilder builder = new StartSuperviseNeOutputBuilder();
                builder.setReturnCode(RpcResultType.Success);
                return SerializeUtil.serializeRpcOutput2Json(builder.build());
            }
            taskCache.put(taskKey, true);
            List<String> registeredNeIds = adapterBalancer.getManagedNeIds().stream().map(
                    ManageNeSynchroInfo::getNeId).collect(Collectors.toList());
            Set<String> unregisteredNeIds = NeManagerUtils.getDifferenceSetByGuava(
                    new HashSet<>(neIds), new HashSet<>(registeredNeIds));
//            List<String> alreadyRegisteredNeIds = neIds.stream().filter(registeredNeIds::contains)
//                    .collect(Collectors.toList());
            batchUpdateState(
                    neIds,
                    SupervisionStatusType.Monitoring, AdminStatus.Up, null);

            if (!unregisteredNeIds.isEmpty()) {
                AsynchronousExecutor.submit(() -> {
                    Set<String> successfullyRegisteredNeIds = new HashSet<>();
                    log.info("start to re-registered {} ne(s)", unregisteredNeIds.size());
                    successfullyRegisteredNeIds = parallelReRegisterNe(unregisteredNeIds);
                    log.info("successfully registered {} ne(s) out of {}",
                            successfullyRegisteredNeIds.size(),
                            unregisteredNeIds.size());
                    if (!successfullyRegisteredNeIds.isEmpty()) {
                        log.info("updating state for {} newly registered ne(s)",
                                successfullyRegisteredNeIds.size());
                        batchUpdateState(
                                new ArrayList<>(successfullyRegisteredNeIds),
                                SupervisionStatusType.Monitoring, AdminStatus.Up, null);
                    }
                });

            }

//            log.info("un registered neId:{} start to reRegister", unregisteredNeIds);
//            if (!unregisteredNeIds.isEmpty()) {
//                log.debug("start to registered the ne ids :{}", unregisteredNeIds);
//                parallelReRegisterNe(unregisteredNeIds);
//            }
            log.info("finish to supervise the ne");
            StartSuperviseNeOutputBuilder builder = new StartSuperviseNeOutputBuilder();
            builder.setReturnCode(RpcResultType.Success);
            return SerializeUtil.serializeRpcOutput2Json(builder.build());
        } catch (Exception ex) {
            log.error("failed to supervise the ne ,the reason is :{}", ex.getMessage(), ex);
            if (ex instanceof CommonException) {
                throw ex;
            } else {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        ex.getMessage());
            }
        }
    }


    @Override
    public String stopSuperviseNe(String input) {
        log.info("start to stop supervise ne the input is:{}", input);
        try {
            StopSuperviseNeInput stopSuperviseNeInput = SerializeUtil.parseRpcInput(input,
                    StopSuperviseNeInput.class);
            inputValidator.validateStopSuperviseNeInput(stopSuperviseNeInput);
            List<String> neIds = stopSuperviseNeInput.getNeIds().stream().map(Uri::getValue)
                    .collect(Collectors.toList());
            String taskKey = "stop-supervise:" + String.join(",", neIds);
            if (taskCache.getIfPresent(taskKey) != null) {
                log.info("Duplicate stop-supervise task detected, skip: {}", taskKey);
                StopSuperviseNeOutputBuilder builder = new StopSuperviseNeOutputBuilder();
                builder.setReturnCode(RpcResultType.Success);
                return SerializeUtil.serializeRpcOutput2Json(builder.build());
            }
            taskCache.put(taskKey, true);
//            List<String> registeredNeIds = adapterBalancer.getManagedNeIds().stream().map(
//                    ManageNeSynchroInfo::getNeId).collect(Collectors.toList());
//            Set<String> stopSuperviseNeIds = NeManagerUtils.getIntersectionSetByGuava(
//                    new HashSet<>(neIds), new HashSet<>(registeredNeIds));
//            Set<String> alreadyStopSuperviseNeIds = neIds.stream()
//                    .filter(id -> !registeredNeIds.contains(id))
//                    .collect(Collectors.toSet());
//            if (!alreadyStopSuperviseNeIds.isEmpty()) {
//                log.info("updating state for :{} already stop supervision nes",
//                        alreadyStopSuperviseNeIds.size());
//                batchUpdateState(new ArrayList<>(alreadyStopSuperviseNeIds),
//                        SupervisionStatusType.Unmonitored,
//                        AdminStatus.Unknown,
//                        CommunicationStatusType.Broken);
//            }
            if (!neIds.isEmpty()) {
                AsynchronousExecutor.submit(() -> {
                    try {
                        Set<String> successfullyStopSuperviseNeIds = new HashSet<>();
                        log.info("start to stop supervision the :{} registered ne(s)",
                                neIds);
                        successfullyStopSuperviseNeIds = parallelStopSupervisingNe(
                                new HashSet<>(neIds));
                        log.info("successfully stop supervision {} ne out of {} ",
                                successfullyStopSuperviseNeIds.size(), neIds.size());
                        if (!successfullyStopSuperviseNeIds.isEmpty()) {
                            log.info("updating state for {} ne(s) to Unmonitored",
                                    successfullyStopSuperviseNeIds.size());
                            batchUpdateState(new ArrayList<>(successfullyStopSuperviseNeIds),
                                    SupervisionStatusType.Unmonitored,
                                    AdminStatus.Unknown,
                                    CommunicationStatusType.Broken);
                        }
                    } catch (Exception e) {
                        log.error("async stop-supervise task failed", e);
                    }
                });
            }

//            }
            log.info("finish to stop supervise the ne");
            StopSuperviseNeOutputBuilder builder = new StopSuperviseNeOutputBuilder();
            builder.setReturnCode(RpcResultType.Success);
            return SerializeUtil.serializeRpcOutput2Json(builder.build());
        } catch (Exception ex) {
            log.error("failed to stop supervise the ne ,the reason is :{}", ex.getMessage(), ex);
            if (ex instanceof CommonException) {
                throw ex;
            } else {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        ex.getMessage());
            }
        }
    }

    private void changeUnSupervisingNeState(Set<String> stoppedSuperviseNeIds) {
        log.info("change unSupervising ne state for ne:{}", stoppedSuperviseNeIds);
        phyNodeDao.batchUpdateConfigPhyNodeSupervisionStateAndAdminStateAndCommunicateStatus(
                new ArrayList<>(stoppedSuperviseNeIds), SupervisionStatusType.Unmonitored,
                AdminStatus.Unknown,
                CommunicationStatusType.Broken);
    }

    private void batchUpdateState(List<String> neIds,
            SupervisionStatusType supervision,
            AdminStatus admin,
            CommunicationStatusType communication) {
        if (CollectionUtils.isEmpty(neIds)) {
            return;
        }

        int batchSize = 100;
        for (int i = 0; i < neIds.size(); i += batchSize) {
            int end = Math.min(i + batchSize, neIds.size());
            List<String> batch = neIds.subList(i, end);
            phyNodeDao.batchUpdateConfigPhyNodeSupervisionStateAndAdminStateAndCommunicateStatus(
                    batch, supervision, admin, communication);
            log.debug("Updated state for batch {}-{}, {} nes", i, end, batch.size());
        }
    }


    private Set<String> parallelStopSupervisingNe(Set<String> stopSuperviseNeIds) {
        log.info("start to parallel stop supervising ne:{}", stopSuperviseNeIds);
        if (CollectionUtils.isEmpty(stopSuperviseNeIds)) {
            log.debug("no ne need to unregister");
            return new HashSet<>();
        }
        List<String> neIdList = new ArrayList<>(stopSuperviseNeIds);
        int batchSize = 10;
        Set<String> successfullyRegisterNeIds = new HashSet<>();
        for (int i = 0; i < neIdList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, neIdList.size());
            List<String> batch = neIdList.subList(i, end);

            log.info("processing batch unregistered {}-{}, {} ne(s)", i, end, batch.size());
            List<CompletableFuture<Boolean>> batchFutures = new ArrayList<>();
            for (String neId : batch) {
                batchFutures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        UNREGISTER_SEMAPHORE.acquire();
                        return stopSupervisionOneNe(neId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    } finally {
                        UNREGISTER_SEMAPHORE.release();
                    }
                }, NE_REGISTER_EXECUTOR));
            }

            for (int j = 0; j < batch.size(); j++) {
                try {
                    boolean success = batchFutures.get(j).get(60, TimeUnit.SECONDS);  // 添加 60 秒超时
                    if (success) {
                        successfullyRegisterNeIds.add(batch.get(j));
                    }
                } catch (TimeoutException e) {
                    log.warn("register ne:{} timeout, continuing", batch.get(j));
                } catch (Exception e) {
                    log.error("register ne:{} failed", batch.get(j), e);
                }
            }

            log.info("stop-supervise batch {}-{} completed", i, end);
        }
        log.info("all stop-supervising tasks finished");
        return successfullyRegisterNeIds;
    }

    private boolean stopSupervisionOneNe(String neId) {
        try {
            log.info("stop supervising ne:{}", neId);
            neManager.unSuperviseNe(neId);
            return true;
        } catch (Exception e) {
            log.error("failed to stop supervision on ne", e);
            return false;
        }
    }


    private Set<String> parallelReRegisterNe(Set<String> reRegisteredNeIds) {
        log.debug("start to parallelReRegister ne:{}", reRegisteredNeIds);
        if (CollectionUtils.isEmpty(reRegisteredNeIds)) {
            log.debug("no ne need to re-register");
            return new HashSet<>();
        }
        Map<String, String> friendlyNameMap = new HashMap<>();

        List<NodeInfoDto> nodes = phyNodeDao.listConfigNodeINfoByIds(
                new ArrayList<>(reRegisteredNeIds));
        for (NodeInfoDto node : nodes) {
            friendlyNameMap.put(node.getId(), node.getFriendlyName());
        }

        Set<String> successfullyRegisterNeIds = new HashSet<>();
        int total = reRegisteredNeIds.size();
        List<String> neIdList = new ArrayList<>(reRegisteredNeIds);
        int batchSize = 10;
        for (int i = 0; i < neIdList.size(); i += batchSize) {
            int end = Math.min(i + batchSize, neIdList.size());
            List<String> batch = neIdList.subList(i, end);

            log.info("processing batch {}-{}, {} ne(s)", i, end, batch.size());
            List<CompletableFuture<Boolean>> batchFutures = new ArrayList<>();
            for (String neId : batch) {
                if (!registerNeCache.tryRegisterLock(neId)) {
                    log.info("[register duplicated] NE {} is registering，skip register", neId);
                    continue;
                }
                batchFutures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        REGISTER_SEMAPHORE.acquire();
                        return safeReRegisterNe(neId, friendlyNameMap.get(neId));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    } finally {
                        REGISTER_SEMAPHORE.release();
                        registerNeCache.unlockRegister(neId);
                    }
                }, NE_REGISTER_EXECUTOR));
            }

            for (int j = 0; j < batch.size(); j++) {
                try {
                    boolean success = batchFutures.get(j).get(60, TimeUnit.SECONDS);  // 添加 60 秒超时
                    if (success) {
                        successfullyRegisterNeIds.add(batch.get(j));
                    }
                } catch (TimeoutException e) {
                    log.warn("register ne:{} timeout, continuing", batch.get(j));
                } catch (Exception e) {
                    log.error("register ne:{} failed", batch.get(j), e);
                }
            }

            log.info("batch {}-{} completed", i, end);
        }

        log.info("all {} ne(s) re-register tasks submitted", total);
        return successfullyRegisterNeIds;
    }

    private boolean safeReRegisterNe(String neId, String friendlyName) {

        try {
            log.debug("start to re-register ne:{}", neId);
            neService.reRegisterNe(neId, friendlyName);
            log.info("re-register ne:{} success", neId);
            return true;
        } catch (Exception e) {
            log.error("re-register ne:{} failed", neId, e);
            return false;
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down ne supervision thread pools");
        STOP_SUPERVISE_POOL.shutdown();
        NE_REGISTER_EXECUTOR.shutdown();
        try {
            if (!STOP_SUPERVISE_POOL.awaitTermination(60, TimeUnit.SECONDS)) {
                STOP_SUPERVISE_POOL.shutdownNow();
            }
            if (!NE_REGISTER_EXECUTOR.awaitTermination(60, TimeUnit.SECONDS)) {
                NE_REGISTER_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            STOP_SUPERVISE_POOL.shutdownNow();
            NE_REGISTER_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
