package net.flex.dci.otn.controller.implement.service.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.ThirdBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class CheckAllXcImpl {
    private PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
    private OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);

    private ChangedObject changedObject = new ChangedObject();
    private final static MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(
            MultipleTransaction.class);
    //
    public void start() {
        checkNode();
        checkOchLink();

        synchronized (mongoTransaction) {
            mongoTransaction.save(changedObject);
        }
    }

    private void checkNode() {
        phyNodeDao.listPhyNodes().forEach(node -> {
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            log.info("check phy node {}({})", node.getNodeId(), nodeAttr.getIp());

            List<CrossConnections> newXcList = nodeAttr.getCrossConnections().stream()
                    .filter(xc -> {
                        if (xc.getCrossConnectionId().getValue().endsWith("_1")) {
                            return false;
                        }
                        return true;
                    }).collect(Collectors.toList());

            if (nodeAttr.getCrossConnections().size() != newXcList.size()) {
                log.warn("find xc with _1 on node");
                Node newNode = new NodeBuilder(node)
                        .addAugmentation(Node1.class, new Node1Builder()
                                .setPhysical(new PhysicalBuilder(nodeAttr)
                                        .setCrossConnections(newXcList)
                                        .build())
                                .build())
                        .build();
                changedObject.addChangedPhyNode(newNode);
            }

            synchronized (mongoTransaction) {
                mongoTransaction.save(changedObject);
            }
        });
    }

    private void checkOchLink() {
        changedObject.getChangedPhyNodeList().keySet().forEach(nodeId -> {
            List<Link> ochLinks = ochLinkDao.queryWithNode(nodeId);
            ochLinks.forEach(ochLink -> {
                Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
                Route route = ochLinkAttr.getExplictRoute().getRoute().get(0);

                boolean changeHappen = false;
                List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> pXcs =
                        checkingRouteXc(route.getPrimary().getCrossConnections());
                if (pXcs.size() != route.getPrimary().getCrossConnections().size()) {
                    changeHappen = true;
                }
                List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> sXcs = null;
                if (route.getSecondary() != null) {
                    sXcs = checkingRouteXc(route.getSecondary().getCrossConnections());
                    if (sXcs.size() != route.getSecondary().getCrossConnections().size()) {
                        changeHappen = true;
                    }
                }
                List<Third> tXcList = null;
                if (route.getThird() != null) {
                    tXcList = new ArrayList<>();
                    for (Third t : route.getThird()) {
                        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> tXcs= null;
                        tXcs = checkingRouteXc(t.getCrossConnections());
                        if (tXcs.size() != t.getCrossConnections().size()) {
                            t = new ThirdBuilder(t)
                                    .setCrossConnections(tXcs)
                                    .build();
                            changeHappen = true;
                        }
                        tXcList.add(t);
                    }
                }
                if (changeHappen) {
                    Link newLink = new LinkBuilder(ochLink)
                            .addAugmentation(Link1.class, new Link1Builder()
                                    .setOch(new OchBuilder(ochLinkAttr)
                                            .setExplictRoute(new ExplictRouteBuilder(ochLinkAttr.getExplictRoute())
                                                    .setRoute(Arrays.asList(
                                                            new RouteBuilder(route)
                                                                    .setPrimary(new PrimaryBuilder(route.getPrimary())
                                                                            .setCrossConnections(pXcs)
                                                                            .build())
                                                                    .setSecondary(route.getSecondary() == null ? null :
                                                                            new SecondaryBuilder(route.getSecondary())
                                                                                    .setCrossConnections(sXcs)
                                                                                    .build())
                                                                    .setThird(route.getThird() == null ? null :
                                                                            tXcList)
                                                                    .build()
                                                    )).build())
                                            .build())
                                    .build())
                            .build();

                    changedObject.addChangedOchLink(newLink);
                }
            });
        });
    }

    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> checkingRouteXc(List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> originalXcs) {
        return originalXcs.stream().map(xc -> {
            if (xc.getCrossConnectionId().getValue().endsWith("_1")) {
                return null;
            } else {
                return xc;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }
}
