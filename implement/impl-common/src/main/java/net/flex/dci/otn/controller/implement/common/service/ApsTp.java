/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.common.service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;

@Slf4j
@Data
public class ApsTp {

    private ImplementState target;

    public ApsTp(ImplementState target) {
        this.target = target;
    }

    private boolean containsInPassTps(String tpId, List<TerminationPoint> passTps) {
        for (TerminationPoint passTp : passTps) {
            if (tpId.equals(passTp.getTpId().getValue())) {
                return true;
            }
        }
        return false;
    }

    public List<NodeBuilder> setApsTp(List<NodeBuilder> phyNodeList)
            throws CommonException {
        List<NodeBuilder> nodeBuilders = new ArrayList<NodeBuilder>();
        for (NodeBuilder node : phyNodeList) {
            if (node.getTerminationPoint() != null && !node.getTerminationPoint().isEmpty()
                    && node.getAugmentation(Node1.class).getPhysical().getCrossConnections() != null
                    && !node.getAugmentation(Node1.class).getPhysical().getCrossConnections()
                    .isEmpty()) {
                List<String> tpIds = new ArrayList<String>();
                for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections xc : node
                        .getAugmentation(Node1.class).getPhysical().getCrossConnections()) {
                    //setApsTp for cmux b
                    if (xc.getAps() != null && xc.getAps().getApsMode() != null) {

                        for (SourceTp srcTp : xc.getSourceTp()) {
                            if (!containsInPassTps(srcTp.getTpRef().getValue(),
                                    node.getTerminationPoint())) {
                                tpIds.add(srcTp.getTpRef().getValue());
                            }
                        }
                        for (DestinationTp desTp : xc.getDestinationTp()) {
                            if (!containsInPassTps(desTp.getTpRef().getValue(),
                                    node.getTerminationPoint())) {
                                tpIds.add(desTp.getTpRef().getValue());
                            }
                        }
                    }
                }

                if (tpIds.size() > 0) {
                    NodeBuilder newNode = new NodeBuilder().setNodeId(node.getNodeId())
                            .setTerminationPoint(new ArrayList<TerminationPoint>());
                    for (String tpId : tpIds) {
                        TerminationPoint newTp = newTp(node.getNodeId().getValue(), tpId);
                        if (newTp != null) {
                            newNode.getTerminationPoint().add(newTp);
                        }
                    }
                    nodeBuilders.add(newNode);
                }
            }
        }
        return nodeBuilders;
    }

    private TerminationPoint newTp(String nodeId, String tpId) throws CommonException {
        TerminationPointDao mongoDaoUtil = SpringBeanFinder.getBean(TerminationPointDao.class);
        TerminationPoint tp = mongoDaoUtil
                .getConfigPhyTpByNeIdAndTpId(nodeId, tpId);
        if (tp == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot find TP used by route " + tpId);
        }

        if (target.equals(tp.getAugmentation(TerminationPoint1.class).getPhysical()
                .getImplementState())) {
            return null;
        }

        PhysicalBuilder tpBuilder = new PhysicalBuilder(
                tp.getAugmentation(TerminationPoint1.class).getPhysical());
        if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getProperties() != null) {
            tpBuilder
                    .setProperties(new PropertiesBuilder().setProperty(new LinkedList<>()).build());
        }
        if (target.equals(ImplementState.Implement)) {
            tpBuilder.setAdminState(AdminStatus.Up)
                    .setImplementState(ImplementState.Doimplementing)
                    .build();
        } else {
            tpBuilder.setAdminState(AdminStatus.Down)
                    .setImplementState(ImplementState.Deimplementing)
                    .build();
        }
        return new TerminationPointBuilder()
                .setTpId(new TpId(tpId))
                .setKey(new TerminationPointKey(new TpId(tpId)))
                .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                        .setPhysical(tpBuilder.build())
                        .build())
                .build();
    }
}
