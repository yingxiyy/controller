package net.flex.dci.otn.controller.implement.physical.validator;

import static net.flex.dci.otc.common.constants.Constants.DOT;

import java.math.BigInteger;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.CommonAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FileTransferProtocol;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsSwitchControlInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.BatchApsSwitchInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RestoreApsPathInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RestoreApsPathInput.TargetApsMember;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UploadNeHistoryPmInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.batch.aps._switch.input.ApsSwitch;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.batch.aps._switch.input.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.upload.ne.history.pm.input.RemoteServer;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TunnelBaseAttributes;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 *
 * 2025/8/12
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class InputValidatorImpl implements InputValidator {

    private final TunnelDao tunnelDao;

    private final PhyNodeDao phyNodeDao;

    private final CrossConnectionsDao crossConnectionsDao;


    @Override
    public void validateBatchApsSwitchInput(BatchApsSwitchInput batchApsSwitchInput) {
        log.debug("validate batch aps switch input :{}", batchApsSwitchInput);
        List<Tunnel> tunnels = batchApsSwitchInput.getTunnel();
        if (CollectionUtils.isEmpty(tunnels)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "batch aps switch tunnels should not be null");
        }
        List<String> tunnelIds = tunnels.stream().map(Tunnel::getTunnelId).collect(
                Collectors.toList());
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel> dbTunnels = tunnelDao.listAllTunnelByIds(
                tunnelIds);
        if (tunnelIds.size() != dbTunnels.size()) {
            // 提取数据库中存在隧道的ID集合
            Set<String> existingIds = dbTunnels.stream()
                    .map(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel::getTunnelId)
                    .map(Uri::getValue)
                    .collect(Collectors.toSet());
            // 找出不存在的ID
            List<String> missingIds = tunnelIds.stream()
                    .filter(id -> !existingIds.contains(id))
                    .collect(Collectors.toList());
            String errorMsg = String.format("%d tunnel(s) not found in database: %s",
                    missingIds.size(), missingIds);
            log.error("Validation failed: {}", errorMsg);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, errorMsg);
        }

        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel> unprotectedTunnels = dbTunnels.stream()
                .filter(tunnel -> tunnel.getProtectionType().isAssignableFrom(
                        ProtectionUnprotected.class)).collect(Collectors.toList());
        if (!unprotectedTunnels.isEmpty()) {
            List<String> tunnelNames = unprotectedTunnels.stream().map(
                    CommonAttributes::getFriendlyName).collect(
                    Collectors.toList());
            List<String> notProtectedTunnelIds = unprotectedTunnels.stream().map(
                    TunnelBaseAttributes::getTunnelId).map(Uri::getValue).collect(
                    Collectors.toList());
            String errorMsg = String.format("%d tunnel(s) have no protection: %s",
                    notProtectedTunnelIds.size(), tunnelNames);
            log.error("Validation failed tunnelId :{} error message: {}", notProtectedTunnelIds,
                    errorMsg);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, errorMsg);
        }
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel> notImplementTunnels = dbTunnels.stream()
                .filter(tunnel -> tunnel.getImplementState() != ImplementState.Implement)
                .filter(tunnel -> tunnel.getImplementState() != ImplementState.PartialImplement)
                .collect(Collectors.toList());
        if (!notImplementTunnels.isEmpty()) {
            List<String> tunnelNames = notImplementTunnels.stream().map(
                    CommonAttributes::getFriendlyName).collect(
                    Collectors.toList());
            List<String> notImplementTunnelIds = notImplementTunnels.stream().map(
                    TunnelBaseAttributes::getTunnelId).map(Uri::getValue).collect(
                    Collectors.toList());
            String errorMsg = String.format("%s tunnel(s) has not implement or partial",
                    String.join(DOT, tunnelNames));
            log.error("the tunnel:{} is not implemented :{}", notImplementTunnelIds, errorMsg);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, errorMsg);
        }
        ApsPath targetPath = batchApsSwitchInput.getTargetPath();
        ApsSwitch apsSwitch = batchApsSwitchInput.getApsSwitch();
        if (null == apsSwitch && targetPath == null) {
            log.error(
                    "Validation failed,the aps Switch should not be null and switch configuration is null");
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "batch aps switch command should not be null");
        }
    }

    @Override
    public void validateApsSwitchControlInput(ApsSwitchControlInput apsSwitchControlInput) {
        log.debug("validate aps switch control input :{}", apsSwitchControlInput);
        NodeId neId = apsSwitchControlInput.getNeId();
        if (Objects.isNull(neId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "execute aps switch command ne id should not be null");
        }
        boolean existNode = phyNodeDao.existsByNodeId(neId.getValue());
        if (!existNode) {
            String errorMsg = "Ne id:%s is not found";
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    String.format(errorMsg, neId.getValue()));
        }
        String apsCrossConnectionId = apsSwitchControlInput.getApsCrossconnectionId();
        CrossConnections apsCrossConnection = crossConnectionsDao.getXCById(apsCrossConnectionId);
        if (apsCrossConnection == null) {
            String apsErrorMsg = "ne %s do not have cross connection :%s";
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format(apsErrorMsg, neId.getValue(), apsCrossConnectionId));
        }
        String apsName = apsSwitchControlInput.getApsName();
        if (!StringUtils.hasText(apsName)) {
            String apsErrorMsg = "execute switch command aps name should not be null";
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    apsErrorMsg);
        }
        String apsRealName = apsCrossConnection.getAps().getName();
        if (!apsRealName.equals(apsName)) {
            String errorMsg = "aps name %s is not correct for the aps cross connection:%S on ne:%s";
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format(errorMsg, apsName, apsCrossConnectionId, neId));
        }

    }

    @Override
    public void validateUploadNeHistoryPmInput(UploadNeHistoryPmInput input) {
        log.debug("validate upload ne history pm input:{}", input);
        NodeId nodeId = input.getNodeId();
        if (nodeId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "upload ne history pm input id should not be null");
        }
        String neId = nodeId.getValue();
        Node node = phyNodeDao.getPhyNodeById(neId);
        if (node == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    String.format("the upload history pm input ne %s is not existed", neId));
        }
        BigInteger startTime = input.getStartTimestamp();
        if (null == startTime) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the upload history pm startTime should not be null");
        }
        BigInteger endTime = input.getEndTimestamp();
        if (null == endTime) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the upload history pm endTime should not be null");
        }
        if (startTime.compareTo(endTime) > 0) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "The start time cannot be after the end time for NE history uploads.");
        }
        BigInteger interval = input.getInterval();
        if (null == interval) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "upload history pm interval should not be null");
        }
        RemoteServer remoteServer = input.getRemoteServer();
        if (null == remoteServer) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "upload history pm remote server should not be null");
        }
        String serverAddress = remoteServer.getAddress();
        if (null == serverAddress) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "upload history pm remote server ip should not be null");
        }
        PortNumber port = remoteServer.getPort();
        if (null == port) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "upload history pm remote server port should not be null");
        }
        String user = remoteServer.getUser();
        if (null == user) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "upload history pm remote server login user should not be null");
        }

        String password = remoteServer.getPassword();
        if (null == password) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "upload history pm remote server login password should not be null");
        }
        String uploadPath = remoteServer.getUploadPath();
        if (null == uploadPath) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "upload history pm remote server upload path should not be null");
        }
        FileTransferProtocol protocol = remoteServer.getProtocol();
        if (protocol == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "upload history pm remote server use protocol should not be null");
        }
        String sourceAddress = remoteServer.getSourceAddress();
        if (null == sourceAddress) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "upload history pm remote server source address should not be null");
        }
    }

    @Override
    public void validateRestoreApsPathInput(RestoreApsPathInput input) {
        log.debug("validate restore aps path input :{}", input);
        NodeId neId = input.getNeId();
        if (Objects.isNull(neId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "execute restore aps path ne id should not be null");
        }
        boolean existNode = phyNodeDao.existsByNodeId(neId.getValue());
        if (!existNode) {
            String errorMsg = "Ne id:%s is not found";
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    String.format(errorMsg, neId.getValue()));
        }
        String apsCrossConnectionId = input.getApsCrossconnectionId();
        CrossConnections apsCrossConnection = crossConnectionsDao.getXCById(apsCrossConnectionId);
        if (apsCrossConnection == null) {
            String apsErrorMsg = "ne %s do not have cross connection :%s";
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format(apsErrorMsg, neId.getValue(), apsCrossConnectionId));
        }
        String apsName = input.getApsName();
        if (!StringUtils.hasText(apsName)) {
            String apsErrorMsg = "execute switch command aps name should not be null";
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    apsErrorMsg);
        }
        String apsRealName = apsCrossConnection.getAps().getName();
        if (!apsRealName.equals(apsName)) {
            String errorMsg = "aps name %s is not correct for the aps cross connection:%S on ne:%s";
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format(errorMsg, apsName, apsCrossConnectionId, neId));
        }
        TargetApsMember targetApsMember = input.getTargetApsMember();
        if (targetApsMember == null) {
            throw new CommonException(
                    CommonExceptionType.INVALID_PARAMETER,
                    "restore-aps-path failed: targetApsMember is required"
            );
        }
    }
}
