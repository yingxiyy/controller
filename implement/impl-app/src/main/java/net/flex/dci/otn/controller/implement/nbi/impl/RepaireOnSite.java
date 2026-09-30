package net.flex.dci.otn.controller.implement.nbi.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
public class RepaireOnSite {
    ChangedObject changedObject = new ChangedObject();
    MultipleTransaction multipleTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
    SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);
    OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);

    List<String> changedNodeList;

    public String start() {
        changedNodeList = new ArrayList<>();
        List<Link> siteLinkList = siteLinkDao.getSiteLinks();

        List<String> changedXcList = new ArrayList<>();
        for (Link siteLink : siteLinkList) {
            String siteLinkId = siteLink.getLinkId().getValue();

            List<Link> ochLinkList = ochLinkDao.getAllBusinessOchLinksUnderSiteLinkIds(Arrays.asList(siteLinkId));
            for (Link ochLink : ochLinkList) {
                changedXcList.addAll(checkingOch(ochLink));
            }
        }
        try {
            writeListToFile(changedXcList);
        } catch (IOException e) {
            log.error("write file failed", e);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "write file failed " + e.getMessage());
        }

        Set<String> changedSet = new HashSet<>(changedNodeList);
        changedObject.getChangedPhyOpNodeList().keySet().removeIf(key -> !changedSet.contains(key));

        multipleTransaction.save(changedObject);
        return "done";
    }

    private List<String> checkingOch(Link ochLink) {
        List<String> changedXcList = new ArrayList<>();

        RouteInfo rInfo = new RouteInfo();
        rInfo.parse(ochLink.getAugmentation(Link1.class).getOch().getExplictRoute().getRoute());

        rInfo.getXcIdList().forEach(xcId->{
            String newXc = checkingXC(xcId, ochLink);
            if (null != newXc) {
                changedXcList.add(newXc);
            }
        });
        return changedXcList;
    }

    private String checkingXC(String xcId, Link ochLink) {
        String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
        Node node = changedObject.getChangedPhyNode(nodeId);

        List<CrossConnections> xcList = node.getAugmentation(Node1.class).getPhysical().getCrossConnections();
        CrossConnections xc = xcList.stream().filter(x -> x.getCrossConnectionId().getValue().equals(xcId)).findAny().orElse(null);
        if (xc == null) {
            log.error("find Node has not this XC {}", xcId);
            return updateNode(nodeId, xcId, ochLink);
        }
        //这个网元有对应XC
        return null;
    }

    private String updateNode(String nodeId, String xcId, Link ochLink) {
        Node node = changedObject.getChangedPhyNode(nodeId);

        CrossConnectionAttributes xc = fetchXcFromOchRoute(ochLink, xcId);
        if (xc == null) {
            log.error("the xc isn't existed in OCH link route {}, {}", xcId, ochLink.getLinkId().getValue());
            return null;
        } else {
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            List<CrossConnections> nodeXcList = nodeAttr.getCrossConnections();

            nodeXcList.add(new CrossConnectionsBuilder(xc).build());

            Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                    .setPhysical(new PhysicalBuilder(nodeAttr)
                            .setCrossConnections(nodeXcList)
                            .build())
                    .build())
                    .build();
            changedNodeList.add(nodeId);
            changedObject.addChangedPhyNode(newNode);
        }
        return xcId;
    }

    private CrossConnectionAttributes fetchXcFromOchRoute(Link ochLink, String xcId) {
        List<CrossConnectionAttributes> ochXcList = new ArrayList<>();

        List<Route> routeList = ochLink.getAugmentation(Link1.class).getOch().getExplictRoute().getRoute();
        for (Route route : routeList) {
            ochXcList.addAll(route.getPrimary().getCrossConnections());

            if (route.getSecondary() != null) {
                ochXcList.addAll(route.getSecondary().getCrossConnections());
            }

            if (route.getThird() != null) {
                route.getThird().forEach(t->{
                    ochXcList.addAll(t.getCrossConnections());
                });
            }
        }

        return ochXcList.stream().filter(x -> x.getCrossConnectionId().getValue().equals(xcId)).findAny().orElse(null);
    }

    private void writeListToFile(List<String> list) throws IOException {
        String filePath = "./changedXcList.txt";
        Path path = Paths.get(filePath);
        Files.write(path, list);
    }

}
