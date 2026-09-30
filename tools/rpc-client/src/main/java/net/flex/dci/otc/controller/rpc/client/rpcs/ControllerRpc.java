package net.flex.dci.otc.controller.rpc.client.rpcs;

import net.flex.dci.otc.common.exception.CommonException;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFriendlyNameInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFriendlyNameOutput;

/**
 * @version 1.0
 * @date 2022/3/14 19:00
 */
public interface ControllerRpc {

    GetFriendlyNameOutput getFriendlyName(GetFriendlyNameInput getFriendlyNameInput)
            throws CommonException;
}
