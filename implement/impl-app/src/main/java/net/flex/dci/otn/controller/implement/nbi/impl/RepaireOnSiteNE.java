package net.flex.dci.otn.controller.implement.nbi.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.impl.ImplActionType;
import net.flex.dci.otn.controller.implement.common.impl.PhysicalNode;
import net.flex.dci.otn.controller.implement.common.impl.StepResult;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class RepaireOnSiteNE {
    private ChangedObject changedObject = new ChangedObject();
    private NeManagerRpc neMgr = SpringBeanFinder.getBean(NeManagerRpc.class);

    public String start() {
        String fileName = "./changedXcList.txt";
        List<String> changedXcList = null;
        try {
            changedXcList = readListFromFile(fileName);
        } catch (IOException e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "read file failed " + e.getMessage());
        }

        Map<String, List<String>> nodeXcList = groupXcByNode(changedXcList);
        nodeXcList.keySet().forEach(nodeId -> {
            List<String> xcList = nodeXcList.get(nodeId);
            write2Ne(nodeId, xcList);
        });

        return "done";
    }

    private Map<String, List<String>> groupXcByNode(List<String> changedXcList) {
        Map<String, List<String>> nodeXcList = new HashMap<>();
        for (String xcId : changedXcList) {
            String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
            List<String> xcList = nodeXcList.getOrDefault(nodeId, null);
            if (xcList == null) {
                xcList = new java.util.ArrayList<>();
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
