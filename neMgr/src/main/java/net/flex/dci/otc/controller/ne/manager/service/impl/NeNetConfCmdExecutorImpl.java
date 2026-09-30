package net.flex.dci.otc.controller.ne.manager.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.ne.manager.components.balancer.AdapterBalancer;
import net.flex.dci.otc.controller.ne.manager.components.validator.InputValidator;
import net.flex.dci.otc.controller.ne.manager.service.NeNetConfCmdExecutor;
import net.flex.dci.otc.controller.rpc.client.dto.ExecuteNetConfResp;
import net.flex.dci.otc.controller.rpc.client.enums.ExecuteStatus;
import net.flex.dci.otc.controller.rpc.client.rpcs.AdapterRpc;
import net.flex.dci.otc.serialization.util.SerializeUtil;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ExecuteNetconfCommandInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ExecuteNetconfCommandOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ExecuteNetconfCommandOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.springframework.stereotype.Service;

/**
 *
 * 2025/12/20
 *
 * @author musa
 * @version 1.0
 **/
@Service
@Slf4j
@RequiredArgsConstructor
public class NeNetConfCmdExecutorImpl implements NeNetConfCmdExecutor {

    private final AdapterBalancer adapterBalancer;

    private final InputValidator inputValidator;

    private final AdapterRpc adapterRpc;

    @Override
    public String executeNetConfCmd(String input) {
        ExecuteNetconfCommandInput executeNetconfCommandInput = SerializeUtil.parseRpcInput(input,
                ExecuteNetconfCommandInput.class);
        inputValidator.validateExecuteNetConfCmd(executeNetconfCommandInput);
        log.info("ne:{} execute NetConf cmd:{}", executeNetconfCommandInput.getNodeId(),
                executeNetconfCommandInput.getNetconfPayload());
        String neId = executeNetconfCommandInput.getNodeId().getValue();
        String payload = executeNetconfCommandInput.getNetconfPayload();
        Adapter adapter = adapterBalancer.getAdapterForNe(neId);
        if (adapter == null) {
            throw new CommonException(CommonExceptionType.CANNOT_FIND_COOPERATOR,
                    "This network element is not connected to any adapters or is not being managed.");
        }
        ExecuteNetconfCommandOutput executeNetconfCommandOutput = executeNetConfCmd(neId, adapter,
                payload);

        return SerializeUtil.serializeRpcOutput2Json(executeNetconfCommandOutput);
    }

    private ExecuteNetconfCommandOutput executeNetConfCmd(String neId, Adapter adapter,
            String payload) {
        log.info("execute NetConf Command to:{}  through adapter:{}", neId, adapter.getName());
        log.info("execute NetConf Command payload:{}", payload);
        ExecuteNetConfResp executeResp = adapterRpc.executeNetConfCmd(adapter, neId, payload);
        if (executeResp.getStatus() == ExecuteStatus.FAILED) {
            throw new CommonException(CommonExceptionType.COMMAND_EXECUTION_ERROR,
                    executeResp.getMessage());
        }
        String netConfResponse = executeResp.getResp();
        log.info("execute NetConf command response :{}", netConfResponse);
        return new ExecuteNetconfCommandOutputBuilder().setReturnCode(RpcResultType.Success)
                .setNetconfResponse(netConfResponse).build();
    }
}
