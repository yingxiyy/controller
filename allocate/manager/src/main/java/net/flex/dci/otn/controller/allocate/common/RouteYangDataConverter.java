package net.flex.dci.otn.controller.allocate.common;

import static net.flex.dci.otn.controller.allocate.designer.namingrule.NEIdGenerator.getCurrentTime;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.NonNull;
import net.flex.dci.otn.controller.allocate.designer.model.Route;
import net.flex.dci.otn.controller.allocate.designer.model.RouteInfo;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NEIdGenerator;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NameGenerator;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlignmentStatusType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.compute.result.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.Links;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.LinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.LinksKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.Nodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.link.route.NodesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.network.sitelinks.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.phy.ne.full.info.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.phy.ne.full.info.TerminationPointBuilder;

public class RouteYangDataConverter {

    public static List<Link> getLinks(List<Links> links) {
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

    public static List<TerminationPoint> getRouteTps(
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
    public static Node getNode(Nodes node) {
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

    public static Third getThird(Route thirdInfo) {
        if (thirdInfo == null) {
            return null;
        }
        return new ThirdBuilder().setCrossConnections(thirdInfo.getXcs()).setLinks(getRouteLinks(thirdInfo.getLinks())).setNodes(getRouteNodes(thirdInfo.getNodes())).build();
    }

    public static RouteInfo getRouteInfo(Main main, Slave slave, Third third) {

        @NonNull Route mainRoute = getMainRoute(main);
        Route slaveRoute = getSlaveRoute(slave);
        Route thirdRoute = getThirdRoute(third);
        return RouteInfo.builder().main(mainRoute).slave(slaveRoute).third(thirdRoute).build();
    }

    private static Route getThirdRoute(Third third) {
        if (third == null) {
            return null;
        }
        return getRoute(third.getNodes(), third.getCrossConnections(), third.getLinks());
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
    public static List<WssLinks> convertToOutputWsslinks(List<Link> wssLinks) {
        List<WssLinks> result = new ArrayList<>();
        for (Link link : wssLinks) {
            result.add(new WssLinksBuilder(link).setPhysical(link.getAugmentation(Link1.class).getPhysical()).build());
        }
        return result;
    }

    public static SiteLinks convertToOutputSitelink(RouteInfo routeInfo, String friendlyName) {
        // main
        @NonNull Route mainInfo = routeInfo.getMain();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.network.sitelinks.site.links.Main main =
                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.network.sitelinks.site.links.MainBuilder().setCrossConnections(mainInfo.getXcs())
                        .setLinks(getRouteLinks(mainInfo.getLinks())).setNodes(getRouteNodes(mainInfo.getNodes())).build();
        //slave
        Route slaveInfo=routeInfo.getSlave();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.network.sitelinks.site.links.Slave slave=null;
        if(slaveInfo!=null){
            slave= new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.network.sitelinks.site.links.SlaveBuilder().setCrossConnections(slaveInfo.getXcs())
                    .setLinks(getRouteLinks(slaveInfo.getLinks())).setNodes(getRouteNodes(slaveInfo.getNodes())).build();
        }

        //third
        Route thirdInfo=routeInfo.getThird();
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.network.sitelinks.site.links.Third third=null;
        if(thirdInfo!=null){
            third= new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.network.sitelinks.site.links.ThirdBuilder().setCrossConnections(thirdInfo.getXcs())
                    .setLinks(getRouteLinks(thirdInfo.getLinks())).setNodes(getRouteNodes(thirdInfo.getNodes())).build();
        }

        return new SiteLinksBuilder().setKey(new SiteLinksKey(friendlyName))
                .setFriendlyName(friendlyName)
                .setMain(main)
                .setSlave(slave)
                .setThird(third).build();

    }
}
