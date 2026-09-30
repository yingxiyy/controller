/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.nbi.impl;

import static net.flex.dci.otn.controller.implement.common.utils.CommonUtils.convert;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.implement.common.nbi.impl.BaseImpl;
import net.flex.dci.otn.controller.implement.common.service.MyExecutor;
import net.flex.dci.otn.controller.implement.physical.util.PhysicalNodeUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.update.termination.point.input.Tps;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @version 1.0
 */

@Slf4j
public class UpdateTerminationPoint extends BaseImpl {

    private static final int customInfoLength = 128;
    private static final int descriptionLength = 128;
    @Autowired
    private PhyNodeDao phyNodeDao;
    private Node dbNode = null;

    public UpdateTerminationPointOutput doIt(UpdateTerminationPointInput input)
            throws CommonException {
        checkParam(input);

//        因为要写入网元，改为异步
        MyExecutor executor = SpringBeanFinder.getBean(MyExecutor.class);
        executor.lazyDo(new Runnable() {
            @Override
            public void run() {
                updatePhysical(input);
            }
        });

        UpdateTerminationPointOutputBuilder outBuilder = new UpdateTerminationPointOutputBuilder();
        outBuilder.setReturnCode(RpcResultType.Success);
        return outBuilder.build();
    }

    private void updatePhysical(UpdateTerminationPointInput input) {
        ZkResourceLock locker = new ZkResourceLock();
        locker.addResource(dbNode.getNodeId().getValue());
        locker.getLock();

        Physical phNodeAttr = dbNode.getAugmentation(Node1.class).getPhysical();

        String dispString = phNodeAttr.getFriendlyName();
        if (dispString == null || dispString.isEmpty()) {
            dispString = dbNode.getNodeId().getValue();
        }

        taskInfoMessage.setResourceId(dbNode.getNodeId().getValue());

        for (Tps tp : input.getTps()) {
            try {
                updatePhysical(tp);
                logMessage(BroadCastConstant.MODIFY_TP, dispString, BLANK);
            } catch (Exception e) {
                logMessage(BroadCastConstant.MODIFY_TP, dispString, e.getMessage());
            }
        }
        locker.unlock();
    }


    private void updatePhysical(Tps tp) throws CommonException {
        List<TerminationPoint> configNodeTpList = new LinkedList<>();
        List<TerminationPoint> implNodeTpList = new LinkedList<>();

        TerminationPoint dbTp = getTp(tp.getTpId().getValue());
        TerminationPoint newTp = new TerminationPointBuilder()
                .setTpId(tp.getTpId())
                .setKey(new TerminationPointKey(tp.getTpId()))
                .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                        .setPhysical(tp.getPhysical()).build())
                .build();

        ImplementState state = dbTp.getAugmentation(TerminationPoint1.class).getPhysical()
                .getImplementState();
        if (state.equals(ImplementState.Allocate) || state.equals(ImplementState.Plan)) {
            configNodeTpList.add(newTp);
        } else {
            configNodeTpList.add(newTp);
            implNodeTpList.add(newTp);
        }

        writeNe(configNodeTpList, implNodeTpList);
    }

    private Node updateConfigNode(List<TerminationPoint> changedTps) {
        List<TerminationPoint> oldTps = dbNode.getTerminationPoint();

        for (TerminationPoint changedTp : changedTps) {
            Optional<TerminationPoint> optTp = dbNode.getTerminationPoint().stream()
                    .filter(t -> t.getTpId().getValue().equals(changedTp.getTpId().getValue()))
                    .findAny();
            if (optTp.isPresent()) {
                TerminationPoint1 tp1 = optTp.get().getAugmentation(TerminationPoint1.class);
                PhysicalBuilder phyBuilder = new PhysicalBuilder(tp1.getPhysical());

                TerminationPoint1 changedTp1 = changedTp.getAugmentation(TerminationPoint1.class);
                if (changedTp1.getPhysical() != null) {
                    if (changedTp1.getPhysical().getOtuClient() != null) {
                        phyBuilder.setOtuClient(changedTp1.getPhysical().getOtuClient());
                    }
                    if (changedTp1.getPhysical().getOtuLine() != null) {
                        phyBuilder.setOtuLine(changedTp1.getPhysical().getOtuLine());
                    }
                    if (changedTp1.getPhysical().getProperties() != null) {
                        phyBuilder.setProperties(new PropertiesBuilder()
                                .setProperty(
                                        updateProps(tp1.getPhysical().getProperties().getProperty(),
                                                changedTp1.getPhysical().getProperties()
                                                        .getProperty()))
                                .build());
                    }
                    if (changedTp1.getPhysical().getWdm() != null) {
                        phyBuilder.setWdm(changedTp1.getPhysical().getWdm());
                    }
                    if (changedTp1.getPhysical().getDcn() != null) {
                        phyBuilder.setDcn(changedTp1.getPhysical().getDcn());
                    }
                }

                TerminationPoint1Builder tp1Builder = new TerminationPoint1Builder(tp1)
                        .setPhysical(phyBuilder.build());
                TerminationPointBuilder tpBuilder = new TerminationPointBuilder(
                        optTp.get()).addAugmentation(TerminationPoint1.class, tp1Builder.build());
                oldTps.removeIf(t -> t.getTpId().getValue().equals(changedTp.getTpId().getValue()));
                oldTps.add(tpBuilder.build());
            }
        }

        return new NodeBuilder(dbNode).build();
    }

    private List<Property> updateProps(List<Property> oldProps, List<Property> newProps) {
        if (newProps == null || newProps.isEmpty()) {
            return oldProps;
        } else {
            for (Property prop : newProps) {
                Optional<Property> optProp = oldProps.stream()
                        .filter(p -> p.getName().equals(prop.getName())).findAny();
                if (optProp.isPresent()) {
                    oldProps.removeIf(p -> p.getName().equals(prop.getName()));
                    oldProps.add(prop);
                } else {
                    oldProps.add(prop);
                }
            }
        }
        return oldProps;
    }

    private void writeNe(List<TerminationPoint> configNodeTpList,
            List<TerminationPoint> implNodeTpList) throws CommonException {
        if (configNodeTpList.size() > 0) {
            ChangedObject changedObject = new ChangedObject();
            changedObject.addChangedPhyNode(updateConfigNode(configNodeTpList));
            MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(
                    MultipleTransaction.class);
            mongoTransaction.save(changedObject);
        }
        Physical nodeAttr = dbNode.getAugmentation(Node1.class).getPhysical();
        if (implNodeTpList != null) {
            Node imNode = new NodeBuilder(dbNode)
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(
                                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(
                                            nodeAttr)
                                            .setInternalLinks(null)
                                            .setEquipments(null)
                                            .setCrossConnections(null)
                                            .setDcn(null)
                                            .setSystem(null)
                                            .setOCMGripGroups(null)
                                            .setProperties(null)
                                            .setSystem(null)
                                            .build())
                            .build())
                    .setTerminationPoint(configNodeTpList)
                    .build();

            NeManagerRpc config = SpringBeanFinder.getBean(NeManagerRpc.class);
            ConfigNeOutput result = config.configNe(imNode);

            if (result.getFailObj() != null && result.getFailObj().getObject() != null) {
                throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                        convert(result.getFailObj()));
            }
        }
    }

    private TerminationPoint getTp(String tpId) throws CommonException {
        for (TerminationPoint tp : dbNode.getTerminationPoint()) {
            if (tp.getTpId().getValue().equalsIgnoreCase(tpId)) {
                return tp;
            }
        }
        throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "cannot find TP: " + tpId);
    }

    private void checkParam(UpdateTerminationPointInput input) throws CommonException {
        for (Tps tp : input.getTps()) {
            String nodeId = PhysicalTpIdNamingRule.getNodeId(tp.getTpId().getValue());
            if (dbNode == null) {
                dbNode = phyNodeDao.getConfigPhyNodeById(nodeId);
            } else if (!dbNode.getNodeId().getValue().equalsIgnoreCase(nodeId)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "this command only support action on one Node");
            }
            boolean found = false;
            String eqId = PhysicalTpIdNamingRule.getEquipId(tp.getTpId().getValue());
            if (PhysicalNodeUtils.checkEquipmentRebooting(dbNode, eqId)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "The TP related equipment is rebooting");
            }

            getTp(tp.getTpId().getValue()); //check does the TP existed in DB

            if (tp.getPhysical().getProperties() != null
                    && tp.getPhysical().getProperties().getProperty() != null
                    && tp.getPhysical().getProperties().getProperty().size() != 0) {
                for (Property pro : tp.getPhysical().getProperties().getProperty()) {
                    if (pro.getName().equals("custom-info")) {
                        if (pro.getValue().length() > customInfoLength) {
                            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                                    "port's custom-info should be smaller than "
                                            + customInfoLength + " characters");
                        }
                    }
                    if (pro.getName().equals("description")) {
                        if (pro.getValue().length() > descriptionLength) {
                            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                                    "port's description should be smaller than "
                                            + descriptionLength + " characters");
                        }
                    }
                }
            }
        }
    }
}
