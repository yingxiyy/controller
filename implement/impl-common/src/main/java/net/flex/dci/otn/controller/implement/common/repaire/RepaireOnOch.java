package net.flex.dci.otn.controller.implement.common.repaire;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Third;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class RepaireOnOch {
    private ChangedObject changedObject = new ChangedObject();
    private NeManagerRpc neMgr = SpringBeanFinder.getBean(NeManagerRpc.class);
    private MultipleTransaction multipleTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
    private OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);


    public String start(String ochLinkId) {
        List<CrossConnectionAttributes> xcList = new ArrayList<>();

        Link ochLink = ochLinkDao.getOchLinkByLinkId(ochLinkId);
        xcList.addAll(fetchXcs(ochLink));

        return updateXcInNode(xcList);
    }

    public String allStart() {
        List<Link> ochLinkList = ochLinkDao.listOchLinks();
        List<CrossConnectionAttributes> xcList = new ArrayList<>();

        ochLinkList.forEach(ochLink -> {
            xcList.addAll(fetchXcs(ochLink));
        });

        return updateXcInNode(xcList);
    }

    private String updateXcInNode(List<CrossConnectionAttributes> xcList) {
        Map<String, List<CrossConnectionAttributes>> grouped =
                xcList.stream()
                        .filter(xc -> xc.getNodeRef() != null)
                        .collect(Collectors.groupingBy(xc -> xc.getNodeRef().getValue()));

        grouped.keySet().parallelStream().forEach(neId-> {
            Node node = changedObject.getChangedPhyNode(neId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            List<CrossConnections> missingXcList = new ArrayList<>();
            List<CrossConnectionAttributes> xcsInRoute = grouped.get(neId);
            for (CrossConnectionAttributes xcAttr : xcsInRoute) {
                CrossConnections xcInNe = nodeAttr.getCrossConnections().stream()
                        .filter(xc -> xc.getCrossConnectionId().getValue().equals(xcAttr.getCrossConnectionId().getValue()))
                        .findAny().orElse(null);
                if (xcInNe == null) {
                    missingXcList.add(new CrossConnectionsBuilder(xcAttr).build());
                }
            }
            if (!missingXcList.isEmpty()) {
                log.info("find missing XC {}, and insert it into cfgNode", missingXcList);

                List<CrossConnections> wholeXcList = new ArrayList<>(nodeAttr.getCrossConnections());
                wholeXcList.addAll(missingXcList);

                Node newNode = new NodeBuilder(node)
                        .addAugmentation(Node1.class, new Node1Builder()
                                .setPhysical(new PhysicalBuilder(nodeAttr)
                                        .setCrossConnections(wholeXcList)
                                        .build())
                                .build())
                        .build();
                changedObject.addChangedPhyNode(newNode);
            }
        });

        multipleTransaction.save(changedObject);
        return "done";
    }

    private List<CrossConnectionAttributes> fetchXcs(Link ochLink) {
        List<CrossConnectionAttributes> xcList = new ArrayList<>();

        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
        for (Route route : ochLinkAttr.getExplictRoute().getRoute()) {
            xcList.addAll(route.getPrimary().getCrossConnections());

            if (route.getSecondary() != null) {
                xcList.addAll(route.getSecondary().getCrossConnections());
            }

            if (route.getThird() != null) {
                for (Third third : route.getThird()) {
                    xcList.addAll(third.getCrossConnections());
                }
            }
        }
        return xcList;
    }

    private Map<String, List<String>> groupXcByNode(List<String> changedXcList) {
        Map<String, List<String>> nodeXcList = new HashMap<>();
        for (String xcId : changedXcList) {
            String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
            List<String> xcList = nodeXcList.getOrDefault(nodeId, null);
            if (xcList == null) {
                xcList = new ArrayList<>();
                nodeXcList.put(nodeId, xcList);
            }
            xcList.add(xcId);
        }

        return nodeXcList;
    }

    private void write2Ne(String nodeId, List<String> xcList) {
        Node opNode = changedObject.getChangedPhyOpNode(nodeId);
        if (opNode == null) {
            log.error("the NE hasn't be managed by adatper yet{}", nodeId);
            return;
        }

        Node node = changedObject.getChangedPhyNode(nodeId);

        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        List<CrossConnections> newXcList = nodeAttr.getCrossConnections().stream()
                        .filter(x -> xcList.contains(x.getCrossConnectionId().getValue()))
                        .collect(Collectors.toList());

        log.debug("start write xc info to NE {}\n{} {}", nodeId, nodeAttr.getIp(),
                newXcList.stream().map(xc -> xc.getDescription()).collect(Collectors.toList()));

        Node changedNode = new NodeBuilder()
                .setNodeId(new NodeId(nodeId))
                .setTerminationPoint(new ArrayList<>())
                .addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder()
                                .setIp(nodeAttr.getIp())
                                .setCrossConnections(newXcList)
                                .build())
                        .build())
                .build();

        try {
            ConfigNeOutput output;
            output = neMgr.configNe(changedNode);
            log.info("write NE done {}, {}", nodeId, output.getFailObj().toString());
        } catch (CommonException e) {
            log.error("write ne error {}", nodeId, e);
        }
    }

    public List<String> readListFromFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        return Files.readAllLines(path);
    }
}
