package net.flex.dci.otc.controller.rpc.client.rpcs.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.rpc.client.rpcs.ControllerRpc;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFriendlyNameInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFriendlyNameOutput;

/**
 * @version 1.0
 * @date 2022/3/14 18:59
 */
@Deprecated
@Slf4j
public class ControllerRpcImpl extends BasicRpc implements ControllerRpc {

    public ControllerRpcImpl() {
        NAMESPACE = "nms";
        MODULE_NAME = "nms-service";
    }


    @Override
    public GetFriendlyNameOutput getFriendlyName(GetFriendlyNameInput getFriendlyNameInput)
            throws CommonException {
        log.info("start to get tunnel paged {} ", getFriendlyNameInput);
        try {
            String requestOp = "get-friendly-name";
            String requestBody = formRpcInput(requestOp, getFriendlyNameInput);
            String responseBody = executeRequest(requestOp, requestBody);
            GetFriendlyNameOutput output = (GetFriendlyNameOutput) formRpcOutPut(requestOp,
                    responseBody);
            return output;
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get tunnel paged " + ex.getMessage(), ex);
        }
    }
}
