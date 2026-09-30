package net.flex.dci.otn.controller.implement.physical.component.ne.attribute;

import static net.flex.dci.otn.controller.implement.common.utils.CommonUtils.convert;
import static net.flex.dci.otn.controller.implement.common.utils.CommonUtils.logMessage;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import net.flex.dci.otn.controller.implement.common.dto.ContactNeResult;
import net.flex.dci.otn.controller.implement.common.enums.SetResultCode;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
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
public class NeFriendlyNameUpdateStrategy extends AbstractNeAttributeUpdateStrategy {

    private final NeManagerRpc neManagerRpc;

    private final AdapterDao adapterDao;

    private final NodePhysicalUpdateTaskMessageHandler taskMessageHandler;

    @Override
    public boolean supports(Nodes nodes) {
        return StringUtils.hasText(nodes.getPhysical().getFriendlyName());
    }

    @Override
    public ContactNeResult execute(Nodes nodes, TaskInfoMessage taskInfoMessage) {
        TaskInfoMessage actionTaskInfo = taskMessageHandler.generateUpdateTaskInfo(
                nodes,
                taskInfoMessage, taskActionType());
        log.info("update ne login info the neId:{}", nodes.getNodeId());
        String oldFriendlyName = phyNodeDao.getFriendlyName(nodes.getNodeId().getValue());
        String friendlyName = nodes.getPhysical().getFriendlyName();
        String operationName = taskMessageHandler.generateUpdateFriendlyNameOperationName(
                oldFriendlyName, friendlyName);
        try {
            validateChangeNodeCommon(nodes);
            validateChangeNodeFriendlyName(nodes, friendlyName);
            String neId = nodes.getNodeId().getValue();
            Node configNode = phyNodeDao.getConfigPhyNodeById(neId);
            ContactNeResult updateResult = updateFriendlyName(nodes, configNode);
            if (updateResult.getCode() == SetResultCode.SUCCESS) {
                logMessage(BroadCastConstant.UPDATE_NODE_FRIENDLY_NAME, operationName, BLANK,
                        actionTaskInfo);
            } else {
                String message = updateResult.getMessage();
                logMessage(BroadCastConstant.UPDATE_NODE_FRIENDLY_NAME, operationName, message,
                        actionTaskInfo);
            }
            return updateResult;
        } catch (Exception ex) {
            log.error("failed to update the node friendlyName :{}", ex.getMessage(), ex);
            //logerror message
            logMessage(BroadCastConstant.UPDATE_NODE_FRIENDLY_NAME, operationName, ex.getMessage(),
                    actionTaskInfo);
            return ContactNeResult.builder().code(SetResultCode.FAILED).message(ex.getMessage())
                    .build();
        }
    }

    private void validateChangeNodeFriendlyName(Nodes nodes, String friendlyName) {
        String neId = nodes.getNodeId().getValue();
        log.debug("validate change node friendlyName the neId friendlyName {}:{}", neId,
                friendlyName);
        String ip = nodes.getPhysical().getIp();
        PortNumber port = nodes.getPhysical().getPort();
        String oldFriendlyName = nodes.getPhysical().getFriendlyName();
        if (StringUtils.hasText(ip) && port != null && phyNodeDao.existsIpPortExcludeNe(ip, port,
                neId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format(
                            "Failed to Config phy node:%s because ip %s and port %s exists on other node",
                            oldFriendlyName,
                            ip,
                            port));
        }

        if (!StringUtils.hasText(friendlyName)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "ne's friendly-name cannot be empty");
        }
        if (friendlyName.contains(" ")) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "ne's friendly-name cannot contain spaces");
        }

        if (friendlyName.length() > Constant.NodefriendlyNameLength) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "ne's friendly-name should be smaller than " + Constant.NodefriendlyNameLength
                            + " characters");
        }
        if (phyNodeDao.existFriendlyNameExcludeNeId(friendlyName, neId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format(
                            "duplicated friendlyName %s on other ne", friendlyName));
        }
    }

    /**
     * as required the NE's friendlyName will be write to NE as hostName. hostName stored at
     * "properties": { "property": [ { "name": "hostName", "value":
     * "CQ-TH-ODC-TMP-TMP-IOPC4-G-01-1616379022426" } ] }
     *
     * @param node
     * @param dbCfgNode
     * @throws CommonException
     */
    private ContactNeResult updateFriendlyName(Nodes node, Node dbCfgNode) throws CommonException {
        String friendlyName = node.getPhysical().getFriendlyName();
        log.debug("start update node{} friendlyName{}", node.getNodeId().getValue(),
                friendlyName);
        Node newNode = updateFriendlyName(dbCfgNode, friendlyName);
        phyNodeDao.saveConfigPhyNode(newNode);
        Adapter adapter = adapterDao.getAdapterByNeId(node.getNodeId().getValue());
        if (adapter != null) {

            log.info("config ne:{} friendly name :{}", node.getNodeId(), friendlyName);
            ConfigNeOutput configOutput = neManagerRpc.configNe(
                    updateFriendlyNameInput(dbCfgNode, friendlyName));
            log.info("finish to config ne:{} friendly name :{},result is:{}", node.getNodeId(),
                    friendlyName, configOutput);
            if (configOutput.getFailObj() != null) {
                String failedMessage = convert(configOutput.getFailObj());
                return ContactNeResult.builder().code(SetResultCode.FAILED).message(failedMessage)
                        .build();
            }
//            newNode = updateFriendlyName(dbOpNode, friendlyName);
//            phyNodeDao.saveOpPhyNode(newNode);
        }
        return ContactNeResult.builder().code(SetResultCode.SUCCESS).build();
    }

    public Node updateFriendlyName(Node dbNode, String friendlyName) {
        String key = "hostName";
        String value = friendlyName;

        Physical physical = dbNode.getAugmentation(Node1.class).getPhysical();
        Properties newProp = PropertyTool.addProperty(physical.getProperties(), key, value);

        return new NodeBuilder(dbNode)
                .addAugmentation(Node1.class,
                        new Node1Builder()
                                .setPhysical(new PhysicalBuilder(physical)
                                        .setFriendlyName(friendlyName)
                                        .setProperties(newProp)
                                        .build())
                                .build())
                .build();
    }

    private Node updateFriendlyNameInput(Node dbOpNode, String friendlyName) {
        String key = "hostName";
        PropertyBuilder pb = new PropertyBuilder()
                .setKey(new PropertyKey(key))
                .setName(key)
                .setValue(friendlyName);

        Node newNode = new NodeBuilder()
                .setKey(dbOpNode.getKey())
                .setNodeId(dbOpNode.getNodeId())
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder()
                                .setProperties(new PropertiesBuilder().setProperty(
                                        Collections.singletonList(pb.build())).build())
                                .build())
                        .build())
                .build();
        return newNode;
    }

    @Override
    public ActionType taskActionType() {
        return ActionType.updateFriendlyName;
    }
}
