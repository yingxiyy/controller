package net.flex.dci.otn.controller.implement.service.impl;

import static net.flex.dci.otn.controller.implement.common.utils.Constants.RUN_AS_ASYNC;

import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.serialization.util.SerializeUtil;
import net.flex.dci.otn.controller.implement.common.dto.RestoreResult;
import net.flex.dci.otn.controller.implement.common.dto.SwitchResult;
import net.flex.dci.otn.controller.implement.common.enums.SetResultCode;
import net.flex.dci.otn.controller.implement.common.utils.AsynchronousExecutor;
import net.flex.dci.otn.controller.implement.physical.component.ApsSwitchManager;
import net.flex.dci.otn.controller.implement.physical.component.aps.ApsTaskMessageHandler;
import net.flex.dci.otn.controller.implement.physical.validator.InputValidator;
import net.flex.dci.otn.controller.implement.service.ApsSwitchService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsSwitchControlInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsSwitchControlOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsSwitchControlOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.BatchApsSwitchInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.BatchApsSwitchOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.BatchApsSwitchOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RestoreApsPathInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RestoreApsPathInput.TargetApsMember;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RestoreApsPathOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RestoreApsPathOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.batch.aps._switch.input.ApsSwitch;
import org.springframework.stereotype.Component;

/**
 * 2025/8/12
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class ApsSwitchServiceImpl implements ApsSwitchService {

    private final InputValidator inputValidator;

    private final ApsSwitchManager apsSwitchManager;

    private final ApsTaskMessageHandler apsTaskMessageHandler;

    @Override
    public String batchApsSwitch(String input, HttpServletRequest request) {
        log.debug("batch aps switch cmd is:{}", input);
        BatchApsSwitchInput batchApsSwitchInput = SerializeUtil.parseRpcInput(input,
                BatchApsSwitchInput.class);
        inputValidator.validateBatchApsSwitchInput(batchApsSwitchInput);
        List<String> tunnelIds = batchApsSwitchInput.getTunnel().stream().map(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.batch.aps._switch.input.Tunnel::getTunnelId)
                .collect(Collectors.toList());
        log.debug("batch aps switch tunnel,tunnel  is:{}", tunnelIds);
        ApsSwitch apsSwitch = batchApsSwitchInput.getApsSwitch();
        ApsPath targetApsPath = batchApsSwitchInput.getTargetPath();
        TaskInfoMessage taskInfo = apsTaskMessageHandler.generateBatchApsTaskInfo(
                request, input, tunnelIds);

        //todo：log the task
        AsynchronousExecutor.execute(() -> {
            log.debug("batch tunnel aps switch task start ,switch aps tunnelIds is:{}", tunnelIds);
            //<task:task name="批量倒换任务 - ${当前时间}"/>
            apsSwitchManager.batchTunnelApsSwitch(tunnelIds, apsSwitch, targetApsPath, taskInfo);
        });
        //todo: log the task finished
        BatchApsSwitchOutput batchApsSwitchOutput = new BatchApsSwitchOutputBuilder().setReturnCode(
                        RpcResultType.Success)
                .setReturnMessage(RUN_AS_ASYNC)
                .build();
        return SerializeUtil.serializeRpcOutput2Json(batchApsSwitchOutput);
    }

    @Override
    public String apsSwitchControl(String input, HttpServletRequest request) {
        log.debug("aps switch control cmd is :{}", input);
        try {
            ApsSwitchControlInput apsSwitchControlInput = SerializeUtil.parseRpcInput(input,
                    ApsSwitchControlInput.class);
            inputValidator.validateApsSwitchControlInput(apsSwitchControlInput);
            TaskInfoMessage apsTaskInfo = apsTaskMessageHandler.generateApsSwitchControlTask(
                    request, input);
            String neId = apsSwitchControlInput.getNeId().getValue();
            String apsName = apsSwitchControlInput.getApsName();
            ApsPath targetPath = apsSwitchControlInput.getTargetPath();
            String apsCrossConnectionId = apsSwitchControlInput.getApsCrossconnectionId();
            SwitchResult switchResult = apsSwitchManager.executeApsSwitch(neId, apsName,
                    apsCrossConnectionId, targetPath, apsTaskInfo);
            if (switchResult.code == SetResultCode.FAILED) {
                throw new CommonException(CommonExceptionType.COMMAND_EXECUTION_ERROR,
                        switchResult.message);
            }
            ApsSwitchControlOutput apsSwitchControlOutput = new ApsSwitchControlOutputBuilder().setReturnCode(
                    RpcResultType.Success).build();
            return SerializeUtil.serializeRpcOutput2Json(apsSwitchControlOutput);
        } catch (Exception ex) {
            log.error("failed to set aps switch the reason is:{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }

    @Override
    public String restoreApsPath(String input, HttpServletRequest request) {
        log.debug("restore aps path input is:{}", input);
        try {
            RestoreApsPathInput restoreApsPathInput = SerializeUtil.parseRpcInput(input,
                    RestoreApsPathInput.class);
            inputValidator.validateRestoreApsPathInput(restoreApsPathInput);
            TaskInfoMessage restoreTaskInfo = apsTaskMessageHandler.generateRestoreApsPathTask(
                    request, input);
            String neId = restoreApsPathInput.getNeId().getValue();
            String apsName = restoreApsPathInput.getApsName();
            String apsCrossConnectionId = restoreApsPathInput.getApsCrossconnectionId();
            TargetApsMember restoreMember = restoreApsPathInput.getTargetApsMember();
            RestoreResult restoreResult = apsSwitchManager.executeRestore(neId, apsName,
                    apsCrossConnectionId, restoreMember, restoreTaskInfo);
            if (restoreResult.getCode() == SetResultCode.FAILED) {
                throw new CommonException(CommonExceptionType.COMMAND_EXECUTION_ERROR,
                        restoreResult.getMessage());
            }
            RestoreApsPathOutput restoreApsPathOutput = new RestoreApsPathOutputBuilder().setReturnCode(
                    RpcResultType.Success).build();
            return SerializeUtil.serializeRpcOutput2Json(restoreApsPathOutput);
        } catch (Exception ex) {
            log.error("failed to restore aps path the reason is:{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }

}
