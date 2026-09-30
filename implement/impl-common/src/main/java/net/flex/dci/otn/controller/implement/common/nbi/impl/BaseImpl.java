/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.common.nbi.impl;

import static net.flex.dci.otn.controller.implement.common.utils.Constants.FAILED;

import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otn.controller.implement.common.dto.ContactNeResult;
import net.flex.dci.otn.controller.implement.common.enums.SetResultCode;
import net.flex.dci.otn.controller.implement.common.utils.CommonUtils;
import net.flex.dci.otn.controller.implement.common.utils.Constants;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateScanLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateScanLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CreateScanLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.DeleteScanLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.DeleteScanLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.DeleteScanLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Export1524TelemetryDataInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.GetNeDataInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.GetNeDataOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.GetNeDataOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveIpInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveIpOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveIpOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveNeInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveNeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveNeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SwitchNeCuActiveStandbyInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SwitchNeCuActiveStandbyOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SwitchNeCuActiveStandbyOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TestConnectionStatusInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TestConnectionStatusOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TestConnectionStatusOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateCrossConnectionInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateCrossConnectionOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateCrossConnectionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateEquipInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateEquipOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateEquipOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateNodeInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateNodeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateNodeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UploadNeHistoryPmInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UploadNeHistoryPmOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UploadNeHistoryPmOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelInput;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 2021/9/6 11:06
 */
@Slf4j
public abstract class BaseImpl {

    protected TaskInfoMessage taskInfoMessage;


    @Autowired
    protected PhyNodeDao phyNodeDao;

    @Autowired
    protected PhyLinkDao phyLinkDao;

    @Autowired
    protected OchLinkDao ochLinkDao;

    @Autowired
    protected SiteLinkDao siteLinkDao;
    @Autowired
    protected MultipleTransaction multipleTransaction;
    @Autowired
    protected NeManagerRpc neManagerRpc;

    @Autowired
    protected AdapterDao adapterDao;


    protected void logMessage(String title, String resourceName, ContactNeResult configResult) {
        taskInfoMessage.setResourceName(resourceName);

        String msg = String.format("%s %s ", title, resourceName);
        StringBuilder msgBuilder = new StringBuilder(msg);
        boolean isOk = false;

        if (configResult.getCode() == SetResultCode.SUCCESS) {
            //创建成功
            isOk = true;
            msgBuilder.append(Constants.SUCCESSFULLY);
        } else {
            isOk = false;
            msgBuilder.append(FAILED)
                    .append(Constants.REASON_IS)
                    .append(configResult.getMessage());

        }
        if (taskInfoMessage != null) {
            taskInfoMessage.setEndTime(System.currentTimeMillis());
            String requestBody = taskInfoMessage.getDetail();
            String errorMessage = configResult.getMessage();
            String detailBody = CommonUtils.buildRequestDetail(requestBody, errorMessage);
            taskInfoMessage.setDetail(detailBody);
            if (taskInfoMessage != null) {
                if (isOk) {
                    taskInfoMessage.setSuccessfully(isOk);
                } else {
                    taskInfoMessage.setSuccessfully(isOk);
                    taskInfoMessage.setErrorReason(FAILED);
                }
                TaskInfoMessager.sendMessage(taskInfoMessage);
            }
        }

        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title(title)
                        .message(msgBuilder.toString())
                        .error(!isOk)
                        .build());
    }


    protected void logMessage(String title, String resourceName, String errorMessage) {

        taskInfoMessage.setResourceName(resourceName);

        String msg = String.format("%s %s ", title, resourceName);
        StringBuilder msgBuilder = new StringBuilder(msg);
        boolean isOk = false;
        if (!StringUtils.hasText(errorMessage)) {
            //创建成功
            isOk = true;
            msgBuilder.append(Constants.SUCCESSFULLY);
        } else {
            isOk = false;
            msgBuilder.append(FAILED)
                    .append(Constants.REASON_IS)
                    .append(errorMessage);

        }
        if (taskInfoMessage != null) {
            taskInfoMessage.setEndTime(System.currentTimeMillis());
            String requestBody = taskInfoMessage.getDetail();
            String detailBody = CommonUtils.buildRequestDetail(requestBody, errorMessage);
            taskInfoMessage.setDetail(detailBody);
            if (taskInfoMessage != null) {
                if (isOk) {
                    taskInfoMessage.setSuccessfully(isOk);
                } else {
                    taskInfoMessage.setSuccessfully(isOk);
                    taskInfoMessage.setErrorReason(FAILED);
                }
                TaskInfoMessager.sendMessage(taskInfoMessage);
            }
        }

        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title(title)
                        .message(msgBuilder.toString())
                        .error(!isOk)
                        .build());
    }


    public void setTaskInfo(TaskInfoMessage taskInfoMessage) {
        this.taskInfoMessage = taskInfoMessage;
    }

    public UpdateNodeOutput doIt(UpdateNodeInput input) {
        return new UpdateNodeOutputBuilder().build();
    }

    public UpdateEquipOutput doIt(UpdateEquipInput input) {
        return new UpdateEquipOutputBuilder().build();
    }

    public UpdateLinkOutput updatePhyLink(UpdateLinkInput input) {
        return new UpdateLinkOutputBuilder().setReturnCode(RpcResultType.Success).build();
    }

    public RemoveIpOutput doIt(RemoveIpInput input) {
        return new RemoveIpOutputBuilder().build();
    }

    public UpdateCrossConnectionOutput doIt(UpdateCrossConnectionInput input) {
        return new UpdateCrossConnectionOutputBuilder().build();
    }

    public UpdateTerminationPointOutput doIt(UpdateTerminationPointInput input) {
        return new UpdateTerminationPointOutputBuilder().build();
    }

    public RemoveNeOutput doIt(RemoveNeInput input) {
        return new RemoveNeOutputBuilder().build();
    }

    public UpdateLinkOutput updateLink(
            String input, HttpServletRequest request) {
        return new UpdateLinkOutputBuilder().build();
    }

    public UpdateEquipOutput updatePhyEquipment(UpdateEquipInput input) {
        UpdateEquipOutputBuilder outputBuilder = new UpdateEquipOutputBuilder();
        outputBuilder.setReturnCode(RpcResultType.Success);
        return outputBuilder.build();
    }

    public UpdateTerminationPointOutput updateTerminationPoint(UpdateTerminationPointInput input) {
        UpdateTerminationPointOutputBuilder outBuilder = new UpdateTerminationPointOutputBuilder();
        outBuilder.setReturnCode(RpcResultType.Success);
        return outBuilder.build();
    }

    public RpcResultType doIt(UpdateTunnelInput input) {
        return RpcResultType.Success;
    }

    public RpcResultType updateTunnelAttribute(UpdateTunnelInput input,
            TaskInfoMessage taskInfoMessage) {
        return RpcResultType.Success;
    }


    public CreateScanLinkOutput doIt(CreateScanLinkInput input) {
        CreateScanLinkOutputBuilder builder = new CreateScanLinkOutputBuilder();
        builder.setReturnCode(RpcResultType.Success);
        return builder.build();
    }

    public RemoveNeOutput removeNe(RemoveNeInput input) throws CommonException {
        RemoveNeOutputBuilder removeNeOutputBuilder = new RemoveNeOutputBuilder();
        removeNeOutputBuilder.setReturnCode(RpcResultType.Success);
        return removeNeOutputBuilder.build();
    }

    public UpdateNodeOutput updateNe(UpdateNodeInput input, TaskInfoMessage taskInfoMessage)
            throws CommonException {
        UpdateNodeOutputBuilder outputBuilder = new UpdateNodeOutputBuilder().setReturnCode(
                RpcResultType.Success);
        return outputBuilder.build();
    }

    public GetNeDataOutput getNeData(GetNeDataInput input) throws CommonException {
        return new GetNeDataOutputBuilder().build();
    }


    public TestConnectionStatusOutput testNeConnectionStatus(TestConnectionStatusInput input)
            throws CommonException {
        return new TestConnectionStatusOutputBuilder().setReturnCode(RpcResultType.Success).build();
    }


    public void export1524TelemetryData(Export1524TelemetryDataInput input) throws CommonException {

    }


    public RemoveIpOutput removeNeIp(RemoveIpInput input) throws CommonException {
        return new RemoveIpOutputBuilder()
                .setReturnCode(RpcResultType.Success)
                .build();
    }

    public UpdateEquipOutput updateEquipPhysical(UpdateEquipInput input) {
        return new UpdateEquipOutputBuilder().build();
    }

    public CreateScanLinkOutput createLink(CreateScanLinkInput input) {
        CreateScanLinkOutputBuilder builder = new CreateScanLinkOutputBuilder();
        builder.setReturnCode(RpcResultType.Success);
        return builder.build();
    }

    public DeleteScanLinkOutput deleteLink(DeleteScanLinkInput input) {
        DeleteScanLinkOutputBuilder builder = new DeleteScanLinkOutputBuilder();
        builder.setReturnCode(RpcResultType.Success);
        return builder.build();
    }

    public UpdateCrossConnectionOutput updateCrossConnections(UpdateCrossConnectionInput input,
            TaskInfoMessage taskInfoMessage) {
        return new UpdateCrossConnectionOutputBuilder().setReturnCode(RpcResultType.Success)
                .build();
    }

    public SwitchNeCuActiveStandbyOutput switchNeCu(SwitchNeCuActiveStandbyInput input) {
        return new SwitchNeCuActiveStandbyOutputBuilder().setReturnCode(RpcResultType.Success)
                .build();
    }

    public UploadNeHistoryPmOutput uploadHistoryPm(UploadNeHistoryPmInput input,
            TaskInfoMessage taskInfoMessage) {
        return new UploadNeHistoryPmOutputBuilder().setReturnCode(RpcResultType.Success).build();
    }
}
