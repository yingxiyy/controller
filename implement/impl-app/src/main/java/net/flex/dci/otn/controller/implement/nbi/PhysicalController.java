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
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.model.TaskInfoMessage.ResourceType;
import net.flex.dci.otc.serialization.util.SerializeUtil;
import net.flex.dci.otn.controller.implement.service.PhysicalService;
import net.flex.dci.otn.controller.implement.service.impl.CheckAllXcImpl;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateScanLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateScanLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.DeleteScanLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.DeleteScanLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Export1524TelemetryDataInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.GetNeDataInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.GetNeDataOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveIpInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveIpOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveNeInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveNeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SwitchNeCuActiveStandbyInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SwitchNeCuActiveStandbyOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TestConnectionStatusInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TestConnectionStatusOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateCrossConnectionInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateCrossConnectionOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateEquipInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateEquipOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateNodeInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateNodeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UploadNeHistoryPmInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UploadNeHistoryPmOutput;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 *         <p>
 *         provide otn-physical.yang api
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class PhysicalController extends BaseController {


    private final PhysicalService physicalService;


    /**
     * support change friendlyName and loginInfo
     *
     * @param json
     * @param request
     * @return
     */
    @RequestMapping(value = "/restconf/operations/otn-phy-topology:update-node", produces = "application/json;charset=UTF-8", method = RequestMethod.POST)
    @ResponseBody
    public String updateNode(@RequestBody String json, HttpServletRequest request) {
        try {
            UpdateNodeInput input = SerializeUtil.parseRpcInput(json, UpdateEquipInput.class);
            log.info("updateNode {}", input);

            TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                    request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                    TaskInfoMessage.ResourceType.device,
                    TaskInfoMessage.ActionType.updateDeviceLoginInfo, //will change based on param
                    json);

//        updatePhyNode.setTaskInfo(taskInfoMessage);
            UpdateNodeOutput output = physicalService.updateNe(input, taskInfoMessage);
            String result = formRpcOutput(output);
            return result;
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }

    /**
     * support change equip adminStatus reboot(warm/cold) custom-info
     *
     * @param json
     * @return
     */
    @PostMapping(value = "/restconf/operations/otn-phy-topology:update-equip", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updateEquip(@RequestBody String json, HttpServletRequest request) {
        UpdateEquipInput input = SerializeUtil.parseRpcInput(json, UpdateEquipInput.class);
        log.info("updateEquip {}", input);
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.device,
                TaskInfoMessage.ActionType.updateDevice, //will change based on param
                json);
//        UpdatePhyEquip impl = SpringBeanFinder.getBean(UpdatePhyEquip.class);
//        UpdateEquipOutput output = impl.doIt(input);
        UpdateEquipOutput output;
        output = physicalService.updateEquipment(input, taskInfoMessage);
        return formRpcOutput(output);
    }


    /**
     * only support change friendlyName  and provider info
     *
     * @param json
     */
    @PostMapping(value = "/restconf/operations/otn-phy-topology:update-link", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updateLink(@RequestBody String json, HttpServletRequest request) {
        UpdateLinkInput input = SerializeUtil.parseRpcInput(json, UpdateLinkInput.class);
        log.info("updateLink {}", input);

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.phyLink,
                TaskInfoMessage.ActionType.updatePhyLink, //will change based on param
                json);
        UpdateLinkOutput output = physicalService.updatePhyLink(input, taskInfoMessage);
        return formRpcOutput(output);
//        PhysicalLink impl = new PhysicalLink();
//        impl.setTaskInfo(taskInfoMessage);
//        impl.doIt(input);
    }

    @PostMapping(value = "/restconf/operations/otn-phy-topology:remove-ip", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String removeIp(@RequestBody String json, HttpServletRequest request) {
        RemoveIpInput input = SerializeUtil.parseRpcInput(json, RemoveIpInput.class);
        log.info("remove the ne Ip ,payload is {}", input);

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.device,
                TaskInfoMessage.ActionType.removeDeviceIp, //will change based on param
                json);

//        NeIplManager impl = new NeIplManager();
//        impl.setTaskInfo(taskInfoMessage);
        RemoveIpOutput output = physicalService.removeNeIp(input, taskInfoMessage);
        return formRpcOutput(output);
    }


    @PostMapping(value = "/restconf/operations/otn-phy-topology:update-cross-connection", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updateCrossConnection(@RequestBody String json, HttpServletRequest request) {
        UpdateCrossConnectionInput input = SerializeUtil.parseRpcInput(json,
                UpdateCrossConnectionInput.class);
        log.info("updateCrossConnection {}", input);

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.device,
                TaskInfoMessage.ActionType.updateDevice, //will change based on param
                json);

//        CrossConnectionManager impl = new CrossConnectionManager();
//        CrossConnectionManager.setTaskInfo(taskInfoMessage);
        UpdateCrossConnectionOutput output = physicalService.updateCrossConnections(input,
                taskInfoMessage);
        return formRpcOutput(output);
    }

    @PostMapping(value = "/restconf/operations/otn-phy-topology:get-ne-data", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String getNeData(@RequestBody String json) {
        log.info("start to get ne Detail data");
//        GetNeDataInput input = (GetNeDataInput) jsonUtil.fromJsonToDataObject(json, true);
        GetNeDataInput input = SerializeUtil.parseRpcInput(json, GetNeDataInput.class);
        log.info("getNeData {}", input);

//        GetNeData impl = SpringBeanFinder.getBean(GetNeData.class);
//        GetNeDataOutput output = impl.doIt(input);
        GetNeDataOutput output = physicalService.getNeData(input);
        return formRpcOutput(output);
    }

    @PostMapping(value = "/restconf/operations/otn-phy-topology:remove-ne", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String removeNe(@RequestBody String json, HttpServletRequest request) {
        RemoveNeInput input = SerializeUtil.parseRpcInput(json, RemoveNeInput.class);
        log.info("removeNe {}", input);

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.device,
                TaskInfoMessage.ActionType.delete,
                json);

//        RemoveNe impl = new RemoveNe();
//        impl.setTaskInfo(taskInfoMessage);
//        RemoveNeOutput output = impl.doIt(input);
        RemoveNeOutput output = physicalService.removeNe(input, taskInfoMessage);

        return formRpcOutput(output);
    }

    @PostMapping(value = "/restconf/operations/otn-phy-topology:update-termination-point", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String updateTerminationPoint(@RequestBody String json, HttpServletRequest request) {
        UpdateTerminationPointInput input = SerializeUtil.parseRpcInput(json,
                UpdateTerminationPointInput.class);
        log.info("updateTerminationPoint {}", json);

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.device,
                TaskInfoMessage.ActionType.changeTp,
                json);

        UpdateTerminationPointOutput output = physicalService.updateTerminationPoint(input,
                taskInfoMessage);

        return formRpcOutput(output);
    }

    @PostMapping(value = "/restconf/operations/otn-phy-topology:export-1524-telemetry-data", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public void export1524TelemetryData(@RequestBody String json) {
        Export1524TelemetryDataInput input = SerializeUtil.parseRpcInput(json,
                Export1524TelemetryDataInput.class);
        log.info("report 1524 telemetry data: {}", input);

        physicalService.export1524TelemetryData(input);
    }

    @PostMapping(value = "/restconf/operations/otn-phy-topology:test-connection-status", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String testConnectionStatus(@RequestBody String json) {
        TestConnectionStatusInput input = SerializeUtil.parseRpcInput(json,
                TestConnectionStatusInput.class);
        log.info("testConnectionStatus {}", input);

        TestConnectionStatusOutput output = physicalService.testNeConnectionStatus(input);

        return formRpcOutput(output);
    }


    /**
     * create physicalLink between OA/ILA card and OTDR/OCM
     *
     * @param json
     * @param request
     * @return
     */
    @PostMapping(value = "/restconf/operations/otn-phy-topology:create-scan-link", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String createLink(@RequestBody String json, HttpServletRequest request) {
        CreateScanLinkInput input = SerializeUtil.parseRpcInput(json, CreateScanLinkInput.class);
        log.info("createLink {}", input);

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.phyLink,
                TaskInfoMessage.ActionType.create,
                json);
//        ScanLink impl = new ScanLink();
//        impl.setTaskInfo(taskInfoMessage);
        CreateScanLinkOutput output = physicalService.createScanLink(input, taskInfoMessage);
        String result = formRpcOutput(output);
        return result;
    }

    /**
     * delete physicalLink between OA/ILA card and OTDR/OCM
     *
     * @param json
     * @param request
     * @return
     */
    @PostMapping(value = "/restconf/operations/otn-phy-topology:delete-scan-link", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String deleteLink(@RequestBody String json, HttpServletRequest request) {
        DeleteScanLinkInput input = SerializeUtil.parseRpcInput(json, DeleteScanLinkInput.class);
        log.info("deleteLink {}", input);

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                TaskInfoMessage.ResourceType.phyLink,
                TaskInfoMessage.ActionType.delete,
                json);
//        ScanLink impl = new ScanLink();
//        impl.setTaskInfo(taskInfoMessage);
        DeleteScanLinkOutput output = physicalService.deleteScanLink(input, taskInfoMessage);
        String result = formRpcOutput(output);
        return result;
    }


    /**
     * switch the ne cu active standby
     *
     * @param json
     * @param request
     * @return
     */
    @PostMapping(value = "/restconf/operations/otn-phy-topology:switch-ne-cu-active-standby", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String switchNeCu(@RequestBody String json, HttpServletRequest request) {
        SwitchNeCuActiveStandbyInput input = SerializeUtil.parseRpcInput(json,
                SwitchNeCuActiveStandbyInput.class);
        log.info("switch ne cu active standby the {}", input);

        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                ResourceType.device,
                ActionType.updateDevice,
                json);
        SwitchNeCuActiveStandbyOutput output = physicalService.switchNeCuActiveStandby(input,
                taskInfoMessage);
        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/restconf/operations/otn-phy-topology:upload-ne-history-pm", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String uploadNeHistoryPm(@RequestBody String requestBody, HttpServletRequest request) {
        UploadNeHistoryPmInput input = SerializeUtil.parseRpcInput(requestBody,
                UploadNeHistoryPmInput.class);
        log.info("upload ne history pm the input is:{}", requestBody);
        TaskInfoMessage taskInfoMessage = new TaskInfoMessage(
                request.getHeader(AuthConstant.USER_TOKEN_HEADER),
                ResourceType.device,
                ActionType.uploadHistoricalPerformance,
                requestBody);
        UploadNeHistoryPmOutput output = physicalService.uploadNeHistoryPm(input, taskInfoMessage);
        String result = formRpcOutput(output);
        return result;
    }

    @PostMapping(value = "/checkAll", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String checkAllXc() {
        CheckAllXcImpl checkXcs = new CheckAllXcImpl();
        checkXcs.start();
        return "check all xc done";
    }
}