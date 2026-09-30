/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.nbi;

import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.AuthConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.config.NEInfoConfig;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfo;
import net.flex.dci.otn.controller.allocate.link.tunnel.FreeResourceQuery;
import net.flex.dci.otn.controller.allocate.link.tunnel.RegAllocator;
import net.flex.dci.otn.controller.allocate.link.tunnel.TunnelAllocator;
import net.flex.dci.otn.controller.allocate.link.tunnel.TunnelAllocator2;
import net.flex.dci.otn.controller.allocate.link.tunnel.TunnelBinder;
import net.flex.dci.otn.controller.allocate.link.tunnel.TunnelComputer;
import net.flex.dci.otn.controller.allocate.link.tunnel.TunnelComputer2;
import net.flex.dci.otn.controller.allocate.link.tunnel.TunnelCreator;
import net.flex.dci.otn.controller.allocate.link.tunnel.TunnelCreator2;
import net.flex.dci.otn.controller.allocate.link.tunnel.TunnelCreator3;
import net.flex.dci.otn.controller.allocate.link.tunnel.TunnelRemover;
import net.flex.dci.otn.controller.allocate.link.tunnel.TunnelSpcManager;
import net.flex.dci.otn.controller.allocate.link.tunnel.TunnelTakeOver;
import net.flex.dci.otn.controller.allocate.link.tunnel.UserDefinedTunnelAllocator;
import net.flex.dci.otn.controller.allocate.nbi.impl.repair.DBRepair;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import net.flex.dci.otn.controller.allocate.ne.Port;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateNetworkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.CreateNetworkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateRegsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateRegsOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateTunnels2Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateTunnels2Output;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateTunnelsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.AllocateTunnelsOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BatchBindTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BatchBindTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BatchGetBindingListInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BatchGetBindingListOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BindTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.BindTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnels2Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnels2Output;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnelsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.ComputeTunnelsOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnel2Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnel2Output;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnel3Input;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnel3Output;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.CreateTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.DesignTunnelsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.DesignTunnelsOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetBindingListInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetBindingListOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetFreeResourceInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetFreeResourceOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetLportNumInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetLportNumOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetSpcBackupRoutesInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.GetSpcBackupRoutesOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RemoveTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RemoveTunnelOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.RemoveTunnelOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TakeoverTunnelsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.TakeoverTunnelsOutput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RestController
public class TunnelController extends AbstractNBIController {

    @Autowired
    private TunnelAllocator tunnelAllocator;
    @Autowired
    @Qualifier("tunnelAllocator2")
    private TunnelAllocator2 tunnelAllocator2;
    @Autowired
    private UserDefinedTunnelAllocator userDefinedTunnelAllocator;
    @Autowired
    private TunnelComputer2 tunnelComputer2;
    @Autowired
    private TunnelSpcManager tunnelSpcManager;
    @Autowired
    private TunnelTakeOver tunnelTakeOver;
    @Autowired
    private RegAllocator regAllocator;
    @Autowired
    private SiteNodeDao siteNodeDao;
    @Autowired
    private NEInfoConfig neInfoConfig;
    @Autowired
    private TunnelBinder tunnelBinder;
    @Autowired
    private FreeResourceQuery freeResourceQuery;

    private static final AtomicLong TASK_GROUP_ID_GENERATOR = new AtomicLong(System.currentTimeMillis());

    @PostMapping(value = "/restconf/operations/tunnel:compute-tunnels", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String computeTunnels(@RequestBody String json) {
        log.info("computeTunnel {}");

        ComputeTunnelsInput input = formRpcInput(json, ComputeTunnelsInput.class);

        TunnelComputer tunnelComputer = new TunnelComputer();
        ComputeTunnelsOutput output = tunnelComputer.doIt(input);

        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/tunnel:compute-tunnels-2", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String computeTunnels2(@RequestBody String json) {
        log.info("computeTunnel {}", json);

        ComputeTunnels2Input input;
        try {
            input = formRpcInput(json, ComputeTunnels2Input.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        ComputeTunnels2Output output;
        try {
            output = tunnelComputer2.doIt(input);
            return formRpcOutput(output);
        } catch (Exception e) {
            log.error("Failed to compute tunnel from input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);

        }
    }

    @PostMapping(value = "/restconf/operations/tunnel:get-spc-backup-routes", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getSpcRoutes(@RequestBody String json) {
        log.info("getSpcRoutes {}", json);

        GetSpcBackupRoutesInput input;
        try {
            input = formRpcInput(json, GetSpcBackupRoutesInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        GetSpcBackupRoutesOutput output;
        try {
            output = tunnelSpcManager.doIt(input);
            return formRpcOutput(output);
        } catch (Exception e) {
            log.error("Failed to get SPC routes from input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);

        }
    }

    @PostMapping(value = "/restconf/operations/tunnel:allocate-tunnels", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String allocateTunnels(@RequestBody String json) {
        log.info("allocateTunnels {}");

        AllocateTunnelsInput input;
        try {
            input = formRpcInput(json, AllocateTunnelsInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        AllocateTunnelsOutput output;
        try {
            output = tunnelAllocator.doIt(input);
            return formRpcOutput(output);
        } catch (Exception e) {
            log.error("Failed to allocate tunnel from input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);

        }
    }

    @PostMapping(value = "/restconf/operations/tunnel:allocate-tunnels-2", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String allocateTunnels2(@RequestBody String json) {
        log.info("allocateTunnels {}", json);

        AllocateTunnels2Input input;
        try {
            input = formRpcInput(json, AllocateTunnels2Input.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        AllocateTunnels2Output output;
        try {
            output = hasSelectedTunnelInfo(input) ? userDefinedTunnelAllocator.doIt(input)
                    : tunnelAllocator2.doIt(input);
            return formRpcOutput(output);
        } catch (Exception e) {
            log.error("Failed to allocate tunnel from input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }
    }

    private boolean hasSelectedTunnelInfo(AllocateTunnels2Input input) {
        if (input.getVendorOccupationRate() == null) {
            return false;
        }
        return input.getVendorOccupationRate().stream()
                .anyMatch(vendor -> vendor.getSelectedTunnelInfo() != null
                        && !vendor.getSelectedTunnelInfo().isEmpty());
    }

    @PostMapping(value = "/restconf/operations/tunnel:get-free-resource", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getFreeResource(@RequestBody String json) {
        log.info("getFreeResource {}", json);

        GetFreeResourceInput input;
        try {
            input = formRpcInput(json, GetFreeResourceInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);
        }

        try {
            GetFreeResourceOutput output = freeResourceQuery.doIt(input);
            return formRpcOutput(output);
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to get free resource from input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(), e);
        }
    }

    @PostMapping(value = "/restconf/operations/tunnel:design-tunnels", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String designTunnels(@RequestBody String json, HttpServletRequest request) {
        log.info("design tunnels {}", json.substring(0, 200) + "......");

        DesignTunnelsInput inputDesign;
        try {
            inputDesign = formRpcInput(json, DesignTunnelsInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        try {
            String srcFriendlyName = siteNodeDao.getSiteFriendlyName(
                    inputDesign.getSrcSite().getValue());
            String dstFriendlyname = siteNodeDao.getSiteFriendlyName(
                    inputDesign.getDstSite().getValue());
            String msg = String.format("Design create Tunnels (%s)  %s, %s ",
                    inputDesign.getBundleNumber(), srcFriendlyName, dstFriendlyname);

            TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                    request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                    TaskInfoMessage.ResourceType.tunnel,
                    TaskInfoMessage.ActionType.design,
                    json);
            taskInfoMessage.setResourceId(msg + System.currentTimeMillis());
            taskInfoMessage.setResourceName(msg);
            taskInfoMessage.setSuccessfully(false);
            taskInfoMessage.setErrorReason("To be confirmed");

            TaskInfoMessager.sendMessage(taskInfoMessage);
            return formRpcOutput(
                    new DesignTunnelsOutputBuilder().setReturnCode(RpcResultType.Success).build());
        } catch (Exception e) {
            log.error("Failed to allocate tunnel from input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }

    }

    /**
     * this version support create normal tunnel, no reuse.
     *
     * @param json
     * @param request
     * @return
     */
    @PostMapping(value = "/restconf/operations/tunnel:create-tunnel", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String createTunnel(@RequestBody String json, HttpServletRequest request) {
        log.info("createTunnel {}");

        CreateTunnelInput input = formRpcInput(json, CreateTunnelInput.class);

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.tunnel,
                TaskInfoMessage.ActionType.create,
                json);
        TunnelCreator tunnelCreator = new TunnelCreator();
        CreateTunnelOutput output = tunnelCreator.setTaskInfo(taskInfoMessage).doIt(input);

        String result = formRpcOutput(output);
        return result;
    }

    /**
     * this version support reuse policy, include create-tunnel full funcitons
     *
     * @param json
     * @param request
     * @return
     */
    @PostMapping(value = "/restconf/operations/tunnel:create-tunnel-2", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String createTunnel2(@RequestBody String json, HttpServletRequest request) {
        log.info("create-tunnel-2");
        CreateTunnel2Input input = formRpcInput(json, CreateTunnel2Input.class);

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.tunnel,
                TaskInfoMessage.ActionType.create,
                json);

        TunnelCreator2 tunnelCreator = new TunnelCreator2();
        CreateTunnel2Output output = tunnelCreator.setTaskInfo(taskInfoMessage).doIt(input);

        String result = formRpcOutput(output);
        return result;
    }


    /**
     * this version support create tunnel over network, include whole create-tunnel, create-tunnel2
     * functions
     *
     * @param json
     * @param request
     * @return
     */
    @PostMapping(value = "/restconf/operations/tunnel:create-tunnel-3", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String createTunnel3(@RequestBody String json, HttpServletRequest request) {

        String pathInfo = request.getServletPath();

        if (!createHashCodes.add(pathInfo, json)) {
            log.info("duplicate request for create-tunnel-3");

            CreateNetworkOutput output = new CreateNetworkOutputBuilder().setReturnCode(
                    RpcResultType.Success).build();
            return formRpcOutput(output);
        }

        try {
            CreateTunnel3Input input;
            try {
                input = formRpcInput(json, CreateTunnel3Input.class);
            } catch (Exception e) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
            }

            CreateTunnel3Input inputUpdated;
            try {
                inputUpdated = tunnelAllocator2.reallocateByFrequency(input);
            } catch (CommonException ce) {
                throw ce;
            } catch (Exception e) {
                log.error("Failed to reallocateByFrequency", e);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        e.getMessage(), e);
            }

            TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                    request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                    TaskInfoMessage.ResourceType.tunnel,
                    TaskInfoMessage.ActionType.create,
                    json);
            taskInfoMessage.setRoot(false);
            taskInfoMessage.setGroupId(TASK_GROUP_ID_GENERATOR.incrementAndGet());

            TunnelCreator3 tunnelCreator = new TunnelCreator3();
            CreateTunnel3Output output = tunnelCreator.setTaskInfo(taskInfoMessage)
                    .doIt(inputUpdated);

            return formRpcOutput(output);
        } catch (CommonException e) {
            log.error("failed to create tunnel :{}", e.getMessage(), e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to create tunnel: " + ExceptionUtils.getRootCauseMessage(e), e);
        }
    }


    @PostMapping(value = "/restconf/operations/tunnel:bind-tunnel", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String bindTunnel(@RequestBody String json, HttpServletRequest request) {

        BindTunnelInput input = null;
        try {
            input = formRpcInput(json, BindTunnelInput.class);
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        }

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.tunnel,
                TaskInfoMessage.ActionType.bind,
                json);
        taskInfoMessage.setRoot(true);
        taskInfoMessage.setResourceName("Bind tunnel " + input.getTunnelId());
        taskInfoMessage.setResourceId("Bind tunnel " + System.currentTimeMillis());
        taskInfoMessage.setGroupId(TASK_GROUP_ID_GENERATOR.incrementAndGet());

        BindTunnelOutput output = tunnelBinder.doIt(input, taskInfoMessage);

        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/tunnel:batch-bind-tunnel", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String batchBindTunnel(@RequestBody String json, HttpServletRequest request) {

        BatchBindTunnelInput input = null;
        try {
            input = formRpcInput(json, BatchBindTunnelInput.class);
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        }

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.tunnel,
                TaskInfoMessage.ActionType.bind,
                json);
        taskInfoMessage.setRoot(true);
        taskInfoMessage.setResourceName("Batch bind tunnel");
        taskInfoMessage.setResourceId("Batch bind tunnel " + System.currentTimeMillis());
        taskInfoMessage.setGroupId(TASK_GROUP_ID_GENERATOR.incrementAndGet());

        BatchBindTunnelOutput output = tunnelBinder.batchDoIt(input, taskInfoMessage);

        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/tunnel:get-binding-list", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getBindingList(@RequestBody String json, HttpServletRequest request) {

        GetBindingListInput input = null;
        try {
            input = formRpcInput(json, GetBindingListInput.class);
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        }
        GetBindingListOutput output;
        try {
            output = tunnelBinder.getBindingList(input);
            return formRpcOutput(output);
        } catch (Exception e) {
            log.error("Failed to getBindingList from input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }
    }

    @PostMapping(value = "/restconf/operations/tunnel:batch-get-binding-list", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String batchGetBindingList(@RequestBody String json, HttpServletRequest request) {

        BatchGetBindingListInput input = null;
        try {
            input = formRpcInput(json, BatchGetBindingListInput.class);
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, e.getMessage());
        }
        BatchGetBindingListOutput output;
        try {
            output = tunnelBinder.batchGetBindingList(input);
            return formRpcOutput(output);
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to batchGetBindingList from input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }
    }

    @PostMapping(value = "/restconf/operations/tunnel:remove-tunnel", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String removeTunnel(@RequestBody String json, HttpServletRequest request) {
        RemoveTunnelInput input = formRpcInput(json, RemoveTunnelInput.class);

        String pathInfo = request.getServletPath();
        if (!createHashCodes.add(pathInfo, json)) {
            log.info("duplicate request for remove-tunnel");

            RemoveTunnelOutput output = new RemoveTunnelOutputBuilder().setReturnCode(
                    RpcResultType.Success).build();
            return formRpcOutput(output);
        }

        log.info("removeTunnel {}, (forceDB: {})", input.getTunnelId(), input.isForceDb());
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.tunnel,
                TaskInfoMessage.ActionType.delete,
                json);

        TunnelRemover tunnelRemover = new TunnelRemover();
        RemoveTunnelOutput output = new RemoveTunnelOutputBuilder()
                .setReturnCode(tunnelRemover.setTaskInfo(taskInfoMessage).asyncRemove(input))
                .build();

        String result = formRpcOutput(output);
        return result;
    }

    /**
     * this version support takeover tunnel to DB
     *
     * @param json
     * @param request
     * @return
     */
    @PostMapping(value = "/restconf/operations/tunnel:takeover-tunnels", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String takeoverTunnels(@RequestBody String json, HttpServletRequest request) {

        TakeoverTunnelsInput input;
        try {
            input = formRpcInput(json, TakeoverTunnelsInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        TakeoverTunnelsOutput output;
        try {
            TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                    request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                    TaskInfoMessage.ResourceType.tunnel,
                    TaskInfoMessage.ActionType.create,
                    json);
            output = tunnelTakeOver.doIt(input, taskInfoMessage);
            return formRpcOutput(output);
        } catch (Exception e) {
            log.error("Failed to takeover tunnel from input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);

        }

    }

    @PostMapping(value = "/restconf/operations/tunnel:allocate-regs", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String allocateRegs(@RequestBody String json, HttpServletRequest request) {

        AllocateRegsInput input;
        try {
            input = formRpcInput(json, AllocateRegsInput.class);
        } catch (Exception e) {
            log.error("Failed to parse json input:{}", json, e);
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, e.getMessage(), e);

        }
        AllocateRegsOutput output;
        try {
            output = regAllocator.doIt(input);
            return formRpcOutput(output);
        } catch (Exception e) {
            log.error("Failed to allocate reg from input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }

    }



    @PostMapping(value = "/repair/tpBusyState", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String repaireTpBusyState() {
        new DBRepair().tpBusyStateRepair();
        return "\ndone\n";
    }


    @PostMapping(value = "/repair/forceRemoveOch/{ochLinkId}", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String forceRemoveOch(@PathVariable String ochLinkId) {
        new DBRepair().removeOch(ochLinkId);
        return "\ndone\n";
    }

    @PostMapping(value = "/restconf/operations/tunnel:get-lport-num", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getLPortList(@RequestBody String json) {
        GetLportNumInput input;
        try {
            input = formRpcInput(json, GetLportNumInput.class);
        } catch (Exception e) {
            log.error("Failed to parse input:{}", json, e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(), e);
        }
        GetLportNumOutputBuilder outputBuilder = new GetLportNumOutputBuilder();
        try {
            NeInfo neInfo = neInfoConfig.getNeInfo(input.getVendorName(), input.getProductType(), input.getNodeType().name());
            Card card = neInfo.getCardByCardType(input.getCardType());
            List<Port> ports = card.getPorts();
            List<Port> lports = ports.stream().filter(port -> port.getPortType().equals("OTU-Line"))
                .collect(Collectors.toList());
            if (!lports.isEmpty()) {
                List<String> lportNames = lports.stream().map(Port::getName).collect(Collectors.toList());
                outputBuilder.setPortName(lportNames);
                return formRpcOutput(outputBuilder.build());
            }
            List<CrossConnection> crossConnections = card.getCrossConnections();
            List<CrossConnection> mux4x100G = crossConnections.stream()
                .filter(crossConnection -> crossConnection.getServiceType().equals(input.getServiceType()))
                .collect(Collectors.toList());
            List<String> flexLportNames = mux4x100G.stream()
                .map(x -> x.getTo().getPortFriendlyName())
                .collect(Collectors.toList());
            outputBuilder.setPortName(flexLportNames);
            return formRpcOutput(outputBuilder.build());
        } catch (NeDesignerException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(), e);
        }
    }

}
