package net.flex.dci.otc.controller.ne.manager.core.service;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.HOSTNAME;
import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.YANG_MODEL;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.ne.NeStatus;
import net.flex.dci.otc.common.model.ne.NeStatusMessage;
import net.flex.dci.otc.common.model.type.NeStatusType;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.controller.ne.manager.components.balancer.AdapterBalancer;
import net.flex.dci.otc.controller.ne.manager.components.communicateState.CommunicateStateUpdater;
import net.flex.dci.otc.controller.ne.manager.components.notification.NeManagerEventNotification;
import net.flex.dci.otc.controller.rpc.client.rpcs.AdapterRpc;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TelemetryDao;
import net.flex.dci.otn.controller.tools.kafka.service.StatusMessageSender;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConfigNeInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConnectNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.telemetry.manager.rev180730.telemetry.manager.TelemetryServer;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CommunicationStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

/**
 * @version 1.0
 * @date 2022/3/5 11:18
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NeService {


    private final AdapterBalancer adapterBalancer;

    private final PhyNodeDao phyNodeDao;

    private final AdapterRpc adapterRpc;

    private final TelemetryDao telemetryDao;

    private final NeManagerEventNotification neManagerEventNotification;

    private final CommunicateStateUpdater communicateStateUpdater;


    @Retryable(value = {
            Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 5000, multiplier = 3), exclude = {
            TimeoutException.class, InterruptedException.class, CommonException.class})
    public void registerNe(String neId, String friendlyName)
            throws Exception {
        log.info("start to connect the ne:{}", neId);
//        if (!registerNeCache.tryRegisterLock(neId)) {
//            log.info("[register duplicate] NE {} is already registering, skip", neId);
//            return;
//        }
        try {
            Node node = _registerNe(neId);
            if (node == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "No NE information found for " + neId);
            }
            Adapter adapter = adapterBalancer.getAdapterForNe(neId);
            communicateStateUpdater.syncing(neId);
//            phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId,
//                    CommunicationStatusType.Syncing);
            neManagerEventNotification.broadcastNeSyncing(neId, friendlyName);
            log.info("Starting async sync for NE: {}", neId);
            adapterRpc.syncNeData(adapter, neId);
            setNeName(adapter, neId, friendlyName);
            log.info("Async sync completed successfully for NE: {}", neId);

        } catch (Exception e) {
            log.error("Async sync failed for NE: {}", neId, e);
            try {
                communicateStateUpdater.syncFailed(neId, e);
            } catch (Exception ex) {
                log.error("Failed to update sync failed status for NE: {}", neId, ex);
            }
        }

        log.info("NE {} registered successfully, sync operation is running asynchronously", neId);
    }

    private void setNeName(Adapter adapter, String neId, String name) {
        log.info("set the ne:{} friendly name:{}", neId, name);
        ConfigNeInputBuilder configNeInputBuilder = new ConfigNeInputBuilder();
        configNeInputBuilder.setNodeId(NodeId.getDefaultInstance(neId));
        PhysicalBuilder physicalBuilder = new PhysicalBuilder();
        physicalBuilder.setFriendlyName(name);
        physicalBuilder.setProperties(PropertyTool.addProperty(null, HOSTNAME, name));
        configNeInputBuilder.setPhysical(physicalBuilder.build());
        adapterRpc.configNe(adapter, configNeInputBuilder.build());
    }


    @Retryable(value = {
            Exception.class}, maxAttempts = 2, backoff = @Backoff(delay = 3000, multiplier = 3), exclude = {
            TimeoutException.class, InterruptedException.class, CommonException.class})
    public void reRegisterNe(String neId, String friendlyName)
            throws Exception {
//        if (!registerNeCache.tryRegisterLock(neId)) {
//            log.info("[register duplicate] NE {} is already registering, skip", neId);
//            return;
//        }
        try {
            Node node = _registerNe(neId);
            if (node == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "No NE information found for " + neId);
            }
            Adapter adapter = adapterBalancer.getAdapterForNe(neId);
            communicateStateUpdater.syncing(neId);
//            phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId,
//                    CommunicationStatusType.Syncing);
            neManagerEventNotification.broadcastNeSyncing(neId, friendlyName);
            log.info("synchronize data to ne {}", neId);
            adapterRpc.syncNeData(adapter, neId);
            setNeName(adapter, neId, friendlyName);
            log.info("synchronize data to ne {} completed", neId);

        } catch (InterruptedException ex) {
            log.error("Registration timeout for NE: {}", neId, ex);
            Thread.currentThread().interrupt();
            throw new InterruptedException(
                    String.format("Registration timeout for NE: %s", neId));
        } catch (Exception ex) {
            log.error("failed to re register the ne :{},the reason is:{}", neId, ex.getMessage(),
                    ex);
//            updatePhyNodeCommunicationStatus(neId, ex);
            communicateStateUpdater.syncFailed(neId, ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("failed to re register the ne :%s,the reason is: %s",
                            friendlyName, ex.getMessage()));
        }
    }


    private Node _registerNe(String neId) throws ExecutionException, InterruptedException {
        log.info("start to register ne :{}", neId);
        log.debug("start to connect ne");

        Node node = phyNodeDao.getConfigPhyNodeById(neId);
        if (node == null || node.getAugmentation(Node1.class) == null
                || node.getAugmentation(Node1.class).getPhysical() == null) {
            log.info(
                    String.format("There is no ne information fo %s", neId));
            return null;
        }

        //remove the node operational data
//        phyNodeDao.deletePhyNodeOPById(neId);
        log.info("start to connect ne,ne id ->:{}", neId);
        final Adapter activeAdapter = adapterBalancer.pickupActiveAdapter(neId);
        log.info("start to connect ne,ne id ->:{},adapter is :{}", neId,
                activeAdapter.getName().getValue());
        ConnectNeOutput connectResult = adapterRpc.connectNe(
                activeAdapter, node);
        log.debug("result is :{}", connectResult);
        if (connectResult.getReturnCode().equals(RpcResultType.Success)) {
            //todo: the ne version is v1.35
//        String neYangVersion = adapterRpc.getNeVersion(adapter, node);
//        adapterRpc.unregisterNe(adapter, node.getNodeId().getValue(), false);
//        if (StringUtils.isBlank(neYangVersion)) {
//
//            log.info("Skip to register ne {} because no ne version info", neId);
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
//                    String.format("Failed to register ne %s because no version information",
//                            neId));
//        }
//        Adapter adapterMatched = adapterService.pickupActiveAdapter(neYangVersion);
//        adapterRpc.connectNe(adapterMatched, node);
            neManagerEventNotification.notifyRegisterNeSuccess(neId);
            updateNeAndAdapterStatus(activeAdapter, node);
            return node;
        } else {
//            phyNodeDao.updateConfigPhyNodeCommunicateStatus(neId,
//                    CommunicationStatusType.LoginFail);
            communicateStateUpdater.loginFail(neId);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    connectResult.getReturnMessage());
        }
    }

    @Retryable(value = {
            Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 1.5))
    public void unregisterNe(String neId, boolean isForce)
            throws Exception {
        log.info("start to un register the ne :{},if force:{}", neId, isForce);
        Adapter adapter = adapterBalancer.getAdapterForNe(
                neId);

        if (adapter != null) {
            adapterBalancer.removeAdapterRegisteredNe(adapter.getName().getValue(),
                    neId);
            adapterRpc.unregisterNe(adapter, neId,
                    isForce);
        }
        unregisterTelemetry(neId);
        Node ne = phyNodeDao.getConfigPhyNodeById(neId);
//            updateNeOpStatus(ne, OperStatus.Down);
        phyNodeDao.deletePhyNodeOPById(neId);
        updateNe2Unregistered(ne, isForce);
        neManagerEventNotification.notifyUnregisterNeSuccess(neId);
        log.info("finish to un register the ne:{}", neId);
    }


    /**
     * unregister telemetry
     *
     * @param neId
     */
    private void unregisterTelemetry(String neId) {
        log.debug("start to unregister telemetry server for ne ,ne id is :{}", neId);
        TelemetryServer telemetryServer = telemetryDao.getTelemetryServerByPhyNodeId(
                neId);
        if (telemetryServer != null) {
            telemetryDao.removeRegisterPhyNode(telemetryServer.getName().getValue(), neId);
        }
    }

    @Recover
    public void recoverRegisterNe(Exception e, String neId, String friendlyName) throws Exception {
        log.error("Register ne failed after retries, neId: {}", neId, e);
        neManagerEventNotification.notifyNeManageFailed(e);
        throw e;
    }


    @Recover
    public void recoverUnregisterNe(Exception e, String neId, boolean isForce) throws Exception {
        log.error("Unregister ne failed after retries, neId: {}", neId, e);
        neManagerEventNotification.notifyNeManageFailed(e);
        throw e;
    }

    /**
     * refactor the ne unregistered model
     *
     * @param ne
     */
    private void updateNe2Unregistered(Node ne, boolean isForce) {
        Physical physical = ne.getAugmentation(Node1.class).getPhysical();
        Physical updatePhysical = new PhysicalBuilder(physical)
                .setImplementState(physical.getImplementState())
                .setAlignmentStatus(AlignmentStatusType.Unknown)
                .setAlarmState(AlarmSeverity.Unknown)
                .setOperationalState(OperStatus.Unknown)
                .setAdminState(AdminStatus.Unknown)
                .setCommunicationStatus(CommunicationStatusType.Broken)
                .build();
        phyNodeDao.updateUnregisteredPhyNodeState(ne.getNodeId().getValue(), updatePhysical);
        updateNeSuperviseState(ne, SupervisionStatusType.Unmonitored);
        StatusMessageSender.sendMessage(NeStatusMessage.builder().neStatuses(
                Collections.singletonList(NeStatus.builder().neStatus(
                        NeStatusType.LOSS_TRACK).neId(ne.getNodeId().getValue()).build())).build());
    }

    private Properties remainProperties(Physical physical) {
        Properties properties = physical.getProperties();
        List<Property> propertyList = new ArrayList<>();
        PropertyTool.putKeyValue(propertyList, HOSTNAME,
                PropertyTool.getValue(properties, HOSTNAME));
        PropertyTool.putKeyValue(propertyList, YANG_MODEL,
                PropertyTool.getValue(properties, YANG_MODEL));
        return new PropertiesBuilder().setProperty(propertyList).build();
    }

    private void updateNeAndAdapterStatus(Adapter adapter, Node node) {
        log.debug("start to update ne status to up");
        updateNeSuperviseState(node, SupervisionStatusType.Monitoring);
        log.debug("start to add the ne to the adapter");
        adapterBalancer.registered2Adapter(adapter, node);
    }

    /**
     * update the ne supervise state
     *
     * @param node
     * @param supervisionStatusType 监控状态
     */
    private void updateNeSuperviseState(Node node, SupervisionStatusType supervisionStatusType) {
        String neId = node.getNodeId().getValue();
        log.debug("update ne Supervise state for the node,the neId is :{}", neId);
        phyNodeDao.updateConfigPhyNodeSupervisionState(neId, supervisionStatusType);

    }


    /**
     * judge current ne status is connected by adapter or not
     *
     * @param neId
     * @return
     */
    public boolean isNeRegistered(String neId) {
        log.debug("is current ne:{} registered", neId);
        Adapter adapter = adapterBalancer.getAdapterForNe(neId);
        return !Objects.isNull(adapter);
    }

//    private void updateNeFriendlyName(Adapter adapter, Node node) {
//        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
//
//        ConfigNeInput input = new ConfigNeInputBuilder()
//                .setNodeId(node.getNodeId())
//                .setPhysical(new PhysicalBuilder()
//                        .setFriendlyName(nodeAttr.getFriendlyName())
//                        .setProperties(PropertyTool.addProperty(null, "hostName", nodeAttr.getFriendlyName()))
//                        .build())
//                .build();
//        adapterRpc.configNe(adapter, input);
//    }
}
