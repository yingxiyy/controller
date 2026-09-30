/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.rpc.client.rpcs;

import java.util.List;
import java.util.concurrent.ExecutionException;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.controller.rpc.client.dto.ExecuteNetConfResp;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ApsSwitchInput.Action;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ApsSwitchOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.BatchConfigNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.BatchConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ChannelAseRestoreOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ClearApsSwitchLogOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.CompareNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConfigNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ConnectNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.GetNeDataInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeOperationLinkOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RefreshDeviceAlarmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveResourceInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.RemoveResourceOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.Report1524TelemetryDataInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.Report1524TelemetryDataOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.SwitchCuActiveStandbyOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.TestNeConnectionOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.UploadHistoryPmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.nes.top.nes.Ne;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ApsPathType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OPERATIONITEM;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OtsOperationType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SftpServerInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TestConnectionStatusInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;

/**
 * @author zhaoxy
 * @version 1.0
 * @date 2021/8/24 16:31
 */
public interface AdapterRpc {

    ExecuteNetConfResp executeNetConfCmd(Adapter adapter, String neId, String payload);

    List<NodeId> getManagedNes(Adapter adapter);

    RemoveNeOutput unregisterNe(Adapter adapter, String neId, boolean isForce)
            throws ExecutionException, InterruptedException;

    ConnectNeOutput connectNe(Adapter adapter, Node node)
            throws ExecutionException, InterruptedException;

    void removeNe();

    void getAlarm();

    void processAlarm();

    void refreshAlarm(Adapter adapter, String neId);

    void getNeData();

    void mergeData(Adapter adapter, String neId) throws CommonException;

    /**
     * compareNe
     *
     * @param adapter
     * @param neId
     * @param physical
     * @param tps
     * @return
     */
    CompareNeOutput compareNe(
            Adapter adapter, String neId,
            Physical physical,
            List<TerminationPoint> tps);

    String getNeVersion(Adapter adapter, Node node) throws ExecutionException, InterruptedException;

    void syncNeData(Adapter adapterMatched, String neId)
            throws ExecutionException, InterruptedException;

    ConfigNeOutput configNe(Adapter adapter, ConfigNeInput configNeInput) throws CommonException;

    BatchConfigNeOutput batchConfigNe(Adapter adapter, BatchConfigNeInput batchConfigNeInput)
            throws CommonException;

    Ne getNe(Adapter adapter, String neId) throws CommonException;

    String getNeData(Adapter adapter, GetNeDataInput emlInput) throws CommonException;

    RemoveResourceOutput removeResource(Adapter adapter, RemoveResourceInput removeResourceInput)
            throws CommonException;

    TestNeConnectionOutput testNeConnection(Adapter adapter,
            TestConnectionStatusInput testInput)
            throws CommonException;

    Report1524TelemetryDataOutput report1524TeleData(Adapter adapter,
            Report1524TelemetryDataInput input) throws CommonException;

    NeDatabaseOperateOutput neDatabaseOperate(Adapter adapter, NeDatabaseOperateInput input)
            throws CommonException;

    NeSoftwareOperateOutput neSoftwareOperate(Adapter adapter, NeSoftwareOperateInput input)
            throws CommonException;

    ClearApsSwitchLogOutput clearApsSwitchLog(Adapter adapter, String neId);

    ApsSwitchOutput apsSwitch(Adapter adapter, String neId, String apsName, ApsPathType apsPathType,
            int index, Action action);

    NeOperationLinkOutput operationLink(Adapter adapter, String nodeId, String tpId,
            OtsOperationType operation, Class<? extends OPERATIONITEM> operationItem);

    SwitchCuActiveStandbyOutput switchCuActiveStandby(Adapter adapter, String neId, String cuId);

    UploadHistoryPmOutput uploadHistoryPm(Adapter adapter, String neId, Long startTimestamp,
            Long endTimestamp, Long interval,
            SftpServerInfo sftpServerInfo);

    ChannelAseRestoreOutput channelAseRestore(Adapter adapter, String neId,
            String crossConnectionId);

    RefreshDeviceAlarmOutput refreshDeviceAlarm(Adapter adapter);
}
