package net.flex.dci.otn.controller.implement.physical.component.aps;

import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.FORCE_TO_PORT;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.FORCE_TO_PORT_TARGET_INDEX;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.FORCE_TO_PORT_TARGET_PATH;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otn.controller.implement.common.dto.SwitchResult;
import net.flex.dci.otn.controller.implement.common.enums.ApsSwitchMode;
import net.flex.dci.otn.controller.implement.common.enums.CustomApsPath;
import net.flex.dci.otn.controller.implement.common.enums.ForceToPort;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ApsPathType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.ApsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.springframework.stereotype.Component;

/**
 *
 * 2025/8/22
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class ForceApsCommand extends AbstractApsCommand {

    public ForceApsCommand(NeManagerRpc neManagerRpc, CrossConnectionsDao crossConnectionsDao,
            ApsTaskMessageHandler apsTaskMessageHandler) {
        super(neManagerRpc, crossConnectionsDao, apsTaskMessageHandler);
    }

    @Override
    public ApsSwitchMode switchMode() {
        return ApsSwitchMode.FORCE;
    }


    @Override
    public SwitchResult executeCommand(String neId, CrossConnections apsCrossConnections,
            String apsName,
            CustomApsPath apsPath, TaskInfoMessage taskInfo) {
        log.debug(
                "start to execute the force command to ne :{} aps crossConnectionId:{} aps Name:{} target path:{}",
                neId, apsCrossConnections.getCrossConnectionId(), apsName, apsPath);
        SwitchResult switchResult = _executeCommand(neId, apsCrossConnections, apsName, apsPath,
                taskInfo);
        return switchResult;
    }

    private SwitchResult _executeCommand(String neId, CrossConnections crossConnections,
            String apsName, CustomApsPath apsPath, TaskInfoMessage taskInfo) {
        log.debug("execute aps command force to port");

        ApsPathType targetPath = apsPath.getApsRealPath();
//        Short index = apsPath.getIndex();
        Short index = getApsCommandSwitchRealIndex(crossConnections, apsPath);
        CrossConnections forceSwitchCommandXC = buildForceSwitchCommandXC(crossConnections, apsName,
                targetPath,
                index);
        Node configNode = buildConfigNode(neId, forceSwitchCommandXC);
        ConfigNeOutput forceSwitchOutput = neManagerRpc.configNe(configNode);
        SwitchResult switchResult = parseOutput(forceSwitchOutput);
        if (taskInfo != null) {
            TaskInfoMessage forceSwitchTaskInfo = apsTaskMessageHandler.enrichApsCommandInfo(
                    taskInfo, neId, apsName, apsPath, ApsSwitchMode.FORCE);
            apsTaskMessageHandler.logApsSwitchResult(forceSwitchTaskInfo, switchResult);
        }
        return switchResult;
    }


    /**
     * build force switch Command XC
     *
     * @param crossConnections
     * @param targetPath
     * @param index
     * @return
     */
    private CrossConnections buildForceSwitchCommandXC(CrossConnections crossConnections,
            String apsName,
            ApsPathType targetPath, Short index) {
        log.debug("build force switch command xc body");
        PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
        List<Property> propertyList = new ArrayList<>();
        //property list force to port
        PropertyTool.putKeyValue(propertyList, FORCE_TO_PORT, ForceToPort.FORCE.name());
        PropertyTool.putKeyValue(propertyList, FORCE_TO_PORT_TARGET_PATH, targetPath.name());
        PropertyTool.putKeyValue(propertyList, FORCE_TO_PORT_TARGET_INDEX, String.valueOf(index));
        propertiesBuilder.setProperty(propertyList);
        ApsBuilder apsBuilder = new ApsBuilder();
        apsBuilder.setName(apsName);
        apsBuilder.setProperties(propertiesBuilder.build());
        CrossConnectionsBuilder crossConnectionsBuilder = new CrossConnectionsBuilder();
        crossConnectionsBuilder.setAps(apsBuilder.build());
        crossConnectionsBuilder.setCrossConnectionId(crossConnections.getCrossConnectionId());
        return crossConnectionsBuilder.build();
    }
}
