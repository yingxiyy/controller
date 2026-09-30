/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.common.service;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;

/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
@Deprecated
public class NodeProperty {

    public final static String PropKey_ErrorMsg = "ErrorMsg";
    public final static String PropKey_Impl = "Impl";
    public final static String PropKey_DeImpl = "DeImpl";
    public final static String PropValue_Pending = "Pending";
    public final static String PropValue_Success = "Success";
    public final static String PropValue_Fail = "Fail";

    private boolean isImplement;

    public NodeProperty(boolean isImplement) {
        this.isImplement = isImplement;
    }


    /**
     * 比对输入的需要下发网元的信息，把OP树上的Impl/Deimpl/ErrorMsg 清除
     *
     * @param node
     * @throws CommonException
     */
    public void clean(NodeBuilder node) throws CommonException {
        try {
            clean(node.build());
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "internal error",
                    e);
        }
    }

    private void clean(Node node) throws CommonException {
        try {
            PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
            Node configNode = phyNodeDao.getConfigPhyNodeById(node.getNodeId().getValue());
            cleanProperties(configNode, node);
            ChangedObject changedObject = new ChangedObject();
            changedObject.addChangedPhyNode(configNode);
            MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
            mongoTransaction.save(changedObject);
        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "internal error",
                    e);
        }
    }

    private void cleanProperties(Node opNode, Node node) {
        opNode = wrapNode(opNode);

        cleanProperties(opNode.getAugmentation(Node1.class).getPhysical().getProperties());
        cleanTpProperties(opNode.getTerminationPoint(), node.getTerminationPoint());
        cleanEqProperties(opNode.getAugmentation(Node1.class).getPhysical().getEquipments(),
                node.getAugmentation(Node1.class).getPhysical().getEquipments());
        cleanXcProperties(
                opNode.getAugmentation(Node1.class).getPhysical().getCrossConnections(),
                node.getAugmentation(Node1.class).getPhysical().getCrossConnections());
        cleanIlProperties(opNode.getAugmentation(Node1.class).getPhysical().getInternalLinks(),
                node.getAugmentation(Node1.class).getPhysical().getInternalLinks());
    }

    /**
     * addToe propeties list when find it's null
     *
     * @param old
     * @return
     */
    private Node wrapNode(Node old) {
        List<TerminationPoint> tpList = new LinkedList<>();
        for (TerminationPoint tp : old.getTerminationPoint()) {
            tpList.add(wrapTp(tp));
        }
        List<Equipments> eqList = new LinkedList<>();
        for (Equipments eq : old.getAugmentation(Node1.class).getPhysical().getEquipments()) {
            eqList.add(wrapEquip(eq));
        }
        List<CrossConnections> xcList = new LinkedList<>();
        for (CrossConnections xc : old.getAugmentation(Node1.class).getPhysical()
                .getCrossConnections()) {
            xcList.add(wrapCrossConnection(xc));
        }
        List<InternalLinks> ilList = new LinkedList<>();
        for (InternalLinks il : old.getAugmentation(Node1.class).getPhysical().getInternalLinks()) {
            ilList.add(wrapInternalLink(il));
        }
        NodeBuilder nb = new NodeBuilder(old);
        PhysicalBuilder pb = new PhysicalBuilder(old.getAugmentation(Node1.class).getPhysical());
        if (old.getAugmentation(Node1.class).getPhysical().getProperties() == null ||
                old.getAugmentation(Node1.class).getPhysical().getProperties().getProperty()
                        == null) {
            pb.setProperties(new PropertiesBuilder()
                    .setProperty(new LinkedList<Property>())
                    .build());
        }
        pb.setInternalLinks(ilList)
                .setCrossConnections(xcList)
                .setEquipments(eqList);
        nb.setTerminationPoint(tpList)
                .addAugmentation(Node1.class,
                        new Node1Builder(old.getAugmentation(Node1.class))
                                .setPhysical(pb.build()).build());

        return nb.build();
    }

    private InternalLinks wrapInternalLink(InternalLinks old) {
        InternalLinksBuilder ilb = new InternalLinksBuilder(old);
        if (ilb.getProperties() == null || ilb.getProperties().getProperty() == null) {
            ilb.setProperties(new PropertiesBuilder()
                    .setProperty(new LinkedList<Property>())
                    .build());
        }
        return ilb.build();
    }

    private CrossConnections wrapCrossConnection(CrossConnections old) {
        CrossConnectionsBuilder cb = new CrossConnectionsBuilder(old);
        if (cb.getProperties() == null || cb.getProperties().getProperty() == null) {
            cb.setProperties(new PropertiesBuilder()
                    .setProperty(new LinkedList<Property>())
                    .build());
        }
        return cb.build();
    }

    private Equipments wrapEquip(Equipments old) {
        EquipmentsBuilder eb = new EquipmentsBuilder(old);
        if (eb.getProperties() == null || eb.getProperties().getProperty() == null) {
            eb.setProperties(new PropertiesBuilder()
                    .setProperty(new LinkedList<Property>())
                    .build());
        }
        return eb.build();
    }

    private TerminationPoint wrapTp(TerminationPoint old) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder pb = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                old.getAugmentation(TerminationPoint1.class).getPhysical());
        if (old.getAugmentation(TerminationPoint1.class).getPhysical().getProperties() == null ||
                old.getAugmentation(TerminationPoint1.class).getPhysical().getProperties()
                        .getProperty() == null) {
            pb.setProperties(new PropertiesBuilder()
                    .setProperty(new LinkedList<Property>())
                    .build());
        }
        TerminationPointBuilder tb = new TerminationPointBuilder(old)
                .addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder(old.getAugmentation(TerminationPoint1.class))
                                .setPhysical(pb.build()).build());
        return tb.build();
    }

    private void cleanIlProperties(List<InternalLinks> opTree, List<InternalLinks> updateTree) {
        for (InternalLinks newData : updateTree) {
            for (InternalLinks opData : opTree) {
                if (newData.getLinkRef().equals(opData.getLinkRef())) {
                    cleanProperties(opData.getProperties());
                    break;
                }
            }
        }
    }

    private void cleanXcProperties(List<CrossConnections> opTree,
            List<CrossConnections> updateTree) {
        for (CrossConnections newData : updateTree) {
            for (CrossConnections opData : opTree) {
                if (newData.getCrossConnectionId().getValue()
                        .equals(opData.getCrossConnectionId().getValue())) {
                    cleanProperties(opData.getProperties());
                    break;
                }
            }
        }
    }

    private void cleanEqProperties(List<Equipments> opTree, List<Equipments> updateTree) {
        for (Equipments newData : updateTree) {
            for (Equipments opData : opTree) {
                if (newData.getEquipmentId().equals(opData.getEquipmentId())) {
                    cleanProperties(opData.getProperties());
                    break;
                }
            }
        }
    }

    private void cleanTpProperties(List<TerminationPoint> opTree,
            List<TerminationPoint> updateTree) {
        for (TerminationPoint newData : updateTree) {
            for (TerminationPoint opData : opTree) {
                if (newData.getTpId().getValue().equals(opData.getTpId().getValue())) {
                    cleanProperties(opData.getAugmentation(TerminationPoint1.class).getPhysical()
                            .getProperties());
                    break;
                }
            }
        }
    }

    private void cleanProperties(Properties properties) {

        Iterator<Property> iter = properties.getProperty().iterator();
        while (iter.hasNext()) {
            Property pro = iter.next();
            if (pro.getName().equals(PropKey_ErrorMsg) ||
                    pro.getName().equals(PropKey_Impl) ||
                    pro.getName().equals(PropKey_DeImpl)) {
                iter.remove();
            }
        }
        properties.getProperty().add(new PropertyBuilder()
                .setKey(new PropertyKey(isImplement ? PropKey_Impl : PropKey_DeImpl))
                .setValue(PropValue_Pending)
                .build());
    }

}
