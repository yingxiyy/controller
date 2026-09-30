package net.flex.dci.otc.controller.ne.manager.core.kafka.service.impl;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.ne.NeStatus;
import net.flex.dci.otc.common.model.ne.NeStatusMessage;
import net.flex.dci.otc.common.model.type.NeStatusType;
import net.flex.dci.otc.controller.ne.manager.components.communicateState.CommunicateStateUpdater;
import net.flex.dci.otc.controller.ne.manager.core.kafka.service.NeOutOfControl;
import net.flex.dci.otc.controller.ne.manager.dto.NeOutOfControlDto;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dto.PhyNodePhysicalStateDto;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.StatusMessageSender;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CommunicationStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.stereotype.Service;

/**
 * when ne is out of control remove operational data for the ne
 *
 * @version 1.0
 * @date 2022/3/25 14:24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NeOutOfControlService extends AbstractService implements NeOutOfControl {

    private final PhyNodeDao phyNodeDao;

    private final CommunicateStateUpdater communicateStateUpdater;


    @Override
    public void handleStateChange(String nodeId, boolean mutable) {
        log.info("the ne :{},communicate state change", nodeId);

        Node phyNode = phyNodeDao.getConfigPhyNodeById(nodeId);
        if (phyNode == null) {
            log.error("can not find the phy node:{} discard it", nodeId);
            return;
        }

        //handle the phyNode state and remove operational data
        changeNeOperationState2Out(nodeId);
//        removeOperationData(nodeId);
        sendNotification(phyNode);
    }

    private void removeOperationData(String nodeId) {
        phyNodeDao.deletePhyNodeOPById(nodeId);
        phyNodeDao.updateConfigPhyNodeCommunicateStatus(nodeId,
                CommunicationStatusType.IpUnreachable);
    }

    private void changeNeOperationState2Out(String nodeId) {
//        PhysicalBuilder pb = new PhysicalBuilder();
//        pb.setOperationalState(OperStatus.NeCommunicationException);
//        phyNodeDao.updatePhysicalByNodeId(nodeId, pb.build());
        log.debug("start to translate the ne:{} to offline", nodeId);
//        //rm adapter
//        Adapter adapter = adapterDao.getAdapterByNeId(nodeId);
//        if (adapter != null) {
//            log.info("[{}] connect the adapter is:{}", nodeId, adapter.getName());
//            adapterDao.removeRegisteredNeId(adapter.getName().getValue(), nodeId);
//        }
//        TelemetryServer telemetryServer = telemetryDao.getTelemetryServerByPhyNodeId(nodeId);
//        if (telemetryServer != null) {
//            log.info("[{}] connect telemetry is:{}", nodeId, telemetryServer.getName());
//            telemetryDao.removeRegisterPhyNode(telemetryServer.getName().getValue(), nodeId);
//        }
//        phyNodeDao.updateConfigPhyNodeCommunicateStatus(nodeId, CommunicationStatusType.Broken);
        communicateStateUpdater.broken(nodeId);
        PhyNodePhysicalStateDto phyNodePhysicalStateDto = PhyNodePhysicalStateDto.builder()
                .alarmSeverity(AlarmSeverity.Unknown).alarmSeverity(AlarmSeverity.Unknown)
                .operStatus(OperStatus.NeCommunicationException)
                .alignmentStatusType(AlignmentStatusType.Unknown)
                .build();
        phyNodeDao.updateConfigPhyNodePhysicalState(nodeId, phyNodePhysicalStateDto);
    }

    @Override
    public void sendNotification(Node phyNode) {
        log.debug("broad cast the ne message");
        Physical nodePhysical = phyNode.getAugmentation(Node1.class).getPhysical();
        String friendlyName = nodePhysical.getFriendlyName();
        NeOutOfControlDto outOfControlDto = NeOutOfControlDto.builder()
                .ip(nodePhysical.getIp())
                .friendlyName(friendlyName)
                .neId(phyNode.getNodeId().getValue())
                .port(String.valueOf(nodePhysical.getPort().getValue()))
                .build();
        BroadcastMessager.publishKafkaMessage(BroadcastMessage.builder().title(
                        BroadCastConstant.NE_OUT_OF_CONTROL)
                .message(outOfControlDto.toString()).error(true).build());
        StatusMessageSender.sendMessage(NeStatusMessage.builder().neStatuses(
                Collections.singletonList(
                        NeStatus.builder().neId(phyNode.getNodeId().getValue()).neStatus(
                                NeStatusType.LOSS_TRACK).build())).build());
    }
}
