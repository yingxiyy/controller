package net.flex.dci.otn.controller.implement.physical.component.aps;

import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.ACTIVE_PATH_INDEX;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.ACTIVE_PATH_PATH;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.FORCE_TO_PORT;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.APS_ELEMENT.PATH_CONDITION_SUFFIX;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otn.controller.implement.common.dto.SwitchResult;
import net.flex.dci.otn.controller.implement.common.enums.ApsSwitchMode;
import net.flex.dci.otn.controller.implement.common.enums.CustomApsPath;
import net.flex.dci.otn.controller.implement.common.enums.ForceToPort;
import net.flex.dci.otn.controller.implement.common.enums.PathState;
import net.flex.dci.otn.controller.implement.common.enums.SetResultCode;
import net.flex.dci.otn.controller.implement.physical.util.ApsSwitchUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchInput.Action;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ApsPathType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.Aps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.ApsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * command auto switch command
 *
 *
 * 2025/8/22
 *
 * @author musa
 * @version 1.0
 **/

@Component
@Slf4j
public class AutoApsCommand extends AbstractApsCommand {

    public AutoApsCommand(NeManagerRpc neManagerRpc, CrossConnectionsDao crossConnectionsDao,
            ApsTaskMessageHandler apsTaskMessageHandler) {
        super(neManagerRpc, crossConnectionsDao, apsTaskMessageHandler);
    }

    @Override
    public ApsSwitchMode switchMode() {
        return ApsSwitchMode.AUTO;
    }


    @Override
    public SwitchResult executeCommand(String neId, CrossConnections apsCrossConnection,
            String apsName,
            CustomApsPath apsPath, TaskInfoMessage taskInfo) {
        log.debug(
                "auto switch command send to ne:{} and aps crossConnection is:{} switch name is :{}",
                neId, apsCrossConnection.getCrossConnectionId(), apsName);

        SwitchResult switchResult = _executeAutoSwitchCommand(neId, apsCrossConnection, apsName,
                taskInfo);
        return switchResult;
    }

    /**
     * force to port is none
     *
     * @param neId
     * @param apsCrossConnection
     * @return
     */
    private SwitchResult _executeAutoSwitchCommand(String neId,
            CrossConnections apsCrossConnection, String apsName, TaskInfoMessage taskInfo) {
        log.debug("execute auto switch command to ne:{} and crossConnections is:{}", neId,
                apsCrossConnection.getCrossConnectionId());
        PathState currentPathState = getCurrentPathState(apsCrossConnection);
        CrossConnections autoSwitchApsCrossConnection = buildAutoSwitchApsCommandXC(
                apsCrossConnection);
        SwitchResult switchResult = null;
        if (currentPathState != PathState.MS) {
            Node configNode = buildConfigNode(neId, autoSwitchApsCrossConnection);
            ConfigNeOutput autoSwitchOutput = neManagerRpc.configNe(configNode);
            switchResult = parseOutput(autoSwitchOutput);
        } else {
            ApsPathInfo apsPathInfo = getApsPathInfo(apsCrossConnection);
            ManageApsSwitchOutput manageSwitchOutput = neManagerRpc.manageApsSwitch(neId,
                    apsName, apsPathInfo.getApsPathType(), apsPathInfo.getIndex(), Action.CLEAR);
            switchResult = SwitchResult.builder().code(manageSwitchOutput.getReturnCode().equals(
                            RpcResultType.Success) ? SetResultCode.SUCCESS : SetResultCode.FAILED)
                    .message(manageSwitchOutput.getReturnMessage()).build();
        }
        if (taskInfo != null) {
            TaskInfoMessage switchTaskInfo = apsTaskMessageHandler.enrichApsCommandInfo(
                    taskInfo, neId, apsName, null, ApsSwitchMode.AUTO);
            apsTaskMessageHandler.logApsSwitchResult(switchTaskInfo, switchResult);
        }
        return switchResult;
    }

    /**
     * get current path state
     *
     * @param apsCrossConnection
     * @return
     */
    private PathState getCurrentPathState(CrossConnections apsCrossConnection) {
        String apsName = apsCrossConnection.getAps().getName();
        log.debug("get current aps:{} path state", apsName);
        CustomApsPath currentActivePath = CustomApsPath.fromApsPath(
                apsCrossConnection.getAps().getActivePath());
//        String apsGroupName = ApsSwitchUtils.getApsGroupNum(apsName);
//        List<Property> properties = apsCrossConnection.getAps().getProperties().getProperty();
        String pathStateKey = getApsCurrentPathStateKey(
                apsCrossConnection.getAps().getProperties().getProperty(),
                currentActivePath.getApsPortNum() + PATH_CONDITION_SUFFIX);
        String pathStateName = ApsSwitchUtils.getApsProperty(apsCrossConnection,
                pathStateKey);
        return PathState.valueOf(pathStateName);
    }

    private String getApsCurrentPathStateKey(List<Property> properties, String pathRegex) {
        log.debug("Searching for path state key with suffix: {}", pathRegex);
        log.debug("Available properties: {}", properties.stream()
                .map(Property::getName)
                .collect(Collectors.toList()));

        for (Property property : properties) {
            String name = property.getName();
            if (name.endsWith(pathRegex)) {
                log.debug("Found matching key: {}", name);
                return name;
            }
        }

        log.warn("No path state key found with suffix: {}", pathRegex);
        return null;
    }


    /**
     * build auto switch aps command cross connections
     *
     * @param apsCrossConnection
     * @return
     */
    private CrossConnections buildAutoSwitchApsCommandXC(CrossConnections apsCrossConnection) {
        log.debug("build auto switch command xc body");
        Aps aps = apsCrossConnection.getAps();
        String apsName = aps.getName();
        PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
        List<Property> propertyList = new ArrayList<>();
        //property list force to port
        PropertyTool.putKeyValue(propertyList, FORCE_TO_PORT, ForceToPort.NONE.name());
        propertiesBuilder.setProperty(propertyList);
        ApsBuilder apsBuilder = new ApsBuilder();
        apsBuilder.setName(apsName);
        apsBuilder.setProperties(propertiesBuilder.build());
        apsBuilder.setForceToPort(ApsPath.NONE);
        CrossConnectionsBuilder crossConnectionsBuilder = new CrossConnectionsBuilder();
        crossConnectionsBuilder.setAps(apsBuilder.build());
        crossConnectionsBuilder.setCrossConnectionId(apsCrossConnection.getCrossConnectionId());
        return crossConnectionsBuilder.build();
    }


    private ApsPathInfo getApsPathInfo(CrossConnections apsCrossConnection) {
        String apsPath = ApsSwitchUtils.getApsProperty(apsCrossConnection, ACTIVE_PATH_PATH);
        String apsPathIndex = ApsSwitchUtils.getApsProperty(apsCrossConnection, ACTIVE_PATH_INDEX);

        if (StringUtils.hasText(apsPath) && StringUtils.hasText(apsPathIndex)) {
            return new ApsPathInfo(
                    ApsPathType.valueOf(apsPath),
                    Short.parseShort(apsPathIndex)
            );
        } else {
            CustomApsPath currentActivePath = CustomApsPath.fromApsPath(
                    apsCrossConnection.getAps().getActivePath());
            return new ApsPathInfo(
                    currentActivePath.getApsRealPath(),
                    currentActivePath.getIndex()
            );
        }
    }

    @Data
    @AllArgsConstructor
    private static class ApsPathInfo {

        private ApsPathType apsPathType;
        private Short index;
    }
}
