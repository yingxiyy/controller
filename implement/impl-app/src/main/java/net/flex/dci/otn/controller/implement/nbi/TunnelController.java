/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.nbi;

import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.AuthConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otn.controller.implement.nbi.impl.RepaireOnNeFriendlyName;
import net.flex.dci.otn.controller.implement.common.repaire.RepaireOnOch;
import net.flex.dci.otn.controller.implement.nbi.impl.RepaireOnSiteNE;
import net.flex.dci.otn.controller.implement.tunnel.impl.TunnelImplementSync;
import net.flex.dci.otn.controller.implement.tunnel.impl.ProtectionLegRemovalService;
import net.flex.dci.otn.controller.implement.tunnel.impl.attribute.TunnelAttributeService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.*;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class TunnelController extends BaseController {

    private final TunnelImplementSync tunnelImplementSync;

    private final TunnelAttributeService tunnelAttributeService;

    private final ProtectionLegRemovalService protectionLegRemovalService;

    /**
     * impl/deimpl tunnel
     *
     * @param json
     * @param request
     * @return
     * @throws CommonException
     */
    @PostMapping(value = "/restconf/operations/tunnel:update-tunnel-sync", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updateTunnelSync(@RequestBody String json, HttpServletRequest request)
            throws CommonException {
        UpdateTunnelSyncInput input = formRpcInput(json, UpdateTunnelSyncInput.class);
        log.debug("updateTunnelSync {}", input);


        String pathInfo = request.getServletPath();
        if (!createHashCodes.add(pathInfo, json)) {
            log.info("duplicate request for update-tunnel-sync");

            UpdateTunnelSyncOutput output = new UpdateTunnelSyncOutputBuilder().setReturnCode(
                    RpcResultType.Success).build();
            return formRpcOutput(output);
        }

        UpdateTunnelSyncOutput output;
//        TunnelImplementSync sync = SpringBeanFinder.getBean(TunnelImplementSync.class);
        output = tunnelImplementSync.start(input,
                (String) (request.getHeader(AuthConstant.USER_TOKEN_HEADER)));

        String result = formRpcOutput(output);
        return result;
    }

    /** Removes one protection leg per OCH represented by the selected tunnels. */
    @PostMapping(value = "/restconf/operations/tunnel:batch-remove-leg",
            produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String batchRemoveLeg(@RequestBody String json, HttpServletRequest request) {
        BatchRemoveLegInput input = formRpcInput(json, BatchRemoveLegInput.class);
        BatchRemoveLegOutput output = protectionLegRemovalService.remove(input,
                request.getHeader(AuthConstant.USER_TOKEN_HEADER));
        return formRpcOutput(output);
    }

    /**
     * tunnel friendlyname, tti/testSignal, frequency, ModulationMode
     *
     * @param json
     * @param request
     * @return
     */
    @PostMapping(value = "/restconf/operations/tunnel:update-tunnel", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updateTunnel(@RequestBody String json, HttpServletRequest request) {
        UpdateTunnelInput input = formRpcInput(json, UpdateTunnelInput.class);
        log.debug("updateTunnel {}", input);

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                (String) (request.getHeader(AuthConstant.USER_TOKEN_HEADER)),
                TaskInfoMessage.ResourceType.tunnel,
                TaskInfoMessage.ActionType.other,
                json);

        tunnelAttributeService.setTaskInfo(taskInfoMessage);
        RpcResultType rpcResultType = tunnelAttributeService.updateTunnelAttribute(input,
                taskInfoMessage);
        UpdateTunnelOutput output = new UpdateTunnelOutputBuilder()
                .setReturnCode(rpcResultType)
                .build();

        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/tunnel:switch-spc", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String switchTunnel(@RequestBody String json, HttpServletRequest request) {
        // Tunnel switch only rewrites one OCH route for ROADM fault bypass. The
        // old SPC path is not covered by current OCH owner/follower protection.
        throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                "tunnel:switch-spc is disabled");
    }

    /**
     * 这是临时入口，用于修改数据库中设备的friendlyName 和设备真实名称不一致的的问题
     * @return
     */
    @PostMapping(value = "/friendlyName", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String friendlyName() {
        log.debug("friendlyName ");
        String result = new RepaireOnNeFriendlyName().start();
        return result;
    }

    @PostMapping(value = "/repairNE", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String repaireNe() {
        log.debug("repaireNe ");
        String result = new RepaireOnSiteNE().start();
        return result;
    }

    //数据库中所有OCH找一次， 把缺失的网元交叉补上
    @PostMapping(value = "/repairAllOch", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String repaireAllOch() {
        log.debug("repaireAllOch ");
        String result = new RepaireOnOch().allStart();
        return result;
    }

    //基于ochLink, 把缺失的网元交叉补上
    @PostMapping(value = "/repairOch/{ochLinkId}", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String repaireOneOch(@PathVariable String ochLinkId) {
        log.debug("repaireOneOch ");
        String result = new RepaireOnOch().start(ochLinkId);
        return result;
    }
}
