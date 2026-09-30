package net.flex.dci.otn.controller.allocate.link.tunnel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import net.flex.dci.otn.controller.allocate.designer.model.Route;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.phy.ne.full.info.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.phy.ne.full.info.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.route.detail.info.route.info.Secondary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.ReusedNodesSnapshot;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.ReusedNodesSnapshotBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.ReusedNodesSnapshotKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result.vendor.RouteInfos;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result.vendor.RouteInfosBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result.vendor.route.infos.Main;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result.vendor.route.infos.MainBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result.vendor.route.infos.Slave;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result.vendor.route.infos.SlaveBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.Links;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.LinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.LinksKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.Nodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.route.NodesKey;

public class RouteYangDataConverter {

    private static List<Link> getLinks(List<Links> links) {
        if (links == null) {
            return null;
        }

        return links.stream().map(item -> getLink(item)).collect(Collectors.toList());
    }

    /**
     * 转换成正常通用的link
     *
     * @param link
     * @return
     */
    private static Link getLink(Links link) {
        return new LinkBuilder(link).addAugmentation(Link1.class, new Link1Builder()
                .setPhysical(link.getPhysical()).build()).build();
    }

    public static List<Links> getRouteLinks(List<Link> links) {
        if (links == null) {
            return null;
        }

        return links.stream().map(item -> getRouteLink(item)).collect(Collectors.toList());
    }

    /**
     * convert from: network-topology@2013-10-21.yang/network-topology/link to:  site-topology.yang/link-route/links
     *
     * @param link
     * @return
     */
    private static Links getRouteLink(Link link) {
        return new LinksBuilder().setLinkId(link.getLinkId())
                .setKey(new LinksKey(link.getKey().getLinkId()))
                .setSource(link.getSource())
                .setDestination(link.getDestination())
                .setPhysical(link.getAugmentation(Link1.class).getPhysical())
                .build();
    }

    /**
     * convert from: network-topology@2013-10-21.yang/network-topology/node to:  site-topology.yang/link-route/nodes
     *
     * @param node
     * @return
     */
    private static Nodes getRouteNode(Node node) {

        Nodes result = new NodesBuilder()
                .setNodeId(node.getNodeId())
                .setKey(new NodesKey(node.getNodeId()))
                .setPhysical(node.getAugmentation(Node1.class).getPhysical())
                .setTerminationPoint(getRouteTps(node.getTerminationPoint()))
                .build();
        return result;
    }

    private static List<TerminationPoint> getRouteTps(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint> tps) {
        if (tps == null) {
            return null;
        }

        return tps.stream().map(item -> getRouteTp(item)).collect(Collectors.toList());
    }

    /**
     * 转换成通用的tp
     *
     * @param tps
     * @return
     */
    private static List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint> getTps(
            List<TerminationPoint> tps) {
        if (tps == null) {
            return null;
        }

        return tps.stream().map(item -> getTp(item)).collect(Collectors.toList());
    }

    /**
     * 转换成正常TP
     *
     * @param tp
     * @return
     */
    private static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint getTp(TerminationPoint tp) {

        TerminationPoint1Builder terminationPoint1Builder = new TerminationPoint1Builder().setPhysical(tp.getPhysical());
        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder(tp)
                .addAugmentation(TerminationPoint1.class, terminationPoint1Builder.build()).build();
    }

    /**
     * Convert from org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint
     *
     * to  org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.phy.ne.full.info.TerminationPoint
     *
     * @param tp
     * @return
     */
    private static TerminationPoint getRouteTp(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint tp) {
        PhysicalBuilder physicalBuilder = new PhysicalBuilder(tp.getAugmentation(TerminationPoint1.class).getPhysical());

        return new TerminationPointBuilder(tp).setPhysical(physicalBuilder.build()).build();
    }


    public static List<Nodes> getRouteNodes(List<Node> nodes) {
        if (nodes == null) {
            return null;
        }

        return nodes.stream().map(item -> getRouteNode(item)).collect(Collectors.toList());
    }

    public static List<Node> getNodes(List<Nodes> nodes) {
        if (nodes == null) {
            return null;
        }

        return nodes.stream().map(item -> getNode(item)).collect(Collectors.toList());
    }


    /**
     * convert from  site-topology.yang/link-route/nodes to network-topology@2013-10-21.yang/network-topology/node
     *
     * 转化成通用的node
     *
     * @param node
     * @return
     */
    private static Node getNode(Nodes node) {
        return new NodeBuilder(node)
                .setNodeId(node.getNodeId())
                .setKey(new NodeKey(node.getNodeId()))
                .addAugmentation(Node1.class, new Node1Builder().setPhysical(node.getPhysical()).build())
                .setTerminationPoint(getTps(node.getTerminationPoint()))
                .build();
    }

    public static Main getMain(Route mainInfo) {
        return new MainBuilder().setCrossConnections(mainInfo.getXcs()).setLinks(getRouteLinks(mainInfo.getLinks())).setNodes(getRouteNodes(mainInfo.getNodes())).build();

    }

    public static Slave getSlave(Route slaveInfo) {
        if (slaveInfo == null) {
            return null;
        }
        return new SlaveBuilder().setCrossConnections(slaveInfo.getXcs()).setLinks(getRouteLinks(slaveInfo.getLinks())).setNodes(getRouteNodes(slaveInfo.getNodes())).build();
    }

    public static RouteInfo getRouteInfo(Main main, Slave slave) {

        @NonNull Route mainRoute = getMainRoute(main);
        Route slaveRoute = getSlaveRoute(slave);
        return RouteInfo.builder().main(mainRoute).slave(slaveRoute).build();
    }

    private static Route getSlaveRoute(Slave slave) {
        if (slave == null) {
            return null;
        }
        return getRoute(slave.getNodes(), slave.getCrossConnections(), slave.getLinks());
    }

    private static Route getMainRoute(Main main) {
        return getRoute(main.getNodes(), main.getCrossConnections(), main.getLinks());
    }

    private static Route getRoute(List<Nodes> nodes, List<CrossConnections> crossConnections, List<Links> links) {
        return Route.builder().nodes(getNodes(nodes)).links(getLinks(links)).xcs(crossConnections).build();
    }


    public static RouteInfos getRouteInfo(RouteInfo info, int index) {
        @NonNull Main main = getMain(info.getMain());
        Slave slave = getSlave(info.getSlave());
        return new RouteInfosBuilder()
                .setIndex(String.valueOf(index))
                .setMain(main)
                .setSlave(slave).build();
    }

    public static Primary convertPrimary(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary primary) {
        return null;
    }

    public static Secondary convertSecondary(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Secondary secondary) {
        //todo:目前还没得，暂时返回null
        return null;
    }

    public static List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.info.RouteInfos> convertRounteInfos(
            List<RouteInfo> routeInfos) {
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.info.RouteInfos> output = new ArrayList<>();
        if (routeInfos == null) {
            return Collections.EMPTY_LIST;
        }
        int size = routeInfos.size();
        for (int i = 0; i < size; i++) {
            RouteInfo routeInfo = routeInfos.get(i);
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.info.route.infos.MainBuilder mainBuilder = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.info.route.infos.MainBuilder()
                    .setCrossConnections(routeInfo.getMain().getXcs())
                    .setLinks(getRouteLinks(routeInfo.getMain().getLinks()))
                    .setNodes(getRouteNodes(routeInfo.getMain().getNodes()));

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.info.RouteInfos r = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.route.info.RouteInfosBuilder()
                    .setIndex(String.valueOf(i))
                    .setMain(mainBuilder.build()).build();
            output.add(r);
        }
        return output;
    }

    public static ReusedNodesSnapshot getReusedNodesSnapshot(Node node) {
        return new ReusedNodesSnapshotBuilder().setNodeId(node.getNodeId())
                .setKey(new ReusedNodesSnapshotKey(node.getNodeId()))
                .setPhysical(node.getAugmentation(Node1.class).getPhysical())
                .setTerminationPoint(getRouteTps(node.getTerminationPoint())).build();
    }

    public static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.Nodes getTunnelNodes(Node node) {
        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.NodesBuilder(node)
                .setNodeId(node.getNodeId())
                .setKey(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.allocate.result._2.NodesKey(node.getNodeId()))
                .setPhysical(node.getAugmentation(Node1.class).getPhysical())
                .setTerminationPoint(getRouteTps(node.getTerminationPoint())).build();
    }
}
