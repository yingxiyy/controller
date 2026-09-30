package net.flex.dci.otc.controller.ne.manager.components.validator;

import static net.flex.dci.otc.common.constants.Constants.DOT;
import static net.flex.dci.otc.common.util.Constant.EquipmentClass.CU;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ChannelAseRestoreInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ClearNeApsSwitchLogsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ExecuteNetconfCommandInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchInput.Action;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeOperationLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.StartSuperviseNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.StopSuperviseNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.SwitchCuActiveStandbyInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UploadHistoryPmInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.clear.ne.aps._switch.logs.input.TargetDevice;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.upload.history.pm.input.RemoteServer;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ApsPathType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OPERATIONITEM;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OtsOperationType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateNeInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 7/25/2023 1:20 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InputValidatorImpl implements InputValidator {

    private final SiteNodeDao siteNodeDao;

    private final PhyNodeDao phyNodeDao;

    private final TerminationPointDao terminationPointDao;

    private final AdapterDao adapterDao;

    private final CrossConnectionsDao crossConnectionsDao;

    @Override
    public void validateCreateNeInput(CreateNeInput createNeInput) {
        log.debug("start to validate create ne input");
        String refSiteId = createNeInput.getSiteId();
        if (StringUtils.isBlank(refSiteId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the site id should not be null");
        }
        Physical physical = createNeInput.getPhysical();
        if (Objects.isNull(physical)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the element physical should not be null");
        }
        String neIp = physical.getIp();
        if (Objects.isNull(neIp)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ne ip should not be null");
        }
        PortNumber port = physical.getPort();
        if (Objects.isNull(port)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ne port should not be null");
        }
        String friendlyName = physical.getFriendlyName();
        if (StringUtils.isBlank(friendlyName)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ne friendly name should not be null");
        }
        String loginAccount = physical.getLoginName();
        if (StringUtils.isBlank(loginAccount)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ne connect account should not be null");
        }
        String password = physical.getLoginPasswd();
        if (StringUtils.isBlank(password)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ne connect password should not be null");
        }
        Node siteNode = siteNodeDao.getSiteNodeById(refSiteId);
        if (Objects.isNull(siteNode)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the site is not existed");
        }
        boolean alreadyExistName = phyNodeDao.existsNeFriendlyName(friendlyName);
        if (alreadyExistName) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the ne name is already existed,the name is %s", friendlyName));
        }
        boolean alreadyExist = phyNodeDao.existsIpPort(neIp, port);
        if (alreadyExist) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format(
                    "the site ip and port is already existed ,the ip is %s the port is %d",
                    neIp, port.getValue()));
        }

    }

    @Override
    public void validateStartSuperviseNeInput(StartSuperviseNeInput startSuperviseNeInput) {
        log.debug("start to validate the supervise ne input ");
        List<NodeId> neIds = startSuperviseNeInput.getNeIds();
        if (null == neIds || neIds.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the aim to start supervise ne id should be not null");
        }
        List<String> phyNodeIds = neIds.stream().map(NodeId::getValue).collect(Collectors.toList());
        List<String> inDbNeIds = phyNodeDao.retrieveAllPhyNodeIdsByNodeIds(phyNodeIds);
        List<String> notExistedNeId = phyNodeIds.stream()
                .filter(neId -> !inDbNeIds.contains(neId)).collect(Collectors.toList());
        if (!notExistedNeId.isEmpty()) {
            String notExistedNeIdString = String.join(DOT, notExistedNeId);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the ne id : %s is not exited,please contract the administrator",
                            notExistedNeIdString));
        }
        List<String> notHaveIpNeIds = phyNodeDao.getHaveNoIpPhyNodeIds(phyNodeIds);
        if (!notHaveIpNeIds.isEmpty()) {
            List<String> friendlyNames = phyNodeDao.getNodesFriendlyNameSet(notHaveIpNeIds);
            String friendlyNameStr = String.join(DOT, friendlyNames);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the nes : %s should be set ip first", friendlyNameStr));
        }
    }

    @Override
    public void validateStopSuperviseNeInput(StopSuperviseNeInput stopSuperviseNeInput) {
        log.debug("start to validate the supervise ne input ");
        List<NodeId> neIds = stopSuperviseNeInput.getNeIds();
        if (null == neIds || neIds.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the aim to start supervise ne id should be not null");
        }
        List<String> stopSuperviseNeIds = neIds.stream().map(NodeId::getValue)
                .collect(Collectors.toList());
//        List<String> notExistedNeId = neIds.stream().map(Uri::getValue)
//                .filter(neId -> !phyNodeDao.existsCfgNode(neId)).collect(Collectors.toList());
        List<String> phyNodeIds = phyNodeDao.retrieveAllPhyNodeIdsByNodeIds(stopSuperviseNeIds);
        List<String> notExistedNeId = stopSuperviseNeIds.stream()
                .filter(neId -> !phyNodeIds.contains(neId))
                .collect(
                        Collectors.toList());

        if (!notExistedNeId.isEmpty()) {
            String notExistedNeIdString = String.join(DOT, notExistedNeId);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the ne id : %s is not exited,please contract the administrator",
                            notExistedNeIdString));
        }
//        List<String> notHaveIpNeIds = neIds.stream().map(Uri::getValue)
//                .filter(neId -> !phyNodeDao.nodeExistIp(neId)).collect(Collectors.toList());
//        if (!notHaveIpNeIds.isEmpty()) {
//            List<String> friendlyNames = phyNodeDao.getNodesFriendlyNameSet(notHaveIpNeIds);
//            String friendlyNameStr = String.join(DOT, friendlyNames);
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    String.format("the nes : %s should be set ip first", friendlyNameStr));
//        }
    }

    @Override
    public void validateClearNeApsSwitchLogsInput(
            ClearNeApsSwitchLogsInput clearNeApsSwitchLogsInput) {
        log.debug("start to validate the clear ne aps switch logs input");
        List<TargetDevice> targetDevices = clearNeApsSwitchLogsInput.getTargetDevice();
        if (CollectionUtils.isEmpty(targetDevices)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "clear ne aps switch logs target device  should not be null");
        }
        List<String> neIds = targetDevices.stream().map(TargetDevice::getDeviceId)
                .collect(Collectors.toList());

//        List<String> notExistedNeId = neIds.stream()
//                .filter(neId -> !phyNodeDao.existsCfgNode(neId)).collect(Collectors.toList());
        List<String> inDbNeIds = phyNodeDao.retrieveAllPhyNodeIdsByNodeIds(neIds);
        List<String> notExistedNeId = neIds.stream()
                .filter(neId -> !inDbNeIds.contains(neId)).collect(Collectors.toList());
        if (!notExistedNeId.isEmpty()) {
            String notExistedNeIdString = String.join(DOT, notExistedNeId);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the ne id : %s is not exited,please contract the administrator",
                            notExistedNeIdString));
        }

        List<String> connectedNeIds = adapterDao.getConnectedNeIdsByNeIds(neIds);
        List<String> notConnectedNeIds = neIds.stream()
                .filter(neId -> !connectedNeIds.contains(neId)).collect(
                        Collectors.toList());

        if (!notConnectedNeIds.isEmpty()) {
            List<String> friendlyNames = phyNodeDao.getNodesFriendlyNameSet(notConnectedNeIds);
            String friendlyNameStr = String.join(DOT, friendlyNames);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the nes : %s should be connected first", friendlyNameStr));
        }
    }

    @Override
    public void validateManageApsSwitchInput(ManageApsSwitchInput manageApsSwitchInput) {
        log.debug("start to validate manage aps switch input");
        NodeId neId = manageApsSwitchInput.getNeId();
        if (neId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "manage aps switch ne id should not be null");
        }
        Node phyNode = phyNodeDao.getPhyNodeById(neId.getValue());
        if (phyNode == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("manage aps switch ne:%s is not existed", neId.getValue()));
        }
        String apsName = manageApsSwitchInput.getName();
        if (StringUtils.isBlank(apsName)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the manage aps switch input name should not be null");
        }
        //detect current ne have the op type card or not
        Physical phyNodePhysical = phyNode.getAugmentation(Node1.class).getPhysical();
        List<Equipments> equipments = phyNodePhysical.getEquipments();
        List<Equipments> opEquipments = equipments.stream()
                .filter(equipment -> equipment.getEquipType() == EquipType.OP
                        || equipment.getEquipType() == EquipType.CMUX64)
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(opEquipments)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("current ne:%s(%s) have no Switching card",
                            phyNodePhysical.getFriendlyName(), neId.getValue()));
        }
        //validate apsName is existed or not
        List<CrossConnections> crossConnections = phyNodePhysical.getCrossConnections();
        CrossConnections refCrossConnection = crossConnections.stream()
                .filter(xc -> xc.getAps() != null)
                .filter(xc -> xc.getAps().getName().equals(apsName)).findAny().orElse(null);
        if (refCrossConnection == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("aps name %s is not existed on ne: %s(%s)", apsName,
                            phyNodePhysical.getFriendlyName(), neId.getValue()));
        }
        Action apsAction = manageApsSwitchInput.getAction();
        if (apsAction == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("manage aps switch ne:%s action should not be null",
                            neId.getValue()));
        }

        ApsPathType path = manageApsSwitchInput.getPath();

        if (path == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the manage aps switch path,element path should not be null ");
        }

        Short index = manageApsSwitchInput.getIndex();
        if (index == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the manage aps switch path index,element path index should not be null ");
        }

    }

    @Override
    public void validateOperationLink(NeOperationLinkInput neOperationLinkInput) {
        if (neOperationLinkInput.getNodeId() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ne id should not be null");
        }
        String nodeId = neOperationLinkInput.getNodeId().getValue();
        if (!phyNodeDao.existsByNodeId(nodeId)) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    String.format("the operation link on ne:%s is not found", nodeId));
        }
        TpId tpId = neOperationLinkInput.getTpId();
        if (tpId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the tp id should not be null");
        }
        String terminationPointId = tpId.getValue();
        TerminationPoint tp = terminationPointDao.getPhyTpByNeIdAndTpId(nodeId, terminationPointId);
        if (tp == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the termination point %s is not exist on the ne:%s",
                            terminationPointId, nodeId
                    ));
        }

        OtsOperationType operationType = neOperationLinkInput.getOperation();
        if (null == operationType) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "operation link operation type should not be null");
        }

        Class<? extends OPERATIONITEM> operationItem = neOperationLinkInput.getOperationItem();
        if (operationItem == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "operation link operation item should not be null");
        }

    }

    @Override
    public void validateSwitchCuActiveStandby(SwitchCuActiveStandbyInput activeStandbyInput) {
        NodeId nodeId = activeStandbyInput.getNodeId();
        String cuId = activeStandbyInput.getTargetCu();
        if (null == nodeId) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "switch cu ne id should not be null");
        }
        if (null == cuId) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "switch cu target cu id should not be null");
        }
        String neId = nodeId.getValue();
        Node phyNode = phyNodeDao.getPhyNodeById(neId);
        if (phyNode == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    String.format("the ne:%s is not found", nodeId));
        }
        Physical phyNodePhysical = phyNode.getAugmentation(Node1.class).getPhysical();
        List<Equipments> equipments = phyNodePhysical.getEquipments();
        Equipments cu = equipments.stream()
                .filter(equipment -> equipment.getEquipmentId().equals(cuId)).findAny()
                .orElse(null);
        if (cu == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    String.format("the cu:%s on ne:%s is not found", cuId, nodeId));
        }
        String cuEquipTypeConfiged = cu.getEquipTypeConfiged() == null ? cu.getEquipTypeInstalled()
                : cu.getEquipTypeConfiged();
        if (!cuEquipTypeConfiged.equals(CU)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the target cu:%s is not CU equipment", cuId));
        }
    }

    @Override
    public void validateUploadHistoryPmInput(UploadHistoryPmInput uploadHistoryPmInput) {
        NodeId nodeId = uploadHistoryPmInput.getNodeId();
        if (null == nodeId) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "upload history pm ne id should not be null");
        }
        String neId = nodeId.getValue();
        Node phyNode = phyNodeDao.getPhyNodeById(neId);
        if (phyNode == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    String.format("the ne:%s is not found", nodeId));
        }
        BigInteger startTime = uploadHistoryPmInput.getStartTimestamp();
        BigInteger endTime = uploadHistoryPmInput.getEndTimestamp();
        if (startTime == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "upload history pm  startTime should not be null");
        }
        if (endTime == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "upload history pm endTime should not be null");
        }
        if (startTime.compareTo(endTime) > 0) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "The start time cannot be after the end time for NE history uploads.");
        }
        BigInteger interval = uploadHistoryPmInput.getInterval();
        if (null == interval) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "upload history pm interval should not be null");
        }
        RemoteServer remoteServer = uploadHistoryPmInput.getRemoteServer();
        if (null == remoteServer) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "upload history pm remote server should not be null");
        }
    }

    @Override
    public void validateChannelAseRestoreInput(ChannelAseRestoreInput channelAseRestoreInput) {
        NodeId nodeId = channelAseRestoreInput.getNodeId();
        if (null == nodeId) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "restore channel ase ne id should not be null");
        }
        String neId = nodeId.getValue();
        Node phyNode = phyNodeDao.getPhyNodeById(neId);
        if (phyNode == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    String.format("the ne:%s is not found", nodeId));
        }
        String crossConnectionId = channelAseRestoreInput.getCrossConnectionId();
        if (crossConnectionId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "restore channel index should not be null");
        }
        CrossConnections crossConnection = crossConnectionsDao.getXCById(crossConnectionId);
        if (crossConnection == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the cross connection :%s is not existed", crossConnectionId));
        }
    }

    @Override
    public void validateExecuteNetConfCmd(ExecuteNetconfCommandInput executeNetconfCommandInput) {
        NodeId nodeId = executeNetconfCommandInput.getNodeId();
        if (null == nodeId) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "execute NetConf Command ne id should not be null");
        }
        Node phyNode = phyNodeDao.getPhyNodeById(nodeId.getValue());
        if (null == phyNode) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the ne:%s is not found", nodeId.getValue()));
        }
        String payload = executeNetconfCommandInput.getNetconfPayload();
        if (StringUtils.isBlank(payload)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the execute NetConf command should not be null");
        }
    }
}
