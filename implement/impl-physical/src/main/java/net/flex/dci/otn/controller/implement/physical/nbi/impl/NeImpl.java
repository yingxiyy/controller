/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.nbi.impl;

import static net.flex.dci.otc.common.constants.Constants.SLASH;
import static net.flex.dci.otc.common.util.Constant.PropKey_HostName;
import static net.flex.dci.otn.controller.implement.common.utils.CommonUtils.generateUploadNeHistoryPmTaskName;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.SWITCH_CU_TITLE;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.controller.rpc.client.rpcs.AdapterRpc;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.EquipmentsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.implement.common.nbi.impl.BaseImpl;
import net.flex.dci.otn.controller.implement.common.utils.AsynchronousExecutor;
import net.flex.dci.otn.controller.implement.common.utils.CommonUtils;
import net.flex.dci.otn.controller.implement.physical.util.ParamValidateUtils;
import net.flex.dci.otn.controller.implement.physical.validator.InputValidator;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import org.apache.curator.shaded.com.google.common.collect.Sets;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.Report1524TelemetryDataInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.Report1524TelemetryDataOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.SwitchCuActiveStandbyOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.UploadHistoryPmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.adapter.manager.Adapter;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.GetNeDataInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.TestNeConnectionOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPoint;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.termination.point.top.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Export1524TelemetryDataInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.GetNeDataInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.GetNeDataOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.GetNeDataOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveNeInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveNeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.RemoveNeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SwitchNeCuActiveStandbyInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.SwitchNeCuActiveStandbyOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TestConnectionStatusInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TestConnectionStatusOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TestConnectionStatusOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateNodeInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateNodeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateNodeOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UploadNeHistoryPmInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UploadNeHistoryPmOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UploadNeHistoryPmOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.update.node.input.Nodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRackBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRackKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/11/9 15:05
 */
@Slf4j
@Component
public class NeImpl extends BaseImpl {

    @Autowired
    private PhyNodeDao phyNodeDao;
    @Autowired
    private AdapterRpc adapterRpc;

    @Autowired
    private EquipmentsDao equipmentDao;

    @Autowired
    private InputValidator inputValidator;

//    @Autowired
//    private RackDao rackDao;

    @Autowired
    private SiteNodeDao siteNodeDao;

    public static Node updateFriendlyName(NodeId nodeId, String friendlyName) {
        PropertyBuilder pb = new PropertyBuilder()
                .setKey(new PropertyKey(PropKey_HostName))
                .setName(PropKey_HostName)
                .setValue(friendlyName);
        List<Property> proList = new LinkedList<>();
        proList.add(pb.build());

        Node1 phyNode = new Node1Builder()
                .setPhysical(new PhysicalBuilder()
                        .setProperties(new PropertiesBuilder()
                                .setProperty(proList)
                                .build())
                        .setFriendlyName(friendlyName)
                        .build())
                .build();

        return new NodeBuilder()
                .setNodeId(nodeId)
                .setKey(new NodeKey(nodeId))
                .addAugmentation(Node1.class, phyNode)
                .build();
    }

    @Override
    public TestConnectionStatusOutput testNeConnectionStatus(TestConnectionStatusInput input)
            throws CommonException {
        log.info("start to test the connection status for the ne {}", input.getNodeId().getValue());
        List<Adapter> adapters = adapterDao.getAdapters();
        if (adapters.size() == 0) {
            throw new CommonException(CommonExceptionType.CANNOT_FIND_COOPERATOR,
                    "hasn't find any Adapter");
        }
        Adapter adapter = adapters.get(((int) Math.random()) * adapters.size());
        Runnable testConnectionTask = () -> {
            //todo:do the execute command
            TestNeConnectionOutput testNeConnectionOutput = adapterRpc.testNeConnection(adapter,
                    input);
            broadcastTestNeConnection(input.getNodeId().getValue(), testNeConnectionOutput);
        };
        AsynchronousExecutor.execute(testConnectionTask);

        return new TestConnectionStatusOutputBuilder().setReturnCode(
                RpcResultType.Success).build();
    }


    @Override
    public UpdateNodeOutput updateNe(UpdateNodeInput input, TaskInfoMessage taskInfoMessage)
            throws CommonException {
        log.info("start to update ne data,the ne ids :{}", input.getNodes());
        ParamValidateUtils.checkUpdateNeParam(input);
        _updateNePhysical(input);
        UpdateNodeOutputBuilder outBuilder = new UpdateNodeOutputBuilder();
        outBuilder.setReturnCode(RpcResultType.Success);
        return outBuilder.build();
    }

    @Override
    public RemoveNeOutput removeNe(RemoveNeInput input) throws CommonException {
        log.debug("start to update tunnel ", input);

        return removeNeImpl(input);
//        MyExecutor executor = SpringBeanFinder.getBean(MyExecutor.class);
//        executor.lazyDo(new Runnable() {
//          @Override
//          public void run() {
//        	  removeNeImpl(input);
//          }
//        });
//        return new RemoveNeOutputBuilder()
//                .setReturnCode(RpcResultType.Success)
//                .build();
    }

    public RemoveNeOutput removeNeImpl(RemoveNeInput input) throws CommonException {
        log.info("start to remove ne ,body is {}", input);
        String neId = input.getNodeId().getValue();
        Node phyNode = phyNodeDao.getConfigPhyNodeById(neId);
        if (phyNode == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "can not found the ne id is :" + neId);
        }
        Physical physical = phyNode.getAugmentation(Node1.class).getPhysical();
        if (physical.getIp() != null) {
            throw new CommonException(
                    CommonExceptionType.INVALID_PARAMETER, "please remove the ne IP at first!");
        }
        ParamValidateUtils.checkUnderLayerLink(phyNode.getNodeId());
        //remove from the site
        removeFormSite(phyNode.getNodeId());

        ChangedObject changedObject = new ChangedObject();
        changedObject.setRemovedPhyNodeList(Sets.newHashSet(input.getNodeId().getValue()));
        MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
        mongoTransaction.save(changedObject);
        return new RemoveNeOutputBuilder()
                .setReturnCode(RpcResultType.Success)
                .build();
    }

    @Override
    public GetNeDataOutput getNeData(GetNeDataInput input) throws CommonException {
        log.info("get current ne properties,the ne is {},properties is {}",
                input.getNodeId().getValue(), input.getProperties().getProperty());
        String neId = input.getNodeId().getValue();
        Node phyNode = phyNodeDao.getConfigPhyNodeById(neId);
        if (phyNode == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "can not found the ne id :" + neId);
        }
        ParamValidateUtils.checkGetNeDataInput(input);
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.GetNeDataInput emlInput = new GetNeDataInputBuilder()
                .setNodeId(input.getNodeId())
                .setProperties(input.getProperties())
                .build();

        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.GetNeDataOutput result = neManagerRpc.getNeData(
                emlInput);

        return new GetNeDataOutputBuilder()
                .setNodeId(input.getNodeId())
                .setProperties(result.getProperties())
                .build();
    }

//    @Override
//    public RemoveIpOutput removeNeIp(RemoveIpInput input) throws CommonException {
//        log.info("start to remove ne ip,the ne id is {}", input.getNodeId().getValue());
//        return new NeIpManager().doIt(input);
//    }

    @Override
    public void export1524TelemetryData(Export1524TelemetryDataInput input) throws CommonException {
        String type;
        if (input.getType() != null) {
            type = input.getType().trim();
        } else {
            type = "all";
        }

        if (!type.equalsIgnoreCase("15min") && !type.equalsIgnoreCase("24hour")
                && !type.equalsIgnoreCase("all")) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the type must be: 15Min or 24Hour or all");
        }

        AsynchronousExecutor.execute(() -> {
            configReport1524TelemeryData(input);
        });

    }

    @Override
    public SwitchNeCuActiveStandbyOutput switchNeCu(SwitchNeCuActiveStandbyInput input) {
        if (input.getNodeId() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the switch cu target ne id should not be null");
        }
        if (input.getTargetCu() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the switch cu target id should not be null");
        }
        String neId = input.getNodeId().getValue();
        String cuId = input.getTargetCu();
        ZkResourceLock locker = new ZkResourceLock();
        String friendlyName = phyNodeDao.getFriendlyName(neId);
        Equipments cu = equipmentDao.getEquipmentByNodeAndEqId(neId, cuId);
        String cuName = cu.getFriendlyName();
        String taskTitle = SWITCH_CU_TITLE + friendlyName + SLASH + cuName;
        try {
            locker.addResource(neId);
            taskInfoMessage.setResourceId(input.getTargetCu());
            SwitchCuActiveStandbyOutput output = neCuSwitch(input);
            if (output.getReturnCode().equals(RpcResultType.Success)) {
                logMessage(BroadCastConstant.SWITCH_CU, taskTitle, BLANK);
            } else {
                logMessage(BroadCastConstant.SWITCH_CU, taskTitle, output.getReturnMessage());
            }
            return super.switchNeCu(input);
        } catch (Exception e) {
            log.error("switch cu error :{}", e.getMessage(), e);
            logMessage(BroadCastConstant.SWITCH_CU, taskTitle, e.getMessage());
            throw e;
        } finally {
            locker.unlock();
        }

    }

    @Override
    public UploadNeHistoryPmOutput uploadHistoryPm(UploadNeHistoryPmInput input,
                                                   TaskInfoMessage taskInfoMessage) {
        log.debug("start to upload ne history pm input:{}", input);
        inputValidator.validateUploadNeHistoryPmInput(input);
        String neId = input.getNodeId().getValue();
        ZkResourceLock locker = new ZkResourceLock();
        String friendlyName = phyNodeDao.getFriendlyName(neId);
        BigInteger interval = input.getInterval();
        String taskTitle = generateUploadNeHistoryPmTaskName(friendlyName, interval);
        try {
            locker.addResource(neId);
            taskInfoMessage.setResourceId(neId);
            UploadHistoryPmOutput output = _uploadHistoryPm(input);
            if (output.getReturnCode().equals(RpcResultType.Success)) {
                CommonUtils.logMessage(BroadCastConstant.UPLOAD_NE_HISTORY_PM, taskTitle, BLANK,
                        taskInfoMessage);
            } else {
                CommonUtils.logMessage(BroadCastConstant.UPLOAD_NE_HISTORY_PM, taskTitle,
                        output.getReturnMessage(), taskInfoMessage);
            }
            return new UploadNeHistoryPmOutputBuilder().setReturnCode(RpcResultType.Success)
                    .build();
        } catch (Exception e) {
            log.error("upload ne history pm error :{}", e.getMessage(), e);
            CommonUtils.logMessage(BroadCastConstant.UPLOAD_NE_HISTORY_PM, taskTitle,
                    e.getMessage(),
                    taskInfoMessage);
            throw e;
        } finally {
            locker.unlock();
        }

    }

    private UploadHistoryPmOutput _uploadHistoryPm(UploadNeHistoryPmInput input) {
        log.debug("upload history pm:{}", input);
        String neId = input.getNodeId().getValue();
        Long startTime = input.getStartTimestamp().longValue();
        Long endTime = input.getEndTimestamp().longValue();
        Long interval = input.getInterval().longValue();

        UploadHistoryPmOutput uploadHistoryPmOutput = neManagerRpc.uploadHistoryPmFromNe(
                neId,
                startTime, endTime, interval, input.getRemoteServer());
        return uploadHistoryPmOutput;
    }

    private SwitchCuActiveStandbyOutput neCuSwitch(SwitchNeCuActiveStandbyInput input) {
        log.debug("switch cu input is:{}", input);
        String neId = input.getNodeId().getValue();
        String cuId = input.getTargetCu();
        SwitchCuActiveStandbyOutput switchOutput = neManagerRpc.switchCuActiveStandby(neId, cuId);
        return switchOutput;
    }

    private void configReport1524TelemeryData(Export1524TelemetryDataInput input) {
        for (NodeId nodeRef : input.getNodeRef()) {
            String neId = nodeRef.getValue();
            Node node = phyNodeDao.getConfigPhyNodeById(neId);
            String neName = node.getAugmentation(Node1.class).getPhysical().getFriendlyName();
            try {
                NeManagerRpc rpc = SpringBeanFinder.getBean(NeManagerRpc.class);
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.Report1524TelemetryDataInputBuilder eb = new Report1524TelemetryDataInputBuilder();
                eb.setNodeRef(neId);
                eb.setType(input.getType());
                Report1524TelemetryDataOutput result = rpc.report1524TelemetryData(
                        eb.build());
                if (result.getReturnCode() != RpcResultType.Success) {
                    log.error("Failed to do config ne {}", neId);
                    logMessage(BroadCastConstant.EXPORT_15MIN_24H_DATA, neName,
                            result.getReturnMessage());
                }
                logMessage(BroadCastConstant.EXPORT_15MIN_24H_DATA, neName, BLANK);
            } catch (Exception e) {
                log.error("Failed to do config ne {}", neId, e);
                logMessage(BroadCastConstant.EXPORT_15MIN_24H_DATA, neName, e.getMessage());
            }
        }
    }

    /**
     * remove the neIp
     *
     * @param nodeId
     */
    private void _removeNeIp(String nodeId) {
        log.debug("start to remove ne ip, ne id is {}", nodeId);
        Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
        Node1 node1 = node.getAugmentation(Node1.class);
//        Physical confPhysical = phyNodeDao.getConfigPhysicalByNode(nodeId);
        Physical confPhysical = node1.getPhysical();

        Physical mergeConf = new PhysicalBuilder(confPhysical)
                .setIp(null)
                .setAdminState(AdminStatus.Unknown)
                .setOperationalState(OperStatus.Unknown)
                .setAlarmState(AlarmSeverity.Unknown)
                .setImplementState(ImplementState.Allocate)
                .setAlignmentStatus(AlignmentStatusType.Unknown)
                .build();

        NodeBuilder nodeBuilder = new NodeBuilder()
                .addAugmentation(Node1.class,
                        new Node1Builder(node1).setPhysical(mergeConf).build());
        ChangedObject changedObject = new ChangedObject();
        changedObject.addChangedPhyNode(nodeBuilder.build());
        changedObject.addRemovedOpPhyNode(nodeId);
        MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
        mongoTransaction.save(changedObject);
    }

    private void removeFormSite(NodeId nodeId) throws CommonException {
        String siteId = PhysicalNodeIdNamingRule.getSiteId(nodeId.getValue());
        //todo: lock for distribute
//        BatchLockTransaction locker = ZooKeeperToolset.instance().newTransaction();
//        locker.lock(siteId);
//        locker.require();

        log.debug("start to remove ne {}", nodeId.getValue());

        Node siteNode = siteNodeDao.getSiteNodeById(siteId);
        if (siteNode == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot find related site node " + siteId);
        }
        //update supporting node
        NodeBuilder siteNodeBuilder = new NodeBuilder(siteNode);
        if (siteNodeBuilder.getSupportingNode() != null) {
            List<SupportingNode> nodes = siteNodeBuilder.getSupportingNode();
            nodes.removeIf(sn -> sn.getNodeRef().getValue().equals(nodeId.getValue()));
            siteNodeBuilder.setSupportingNode(nodes);
//            siteNodeDao.rewriteSiteNode(siteNodeBuilder.build());
            ChangedObject changedObject = new ChangedObject();
            changedObject.addChangedSiteNode(siteNodeBuilder.build());
            MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(
                    MultipleTransaction.class);
            mongoTransaction.save(changedObject);
        }

        Site siteAttr = siteNode.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                .getSite();
        SupportingRack relatedRack = null;
        relatedRack = getSupportingRack(nodeId, siteAttr, relatedRack);
        if (relatedRack != null) {
            if (relatedRack.getSupportingNe().size() == 1) {
//                rackDao.deleteSiteRack(siteId, relatedRack.getRackId().getValue());
                ChangedObject changedObject = new ChangedObject();
                changedObject.addChangedSiteNode(delRackFromSite(siteNode, relatedRack.getKey()));
                MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(
                        MultipleTransaction.class);
                mongoTransaction.save(changedObject);
            } else {
//                rackDao.deleteSiteNodeRackSupportingNe(siteId,
//                        relatedRack.getRackId().getValue(), nodeId.getValue());
                ChangedObject changedObject = new ChangedObject();
                changedObject.addChangedSiteNode(
                        delSupportingNodeFromSite(siteNode, relatedRack.getKey(), nodeId));
                MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(
                        MultipleTransaction.class);
                mongoTransaction.save(changedObject);
            }
        }
        log.debug("end remove ne {}", nodeId.getValue());
    }

    private Node delRackFromSite(Node siteNode, SupportingRackKey rackKey) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1 node1 =
                siteNode.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class);
        List<SupportingRack> supportingRacks = node1.getSite().getSupportingRack();
        supportingRacks.removeIf(sr -> sr.getKey().equals(rackKey));
        NodeBuilder siteNodeBuilder = new NodeBuilder(siteNode).addAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class,
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder(
                        node1)
                        .setSite(new SiteBuilder(node1.getSite()).setSupportingRack(supportingRacks)
                                .build()).build());
        return siteNodeBuilder.build();
    }

    private Node delSupportingNodeFromSite(Node siteNode, SupportingRackKey rackKey,
                                           NodeId supportingNodeId) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1 node1 =
                siteNode.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class);
        List<SupportingRack> supportingRacks = node1.getSite().getSupportingRack();
        Optional<SupportingRack> optSupportingRack = supportingRacks.stream()
                .filter(sr -> sr.getKey().equals(rackKey)).findAny();
        if (optSupportingRack.isPresent()) {
            List<SupportingNe> supportingNes = optSupportingRack.get().getSupportingNe();
            supportingNes.removeIf(sn -> sn.getKey().getNodeRef().equals(supportingNodeId));
            SupportingRackBuilder srBuilder = new SupportingRackBuilder(
                    optSupportingRack.get()).setSupportingNe(supportingNes);
            supportingRacks.removeIf(sr -> sr.getKey().equals(rackKey));
            supportingRacks.add(srBuilder.build());
        }
        NodeBuilder siteNodeBuilder = new NodeBuilder(siteNode).addAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class,
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder(
                        node1)
                        .setSite(new SiteBuilder(node1.getSite()).setSupportingRack(supportingRacks)
                                .build()).build());
        return siteNodeBuilder.build();
    }

    private SupportingRack getSupportingRack(NodeId nodeId, Site siteAttr,
                                             SupportingRack relatedRack) {
        for (SupportingRack sr : siteAttr.getSupportingRack()) {
            if (sr.getSupportingNe() != null) {
                for (SupportingNe supportingNe : sr.getSupportingNe()) {
                    if (supportingNe.getNodeRef().getValue().equals(nodeId.getValue())) {
                        relatedRack = sr;
                        break;
                    }
                }
            }
        }
        return relatedRack;
    }

    private void _updateNePhysical(UpdateNodeInput input) throws CommonException {
        List<Nodes> nodeList = input.getNodes();
        log.info("start to update the physical ne");
        //check validation of inputing node's parameter
        Runnable registerNeTask = () -> {
            for (Nodes node : nodeList) {
                log.info("Start to config phy node {}", node.getNodeId().getValue());
                Physical phy = node.getPhysical();
                if (phy.getFriendlyName() != null && !phy.getFriendlyName().isEmpty()) {
                    updateNeFriendlyName(node);
                }
                if (phy.getIp() != null && phy.getPort() != null) {
                    //change Ne login info.
                    updateNeLoginInfo(node);
                }
                log.info("Finish to config phy node {}", node.getNodeId().getValue());
            }
        };
        AsynchronousExecutor.execute(registerNeTask);

    }

    /**
     * update login info
     *
     * @param node
     */
    private void updateNeLoginInfo(Nodes node) {

    }

    /**
     * update ne friendly name
     *
     * @param node
     */
    private void updateNeFriendlyName(Nodes node) {
        String friendlyName = node.getPhysical().getFriendlyName();
        log.debug("start update node{} friendlyName{}", node.getNodeId().getValue(),
                friendlyName);
        if (friendlyName.length() > Constant.NodefriendlyNameLength) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "ne's friendly-name should be smaller than " + Constant.NodefriendlyNameLength
                            + " characters");
        }

        if (phyNodeDao.existsNeFriendlyName(friendlyName)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Duplicate friendlyName with Node " + friendlyName);
        }

        Node mongoNode = phyNodeDao.getConfigPhyNodeById(node.getNodeId().getValue());
        Node newNode = updateFriendlyName(mongoNode.getNodeId(), friendlyName);

        ImplementState state = mongoNode.getAugmentation(Node1.class).getPhysical()
                .getImplementState();
        writeNe(newNode, state);
    }

    private void writeNe(Node node, ImplementState state) throws CommonException {
        if (state.equals(ImplementState.Allocate) || state.equals(ImplementState.Plan)) {
            //waiting for NE implState change to impl,
            //this value will be set on NE at when impl NE
            //Here just store in OP/CONF database.
//            phyNodeDao.saveConfigPhyNode(node);
            ChangedObject changedObject = new ChangedObject();
            changedObject.addChangedPhyNode(node);
            MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(
                    MultipleTransaction.class);
            mongoTransaction.save(changedObject);
        } else {
            //store in OP/CONF database. and write to NE
//            phyNodeDao.saveConfigPhyNode(node);
            ChangedObject changedObject = new ChangedObject();
            changedObject.addChangedPhyNode(node);
            MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(
                    MultipleTransaction.class);
            mongoTransaction.save(changedObject);

            NeManagerRpc neManagerRpc = SpringBeanFinder.getBean(NeManagerRpc.class);
            ConfigNeOutput result = neManagerRpc.configNe(configNeInput(node));
            if (result.getFailObj() != null) {
                throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                        result.getFailObj().toString());
            }
        }
    }

    private ConfigNeInput configNeInput(Node node) {
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
        return input;
    }


    /**
     * broad case test ne Connection state
     *
     * @param testNeConnectionOutput
     */
    private void broadcastTestNeConnection(String neId,
                                           TestNeConnectionOutput testNeConnectionOutput) {
        log.debug("to broad cast test ne connection return  message");
        String neFriendlyName = phyNodeDao.getFriendlyName(neId);

        RpcResultType returnCode = testNeConnectionOutput.getReturnCode();
        String returnMessage = testNeConnectionOutput.getReturnMessage();
        if (returnCode.equals(RpcResultType.Success)) {
            BroadcastMessager.publishKafkaMessage(
                    BroadcastMessage.builder().title(BroadCastConstant.TEST_NE_IS_REACHABLE)
                            .message(
                                    String.format("The ne :%s(%s) is reachable", neFriendlyName,
                                            neId))
                            .error(false).build());
        } else {

            BroadcastMessager.publishKafkaMessage(
                    BroadcastMessage.builder().title(BroadCastConstant.TEST_NE_IS_REACHABLE)
                            .message(
                                    String.format(
                                            "The ne :%s(%s) is unreachable ,the reason is: %s",
                                            neFriendlyName, neId, returnMessage))
                            .error(true).build());
        }
    }

}
