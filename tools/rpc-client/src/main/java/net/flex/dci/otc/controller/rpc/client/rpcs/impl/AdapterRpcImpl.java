/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.rpcs.impl;

import static net.flex.dci.otc.controller.rpc.client.utils.RpcConstants.COLON;
import static net.flex.dci.otc.controller.rpc.client.utils.RpcConstants.COMMON_CONNECTION_TIMEOUT;
import static net.flex.dci.otc.controller.rpc.client.utils.RpcConstants.COMMON_READ_TIMEOUT;
import static net.flex.dci.otc.controller.rpc.client.utils.RpcConstants.EXECUTE_READ_TIMEOUT;
import static net.flex.dci.otc.controller.rpc.client.utils.RpcConstants.HTTP_PREFIX;
import static net.flex.dci.otc.controller.rpc.client.utils.RpcConstants.POUND_SIGN;
import static net.flex.dci.otc.controller.rpc.client.utils.RpcConstants.URL_POUND;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.data.YangDataUtil;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.rpc.client.constants.RpcCommand.EmlRpcCmd;
import net.flex.dci.otc.controller.rpc.client.dto.ExecuteNetConfResp;
import net.flex.dci.otc.controller.rpc.client.enums.ExecuteStatus;
import net.flex.dci.otc.controller.rpc.client.httpclient.OdlRpcClientConfig;
import net.flex.dci.otc.controller.rpc.client.httpclient.OdlRpcClientConfig.Builder;
import net.flex.dci.otc.controller.rpc.client.rpcs.AdapterRpc;
import net.flex.dci.otc.controller.rpc.client.utils.RpcConstants;
import org.asynchttpclient.Response;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ne.database.operate.input.SftpServer;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ApsSwitchInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ApsSwitchInput.Action;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ApsSwitchInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ApsSwitchOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.BatchConfigNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.BatchConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ChannelAseRestoreInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ChannelAseRestoreInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ChannelAseRestoreOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ClearApsSwitchLogInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ClearApsSwitchLogInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ClearApsSwitchLogOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.CompareNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.CompareNeInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.CompareNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConfigNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConnectNeInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConnectNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ExecuteXmlInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ExecuteXmlInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ExecuteXmlOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.GetManagedNesOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.GetNeDataInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.GetNeDataInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.GetNeDataOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.MergeDataInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.MergeDataInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeDatabaseOperateInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeOperationLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeOperationLinkInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeOperationLinkOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeSoftwareOperateInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RefreshAlarmInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RefreshAlarmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RefreshDeviceAlarmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveNeInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveResourceInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveResourceOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.Report1524TelemetryDataInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.Report1524TelemetryDataOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.SwitchCuActiveStandbyInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.SwitchCuActiveStandbyInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.SwitchCuActiveStandbyOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.SyncNeDataInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.SyncNeDataOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.TestNeConnectionInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.TestNeConnectionInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.TestNeConnectionOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.UploadHistoryPmInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.UploadHistoryPmInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.UploadHistoryPmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ne.database.operate.input.SftpServerBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.nes.top.nes.Ne;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPointKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.upload.history.pm.input.RemoteServerBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ApsPathType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OPERATIONITEM;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OtsOperationType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SftpServerInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TestConnectionStatusInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/8/24 16:34
 */
@Component
@Slf4j
public class AdapterRpcImpl extends BasicRpc implements AdapterRpc {


    public AdapterRpcImpl() {
        this.NAMESPACE = "eml";
    }

    @Override
    public ExecuteNetConfResp executeNetConfCmd(Adapter adapter, String neId, String payload) {
        log.info("execute NetConf command to:{} ", neId);
        ExecuteXmlInput executeXmlInput = buildExecuteXmlInput(neId, payload);
        String cmd = EmlRpcCmd.EXECUTE_XML;
        try {
            String body = formRpcInput(cmd, executeXmlInput);
            Response response = odlRpcClient.setConfig(
                            new Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd())
                                    .build())
                    .postReq(getManagerUrl(adapter, cmd), body, EXECUTE_READ_TIMEOUT);
            if (response.getStatusCode() == HttpStatus.OK.value()) {
                String responseBody = response.getResponseBody();
                ExecuteXmlOutput executeXmlOutput = formRpcOutPut(cmd, responseBody,
                        ExecuteXmlOutput.class);
                return ExecuteNetConfResp.builder().status(ExecuteStatus.SUCCESS)
                        .resp(executeXmlOutput.getResponse()).message("SUCCESS").build();
            } else {
                log.warn("Adapter {} execute netConf command to {} failed", adapter.getName(),
                        neId);
                return ExecuteNetConfResp.builder().status(ExecuteStatus.FAILED)
                        .resp(response.getResponseBody())
                        .message("failed to execute the NetConf command").build();
            }
        } catch (Exception exception) {
            log.error("Failed to execute the NetConf cmd adapter {}", adapter, exception);
            return ExecuteNetConfResp.builder().status(ExecuteStatus.FAILED)
                    .message(exception.getMessage()).build();
        }

    }


    @Override
    public List<NodeId> getManagedNes(Adapter adapter) {
        log.info("get manage nes ");
        try {
            String requestOp = EmlRpcCmd.GET_MANAGED_NES;
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd())
                                    .connectionTimeout(COMMON_CONNECTION_TIMEOUT)
                                    .readTimeout(COMMON_READ_TIMEOUT)
                                    .build())
                    .get(getManagerUrl(adapter, requestOp));
            log.info("result output is:{}", result);

            GetManagedNesOutput output = formRpcOutPut(requestOp, result,
                    GetManagedNesOutput.class);
            return output.getNodeId();
        } catch (Exception e) {
            log.error("Failed to get managed ne for adapter {}", adapter, e);
            return new ArrayList<>();
        }
    }

    @Override
    public RemoveNeOutput unregisterNe(Adapter adapter, String neId, boolean isForce)
            throws ExecutionException, InterruptedException {
        log.info("start to unregistered ne {}", neId);
        RemoveNeInputBuilder builder = new RemoveNeInputBuilder();
        builder.setNodeId(new NodeId(neId));
        builder.setForce(isForce);
        String requestOp = EmlRpcCmd.REMOVE_NE;
        String body = formRpcInput(requestOp, builder.build());

        String result = odlRpcClient.setConfig(
                        new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                .password(adapter.getLoginPasswd())
                                .connectionTimeout(RpcConstants.UNREGISTER_NE_CONNECTION_TIMEOUT)
                                .readTimeout(RpcConstants.UNREGISTER_NE_READ_TIMEOUT)
                                .build())
                .post(getManagerUrl(adapter, requestOp), body);
        log.info("result output is:{}", result);
        RemoveNeOutput removeNeOutput = formRpcOutPut(requestOp,
                result, RemoveNeOutput.class);
        if (removeNeOutput.getReturnCode() != RpcResultType.Success) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("Failed to unregister ne %s. Reason: %s", neId,
                            removeNeOutput.getReturnMessage()));
        }
        return removeNeOutput;
    }


    @Override
    public ConnectNeOutput connectNe(Adapter adapter, Node ne)
            throws ExecutionException, InterruptedException {
        log.info("start to registered ne the ne id is {}", ne.getNodeId().getValue());
        String requestOp = EmlRpcCmd.CONNECT_NE;
        ConnectNeInputBuilder builder = new ConnectNeInputBuilder();
        if (ne == null || ne.getAugmentation(Node1.class) == null
                || ne.getAugmentation(Node1.class).getPhysical() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("Ne %s does not exist. ", ne.getNodeId().getValue()));
        }
        Physical phy = ne.getAugmentation(Node1.class).getPhysical();
        builder.setNodeId(ne.getNodeId()).setIp(phy.getIp()).setPort(phy.getPort())
                .setUsername(phy.getLoginName())
                .setPassword(phy.getLoginPasswd()).setNodeType(phy.getNodeType());
        String requestBody = formRpcInput(requestOp, builder.build());
        String result = odlRpcClient.setConfig(
                        new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                .password(adapter.getLoginPasswd())
                                .connectionTimeout(RpcConstants.CONNECT_NE_CONNECTION_TIMEOUT)
                                .readTimeout(RpcConstants.CONNECT_NE_READ_TIMEOUT)
                                .build())
                .post(getManagerUrl(adapter, requestOp), requestBody);
        log.info("result output is:{}", result);
        ConnectNeOutput connectNeOutput = formRpcOutPut(requestOp, result, ConnectNeOutput.class);
        return connectNeOutput;
    }

    @Override
    public void removeNe() {

    }


    @Override
    public void getAlarm() {

    }

    @Override
    public void processAlarm() {

    }

    @Override
    public void refreshAlarm(Adapter adapter, String neId) throws CommonException {
        log.info("refresh the ne :{} alarm", neId);
        try {
            RefreshAlarmInputBuilder builder = new RefreshAlarmInputBuilder();
            builder.setNodeId(neId);
            String requestOp = EmlRpcCmd.REFRESH_ALARM;
            String req = formRpcInput(requestOp, builder.build());
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .post(getManagerUrl(requestOp), req);
            log.info("result output is:{}", result);

            RefreshAlarmOutput output = formRpcOutPut(requestOp, result, RefreshAlarmOutput.class);
            if (output == null || output.getReturnCode() != RpcResultType.Success) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        String.format("Failed to refresh alarm for ne %s.", neId));
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }


    @Override
    public void getNeData() {

    }

    @Override
    public void mergeData(Adapter adapter, String neId) throws CommonException {
        log.info("start to merge for ne Id :{}", neId);
        String requestOp = EmlRpcCmd.MERGE_DATA;
        try {
            MergeDataInput compareNeInput = new MergeDataInputBuilder()
                    .setNodeId(new NodeId(neId))
                    .build();
            String requestBody = formRpcInput(requestOp, compareNeInput);
            Response response = odlRpcClient.setConfig(
                            new Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd())
                                    .build())
                    .postReq(getManagerUrl(adapter, requestOp), requestBody,
                            RpcConstants.MERGE_DATA_READ_TIMEOUT);
            log.info("result output is response code is:{} body :{}", response.getStatusCode(),
                    response.getResponseBody());
            if (response.getStatusCode() != HttpStatus.OK.value()) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "ask to merge data error");
            }
        } catch (Exception ex) {
            log.error("failed to execute the rpc cmd, reason is {}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to merge the ne");
        }
    }

    @Override
    public CompareNeOutput compareNe(
            Adapter adapter, String neId,
            Physical physical,
            List<TerminationPoint> tps) {
        log.info("start to compareNe for ne Id :{}", neId);
        String requestOp = EmlRpcCmd.COMPARE_NE;
        try {
            CompareNeInput compareNeInput = new CompareNeInputBuilder()
                    .setNodeId(new NodeId(neId))
                    .setPhysical(physical)
                    .setTerminationPoint(tps)
                    .build();
            String requestBody = formRpcInput(requestOp, compareNeInput);
            String responseBody = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .longTimePost(getManagerUrl(adapter, requestOp), requestBody);
            log.info("result output is:{}", responseBody);

            CompareNeOutput output = formRpcOutPut(requestOp, responseBody, CompareNeOutput.class);
            return output;
        } catch (Exception ex) {
            log.error("failed to execute the rpc cmd, reason is {}", ex.getMessage());
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to graphics the ne");
        }
    }

    /**
     * get me version information
     *
     * @param adapter
     * @param node
     * @return
     */
    @Override
    public String getNeVersion(Adapter adapter, Node node)
            throws ExecutionException, InterruptedException {
        log.info("start to get the ne version info {}", node.getNodeId().getValue());
        Physical phy = node.getAugmentation(Node1.class).getPhysical();
        Properties properties = nePropertiesBuilder(phy);
        GetNeDataInputBuilder builder = new GetNeDataInputBuilder();
        builder.setNodeId(node.getNodeId());
        builder.setProperties(properties);
        String requestOp = EmlRpcCmd.GET_NE_DATA;
        String body = formRpcInput(requestOp, builder.build());

        String result = odlRpcClient.setConfig(
                        new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                .password(adapter.getLoginPasswd())
                                .build())
                .post(getManagerUrl(adapter, requestOp), body, COMMON_CONNECTION_TIMEOUT);
        log.info("result output is:{}", result);
        GetNeDataOutput output = formRpcOutPut(requestOp, result, GetNeDataOutput.class);
        return extractNeVersion(output, phy, node.getNodeId().getValue());
    }

    @Override
    public void syncNeData(Adapter adapter, String neId)
            throws ExecutionException, InterruptedException {
        log.info("start to synchro the ne data {} through adapter {}", neId,
                adapter.getName().getValue());
        SyncNeDataInputBuilder builder = new SyncNeDataInputBuilder();
        builder.setNodeId(NodeId.getDefaultInstance(neId));
//        this.constructSyncDataInput(builder, node);
        String requestOp = EmlRpcCmd.SYNC_NE_DATA;
        String body = formRpcInput(requestOp, builder.build());
        try {
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd())
//                                    .connectionTimeout(RpcConstants.SYNC_DATA_CONNECTION_TIMEOUT)
//                                    .readTimeout(RpcConstants.SYNC_DATA_READ_TIMEOUT)
                                    .build())
                    .post(getManagerUrl(adapter, requestOp), body,
                            RpcConstants.SYNC_DATA_READ_TIMEOUT);
            log.info("result output is:{}", result);
            SyncNeDataOutput syncNeDataOutput = formRpcOutPut(requestOp, result,
                    SyncNeDataOutput.class);
            RpcResultType returnCode = syncNeDataOutput.getReturnCode();
            if (!returnCode.equals(RpcResultType.Success)) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        syncNeDataOutput.getReturnMessage());
            }
            log.debug("finish to sync data the result is :{}", result);
        } catch (Exception ex) {
            // syncNeData response timeout: adapter may have completed sync, ignore
            if (ex instanceof java.util.concurrent.ExecutionException
                    && ex.getCause() instanceof java.util.concurrent.TimeoutException) {
                log.warn(
                        "Sync NE data response timeout for ne: {}, adapter may have completed sync, ignoring",
                        neId);
                return;
            }
            log.error("failed to synchronize the ne data ,the reason is :{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public ConfigNeOutput configNe(Adapter adapter, ConfigNeInput configNeInput)
            throws CommonException {
        log.info("start to config ne ,the ne id is :{},adapter is :{}", configNeInput.getNodeId(),
                adapter.getName().getValue());
        String requestOp = EmlRpcCmd.CONFIG_NE;
        try {
            String body = formRpcInput(requestOp, configNeInput);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd())
                                    .build())
                    .post(getManagerUrl(adapter, requestOp), body);
            log.info("result output is:{}", result);
            return formRpcOutPut(requestOp, result, ConfigNeOutput.class);
        } catch (Exception ex) {
            log.error("failed to config ne ,the reason is :{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public BatchConfigNeOutput batchConfigNe(Adapter adapter, BatchConfigNeInput batchConfigNeInput)
            throws CommonException {
        log.info("start to batch config ne ,the ne id is :{},adapter is :{}",
                batchConfigNeInput.getNodeId(),
                adapter.getName().getValue());
        String requestOp = EmlRpcCmd.BATCH_CONFIG_NE;
        try {
            String body = formRpcInput(requestOp, batchConfigNeInput);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd())
                                    .build())
                    .post(getManagerUrl(adapter, requestOp), body);
            log.info("result output is:{}", result);
            return formRpcOutPut(requestOp, result, BatchConfigNeOutput.class);
        } catch (Exception ex) {
            log.error("failed to batch config ne ,the reason is :{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public Ne getNe(Adapter adapter, String neId) throws CommonException {
        log.info("start to get detail info from adapter {} ,neId {}", adapter.getName().getValue(),
                neId);
        String requestOp = EmlRpcCmd.GET_NE;
        try {
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd())
                                    .build())
                    .get(getOperationalUrl(adapter, requestOp, neId));
            log.info("result output is:{}", result);
            Ne ne = form2DataObject(YangDataUtil.getNeIID(neId), result, Ne.class);
            return ne;
        } catch (Exception ex) {
            log.error("failed to get ne information", ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get ne information");
        }
    }

    @Override
    public String getNeData(Adapter adapter, GetNeDataInput emlInput) throws CommonException {
        log.info("start to get detail info from adapter {} ,neId {}", adapter.getName(),
                emlInput.getNodeId().getValue());
        String requestOp = EmlRpcCmd.GET_NE_DATA;
        try {
            String requestBody = formRpcInput(requestOp, emlInput);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd())
                                    .connectionTimeout(COMMON_CONNECTION_TIMEOUT)
                                    .readTimeout(COMMON_READ_TIMEOUT)
                                    .build())
                    .post(getManagerUrl(adapter, requestOp), requestBody);
            log.info("result output is:{}", result);
            return result;
        } catch (Exception ex) {
            log.error("failed to get ne information", ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get ne information", ex);
        }
    }

    @Override
    public RemoveResourceOutput removeResource(
            Adapter adapter,
            RemoveResourceInput removeResourceInput) throws CommonException {
        log.info("start to remove ne :{} resource from adapter :{}",
                removeResourceInput.getNodeId().getValue(), adapter.getName().getValue());
        String requestOp = EmlRpcCmd.REMOVE_RESOURCE;
        try {
            String requestBody = formRpcInput(requestOp, removeResourceInput);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .longTimePost(getManagerUrl(adapter, requestOp), requestBody);
            log.info("result output is:{}", result);
            RemoveResourceOutput output = formRpcOutPut(requestOp, result,
                    RemoveResourceOutput.class);
            return output;
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to remove the ne:" + removeResourceInput.getNodeId().getValue()
                            + " resources", ex);
        }
    }

    @Override
    public TestNeConnectionOutput testNeConnection(Adapter adapter,
            TestConnectionStatusInput testConnectionStatusInput) throws CommonException {
        log.info("start to test the node {} connection stats ",
                testConnectionStatusInput.getNodeId().getValue());
        String requestOp = EmlRpcCmd.TEST_NE_CONNECTION;
        try {
            TestNeConnectionInput input = convertToEmlTestNeConnection(testConnectionStatusInput);
            String requestBody = formRpcInput(requestOp, input);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd())
                                    .build())
                    .post(getManagerUrl(adapter, requestOp), requestBody,
                            COMMON_CONNECTION_TIMEOUT);
            log.info("result output is:{}", result);
            TestNeConnectionOutput output = formRpcOutPut(requestOp, result,
                    TestNeConnectionOutput.class);
            return output;
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to remove the ne:" + testConnectionStatusInput.getNodeId().getValue()
                            + " resources", ex);
        }
    }

    private TestNeConnectionInput convertToEmlTestNeConnection(
            TestConnectionStatusInput testConnectionStatusInput) {
        TestNeConnectionInputBuilder builder = new TestNeConnectionInputBuilder().setNodeId(
                        testConnectionStatusInput.getNodeId())
                .setIp(testConnectionStatusInput.getPhysical().getIp())
                .setNodeType(testConnectionStatusInput.getPhysical().getNodeType())
                .setPassword(testConnectionStatusInput.getPhysical().getLoginPasswd())
                .setPort(testConnectionStatusInput.getPhysical().getPort())
                .setUsername(testConnectionStatusInput.getPhysical().getLoginName());
        return builder.build();
    }


    private String extractNeVersion(GetNeDataOutput output, Physical phy, String neId) {
        Properties properties = output.getProperties();
        String neYangVersion = null;
        String vendorName = null;
        String vendorType = null;
        if (properties.getProperty() != null) {
            for (Property p : properties.getProperty()) {
                if (p.getName().equals("ne.yang-version")) {
                    neYangVersion = p.getValue();

                    if (neYangVersion.startsWith("Error")) {
                        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                                neYangVersion);
                    }
                }
                if (p.getName().equals("vendor-name")) {
                    vendorName = p.getValue();
                }
                if (p.getName().equals("vendor-type")) {
                    vendorType = p.getValue();
                }
            }
        }

        //special process.
        String nVendorName = phy.getVendorName();
        log.info(
                "Get ne {} yang version {}, vendor-name {}, vendor-type {}. And designed is: {}, {} ",
                neId, neYangVersion, vendorName, vendorType, nVendorName,
                phy.getVendorType());

        //以后如果需要支持多厂家就在这个地方加

        //vendorType is provided by diff attribute on NE. thus discard the graphics.
        if (vendorName != null && vendorName.equals(nVendorName)) {
            return neYangVersion;
        } else {
            if (vendorName != null) {
                log.debug("designed NE vendor-name {}, vendor-type {}", nVendorName,
                        phy.getVendorType());
                throw new CommonException(
                        CommonExceptionType.INVALID_PARAMETER,
                        "Error: the NE's vendorName difference with required. real is: "
                                + vendorName + ". and design is: " + nVendorName);
            } else {
                return neYangVersion;
            }
        }
    }

    private Properties nePropertiesBuilder(Physical phy) {
        log.debug("init default properties");
        String neUserName = phy.getLoginName();
        String nePass = phy.getLoginPasswd();
        String neIp = phy.getIp();
        Integer port = phy.getPort().getValue();
        PropertiesBuilder pBuilder = new PropertiesBuilder();
        List<Property> list = new ArrayList<>();
        list.add(new PropertyBuilder().setKey(new PropertyKey("ne.yang-version"))
                .setName("ne.yang-version").build());
        list.add(new PropertyBuilder().setKey(new PropertyKey("ip")).setName("ip").setValue(neIp)
                .build());
        list.add(new PropertyBuilder().setKey(new PropertyKey("port")).setName("port")
                .setValue(String.valueOf(port)).build());
        list.add(new PropertyBuilder().setKey(new PropertyKey("username")).setName("username")
                .setValue(neUserName).build());
        list.add(new PropertyBuilder().setKey(new PropertyKey("password")).setName("password")
                .setValue(nePass).build());
        if (phy.getVendorName() != null) {
            list.add(new PropertyBuilder().setKey(new PropertyKey("vendor-name"))
                    .setName("vendor-name")
                    .setValue("").build());
        }
        if (phy.getVendorName() != null) {
            list.add(new PropertyBuilder().setKey(new PropertyKey("vendor-type"))
                    .setName("vendor-type")
                    .setValue("").build());
        }

        pBuilder.setProperty(list);
        return pBuilder.build();
    }

//    private String getManagerUrl(Adapter adapter, String requestOp) {
//        return new StringBuilder(HTTP_PREFIX).append(adapter.getIp()).append(COLON)
//                .append(adapter.getPort().getValue())   //adapter.getPort()
//                .append(RpcConstants.RPC_PREFIX)
//                .append(NAMESPACE)
//                .append(COLON)
//                .append(requestOp)
//                .toString();
//    }

    private String getOperationalUrl(
            Adapter adapter,
            String requestOp, String neId) {
        return HTTP_PREFIX + adapter.getIp() + COLON
                + adapter.getPort().getValue()  //adapter.getPort()
                + RpcConstants.TOPOLOGY_PREFIX
                + NAMESPACE
                + COLON
                + requestOp
                + neId.replace(POUND_SIGN, URL_POUND);
    }

    private void constructSyncDataInput(SyncNeDataInputBuilder builder, Node node)
            throws CommonException {
        if (node != null
                && node.getAugmentation(Node1.class) != null
                && node.getAugmentation(Node1.class).getPhysical() != null) {
            Physical phy = node.getAugmentation(Node1.class).getPhysical();
            if (phy.getEquipments() != null && phy.getEquipments().size() > 0) {
                PhysicalBuilder phyBuilder = new PhysicalBuilder();
                List<Equipments> equips = new ArrayList<>();
                for (Equipments equip : phy.getEquipments()) {
                    EquipmentsBuilder equipBuilder = new EquipmentsBuilder();
                    equipBuilder.setEquipmentId(equip.getEquipmentId());
                    equipBuilder.setImplementState(equip.getImplementState());
                    equipBuilder.setKey(equip.getKey());
                    equips.add(equipBuilder.build());
                }
                phyBuilder.setEquipments(equips);
                builder.setPhysical(phyBuilder.build());
            }
        }
        if (node.getTerminationPoint() != null
                && node.getTerminationPoint().size() > 0) {
            List<TerminationPoint> tps = new ArrayList<>();
            for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint tp : node
                    .getTerminationPoint()) {
                TerminationPointBuilder tpBuilder = new TerminationPointBuilder();
                tpBuilder.setKey(new TerminationPointKey(tp.getTpId()));
                tpBuilder.setTpId(tp.getTpId());
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder tpPhyBuilder =
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder();
                tpPhyBuilder.setImplementState(
                        tp.getAugmentation(TerminationPoint1.class).getPhysical()
                                .getImplementState());
                tpBuilder.setPhysical(tpPhyBuilder.build());
                tps.add(tpBuilder.build());
            }
            builder.setTerminationPoint(tps);
        }
    }

    public Report1524TelemetryDataOutput report1524TeleData(Adapter adapter,
            Report1524TelemetryDataInput input) throws CommonException {
        log.info("start to report 1524 telemetry data ,the ne id is :{}", input.getNodeId());
        String requestOp = EmlRpcCmd.REPORT_1524_TELEMETRY_DATA;
        try {
            String body = formRpcInput(requestOp, input);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .post(getManagerUrl(adapter, requestOp), body);
            return formRpcOutPut(requestOp, result, Report1524TelemetryDataOutput.class);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public NeDatabaseOperateOutput neDatabaseOperate(Adapter adapter, NeDatabaseOperateInput input)
            throws CommonException {
        log.info("start to operate ne database,the ne id is:{}", input.getNodeId());
        String cmd = EmlRpcCmd.NE_DATABASE_OPERATE;
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeDatabaseOperateInput emlInput = convertNeDatabaseOperateInput2eEml(
                input);
        try {
            String requestBody = formRpcInput(cmd, emlInput);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .post(getManagerUrl(adapter, cmd), requestBody);
            log.info("result output is:{}", result);
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeDatabaseOperateOutput neDatabaseOperateOutput = (org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeDatabaseOperateOutput) formRpcOutPut(
                    cmd, result);
            NeDatabaseOperateOutput output = convertDbOperateToEmlManagerOutput(
                    neDatabaseOperateOutput);
            return output;
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }


    @Override
    public NeSoftwareOperateOutput neSoftwareOperate(Adapter adapter, NeSoftwareOperateInput input)
            throws CommonException {
        log.info("start to operate ne software,the ne id is:{}", input.getNodeId());
        String cmd = EmlRpcCmd.NE_SOFTWARE_OPERATE;
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeSoftwareOperateInput emlInput = convertNeSwOpInput2Eml(
                input);

        try {
            String requestBody = formRpcInput(cmd, emlInput);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .post(getManagerUrl(adapter, cmd), requestBody);
            log.info("result output is:{}", result);
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeSoftwareOperateOutput emlOutput = (org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeSoftwareOperateOutput) formRpcOutPut(
                    cmd, result);

            return convertNeSwEmlManagerOutput(emlOutput);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public ClearApsSwitchLogOutput clearApsSwitchLog(Adapter adapter,
            String neId) {
        log.info("start to clear the ne :{} aps switch log ", neId);
        String cmd = EmlRpcCmd.CLEAR_APS_SWITCH_LOG;
        try {
            ClearApsSwitchLogInput input = new ClearApsSwitchLogInputBuilder().setNodeId(
                    NodeId.getDefaultInstance(neId)).build();
            String body = formRpcInput(cmd, input);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .post(getManagerUrl(adapter, cmd), body);
            log.info("clear the ne:{} aps switch log result is:{}", neId, result);
            return formRpcOutPut(cmd, result, ClearApsSwitchLogOutput.class);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public ApsSwitchOutput apsSwitch(Adapter adapter, String neId, String apsName,
            ApsPathType apsPathType, int index, Action action) {
        log.info(
                "start to send aps switch command,received ne is:{} aps name is:{} apsPathType:{} line path index:{} and action is:{}",
                neId, apsName, apsPathType.name(), index, action.name());
        String cmd = EmlRpcCmd.APS_SWITCH;
        try {
            ApsSwitchInput input = buildApsSwitchInput(neId, apsName, apsPathType, index, action);
            String requestBody = formRpcInput(cmd, input);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .post(getManagerUrl(adapter, cmd), requestBody);
            log.info("ne:{} aps switch command send receive result is:{}", neId, result);
            return formRpcOutPut(cmd, result, ApsSwitchOutput.class);
        } catch (ExecutionException | InterruptedException ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }

    }

    @Override
    public NeOperationLinkOutput operationLink(Adapter adapter, String nodeId, String tpId,
            OtsOperationType operation, Class<? extends OPERATIONITEM> operationItem) {
        log.info(
                "start to send operation link command to ne:{} operation is:{} operationItem is:{} through adapter:{}",
                nodeId, operation, operationItem, adapter.getName());
        String cmd = EmlRpcCmd.NE_OPERATION_LINK;
        try {
            NeOperationLinkInput neOperationLinkInput = buildNeOperationLinkInput(nodeId, tpId,
                    operation, operationItem);
            String requestBody = formRpcInput(cmd, neOperationLinkInput);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .post(getManagerUrl(adapter, cmd), requestBody);
            log.info("ne:{} operation link result is:{}", nodeId, result);
            return formRpcOutPut(cmd, result, NeOperationLinkOutput.class);
        } catch (ExecutionException | InterruptedException ex) {
            log.error("failed to execute the ne operation link :{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public SwitchCuActiveStandbyOutput switchCuActiveStandby(Adapter adapter, String neId,
            String cuId) {
        log.info("start to send cu:{} by ne:{} switch cu active standby through adapter:{}", cuId,
                neId, adapter);
        String cmd = EmlRpcCmd.SWITCH_CU_ACTIVE_STANDBY;
        try {
            SwitchCuActiveStandbyInput switchCuActiveStandbyInput = buildSwitchCuActiveStandbyInput(
                    neId, cuId);
            String requestBody = formRpcInput(cmd, switchCuActiveStandbyInput);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .post(getManagerUrl(adapter, cmd), requestBody);
            log.info("ne:{} cu:{} switch result is:{}", neId, cuId, result);
            return formRpcOutPut(cmd, result, SwitchCuActiveStandbyOutput.class);
        } catch (ExecutionException | InterruptedException ex) {
            log.error("failed to execute the switch cu active standby:{}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public UploadHistoryPmOutput uploadHistoryPm(Adapter adapter, String neId, Long startTimestamp,
            Long endTimestamp, Long interval, SftpServerInfo sftpServerInfo) {
        log.info(
                "start to upload history pm from neId:{} time between :{} and :{} ,interval is:{} sftp server is:{}",
                neId, startTimestamp, endTimestamp, interval, sftpServerInfo.getAddress());
        String cmd = EmlRpcCmd.UPLOAD_HISTORY_PM;
        try {
            UploadHistoryPmInput uploadHistoryPmInput = buildUploadHistoryPmInput(neId,
                    startTimestamp, endTimestamp, interval, sftpServerInfo);
            String requestBody = formRpcInput(cmd, uploadHistoryPmInput);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .post(getManagerUrl(adapter, cmd), requestBody);
            log.info("ne:{} upload history pm upload history pm  result is:{}", neId, result);
            return formRpcOutPut(cmd, result, UploadHistoryPmOutput.class);
        } catch (ExecutionException | InterruptedException ex) {
            log.error("failed to execute the upload history pm from neId:{} reason:{}", neId,
                    ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }

    @Override
    public ChannelAseRestoreOutput channelAseRestore(Adapter adapter, String neId,
            String crossConnectionId) {
        log.info(
                "start to execute channel ase restore rpc for neId:{} channel index ref cross connection id:{} through adapter:{}",
                neId, crossConnectionId, adapter.getName());
        String cmd = EmlRpcCmd.CHANNEL_ASE_RESTORE;
        try {
            ChannelAseRestoreInput channelAseRestoreInput = buildChannelAseRestoreInput(neId,
                    crossConnectionId);
            String requestBody = formRpcInput(cmd, channelAseRestoreInput);
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .post(getManagerUrl(adapter, cmd), requestBody);
            log.info("ne:{} channel ase restore  result is:{}", neId, result);
            return formRpcOutPut(cmd, result, ChannelAseRestoreOutput.class);
        } catch (ExecutionException | InterruptedException e) {
            log.error("failed to execute the channel ase restore for neId:{} reason:{}", neId,
                    e.getMessage(), e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }
    }

    @Override
    public RefreshDeviceAlarmOutput refreshDeviceAlarm(Adapter adapter) {
        log.info("start to refresh device alarm by adapter:{}", adapter.getName());
        String cmd = EmlRpcCmd.REFRESH_DEVICE_ALARM;
        try {
            String result = odlRpcClient.setConfig(
                            new OdlRpcClientConfig.Builder().user(adapter.getLoginName())
                                    .password(adapter.getLoginPasswd()).build())
                    .post(getManagerUrl(adapter, cmd), null);
            log.info("refresh device alarm rpc out put is :{}", result);
            return formRpcOutPut(cmd, result, RefreshDeviceAlarmOutput.class);
        } catch (ExecutionException | InterruptedException e) {
            log.error("failed to execute refresh device alarm reason:{}",
                    e.getMessage(), e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, e.getMessage(),
                    e);
        }

    }

    private ChannelAseRestoreInput buildChannelAseRestoreInput(String neId,
            String crossConnectionId) {
        return new ChannelAseRestoreInputBuilder().setCrossConnectionId(crossConnectionId)
                .setNodeId(
                        NodeId.getDefaultInstance(neId)).build();
    }

    private UploadHistoryPmInput buildUploadHistoryPmInput(String neId, Long startTimestamp,
            Long endTimestamp, Long interval, SftpServerInfo sftpServerInfo) {
        RemoteServerBuilder remoteServerBuilder = new RemoteServerBuilder(sftpServerInfo);
        UploadHistoryPmInputBuilder uploadHistoryPmInputBuilder = new UploadHistoryPmInputBuilder();
        uploadHistoryPmInputBuilder.setNodeId(NodeId.getDefaultInstance(neId));
        uploadHistoryPmInputBuilder.setInterval(BigInteger.valueOf(interval));
        uploadHistoryPmInputBuilder.setStartTimestamp(BigInteger.valueOf(startTimestamp));
        uploadHistoryPmInputBuilder.setEndTimestamp(BigInteger.valueOf(endTimestamp));
        uploadHistoryPmInputBuilder.setRemoteServer(remoteServerBuilder.build());
        return uploadHistoryPmInputBuilder.build();
    }


    private SwitchCuActiveStandbyInput buildSwitchCuActiveStandbyInput(String neId, String cuId) {
        SwitchCuActiveStandbyInputBuilder switchCuActiveStandbyInputBuilder = new SwitchCuActiveStandbyInputBuilder();
        switchCuActiveStandbyInputBuilder.setNodeId(NodeId.getDefaultInstance(neId));
        switchCuActiveStandbyInputBuilder.setTargetCu(cuId);
        return switchCuActiveStandbyInputBuilder.build();
    }

    private NeOperationLinkInput buildNeOperationLinkInput(String nodeId, String tpId,
            OtsOperationType operation, Class<? extends OPERATIONITEM> operationItem) {
        NeOperationLinkInputBuilder neOperationLinkInputBuilder = new NeOperationLinkInputBuilder();
        neOperationLinkInputBuilder.setOperation(operation);
        neOperationLinkInputBuilder.setOperationItem(operationItem);
        neOperationLinkInputBuilder.setNodeId(NodeId.getDefaultInstance(nodeId));
        neOperationLinkInputBuilder.setTpId(TpId.getDefaultInstance(tpId));
        return neOperationLinkInputBuilder.build();
    }

    private ApsSwitchInput buildApsSwitchInput(String neId, String apsName, ApsPathType apsPathType,
            int index, Action action) {
        ApsSwitchInput apsSwitchInput = new ApsSwitchInputBuilder()
                .setName(apsName)
                .setNeId(NodeId.getDefaultInstance(neId))
                .setIndex((short) index)
                .setAction(action)
                .setPath(apsPathType)
                .build();
        return apsSwitchInput;
    }


    private org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeSoftwareOperateInput convertNeSwOpInput2Eml(
            NeSoftwareOperateInput input) {
        NeSoftwareOperateInputBuilder inputBuilder = new NeSoftwareOperateInputBuilder();
        inputBuilder.setFileName(input.getFileName());
        inputBuilder.setNodeId(input.getNodeId());
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ne.software.operate.input.SftpServerBuilder sftpServerBuilder = new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ne.software.operate.input.SftpServerBuilder();
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ne.software.operate.input.SftpServer emlMgrSftServer = input.getSftpServer();
        sftpServerBuilder.setPassword(emlMgrSftServer.getPassword());
        sftpServerBuilder.setUploadPath(emlMgrSftServer.getUploadPath());
        sftpServerBuilder.setDownloadPath(emlMgrSftServer.getDownloadPath());
        sftpServerBuilder.setUser(emlMgrSftServer.getUser());
        sftpServerBuilder.setPort(emlMgrSftServer.getPort());
        sftpServerBuilder.setAddress(emlMgrSftServer.getAddress());
        sftpServerBuilder.setSourceAddress(emlMgrSftServer.getSourceAddress());
        sftpServerBuilder.setProtocol(emlMgrSftServer.getProtocol());
        inputBuilder.setSftpServer(sftpServerBuilder.build());
        inputBuilder.setVendorType(input.getVendorType());
//        TelnetInfoBuilder telnetInfoBuilder = new TelnetInfoBuilder();
//        TelnetInfo telnetInfo = input.getTelnetInfo();
//        telnetInfoBuilder.setAddress(telnetInfo.getAddress());
//        telnetInfoBuilder.setPassword(telnetInfo.getPassword());
//        telnetInfoBuilder.setPort(telnetInfo.getPort());
//        telnetInfoBuilder.setUser(telnetInfo.getUser());
//        telnetInfoBuilder.setType(telnetInfo.getType());
//        inputBuilder.setTelnetInfo(telnetInfoBuilder.build());
        inputBuilder.setSoftwareOperation(input.getSoftwareOperation());
        return inputBuilder.build();
    }

    private NeSoftwareOperateOutput convertNeSwEmlManagerOutput(
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeSoftwareOperateOutput emlOutput) {
        NeSoftwareOperateOutputBuilder neSoftwareOperateOutputBuilder = new NeSoftwareOperateOutputBuilder();
        neSoftwareOperateOutputBuilder.setResult(emlOutput.getResult());
        return neSoftwareOperateOutputBuilder.build();
    }


    private NeDatabaseOperateOutput convertDbOperateToEmlManagerOutput(
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeDatabaseOperateOutput neDatabaseOperateOutput) {
        NeDatabaseOperateOutputBuilder outputBuilder = new NeDatabaseOperateOutputBuilder();
        outputBuilder.setResult(neDatabaseOperateOutput.getResult());
        return outputBuilder.build();
    }

    private org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeDatabaseOperateInput convertNeDatabaseOperateInput2eEml(
            NeDatabaseOperateInput input) {
        NeDatabaseOperateInputBuilder inputBuilder = new NeDatabaseOperateInputBuilder();
        inputBuilder.setDbOperation(input.getDbOperation());
        inputBuilder.setNodeId(input.getNodeId());
        inputBuilder.setFileName(input.getFileName());
        SftpServerBuilder sftpServerBuilder = new SftpServerBuilder();
        SftpServer emlMgrSftServer = input.getSftpServer();
        sftpServerBuilder.setPassword(emlMgrSftServer.getPassword());
        sftpServerBuilder.setUploadPath(emlMgrSftServer.getUploadPath());
        sftpServerBuilder.setDownloadPath(emlMgrSftServer.getDownloadPath());
        sftpServerBuilder.setUser(emlMgrSftServer.getUser());
        sftpServerBuilder.setPort(emlMgrSftServer.getPort());
        sftpServerBuilder.setAddress(emlMgrSftServer.getAddress());
        sftpServerBuilder.setSourceAddress(emlMgrSftServer.getSourceAddress());
        sftpServerBuilder.setProtocol(emlMgrSftServer.getProtocol());
        inputBuilder.setSftpServer(sftpServerBuilder.build());
        inputBuilder.setVendorType(input.getVendorType());
        return inputBuilder.build();
    }

    private ExecuteXmlInput buildExecuteXmlInput(String neId, String payload) {
        ExecuteXmlInputBuilder executeXmlInputBuilder = new ExecuteXmlInputBuilder();
        executeXmlInputBuilder.setNodeId(neId);
        executeXmlInputBuilder.setRequest(payload);
        return executeXmlInputBuilder.build();
    }
}
