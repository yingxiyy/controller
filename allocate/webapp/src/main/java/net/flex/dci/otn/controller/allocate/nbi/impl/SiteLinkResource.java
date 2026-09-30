package net.flex.dci.otn.controller.allocate.nbi.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.GetAllNeResourcesInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.GetAllNeResourcesOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.GetAllNeResourcesOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.get.all.ne.resources.output.Nodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.get.all.ne.resources.output.NodesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.get.all.ne.resources.output.NodesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class SiteLinkResource {
    private SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);
    private OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);
    private ChangedObject changedObject = new ChangedObject();


    public GetAllNeResourcesOutput getNes(GetAllNeResourcesInput input) {
        Set<Nodes> nodeList = new HashSet<>();

        input.getSiteLinkIds().forEach(siteLinkId -> {
            Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
            Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
            nodeList.addAll(getSiteLinkNes(siteLink));

            List<Link> ochLinkList = ochLinkDao.getAllBusinessOchLinksUnderSiteLinkIds(Arrays.asList(siteLinkId));
            ochLinkList.forEach(ochLink -> {
                nodeList.addAll(getOchLinkNes(ochLink, siteLinkAttr.getFriendlyName()));
            });
        });

        
        GetAllNeResourcesOutput output = new GetAllNeResourcesOutputBuilder()
                .setNodes(new ArrayList<>(nodeList)).build();
        return output;
    }

    private Set<Nodes> getOchLinkNes(Link ochLink, String siteLinkFriendlyName) {
        List<Route> routeList = ochLink
                .getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                .getOch().getExplictRoute().getRoute();

        List<String> nodeIdList = fetchNodeList(routeList);
        return fetchNodes(nodeIdList, siteLinkFriendlyName);
    }

    private Set<Nodes> getSiteLinkNes(Link siteLink) {
        List<Route> routeList = siteLink.getAugmentation(Link1.class).getSite().getExplictRoute().getRoute();

        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
        List<String> nodeIdList = fetchNodeList(routeList);
        return fetchNodes(nodeIdList, siteLinkAttr.getFriendlyName());
    }

    private Set<Nodes> fetchNodes(List<String> nodeIdList, String siteLinkFriendlyName) {
        return nodeIdList.parallelStream().map(nodeId -> {
            String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(nodeId);
            Node siteNode = changedObject.getChangedSiteNode(siteNodeId);
            String siteNodeFriendlyName = siteNode
                    .getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                    .getSite()
                    .getFriendlyName();

            Node node = changedObject.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            Properties prop;
            prop = PropertyTool.addProperty(null,"site-name", siteNodeFriendlyName);
            prop = PropertyTool.addProperty(prop,"siteLink-name", siteLinkFriendlyName);

            return new NodesBuilder()
                    .setNodeId(new NodeId(nodeId))
                    .setKey(new NodesKey(new NodeId(nodeId)))
                    .setPhysical(new PhysicalBuilder(nodeAttr)
                            .setCrossConnections(null)
                            .setOCMGripGroups(null)
                            .setEquipments(null)
                            .setDcn(null)
                            .setSystem(null)
                            .setCreationTime(null)
                            .setActivationTime(null)
                            .setProperties(prop)
                            .build())
                    .build();
        }).collect(Collectors.toSet());
    }

    private List<String> fetchNodeList(List<Route> routeList) {
        List<CrossConnectionAttributes> xcList = new ArrayList<>();
        routeList.forEach(route -> {
            xcList.addAll(route.getPrimary().getCrossConnections());

            if (route.getSecondary() != null) {
                xcList.addAll(route.getSecondary().getCrossConnections());
            }

            if (route.getThird() != null) {
                route.getThird().forEach(third -> {
                    if (third.getCrossConnections() != null) {
                        xcList.addAll(third.getCrossConnections());
                    }
                });
            }
        });

        return xcList.stream().map(x->{
            String xcId = x.getCrossConnectionId().getValue();
            String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
            return nodeId;
        }).collect(Collectors.toList());
    }
}
