package net.flex.dci.otn.controller.implement.physical.component.aps;

import static net.flex.dci.otn.controller.implement.physical.util.PhysicalUtils.convertFailObj;
import static net.flex.dci.otn.controller.implement.physical.util.PhysicalUtils.convertSuccessObj;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otn.controller.implement.common.dto.SwitchResult;
import net.flex.dci.otn.controller.implement.common.enums.ApsSwitchMode;
import net.flex.dci.otn.controller.implement.common.enums.CustomApsPath;
import net.flex.dci.otn.controller.implement.common.enums.SetResultCode;
import net.flex.dci.otn.controller.implement.physical.util.ApsSwitchUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.FailObj;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.SuccessObj;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.springframework.util.CollectionUtils;

/**
 *
 * 2025/8/22
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractApsCommand implements ApsCommand {

    protected final NeManagerRpc neManagerRpc;

    protected final CrossConnectionsDao crossConnectionsDao;

    protected final ApsTaskMessageHandler apsTaskMessageHandler;

    public abstract ApsSwitchMode switchMode();


    /**
     * build config node force command method
     *
     * @param neId
     * @param apsSwitchCommandXC
     * @return
     */
    protected Node buildConfigNode(String neId, CrossConnections apsSwitchCommandXC) {
        log.debug("build config node neId:{} and cross connections is:{}", neId,
                apsSwitchCommandXC);
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(NodeId.getDefaultInstance(neId));
        PhysicalBuilder physicalBuilder = new PhysicalBuilder();
        physicalBuilder.setCrossConnections(Collections.singletonList(apsSwitchCommandXC));
        Node1Builder node1Builder = new Node1Builder();

        node1Builder.setPhysical(physicalBuilder.build());
        nodeBuilder.addAugmentation(Node1.class, node1Builder.build());
        return nodeBuilder.build();
    }

    protected SwitchResult parseOutput(ConfigNeOutput switchOutput) {
        log.debug("parse switch output ");
        SuccessObj successObj = switchOutput.getSuccessObj();
        FailObj failObj = switchOutput.getFailObj();
        SwitchResult switchResult = null;
        if (successObj != null && !CollectionUtils.isEmpty(successObj.getObject())) {
            String successMessage = convertSuccessObj(successObj);
            switchResult = SwitchResult.builder().code(SetResultCode.SUCCESS)
                    .message(successMessage).build();
        } else {
            String failObjMessage = convertFailObj(failObj);
            log.error("switch command execute failed,the message is failObjMessage:{}",
                    failObjMessage);
            switchResult = SwitchResult.builder().code(SetResultCode.FAILED)
                    .message(failObjMessage).build();
        }
        return switchResult;
    }

    protected short getApsCommandSwitchRealIndex(CrossConnections apsCrossConnection,
            CustomApsPath apsPath) {
        log.debug("get aps cross connection:{} switch path command:{}",
                apsCrossConnection.getCrossConnectionId(), apsPath);
        String switchPathIndex = ApsSwitchUtils.getApsProperty(apsCrossConnection,
                apsPath.getActualIndexPropertyKey());
        log.debug("aps cross connection:{} switch command real index:{}",
                apsCrossConnection.getCrossConnectionId(), switchPathIndex);
        return Short.parseShort(switchPathIndex);

    }


}
