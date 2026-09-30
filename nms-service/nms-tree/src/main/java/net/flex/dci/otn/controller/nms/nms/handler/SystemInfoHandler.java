/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.ThreadPoolUtil;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.NeSystemDefaultInfo;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.System;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.SystemBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.system.rev180821.ne.system.info.system.Telemetry;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.UpdateSystemInfoOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.UpdateSystemInfoOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.springframework.stereotype.Component;

/**
 * @date: 2021/4/8
 */
@Slf4j
@Component
public class SystemInfoHandler extends AbstractBaseHandler {


    public SystemInfoHandler(NetconfTopology netconfTopology) {
        super(netconfTopology);
    }

    @Override
    public UpdateSystemInfoOutput updateSystemInfo() throws Exception {

        UpdateSystemInfoOutputBuilder outputBuilder = new UpdateSystemInfoOutputBuilder();

        NeSystemDefaultInfo systemInfo = netconfTopology.getNeSystemDefault();

        List<Node> nes = getNes();

        Map<String, Future<Boolean>> map = new HashMap<String, Future<Boolean>>();
        if (nes != null && nes.size() > 0) {
            for (Node ne : nes) {
                ImplementState impl = ne.getAugmentation(Node1.class).getPhysical()
                        .getImplementState();
                if (ImplementState.Allocate != impl) {
                    Node uptNe = getUptNe(ne, systemInfo);
                    Future<Boolean> nodeTask = updateSysInfo(uptNe);
                    map.put(ne.getNodeId().getValue(), nodeTask);
                }
            }

            String error = "";
            for (String neId : map.keySet()) {
                try {
                    log.info(String.format("thread waitForFutureTaskFinish impl node[%s] start",
                            neId));
                    boolean finished = ThreadPoolUtil.instance()
                            .waitForFutureTaskFinish(map.get(neId));
                    if (finished) {
                        log.info(String.format(
                                "thread waitForFutureTaskFinish impl node[%s] finished", neId));
                    }
                    log.info(String.format("thread waitForFutureTaskFinish impl node[%s] end",
                            neId));
                } catch (Exception e) {
                    log.info(
                            String.format("thread waitForFutureTaskFinish impl node[%s] error:[%s]",
                                    neId, e.getMessage()));
                    if ("".equals(error)) {
                        error = neId;
                    } else {
                        error = error + "," + neId;
                    }
                }
            }

            if (!"".equals(error)) {
                throw new Exception(
                        String.format("Nodes:[%] update system info error", error));
            }
        }
        outputBuilder.setReturnCode(RpcResultType.Success);
        return outputBuilder.build();
    }


    private List<Node> getNes() {
        Topology topo = netconfTopology.getTopology(new TopologyId(Constants.PHY_TOPO_KEY));
        if (topo != null) {
            return topo.getNode();
        }
        return null;
    }

    private Node getUptNe(Node ne, NeSystemDefaultInfo systemInfo) {
        List<Telemetry> telemetrys = new ArrayList<Telemetry>();
        for (Telemetry telemetry : systemInfo.getSystem().getTelemetry()) {
            if (NodeType.OD.name()
                    .equals(ne.getAugmentation(Node1.class).getPhysical().getNodeType().name())
                    && "OD".equals(telemetry.getSensorGroupName())) {
                telemetrys.add(telemetry);
            } else if (NodeType.TD.name()
                    .equals(ne.getAugmentation(Node1.class).getPhysical().getNodeType().name())
                    && "TD".equals(telemetry.getSensorGroupName())) {
                telemetrys.add(telemetry);
            }
        }

        System sys = new SystemBuilder()
                .setNtp(systemInfo.getSystem().getNtp())
                .setProperties(systemInfo.getSystem().getProperties())
                .setRadius(systemInfo.getSystem().getRadius())
                .setSyslog(systemInfo.getSystem().getSyslog())
                .setTelemetry(telemetrys)
                .build();

        Node uptNe = new NodeBuilder()
                .setNodeId(ne.getNodeId())
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder()
                                .setFriendlyName(ne.getAugmentation(Node1.class).getPhysical()
                                        .getFriendlyName())
                                .setIp(ne.getAugmentation(Node1.class).getPhysical().getIp())
                                .setLoginName(ne.getAugmentation(Node1.class).getPhysical()
                                        .getLoginName())
                                .setLoginPasswd(ne.getAugmentation(Node1.class).getPhysical()
                                        .getLoginPasswd())
                                .setNodeType(
                                        ne.getAugmentation(Node1.class).getPhysical().getNodeType())
                                .setPort(ne.getAugmentation(Node1.class).getPhysical().getPort())
                                .setSystem(sys).build())
                        .build())
                .build();
        return uptNe;
    }

    private Future<Boolean> updateSysInfo(Node ne) throws Exception {
        return ThreadPoolUtil.instance().addFutureTask(() -> {
            updateNeImpl(ne);
            return Boolean.TRUE;
        });
    }

    private ConfigNeInput convertConfigNe(Node ne) {
        NodeId nodeId = ne.getNodeId();
        ConfigNeInput configNe = new ConfigNeInputBuilder().setNodeId(nodeId)
                .setPhysical(ne.getAugmentation(Node1.class).getPhysical()).build();
        return configNe;
    }

    private void updateNeImpl(Node yangNe) throws Exception {
//        try {
//            Future<RpcResult<RegisteNeOutput>> regOutput = emlService
//                    .registeNe(new RegisteNeInputBuilder().setNodeId(yangNe.getNodeId()).build());
//            if (!regOutput.get().isSuccessful()) {
//                String errInfo = String.format("Failed to registe NE %s.Reason: %s",
//                        yangNe.getAugmentation(Node1.class).getPhysical().getFriendlyName(),
//                        regOutput.get().getErrors().toArray(new RpcRequestError[0])[0].getMessage());
//                log.error(errInfo);
//                throw new SotnException(SotnException.TYPE_bad_parameter, errInfo);
//            }
//
//            ConfigNeInput inputNe = convertConfigNe(yangNe);
//            Future<RpcResult<ConfigNeOutput>> confOutput = emlService.configNe(inputNe);
//            if (!confOutput.get().isSuccessful()) {
//                String errInfo = String.format("Failed to config NE %s.Reason: %s",
//                        yangNe.getAugmentation(Node1.class).getPhysical().getFriendlyName(),
//                        confOutput.get().getErrors().toArray(new RpcRequestError[0])[0].getMessage());
//                log.error(errInfo);
//                throw new SotnException(SotnException.TYPE_bad_parameter, errInfo);
//            }
//
//            if (confOutput.get().getResult().getFailObj() != null
//                    && confOutput.get().getResult().getFailObj().getObject() != null
//                    && confOutput.get().getResult().getFailObj().getObject().size() > 0) {
//                String errInfo = String.format("Failed to config NE[%s]",
//                        yangNe.getAugmentation(Node1.class).getPhysical().getFriendlyName());
//                throw new SotnException(SotnException.TYPE_bad_parameter, errInfo);
//            }
//            removeSystemInfo(yangNe.getNodeId());
//
//            InstanceIdentifier<Node> yangObjPath = InstanceIdentifier.builder(NetworkTopology.class)
//                    .build()
//                    .child(Topology.class,
//                            new TopologyKey(new TopologyId(OtnPhyTopology.QNAME.getLocalName())))
//                    .child(Node.class, new NodeKey(yangNe.getKey()));
//
//            yangTreeOp.merge(LogicalDatastoreType.CONFIGURATION, yangObjPath, yangNe);
////            yangTreeOp.merge(LogicalDatastoreType.OPERATIONAL, yangObjPath, yangNe);
//
//        } catch (InterruptedException | ExecutionException e) {
//            log.error("updateState error", e);
//            throw new SotnException(SotnException.TYPE_bad_parameter, e.toString());
//        }
    }

//    private void removeSystemInfo(NodeId nodeId) throws Exception {
//        InstanceIdentifier<System> neSystemPath = InstanceIdentifier.builder(NetworkTopology.class)
//                .build()
//                .child(Topology.class,
//                        new TopologyKey(new TopologyId(OtnPhyTopology.QNAME.getLocalName())))
//                .child(Node.class, new NodeKey(nodeId)).augmentation(Node1.class)
//                .child(Physical.class).child(System.class);
//        System nodeSystem = (System) this.netconfTopology.read(DataStoreType.CONFIG, neSystemPath);
//        if (nodeSystem != null) {
//            this.netconfTopology.put(DataStoreType.CONFIG, neSystemPath,
//                    new SystemBuilder().build());
////            yangTreeOp.put(LogicalDatastoreType.OPERATIONAL, neSystemPath, new SystemBuilder().build());
//        }
//    }

}
