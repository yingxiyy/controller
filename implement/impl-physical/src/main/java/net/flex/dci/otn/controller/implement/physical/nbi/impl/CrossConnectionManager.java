/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.nbi.impl;

import static net.flex.dci.otc.common.constants.Constants.SLASH;
import static net.flex.dci.otn.controller.implement.common.utils.CommonUtils.convert;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otn.controller.implement.common.nbi.impl.BaseImpl;
import net.flex.dci.otn.controller.implement.common.service.MyExecutor;
import net.flex.dci.otn.controller.implement.common.utils.CommonUtils;
import net.flex.dci.otn.controller.implement.common.utils.MergeData;
import net.flex.dci.otn.controller.implement.physical.component.ApsSwitchManager;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateCrossConnectionInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateCrossConnectionOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateCrossConnectionOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.update.cross.connection.input.Xcs;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CrossConnectionManager extends BaseImpl {

    private final CrossConnectionsDao crossConnectionsDao;

    private final PhyNodeDao phyNodeDao;

    private final ApsSwitchManager apsSwitchManager;

    public UpdateCrossConnectionOutput updateCrossConnections(UpdateCrossConnectionInput input,
            TaskInfoMessage taskInfoMessage) {
        log.debug("start to update cross connections,input is:{}", input);
        validateUpdateInput(input);
        updatePhysical(input, taskInfoMessage);
        return new UpdateCrossConnectionOutputBuilder().setReturnCode(RpcResultType.Success)
                .build();
    }

    private void validateUpdateInput(UpdateCrossConnectionInput input) {
        log.debug("validate update input:{}", input);
        List<Xcs> xcs = input.getXcs();

        if (CollectionUtils.isEmpty(xcs)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "update cross connection input should not be null");
        }
        List<String> refNodeIds = getUpdateXcRefNode(xcs);
        if (refNodeIds.size() != 1) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "update cross connection only support one node ref cross connection");
        }
        String nodeId = refNodeIds.get(0);
        boolean isExist = phyNodeDao.existsByNodeId(nodeId);
        if (!isExist) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "update cross connection ref node:" + nodeId + " not found");
        }
        for (Xcs xc : xcs) {
            validateCrossConnection(xc);
        }
    }

    private void validateCrossConnection(Xcs xc) {
        log.debug("validate cross connection update:{}", xc);
        String nodeId = xc.getNodeId().getValue();
        String crossConnectionId = xc.getCrossConnectionId().getValue();
        CrossConnections crossConnection = crossConnectionsDao.getXCByNodeIdAndXcRef(nodeId,
                crossConnectionId);
        if (Objects.isNull(crossConnection)) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    "the cross connection:" + crossConnectionId + " is not found");
        }
        checkScaleLength(xc);
    }

    private List<String> getUpdateXcRefNode(List<Xcs> xcs) {
        List<String> refNodeIds = xcs.stream().map(Xcs::getNodeId)
                .map(Uri::getValue).distinct().collect(Collectors.toList());
        return refNodeIds;
    }

    public UpdateCrossConnectionOutput doIt(UpdateCrossConnectionInput input)
            throws CommonException {
//        checkParam(input);

//        MyExecutor executor = SpringBeanFinder.getBean(MyExecutor.class);
//        executor.lazyDo(new Runnable() {
//            @Override
//            public void run() {
//                updatePhysical(input);
//            }
//        });
//        try {
////            lockResource(input);
//            updatePhysical(input);
//        } catch (Exception e) {
//            throw e;
//        }
        UpdateCrossConnectionOutputBuilder outBuilder = new UpdateCrossConnectionOutputBuilder();
        outBuilder.setReturnCode(RpcResultType.Success);
        return outBuilder.build();
    }

//    private void lockResource(UpdateCrossConnectionInput input) throws CommonException {
//        List<NodeId> nodes = nodeGroup(input);
//
//        for (NodeId nodeId : nodes) {
//            locker.addResource(nodeId.getValue());
//        }
//        locker.getLock();
//    }

//    private void checkParam(UpdateCrossConnectionInput input) throws CommonException {
//        List<NodeId> nodes = nodeGroup(input);
//        if (nodes.size() != 1) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "only update one NE");
//        }
//
//        NodeId neId = nodes.get(0);
//        Node dbNode = changedObject.getChangedPhyNode(neId.getValue());
//        if (dbNode == null) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    "cannot find node " + neId);
//        }
//        if (PhysicalNodeUtils.checkNodeRebooting(dbNode)) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                    dbNode.getAugmentation(Node1.class).getPhysical().getFriendlyName()
//                            + " still in rebooting");
//        }
//
//        for (Xcs xc : input.getXcs()) {
//            String crossConnId = xc.getCrossConnectionId().getValue();
//            CrossConnections dbXc = getCrossConnection(dbNode, crossConnId);
//            if (dbXc == null) {
//                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                        "error CrossConnection info");
//            }
//            checkScaleLength(xc);
//        }
//    }

    private List<NodeId> nodeGroup(UpdateCrossConnectionInput input) {
        List<NodeId> nodes = new ArrayList<NodeId>();
        for (Xcs xc : input.getXcs()) {
            if (!nodes.contains(xc.getNodeId())) {
                nodes.add(xc.getNodeId());
            }
        }
        return nodes;
    }

    private void updatePhysical(UpdateCrossConnectionInput input, TaskInfoMessage taskInfoMessage)
            throws CommonException {
        log.debug("update  cross connection:{}", input);
        List<String> neIds = getUpdateXcRefNode(input.getXcs());
        String neId = neIds.get(0);
        Node refPhyNode = phyNodeDao.getConfigPhyNodeById(neId);
        Physical phNodeAttr = refPhyNode.getAugmentation(Node1.class).getPhysical();
        taskInfoMessage.setResourceId(neId);
        String dispString = phNodeAttr.getFriendlyName();
        if (!StringUtils.hasText(dispString)) {
            dispString = refPhyNode.getNodeId().getValue();
        }
        for (Xcs xc : input.getXcs()) {
            try {
                String crossConnId = xc.getCrossConnectionId().getValue();
                CrossConnections configXc = getCrossConnection(refPhyNode, crossConnId);
                CrossConnections operationalXc = configXc;
                boolean foundInConfig = (configXc != null);
                if (!foundInConfig) {
                    operationalXc = crossConnectionsDao.getOpXC(neId, crossConnId);
                    if (operationalXc == null) {
                        log.error("cannot find required XC {}", crossConnId);
                        continue;
                    }
                    log.info("XC {} found in operational DB only, will write to NE", crossConnId);
                }
                String description = operationalXc.getDescription();
                dispString += SLASH + description;
                log.info("Processing XC: {} - {}", crossConnId, description);

                String xcName = xc.getCrossConnectionId().getValue();
//                ImplementState state = dbXc.getImplementState();
//                if (!isConfig) {
//                    if (state.equals(ImplementState.Allocate) || state.equals(
//                            ImplementState.Plan)) {
//                        state = ImplementState.Implement;
//                    }
//                    writeNe(xc, xcName);
//                }
//                updateConfNode(refPhyNode, xc, state);
                boolean configDb = false;
                if (foundInConfig) {
                    xcName = updateConfNode(refPhyNode, xc);
                    dispString += " [in config db]";
                    configDb = true;
                }

                boolean shouldWriteToNetworkElement = determineIfShouldWrite(configXc,
                        operationalXc);
//                if (dbXc != null) {
//                    ImplementState state = dbXc.getImplementState();
//                    if (!(state.equals(ImplementState.Allocate) || state.equals(
//                            ImplementState.Plan))) {
//                        writeNe(xc, xcName);
//                    }
//                } else {
                if (shouldWriteToNetworkElement) {
                    log.info("Processing XC:{} - {} to device", crossConnId, description);
                    dispString += configDb ? " and " : "";
                    dispString += " [to device ]";
                    writeNe(xc, xcName);
                }

                log.debug("update cross connection success");
                CommonUtils.logMessage(BroadCastConstant.UPDATE_XC, dispString, BLANK,
                        taskInfoMessage);
            } catch (Exception e) {
                log.error("update cross connection error reason is:{}", e.getMessage(), e);
                CommonUtils.logMessage(BroadCastConstant.UPDATE_XC, dispString, e.getMessage(),
                        taskInfoMessage);
                throw e;
            }
        }

    }

    private boolean determineIfShouldWrite(CrossConnections configXc,
            CrossConnections operationalXc) {
        if (configXc != null) {
            ImplementState state = configXc.getImplementState();
            return !(state.equals(ImplementState.Allocate) ||
                    state.equals(ImplementState.Plan));
        }

        if (operationalXc != null) {
//            ImplementState opState = operationalXc.getImplementState();
//            return !opState.equals(ImplementState.Decommissioned);
            return true;
        }

        return false;
    }


    private String updateNode(Node dbNode, Xcs xc) {
        Node1 node1 = dbNode.getAugmentation(Node1.class);
        List<CrossConnections> xcs = node1.getPhysical().getCrossConnections();
        Optional<CrossConnections> optXc = xcs.stream()
                .filter(xci -> xci.getCrossConnectionId().equals(xc.getCrossConnectionId()))
                .findAny();

        String xcName = xc.getCrossConnectionId().getValue();
        if (optXc.isPresent()) {
            CrossConnections existedXc = optXc.get();
            if (existedXc.getDescription() != null) {
                xcName = existedXc.getDescription();
            }
            CrossConnections newXC = mergeXC(xc, existedXc);
            xcs.removeIf(xci -> xci.getCrossConnectionId().equals(xc.getCrossConnectionId()));
            xcs.add(newXC);
        } else {
            xcs.add(new CrossConnectionsBuilder(xc).build());
        }

        NodeBuilder nodeBuilder = new NodeBuilder(dbNode).addAugmentation(Node1.class,
                new Node1Builder(node1)
                        .setPhysical(new PhysicalBuilder(node1.getPhysical())
                                .setCrossConnections(xcs).build())
                        .build());

        ChangedObject changedObject = new ChangedObject();
        changedObject.addChangedPhyNode(nodeBuilder.build());
        MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
        mongoTransaction.save(changedObject);

        return xcName;
    }

    private CrossConnections mergeXC(Xcs xc, CrossConnections dbXC) {
        CrossConnectionsBuilder xcBuilder = new CrossConnectionsBuilder(dbXC);
        MergeData.merge(xc, xcBuilder);
        return xcBuilder.build();

//        if (xc.getAdminState() != null) {
//            xcBuilder.setAdminState(xc.getAdminState());
//        }
//        if (xc.getAmplifier() != null) {
//            Amplifier amplifier = xc.getAmplifier();
//            AmplifierBuilder ab = new AmplifierBuilder(dbXC.getAmplifier());
//            mergeData(amplifier, ab);
//            xcBuilder.setAmplifier(ab.build());
//        }
//        if (xc.getAps() != null) {
//            Aps aps = xc.getAps();
//            ApsBuilder ab = new ApsBuilder(dbXC.getAps());
//            mergeData(aps, ab);
//            xcBuilder.setAps(ab.build());
//        }
//        if (xc.getWssChannel() != null) {
//            WssChannel wssChannel = xc.getWssChannel();
//            WssChannelBuilder wb = new WssChannelBuilder(dbXC.getWssChannel());
//            mergeData(wssChannel, wb);
//            xcBuilder.setWssChannel(wb.build());
//        }
//        if (xc.getProperties() != null) {
//            List<Property> propList = new ArrayList<>();
//            propList.addAll(xc.getProperties().getProperty());
//            for (Property prop : xcBuilder.getProperties().getProperty()) {
//                if (propList.stream().filter(t -> t.getName().equals(prop.getName())).findAny()
//                        .isPresent()) {
//                    continue;
//                } else {
//                    propList.add(prop);
//                }
//            }
//            xcBuilder.setProperties(new PropertiesBuilder().setProperty(propList).build());
//        }
//        return xcBuilder.build();
    }

    private void mergeData(Object source, Object ab) {
        for (Field field : ab.getClass().getDeclaredFields()) {
            StringBuilder get = new StringBuilder();
            StringBuilder set = new StringBuilder();

            String firstLetter;
            String otherLetters;
            if (field.getName().equals("augmentation")) {
                get.append("get");
                set.append("add");

                firstLetter = field.getName().substring(0, 1).toUpperCase(Locale.ROOT);
                otherLetters = field.getName().substring(1);
            } else {
                if (field.getType().getTypeName().equals(Boolean.class.getTypeName())) {
                    get.append("is");
                    set.append("set");
                } else {
                    get.append("get");
                    set.append("set");
                }
                firstLetter = field.getName().substring(1, 2).toUpperCase(Locale.ROOT);
                otherLetters = field.getName().substring(2);
            }
            get.append(firstLetter);
            get.append(otherLetters);

            set.append(firstLetter);
            set.append(otherLetters);

            try {
                Method getMethod = source.getClass().getMethod(get.toString());
                Method setMethod = ab.getClass()
                        .getMethod(set.toString(), getMethod.getReturnType());

                if (getMethod.invoke(source) != null) {
                    setMethod.invoke(ab, getMethod.invoke(source));
                }
            } catch (Exception e) {
                log.error("merge {}} error", source.getClass().getSimpleName(), e);
            }
        }
    }

    private String updateConfNode(Node dbNode, Xcs xc) {
        log.debug("update configuration cross connection");
        return updateNode(dbNode, xc);
    }

    private void writeNe(Xcs xc, String xcName) throws CommonException {
        log.debug("send update cross connection configuration method to the ne");
        Node imNode = new NodeBuilder()
                .setNodeId(xc.getNodeId())
                .addAugmentation(Node1.class,
                        new Node1Builder().setPhysical(new PhysicalBuilder()
                                        .setCrossConnections(
                                                Collections.singletonList(
                                                        new CrossConnectionsBuilder(xc).build()))
                                        .build())
                                .build())
                .build();

        NeManagerRpc config = SpringBeanFinder.getBean(NeManagerRpc.class);
        ConfigNeOutput result = config.configNe(imNode);

        if (result.getFailObj() != null && result.getFailObj().getObject() != null) {
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    convert(xcName, result.getFailObj()));
        }
    }

    private void checkScaleLength(Xcs crossConn) throws CommonException {
        if (crossConn.getAmplifier() != null && crossConn.getAmplifier().getTargetGain() != null) {
            if (crossConn.getAmplifier().getTargetGain().scale() > 1) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        String.format("Xc[%s] Scale Length of target-gain is error",
                                crossConn.getCrossConnectionId().getValue()));
            }
        }

        if (crossConn.getAmplifier() != null
                && crossConn.getAmplifier().getTargetAttenuation() != null) {
            if (crossConn.getAmplifier().getTargetAttenuation().scale() > 1) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        String.format("Xc[%s] Scale Length of target-attenuation is error",
                                crossConn.getCrossConnectionId().getValue()));
            }
        }
    }

    private void addXc(PhysicalBuilder node, Xcs xc) {
        List<CrossConnections> xcList = node.getCrossConnections();
        if (xcList == null) {
            xcList = new LinkedList<>();
        }
        xcList.add(new CrossConnectionsBuilder(xc).build());
    }

    private CrossConnections getCrossConnection(Node dbNode, String crossConnId) {
        List<CrossConnections> xcList = dbNode.getAugmentation(Node1.class).getPhysical()
                .getCrossConnections();
        CrossConnections crossConnections = xcList.stream()
                .filter(crossConnection -> crossConnection.getCrossConnectionId().getValue()
                        .equals(crossConnId))
                .findAny().orElse(null);
        return crossConnections;
    }

    public UpdateCrossConnectionOutput start(UpdateCrossConnectionInput input) {
        log.debug("start to update xc:{} ", input);

//    	ImplLocker locker = SpringBeanFinder.getBean(ImplLocker.class);
//        locker.lockResource(TopoNameConstants.Phy_Topo_Key); //锁全网
//        locker.lock(); //where to unlock?

        //由于耗时, 把这个同步命令改为异步

        MyExecutor executor = SpringBeanFinder.getBean(MyExecutor.class);
        executor.lazyDo(new Runnable() {
            @Override
            public void run() {
                doIt(input);
            }
        });
        UpdateCrossConnectionOutputBuilder outBuilder = new UpdateCrossConnectionOutputBuilder();
        outBuilder.setReturnCode(RpcResultType.Success);
        return outBuilder.build();
    }
}
