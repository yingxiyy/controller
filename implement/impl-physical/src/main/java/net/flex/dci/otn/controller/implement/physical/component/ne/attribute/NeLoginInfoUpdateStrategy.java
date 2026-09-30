package net.flex.dci.otn.controller.implement.physical.component.ne.attribute;

import static net.flex.dci.otn.controller.implement.common.utils.CommonUtils.logMessage;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;
import static net.flex.dci.otn.controller.implement.physical.util.PhysicalUtils.isValidIp;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otn.controller.implement.common.dto.ContactNeResult;
import net.flex.dci.otn.controller.implement.common.enums.SetResultCode;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RegisteNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RegisteNeInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RegisteNeOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SupervisionStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.update.node.input.Nodes;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 2026/4/14
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class NeLoginInfoUpdateStrategy extends AbstractNeAttributeUpdateStrategy {

    private final NeManagerRpc neManagerRpc;

    private final NodePhysicalUpdateTaskMessageHandler taskMessageHandler;

    @Override
    public boolean supports(Nodes nodes) {
        Physical physical = nodes.getPhysical();
        String ip = physical.getIp();
        PortNumber port = physical.getPort();
        String loginName = physical.getLoginName();
        String loginPassword = physical.getLoginPasswd();

        return StringUtils.hasText(physical.getIp()) && port != null &&
                loginName != null && loginPassword != null && StringUtils.hasText(ip);
    }

    @Override
    public ContactNeResult execute(Nodes node, TaskInfoMessage taskInfoMessage) {
        TaskInfoMessage actionTaskInfo = taskMessageHandler.generateUpdateTaskInfo(
                node,
                taskInfoMessage, taskActionType());
        log.info("update ne login info the neId:{}", node.getNodeId());
        String friendlyName = phyNodeDao.getFriendlyName(node.getNodeId().getValue());
        String operationName = taskMessageHandler.generateUpdateNeLoginInfoOperationName(
                friendlyName);
        try {
            log.info("update ne login info the neId:{}", node.getNodeId());
            log.debug("change the ne login info");
            validateChangeNodeCommon(node);
            validateChangeNodeLoginInfo(node);
            String neId = node.getNodeId().getValue();
            Node configNode = phyNodeDao.getConfigPhyNodeById(neId);
            ContactNeResult contactNeResult = updateLoginInfoAndRegisterNe(node, configNode);
            if (contactNeResult.getCode() == SetResultCode.SUCCESS) {
                logMessage(BroadCastConstant.UPDATE_NODE_LOGIN_INFO, operationName,
                        BLANK, actionTaskInfo);
            } else {
                logMessage(BroadCastConstant.UPDATE_NODE_LOGIN_INFO, operationName,
                        contactNeResult.getMessage(), actionTaskInfo);
            }
            return contactNeResult;
        } catch (Exception ex) {
            log.error("failed to update the ne login info :{}", ex.getMessage(), ex);
            //log error taskInfo
            logMessage(BroadCastConstant.UPDATE_NODE_LOGIN_INFO, operationName, ex.getMessage(),
                    actionTaskInfo);
            return ContactNeResult.builder().code(SetResultCode.FAILED).message(ex.getMessage())
                    .build();
        }
    }

    private ContactNeResult updateLoginInfoAndRegisterNe(Nodes node, Node configNode)
            throws CommonException {
        log.info("start to update the login info and register ne the neId :{}",
                node.getNodeId().getValue());
        String neId = node.getNodeId().getValue();
        updateLoginInfo(node, configNode);
        RegisteNeInput registeNeInput = new RegisteNeInputBuilder().setNodeId(
                NodeId.getDefaultInstance(neId)).build();
        RegisteNeOutput resultOutput = neManagerRpc.registeredNe(registeNeInput);
        ContactNeResult contactNeResult = ContactNeResult.builder()
                .code(resultOutput.getReturnCode() == RpcResultType.Success
                        ? SetResultCode.SUCCESS : SetResultCode.FAILED)
                .message(resultOutput.getReturnMessage())
                .build();
        log.debug("registered the ne result is :{}", resultOutput);
        return contactNeResult;
    }

    /**
     * ����޸���loginInfo, ����true
     *
     * @param node
     * @param configNode
     * @throws CommonException
     */
    private void updateLoginInfo(Nodes node, Node configNode) throws CommonException {
        log.debug("change ne login info:{}", node);
        Physical physical = configNode.getAugmentation(Node1.class).getPhysical();

        String ip = null;
        PortNumber port = null;
        String user = null;
        String passwd = null;
        if (node.getPhysical().getIp() != null) {
            ip = node.getPhysical().getIp();
        }
        if (node.getPhysical().getPort() != null) {
            port = node.getPhysical().getPort();
        }
        if (node.getPhysical().getLoginName() != null) {
            user = node.getPhysical().getLoginName();
        }
        if (node.getPhysical().getLoginPasswd() != null) {
            passwd = node.getPhysical().getLoginPasswd();
        }

        if (ip != null || port != null || user != null || passwd != null) {
            Node newNode = new NodeBuilder()
                    .setNodeId(configNode.getNodeId())
                    .addAugmentation(Node1.class,
                            new Node1Builder()
                                    .setPhysical(new PhysicalBuilder(physical)
                                            .setIp(ip == null ? physical.getIp() : ip.toLowerCase())
                                            .setPort(port == null ? physical.getPort() : port)
                                            .setLoginName(
                                                    user == null ? physical.getLoginName() : user)
                                            .setLoginPasswd(
                                                    passwd == null ? physical.getLoginPasswd()
                                                            : passwd)
                                            .setAdminState(AdminStatus.Up)
                                            .build())
                                    .build())
                    .build();
            //todo: set current phy node supervision type
            log.debug("update current phy node:{} supervision state to monitoring",
                    configNode.getNodeId());
            phyNodeDao.updateConfigPhyNodeSupervisionState(configNode.getNodeId().getValue(),
                    SupervisionStatusType.Monitoring);
            phyNodeDao.updatePhyNodeLoginInfo(newNode);
        }
    }

    private void validateChangeNodeLoginInfo(Nodes nodes) {
        String neId = nodes.getNodeId().getValue();
        Physical physical = nodes.getPhysical();
        validateIpPort(physical, neId);
    }

    private void validateIpPort(Physical phy, String neId) {
        log.debug("validate ip and port modify ");
        if (phy.getIp() == null) {
            return;
        }
        Physical nePhysical = phyNodeDao.getConfigPhysicalByNode(neId);
        if (phy.getPort() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("NE: %s port is mandatory ", nePhysical.getFriendlyName()));
        }
        String ip = phy.getIp();
        Integer sPort = phy.getPort() == null ? null : phy.getPort().getValue();
        if (StringUtils.hasText(ip) && sPort != null && (!StringUtils.hasText(phy.getLoginName())
                && !StringUtils.hasText(phy.getLoginPasswd()))) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "change the ip and port login account and password should not be null");
        }
        String friendlyName = phy.getFriendlyName();

        if (StringUtils.hasText(friendlyName) && phyNodeDao.existFriendlyNameExcludeNeId(
                friendlyName, neId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format(
                            "duplicated friendlyName %s on other ne", friendlyName));
        }

        if (nePhysical.getSupervisionStatus() == SupervisionStatusType.Monitoring
                && nePhysical.getIp() != null) {
            String errorMessage = "current ne:%s(%s) is supervised,should be unsupervised before modifying ip";
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format(errorMessage, nePhysical.getFriendlyName(),
                            neId));
        }

        if (!isValidIp(ip)) {
            String errorMessage = String.format("the ip %s is not valid", ip);
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, errorMessage);
        }
        if (phyNodeDao.existsIpPortExcludeNe(phy.getIp(), phy.getPort(), neId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format(
                            "Failed to Config phy node:%s because ip %s and port %s exists on other node",
                            nePhysical.getFriendlyName(),
                            phy.getIp(),
                            phy.getPort().getValue()));
        }

    }


    @Override
    public ActionType taskActionType() {
        return ActionType.updateDeviceLoginInfo;
    }
}
