/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.rpcs.impl;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.rpc.client.constants.RpcCommand.NeManagerRpcCmd;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import org.asynchttpclient.Response;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.BatchConfigNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.BatchConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ChannelAseRestoreInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ChannelAseRestoreInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ChannelAseRestoreOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ClearNeApsSwitchLogsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ClearNeApsSwitchLogsInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ClearNeApsSwitchLogsOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigureNorthboundTelemetryOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchInput.Action;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.MergeDataInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.MergeDataInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeOperationLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeOperationLinkInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeOperationLinkOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ReassignNtpServerOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RegisteNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RegisteNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RemoveResourceInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RemoveResourceInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RemoveResourceOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.Report1524TelemetryDataInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.Report1524TelemetryDataOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.SwitchCuActiveStandbyInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.SwitchCuActiveStandbyInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.SwitchCuActiveStandbyOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UnregisteNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UnregisteNeInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UnregisteNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UploadHistoryPmInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UploadHistoryPmInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UploadHistoryPmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UploadNeInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UploadNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.clear.ne.aps._switch.logs.input.TargetDeviceBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.upload.history.pm.input.RemoteServerBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.GetNeDataInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.GetNeDataOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ApsPathType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OPERATIONITEM;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OtsOperationType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SftpServerInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/8/25 11:18
 */
@Component
@Slf4j
public class NeManagerRpcImpl extends BasicRpc implements NeManagerRpc {


    public NeManagerRpcImpl() {
        NAMESPACE = "eml-manager";
        MODULE_NAME = "neMgr";
    }


    /**
     * rpc manage-ne
     *
     * @throws Exception
     */
    @Override
    public void manageNe() throws CommonException {
        log.debug("start to manage the ne");
        String requestOp = NeManagerRpcCmd.MANAGE_NE;
        try {
            String result = executeRequest(requestOp, null);
            ManageNeOutput output = formRpcOutPut(requestOp, result,
                    ManageNeOutput.class);
            if (output.getReturnCode() != RpcResultType.Success) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "Failed to manager ne");
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Failed to manager ne ,the reason is " + ex.getMessage(), ex);
        }
    }

    @Override
    public ReassignNtpServerOutput reAssignNtpServer() throws CommonException {
        log.info("start to reassign ntp server");
        String requestOp = NeManagerRpcCmd.REASSIGN_NTP_SERVER;
        try {
            String result = executeRequest(requestOp, null);
            ReassignNtpServerOutput output = formRpcOutPut(requestOp, result,
                    ReassignNtpServerOutput.class);
            if (output.getReturnCode() != RpcResultType.Success) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "Failed to reassign ntp server");
            }
            return output;
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Failed to reassign ne,the reason is " + e.getMessage(), e);
        }

    }

    @Override
    public ConfigureNorthboundTelemetryOutput configureNorthboundTelemetry()
            throws CommonException {
        log.info("start to configure northbound telemetry");
        String requestOp = NeManagerRpcCmd.CONFIG_NORTHBOUND_TELEMETRY;
        try {
            String result = executeRequest(requestOp, null);
            ConfigureNorthboundTelemetryOutput output = formRpcOutPut(requestOp, result,
                    ConfigureNorthboundTelemetryOutput.class);
            if (output.getReturnCode() != RpcResultType.Success) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "Failed to configure northbound telemetry");
            }
            return output;
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Failed to configure northbound telemetry,the reason is " + e.getMessage(), e);
        }

    }

    /**
     * rpc registered ne
     *
     * @param input
     * @return
     */
    @Override
    public RegisteNeOutput registeredNe(RegisteNeInput input) throws CommonException {
        log.debug("start to registered ne");
        try {
            String requestOp = NeManagerRpcCmd.REGISTER_NE;
            String requestBody = formRpcInput(requestOp, input);
            String result = executeRequest(requestOp, requestBody);
            RegisteNeOutput registeNeOutput = formRpcOutPut(requestOp, result,
                    RegisteNeOutput.class);
            if (registeNeOutput.getReturnCode() != RpcResultType.Success) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        String.format("Failed to register ne %s",
                                input.getNodeId().getValue()));
            }
            return registeNeOutput;
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("Failed to register ne %s",
                            ex.getMessage()), ex);
        }
    }

    /**
     * rpc config-ne
     *
     * @param input
     * @return
     */
    @Override
    public ConfigNeOutput configNe(ConfigNeInput input) throws CommonException {
        log.debug("start to config ne the input is {}", input);
        try {
            String requestOp = NeManagerRpcCmd.CONFIG_NE;
            String requestBody = formRpcInput(requestOp, input);
            Response response = executeReq(requestOp, requestBody);
            String result = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                ConfigNeOutput configNeOutput = formRpcOutPut(requestOp, result,
                        ConfigNeOutput.class);
                return configNeOutput;
            } else {
                String errorMsg = getErrorDetail(result);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }

        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public BatchConfigNeOutput batchConfigNe(BatchConfigNeInput input) throws CommonException {
        log.debug("start to batch config ne the input is {}", input);
        try {
            String requestOp = NeManagerRpcCmd.BATCH_CONFIG_NE;
            String requestBody = formRpcInput(requestOp, input);
            Response response = executeReq(requestOp, requestBody);
            String result = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                BatchConfigNeOutput batchConfigNeOutput = formRpcOutPut(requestOp, result,
                        BatchConfigNeOutput.class);
                return batchConfigNeOutput;
            } else {
                String errorMsg = getErrorDetail(result);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }

        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }


    @Override
    public void mergeNe(NodeId nodeId) throws CommonException {
        mergeNe(new MergeDataInputBuilder().setNodeId(nodeId).build());
    }

    private void mergeNe(MergeDataInput input) throws CommonException {
        log.debug("start to merge ne the input is {}", input);
        try {
            String requestOp = NeManagerRpcCmd.MERGE_DATA;
            String requestBody = formRpcInput(requestOp, input);
            Response response = executeReq(requestOp, requestBody);
            if (response.getStatusCode() != HttpStatus.OK.value()) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "ask to merge data error");
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public ConfigNeOutput configNe(Node node) throws CommonException {
        log.debug("start to config ne the node is {}", node);
        List<TerminationPoint> confTps = new ArrayList<TerminationPoint>();
        if (node.getTerminationPoint() != null && !node.getTerminationPoint().isEmpty()) {
            for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint tp : node
                    .getTerminationPoint()) {
                TerminationPoint confTp = new TerminationPointBuilder()
                        .setKey(new TerminationPointKey(
                                new TpId(tp.getTpId())))
                        .setPhysical(tp.getAugmentation(TerminationPoint1.class).getPhysical())
                        .setTpId(new TpId(tp.getTpId())).build();

                confTps.add(confTp);
            }
        }
        ConfigNeInput input = new ConfigNeInputBuilder()
                .setNodeId(node.getNodeId())
                .setPhysical(node.getAugmentation(Node1.class).getPhysical())
                .setTerminationPoint(confTps)
                .build();
        return this.configNe(input);
    }

    @Override
    public void uploadNe(String neId) throws CommonException {
        log.debug("start to upload ne ");
        try {
            String requestOp = NeManagerRpcCmd.UPLOAD_NE;
            String requestBody = formRpcInput(requestOp,
                    new UploadNeInputBuilder().setNodeId(new NodeId(neId)).build());
            String result = executeRequest(requestOp, requestBody);
            UploadNeOutput output = formRpcOutPut(requestOp, result,
                    UploadNeOutput.class);
            if (output.getReturnCode() != RpcResultType.Success) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        String.format("Failed to upload ne %s",
                                neId));
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public GetNeDataOutput getNeData(GetNeDataInput emlInput) throws CommonException {
        log.debug("start to get ne data");
        try {
            String requestOp = NeManagerRpcCmd.GET_NE_DATA;
            String requestBody = formRpcInput(requestOp, emlInput);
            String responseBody = executeRequest(requestOp, requestBody);
            GetNeDataOutput output = formRpcOutPut(requestOp, responseBody,
                    GetNeDataOutput.class);
            return output;
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public UnregisteNeOutput unregisteredNe(NodeId nodeId) throws CommonException {
        log.debug("start to unregister ne data :{}", nodeId);
        String requestOp = NeManagerRpcCmd.UNREGISTER_NE;
        try {
            UnregisteNeInput input = new UnregisteNeInputBuilder().setNodeId(nodeId).setForce(true)
                    .build();
            String requestBody = formRpcInput(requestOp, input);
            String responseBody = executeRequest(requestOp, requestBody);
            UnregisteNeOutput output = formRpcOutPut(requestOp, responseBody,
                    UnregisteNeOutput.class);
            return output;
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public RemoveResourceOutput removeResource(Node node) throws CommonException {
        log.debug("start to remove the resource for node id:{}", node.getNodeId().getValue());
        log.debug("remove resource ne body:{}", node);
        String requestOp = NeManagerRpcCmd.REMOVE_RESOURCE;
        try {

            List<TerminationPoint> confTps = new ArrayList<TerminationPoint>();

            if (node.getTerminationPoint() != null && !node.getTerminationPoint().isEmpty()) {
                for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint tp : node
                        .getTerminationPoint()) {
                    TerminationPoint confTp = new TerminationPointBuilder()
                            .setKey(new TerminationPointKey(
                                    new TpId(tp.getTpId())))
                            .setPhysical(tp.getAugmentation(TerminationPoint1.class).getPhysical())
                            .setTpId(new TpId(tp.getTpId())).build();

                    confTps.add(confTp);
                }
            }

            RemoveResourceInput removeResourceInput = new RemoveResourceInputBuilder()

                    .setNodeId(node.getNodeId())
                    .setPhysical(node.getAugmentation(Node1.class).getPhysical())
                    .setTerminationPoint(confTps)
                    .build();
            String requestBody = formRpcInput(requestOp, removeResourceInput);
            String responseBody = executeRequest(requestOp, requestBody);
            RemoveResourceOutput output = formRpcOutPut(requestOp,
                    responseBody, RemoveResourceOutput.class);
            return output;

        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public NeDatabaseOperateOutput neDatabaseOperate(NeDatabaseOperateInput input)
            throws CommonException {
        log.debug("start to ne database operate ne id is:{}", input.getNodeId());
        String requestOp = NeManagerRpcCmd.NE_DATABASE_OPERATE;
        try {
            String requestBody = formRpcInput(requestOp, input);
            Response response = executeReq(requestOp, requestBody);
            String rspBody = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                NeDatabaseOperateOutput output = formRpcOutPut(
                        requestOp, rspBody, NeDatabaseOperateOutput.class);
                return output;
            } else {
                String errorMsg = getErrorDetail(rspBody);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public NeSoftwareOperateOutput neSoftwareOperate(NeSoftwareOperateInput input)
            throws CommonException {
        log.debug("start to ne software operate ne id is:{}", input.getNodeId());
        String requestOp = NeManagerRpcCmd.NE_SOFTWARE_OPERATE;
        try {
            String requestBody = formRpcInput(requestOp, input);
            Response response = executeReq(requestOp, requestBody);
            String rspBody = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                NeSoftwareOperateOutput output = formRpcOutPut(
                        requestOp, rspBody, NeSoftwareOperateOutput.class);
                return output;
            } else {
                String errorMsg = getErrorDetail(rspBody);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public ClearNeApsSwitchLogsOutput clearNeApsSwitchLogs(List<String> neIds)
            throws CommonException {
        log.debug("start to clear ne aps switch logs,neIds :{}", neIds);
        String requestOp = NeManagerRpcCmd.CLEAR_NE_SWITCH_LOGS;
        ClearNeApsSwitchLogsInput input = new ClearNeApsSwitchLogsInputBuilder().setTargetDevice(
                neIds.stream().map(neId -> new TargetDeviceBuilder().setDeviceId(neId).build())
                        .collect(
                                Collectors.toList())).build();
        try {
            String requestBody = formRpcInput(requestOp, input);
            Response response = executeReq(requestOp, requestBody);
            String rspBody = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                ClearNeApsSwitchLogsOutput output = formRpcOutPut(
                        requestOp, rspBody, ClearNeApsSwitchLogsOutput.class);
                return output;
            } else {
                String errorMsg = getErrorDetail(rspBody);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public ManageApsSwitchOutput manageApsSwitch(String neId, String apsName,
            ApsPathType path, Short index, Action action) throws CommonException {
        log.debug("start to manage ne aps switch neId:{} aps name:{} path:{} index:{} action:{}",
                neId, apsName, path, index, action);
        String requestOp = NeManagerRpcCmd.MANAGE_NE_APS_SWITCH;
        ManageApsSwitchInput input = new ManageApsSwitchInputBuilder().setAction(action)
                .setIndex(index).setPath(path).setName(apsName).setNeId(
                        NodeId.getDefaultInstance(neId)).build();
        try {
            String requestBody = formRpcInput(requestOp, input);
            Response response = executeReq(requestOp, requestBody);
            String rspBody = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                ManageApsSwitchOutput output = formRpcOutPut(
                        requestOp, rspBody, ManageApsSwitchOutput.class);
                return output;
            } else {
                String errorMsg = getErrorDetail(rspBody);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public NeOperationLinkOutput neOperationLink(String neId, String tpId,
            OtsOperationType operationType, Class<? extends OPERATIONITEM> operationItem) {
        log.debug("start to ne operation link ne:{} tpId:{} operationType:{} operationItem:{}",
                neId, tpId, operationType, operationItem);
        String cmd = NeManagerRpcCmd.NE_OPERATION_LINK;
        try {
            NeOperationLinkInput input = buildNeOperationLinkRpcInput(neId, tpId, operationType,
                    operationItem);
            String requestBody = formRpcInput(cmd, input);
            Response response = executeReq(cmd, requestBody);
            String rspBody = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                NeOperationLinkOutput output = formRpcOutPut(
                        cmd, rspBody, NeOperationLinkOutput.class);
                return output;
            } else {
                String errorMsg = getErrorDetail(rspBody);
                log.error("failed to execute ne operation link:{}", errorMsg);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }
        } catch (Exception ex) {
            log.error("failed to execute ne operation link:{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public SwitchCuActiveStandbyOutput switchCuActiveStandby(String neId, String cuId) {
        log.debug("switch ne:{} cu:{} standby", neId, cuId);
        String cmd = NeManagerRpcCmd.SWITCH_CU_ACTIVE_STANDBY;
        try {
            SwitchCuActiveStandbyInput input = buildSwitchCuActiveStandbyInput(neId, cuId);
            String requestBody = formRpcInput(cmd, input);
            Response response = executeReq(cmd, requestBody);
            String rspBody = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                SwitchCuActiveStandbyOutput output = formRpcOutPut(
                        cmd, rspBody, SwitchCuActiveStandbyOutput.class);
                return output;
            } else {
                String errorMsg = getErrorDetail(rspBody);

                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }
        } catch (Exception ex) {
            log.error("failed to execute ne cu switch active standby:{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public UploadHistoryPmOutput uploadHistoryPmFromNe(String neId, Long startTime, Long endTime,
            Long interval, SftpServerInfo sftpServerInfo) {
        log.debug(
                "upload history pm from ne:{} interval :{} from time:{} to time:{},sftpServer address is:{}",
                neId, interval, startTime, endTime, sftpServerInfo.getAddress());
        String cmd = NeManagerRpcCmd.UPLOAD_HISTORY_PM;
        try {
            UploadHistoryPmInput input = buildUploadHistoryPmInput(neId, startTime, endTime,
                    interval, sftpServerInfo);
            String requestBody = formRpcInput(cmd, input);
            Response response = executeReq(cmd, requestBody);
            String rspBody = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                UploadHistoryPmOutput output = formRpcOutPut(
                        cmd, rspBody, UploadHistoryPmOutput.class);
                return output;
            } else {
                String errorMsg = getErrorDetail(rspBody);

                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }
        } catch (Exception ex) {
            log.error("failed to execute upload ne history pm from ne:{}({})", neId,
                    ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public ChannelAseRestoreOutput channelAseRestore(String neId, String crossConnectionId) {
        log.debug(
                "channel ase restore for ne:{} channelIndex cross connection:{} ",
                neId, crossConnectionId);
        String cmd = NeManagerRpcCmd.CHANNEL_ASE_RESTORE;
        try {
            ChannelAseRestoreInput input = buildChannelAseRestoreInput(neId, crossConnectionId);
            String requestBody = formRpcInput(cmd, input);
            Response response = executeReq(cmd, requestBody);
            String rspBody = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                ChannelAseRestoreOutput output = formRpcOutPut(
                        cmd, rspBody, ChannelAseRestoreOutput.class);
                return output;
            } else {
                String errorMsg = getErrorDetail(rspBody);

                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }
        } catch (Exception ex) {
            log.error("failed to execute channel ase restore for ne:{}({})", neId,
                    ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    private ChannelAseRestoreInput buildChannelAseRestoreInput(String neId,
            String crossConnectionId) {
        return new ChannelAseRestoreInputBuilder().setNodeId(NodeId.getDefaultInstance(neId))
                .setCrossConnectionId(crossConnectionId).build();
    }

    private UploadHistoryPmInput buildUploadHistoryPmInput(String neId, Long startTime,
            Long endTime, Long interval, SftpServerInfo sftpServerInfo) {
        UploadHistoryPmInputBuilder uploadHistoryPmInputBuilder = new UploadHistoryPmInputBuilder();
        uploadHistoryPmInputBuilder.setNodeId(NodeId.getDefaultInstance(neId));
        uploadHistoryPmInputBuilder.setStartTimestamp(BigInteger.valueOf(startTime));
        uploadHistoryPmInputBuilder.setEndTimestamp(BigInteger.valueOf(endTime));
        uploadHistoryPmInputBuilder.setInterval(BigInteger.valueOf(interval));
        RemoteServerBuilder remoteServerBuilder = new RemoteServerBuilder(sftpServerInfo);
        uploadHistoryPmInputBuilder.setRemoteServer(remoteServerBuilder.build());
        return uploadHistoryPmInputBuilder.build();
    }

    private SwitchCuActiveStandbyInput buildSwitchCuActiveStandbyInput(String neId, String cuId) {
        SwitchCuActiveStandbyInputBuilder switchCuActiveStandbyInputBuilder = new SwitchCuActiveStandbyInputBuilder();
        switchCuActiveStandbyInputBuilder.setTargetCu(cuId);
        switchCuActiveStandbyInputBuilder.setNodeId(NodeId.getDefaultInstance(neId));
        return switchCuActiveStandbyInputBuilder.build();
    }

    private NeOperationLinkInput buildNeOperationLinkRpcInput(String neId, String tpId,
            OtsOperationType operationType, Class<? extends OPERATIONITEM> operationItem) {
        NeOperationLinkInputBuilder neOperationLinkInputBuilder = new NeOperationLinkInputBuilder();
        neOperationLinkInputBuilder.setNodeId(NodeId.getDefaultInstance(neId));
        neOperationLinkInputBuilder.setTpId(TpId.getDefaultInstance(tpId));
        neOperationLinkInputBuilder.setOperation(operationType);
        neOperationLinkInputBuilder.setOperationItem(operationItem);
        return neOperationLinkInputBuilder.build();
    }

    public Report1524TelemetryDataOutput report1524TelemetryData(
            Report1524TelemetryDataInput input) throws CommonException {
        log.debug("start to report1524TelemetryData ne data :{}, type : {}", input.getNodeRef(),
                input.getType());
        String requestOp = NeManagerRpcCmd.REPORT_1524_TELEMETRY_DATA;
        try {
            String requestBody = formRpcInput(requestOp, input);
            Response response = executeReq(requestOp, requestBody);
            String rspBody = response.getResponseBody();
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.Report1524TelemetryDataOutput output = formRpcOutPut(
                        requestOp, rspBody, Report1524TelemetryDataOutput.class);
                return output;
            } else {
                String errorMsg = getErrorDetail(rspBody);
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errorMsg);
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }
}
