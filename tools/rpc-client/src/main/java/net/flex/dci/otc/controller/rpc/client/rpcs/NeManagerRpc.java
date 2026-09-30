/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.rpcs;

import java.util.List;
import net.flex.dci.otc.common.exception.CommonException;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.BatchConfigNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.BatchConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ChannelAseRestoreOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ClearNeApsSwitchLogsOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigureNorthboundTelemetryOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchInput.Action;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ManageApsSwitchOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeOperationLinkOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ReassignNtpServerOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RegisteNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RegisteNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.RemoveResourceOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.Report1524TelemetryDataInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.Report1524TelemetryDataOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.SwitchCuActiveStandbyOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UnregisteNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UploadHistoryPmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.GetNeDataInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.GetNeDataOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ApsPathType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OPERATIONITEM;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OtsOperationType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SftpServerInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/8/25 11:16
 */
public interface NeManagerRpc {

    /**
     * manage-ne
     *
     * @throws Exception
     */
    void manageNe() throws CommonException;


    ReassignNtpServerOutput reAssignNtpServer() throws CommonException;


    ConfigureNorthboundTelemetryOutput configureNorthboundTelemetry() throws CommonException;

    /**
     * register ne
     *
     * @param input
     * @throws CommonException
     */
    RegisteNeOutput registeredNe(RegisteNeInput input)
            throws CommonException;

    /**
     * config ne
     *
     * @param build
     * @return
     * @throws CommonException
     */
    ConfigNeOutput configNe(ConfigNeInput build) throws CommonException;

    BatchConfigNeOutput batchConfigNe(BatchConfigNeInput input) throws CommonException;

    ConfigNeOutput configNe(Node node) throws CommonException;

    void mergeNe(NodeId nodeId) throws CommonException;

    void uploadNe(String neId) throws CommonException;

    GetNeDataOutput getNeData(GetNeDataInput emlInput) throws CommonException;

    UnregisteNeOutput unregisteredNe(NodeId nodeId) throws CommonException;

    Report1524TelemetryDataOutput report1524TelemetryData(Report1524TelemetryDataInput input)
            throws CommonException;

    RemoveResourceOutput removeResource(Node build) throws CommonException;

    NeDatabaseOperateOutput neDatabaseOperate(NeDatabaseOperateInput input) throws CommonException;

    NeSoftwareOperateOutput neSoftwareOperate(NeSoftwareOperateInput input) throws CommonException;

    ClearNeApsSwitchLogsOutput clearNeApsSwitchLogs(List<String> neIds) throws CommonException;

    ManageApsSwitchOutput manageApsSwitch(String neId, String apsName, ApsPathType path,
            Short index,
            Action action) throws CommonException;

    NeOperationLinkOutput neOperationLink(String neId, String tpId, OtsOperationType operationType,
            Class<? extends OPERATIONITEM> operationItem);

    SwitchCuActiveStandbyOutput switchCuActiveStandby(String neId, String cuId);

    UploadHistoryPmOutput uploadHistoryPmFromNe(String neId, Long startTime, Long endTime,
            Long interval,
            SftpServerInfo sftpServerInfo);

    ChannelAseRestoreOutput channelAseRestore(String neId, String crossConnectionId);
}
