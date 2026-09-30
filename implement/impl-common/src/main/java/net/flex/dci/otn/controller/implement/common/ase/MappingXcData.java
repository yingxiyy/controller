package net.flex.dci.otn.controller.implement.common.ase;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.SecondaryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.ThirdBuilder;
import org.springframework.util.CollectionUtils;

/**
 * Adding business OCH:   [191400000,191475000] remove dummy och (2), [[191350000,191450000],
 * [191450000,191550000]] add dummy och (2),    [[191350000,191400000], [191475000,191525000]]
 *
 * 这种情况下可以看出removed och 的中心频率 和 将要添加的假波的中心频率一样都是  191500000
 *
 * 这个方法在dummyOch 删除成功后调用， 把_1 的交叉ID 换成正常不带_1 的xcID
 */
@Slf4j
public class MappingXcData {

    private final ChangedObject changedObject;
    private final Link ochLink;

    public MappingXcData(ChangedObject changedObject, Link ochLink) {
        this.changedObject = changedObject;
        this.ochLink = ochLink;
    }

    public boolean convert() {
        RouteInfo rInfo = new RouteInfo();
        rInfo.parse(ochLink.getAugmentation(Link1.class).getOch().getExplictRoute().getRoute());
        boolean shouldConvert = rInfo.getXcIdList().stream().anyMatch(xc -> xc.endsWith("_1"));

        if (shouldConvert) {
            log.debug("find duplicatedXC naming, need convert naming to default");
        }

        if (shouldConvert) {
            for (String xcId : rInfo.getXcIdList()) {
                String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
                changeNodeXc(nodeId, xcId);
            }
        }

        if (shouldConvert) {
            changeOchRouteXc();
        }
        return shouldConvert;
    }

    private void changeOchRouteXc() {
        Och linkAttr = ochLink.getAugmentation(Link1.class).getOch();
        List<Route> newRoute = linkAttr.getExplictRoute().getRoute().stream()
                .map(this::processRoute).collect(Collectors.toList());

        Link newOchLink = new LinkBuilder(ochLink).addAugmentation(Link1.class, new Link1Builder()
                        .setOch(new OchBuilder(linkAttr)
                                .setExplictRoute(new ExplictRouteBuilder(linkAttr.getExplictRoute())
                                        .setRoute(newRoute)
                                        .build())
                                .build())
                        .build())
                .build();

        changedObject.addChangedOchLink(newOchLink);
    }

    private Route processRoute(Route route) {
        RouteBuilder routeBuilder = new RouteBuilder();
        routeBuilder.setPrimary(new PrimaryBuilder(route.getPrimary())
                .setCrossConnections(processRouteXC(route.getPrimary().getCrossConnections()))
                .build());
        if (route.getSecondary() != null) {
            routeBuilder.setSecondary(new SecondaryBuilder(route.getSecondary())
                    .setCrossConnections(processRouteXC(route.getSecondary().getCrossConnections()))
                    .build());
        }
        if (!CollectionUtils.isEmpty(route.getThird())) {
            routeBuilder.setThird(getThirdRoute(route.getThird()));
        }
        //        Route newRoute = new RouteBuilder(route)
//                .setPrimary(new PrimaryBuilder(route.getPrimary())
//                        .setCrossConnections(processRouteXC(route.getPrimary().getCrossConnections()))
//                        .build())
//                .setSecondary(new SecondaryBuilder(route.getSecondary())
//                        .setCrossConnections(processRouteXC(route.getSecondary().getCrossConnections()))
//                        .build())
//                .setThird(getThirdRoute(route.getThird()))
//                .build();

        return routeBuilder.build();
    }

    private List<Third> getThirdRoute(List<Third> third) {
        List<Third> newThird = third.stream().map(th -> {
            return new ThirdBuilder(th).setCrossConnections(
                    processRouteXC(th.getCrossConnections())).build();
        }).collect(Collectors.toList());

        return newThird;
    }

    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> processRouteXC(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> crossConnections) {
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections> newXCs;
        newXCs = crossConnections.stream().map(xc -> {
            String xcId = xc.getCrossConnectionId().getValue();
            if (xcId.endsWith("_1")) {
                String correctXcId = xcId.substring(0, xcId.length() - 2);
                return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder(
                        xc)
                        .setCrossConnectionId(new Uri(correctXcId))
                        .build();
            }
            return xc;
        }).collect(Collectors.toList());

        return newXCs;
    }

    /**
     *
     * @param nodeId
     * @param xcId
     * @return when original XC still working, this throw exception;
     */
    private void changeNodeXc(String nodeId, String xcId) {
        Node node = changedObject.getChangedPhyNode(nodeId);
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

        //checking at first, check does the convert should be changed.
        if (xcId.endsWith("_1")) {
            log.debug("change this into correct {}", xcId);
            String correctXcId = xcId.substring(0, xcId.length() - 2);
            boolean existed = nodeAttr.getCrossConnections().stream().anyMatch(
                    xc -> xc.getCrossConnectionId().getValue().equalsIgnoreCase(correctXcId));
            if (existed) {
                String msg = String.format("I want to convert the duplicate xcId to correct, but find the corrected still used %s",
                        xcId);
                log.error(msg);
                throw new RuntimeException(msg);
            }

            boolean shouldConvert = nodeAttr.getCrossConnections().stream()
                    .anyMatch(xc -> xc.getCrossConnectionId().getValue().equalsIgnoreCase(xcId));
            if (shouldConvert) {
                List<CrossConnections> newXcList = nodeAttr.getCrossConnections().stream()
                        .map(xc -> {
                            if (xc.getCrossConnectionId().getValue().equalsIgnoreCase(xcId)) {
                                return new CrossConnectionsBuilder(xc)
                                        .setCrossConnectionId(new Uri(correctXcId))
                                        .setKey(new CrossConnectionsKey(new Uri(correctXcId)))
                                        .build();
                            }
                            return xc;
                        }).collect(Collectors.toList());

                Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                                .setPhysical(new PhysicalBuilder(nodeAttr)
                                        .setCrossConnections(newXcList)
                                        .build())
                                .build())
                        .build();

                changedObject.addChangedPhyNode(newNode);
            }
        }
    }
}
