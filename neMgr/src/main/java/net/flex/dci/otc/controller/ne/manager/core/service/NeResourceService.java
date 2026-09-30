package net.flex.dci.otc.controller.ne.manager.core.service;

import java.time.LocalTime;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.ne.manager.components.balancer.AdapterBalancer;
import net.flex.dci.otc.controller.ne.manager.components.communicateState.CommunicateStateUpdater;
import net.flex.dci.otc.controller.ne.manager.components.notification.NeManagerEventNotification;
import net.flex.dci.otc.controller.rpc.client.rpcs.AdapterRpc;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveResourceInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveResourceOutput;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * @version 1.0
 * @date 2022/3/5 11:34
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NeResourceService {

    private final AdapterBalancer adapterBalancer;

    private final AdapterRpc adapterRpc;

    private final PhyNodeDao phyNodeDao;

    private final NeManagerEventNotification managerEventNotification;

    private final CommunicateStateUpdater communicateStateUpdater;

    @Retryable(value = {
            Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2))
    public RemoveResourceOutput removeResource(
            RemoveResourceInput removeResourceInput)
            throws CommonException {
        Adapter adapter = adapterBalancer.getAdapterForNe(
                removeResourceInput.getNodeId().getValue());
        log.info("remove the ne resource from adapter :{}", adapter);
        if (adapter != null) {
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveResourceInput emlRemoveInput = convert2EmlRpcInput(
                    removeResourceInput);
            RemoveResourceOutput removeOutput = adapterRpc.removeResource(adapter, emlRemoveInput);
            log.debug("remove resource output is:{}", removeOutput);
            return removeOutput;
        } else {
            String name = phyNodeDao.getFriendlyName(removeResourceInput.getNodeId().getValue());
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format(
                            "Failed to remove resource for ne %s, (%s) because no adapter for it.",
                            name, removeResourceInput.getNodeId().getValue()));
        }
    }

    public void SyncNe(String neId, String neFriendlyName) {
//
        try {
            log.info("start to sync ne the ne id is :{}", neId);
            Adapter adapter = adapterBalancer.getAdapterForNe(neId);
            if (adapter == null) {
                log.warn("No adapter found for NE: {}, skip sync", neId);
//                phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId,
//                        CommunicationStatusType.SyncFailed);
                return;

            }
            communicateStateUpdater.syncing(neId);
            managerEventNotification.broadcastNeSyncing(neId, neFriendlyName);
//
//                Ne ne = adapterRpc.getNe(adapter, neId);
////                neDataSynchronizer.synchronize(ne, true);
//                NeData neData = new NeData(ne);
//                neData.mergeAndStore();
            adapterRpc.syncNeData(adapter, neId);
        } catch (CommonException | ExecutionException | InterruptedException e) {
//            phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId,
//                    CommunicationStatusType.SyncFailed);
            communicateStateUpdater.syncFailed(neId);
            log.error("failed to sync ne data ,exception message is :{}", e.getMessage(), e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("Failed to synchronise ne: %s,the reason is:%s",
                            neFriendlyName, e.getMessage()));
        }
    }

    @Retryable(value = {
            Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1500, multiplier = 2))
    public void mergeNe(String neId) {
        log.info("start to merge ne the ne id is :{}", neId);
        boolean managed = adapterBalancer.isManagedNeId(neId);
        if (!managed) {
            log.error("current ne:{} don't be supervised,do nothing", neId);
            return;
        }
        Adapter adapter = adapterBalancer.getAdapterForNe(neId);
        if (adapter == null) {
//            phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId,
//                    CommunicationStatusType.SyncFailed);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "the ne is not supervised: " + neId);
        }
        adapterRpc.mergeData(adapter, neId);
        //send notification
//        managerEventNotification.notifyNeDataMergeSuccess(neId);

    }

    @Recover
    public void recover(Exception e, String neId) {
        log.warn("manager ne failed after retries!!! at time: " + LocalTime.now());
        log.error("manager ne failed, neId: {}, error: {}", neId, e.getMessage(), e);
//        managerEventNotification.notifyNeResourceManageFailed(e);
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                String.format("Failed to manager ne resource: %s after retries", neId), e);
    }


    /**
     * transfer eml manager rpc input to eml rpc input
     *
     * @param removeResourceInput
     * @return
     */
    private org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveResourceInput convert2EmlRpcInput(
            RemoveResourceInput removeResourceInput) {
        return new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveResourceInputBuilder().setNodeId(
                        removeResourceInput.getNodeId())
                .setPhysical(removeResourceInput.getPhysical())
                .setTerminationPoint(removeResourceInput.getTerminationPoint())
                .build();
    }

}
