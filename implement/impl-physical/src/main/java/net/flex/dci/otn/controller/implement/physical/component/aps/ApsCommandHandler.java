package net.flex.dci.otn.controller.implement.physical.component.aps;

import static net.flex.dci.otn.controller.implement.physical.util.PhysicalNodeUtils.buildConfigNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otn.controller.implement.common.dto.RestoreResult;
import net.flex.dci.otn.controller.implement.common.dto.SwitchResult;
import net.flex.dci.otn.controller.implement.common.enums.ApsMember;
import net.flex.dci.otn.controller.implement.common.enums.ApsSwitchMode;
import net.flex.dci.otn.controller.implement.common.enums.CustomApsPath;
import net.flex.dci.otn.controller.implement.common.enums.SetResultCode;
import net.flex.dci.otn.controller.implement.physical.util.ApsSwitchUtils;
import net.flex.dci.otn.controller.implement.physical.util.PhysicalUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchInput.Action;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ApsPathType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.batch.aps._switch.input.ApsSwitch;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
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
public class ApsCommandHandler implements ApsCommand {

    private final Map<ApsSwitchMode, AbstractApsCommand> apsSwitchModeMap;

    private final NeManagerRpc neManagerRpc;

    private final ApsTaskMessageHandler apsTaskMessageHandler;

    public ApsCommandHandler(List<AbstractApsCommand> apsCommandLists, NeManagerRpc neManagerRpc,
            ApsTaskMessageHandler apsTaskMessageHandler) {
        apsSwitchModeMap = apsCommandLists.stream().collect(HashMap::new,
                (apsSwitchModeAbstractApsCommandMap, command) -> apsSwitchModeAbstractApsCommandMap.put(
                        command.switchMode(), command), HashMap::putAll);
        this.neManagerRpc = neManagerRpc;
        this.apsTaskMessageHandler = apsTaskMessageHandler;
    }

    @Override
    public SwitchResult executeCommand(String neId, CrossConnections apsCrossConnection,
            String apsName,
            CustomApsPath apsPath, TaskInfoMessage taskInfo) {
        log.info(
                "start to execute command to neId:{} and aps cross connection id is :{} aps Name is:{} command is:{}",
                neId, apsCrossConnection.getCrossConnectionId(),
                apsName, apsPath);
        ApsSwitchMode apsSwitchMode = apsPath.getApsSwitchMode();
        return apsSwitchModeMap.get(apsSwitchMode)
                .executeCommand(neId, apsCrossConnection, apsName, apsPath, taskInfo);
    }

    @Override
    public SwitchResult configAps(String refNeId, String apsCrossConnectionId,
            String apsName,
            ApsSwitch apsSwitch) {
        log.debug("config aps ref ne:{} ref aps cross connection :{} and apsName :{} ", refNeId,
                apsCrossConnectionId, apsName);
        try {
            Node configNode = buildConfigNode(refNeId, apsCrossConnectionId, apsName, apsSwitch);
            ConfigNeOutput output = neManagerRpc.configNe(configNode);
            boolean hasError = false;
            String successMessage = null;
            String failMessage = null;
            if (output.getSuccessObj() != null && !output.getSuccessObj().getObject().isEmpty()) {
                successMessage = PhysicalUtils.convertSuccessObj(output.getSuccessObj());
                log.debug("config aps success output :{}", successMessage);
            }
            if (output.getFailObj() != null && !output.getFailObj().getObject().isEmpty()) {
                hasError = true;
                failMessage = PhysicalUtils.convertFailObj(output.getFailObj());
                log.debug("config aps failed  output :{}", failMessage);
            }
            SetResultCode switchResultCode =
                    hasError ? SetResultCode.FAILED : SetResultCode.SUCCESS;
            String message = hasError ? failMessage : successMessage;
            return SwitchResult.builder().code(switchResultCode).message(message).build();
        } catch (Exception ex) {
            log.error("failed to config aps the ");
            return SwitchResult.builder().code(SetResultCode.FAILED).message(ex.getMessage())
                    .build();
        }
    }

    @Override
    public RestoreResult executeRestoreCommand(String neId, CrossConnections apsCrossConnection,
            String apsName, ApsMember apsMember, TaskInfoMessage restoreTaskInfo) {
        log.debug("execute restore command ne:{} apsName:{} apsMember:{}", neId, apsName,
                apsMember);

        Short index = getRestoreMemberRealIndex(apsCrossConnection, apsMember);
        ApsPathType path = apsMember.getApsPathType();
        ManageApsSwitchOutput result = neManagerRpc.manageApsSwitch(neId, apsName, path, index,
                Action.RESTORE);
        RestoreResult restoreResult = RestoreResult.builder().build();
        if (result.getReturnCode().equals(RpcResultType.Success)) {
            restoreResult.setCode(SetResultCode.SUCCESS);
            restoreResult.setMessage(result.getReturnMessage());
        } else {
            restoreResult.setCode(SetResultCode.FAILED);
            restoreResult.setMessage(result.getReturnMessage());
        }
        if (restoreTaskInfo != null) {
            TaskInfoMessage switchTaskInfo = apsTaskMessageHandler.enrichRestoreCommandInfo(
                    restoreTaskInfo, neId, apsName,
                    apsMember);
            apsTaskMessageHandler.logApsRestoreResult(switchTaskInfo, restoreResult);
        }
        return restoreResult;

    }

    private Short getRestoreMemberRealIndex(CrossConnections apsCrossConnection,
            ApsMember apsMember) {
        log.debug("get  aps cross connection:{} restore member aps member:{}",
                apsCrossConnection.getCrossConnectionId(), apsMember);
        String switchPathIndex = ApsSwitchUtils.getApsProperty(apsCrossConnection,
                apsMember.getActualIndexPropertyKey());
        log.debug("aps cross connection:{} restore apsMember:{} real index:{}",
                apsCrossConnection.getCrossConnectionId(), apsMember, switchPathIndex);
        return Short.parseShort(switchPathIndex);

    }


}
