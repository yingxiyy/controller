package net.flex.dci.otn.controller.implement.nbi;


import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j

@RestController
public class TestController {
    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    private NeManagerRpc neMgr;

    @PostMapping(value = "/test/xc/{nodeId}", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String writeXc(@PathVariable("nodeId") String nodeId) {
        log.info("Write XC Received nodeId: {}", nodeId);
        Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        Node newNode = new NodeBuilder().setNodeId(node.getNodeId()).addAugmentation(Node1.class,
                new Node1Builder().setPhysical(new PhysicalBuilder()
                        .setCrossConnections(nodeAttr.getCrossConnections())
                        .build())
                    .build())
            .build();

        return write2Ne(newNode);
    }

    @PostMapping(value = "/test/ocm/{nodeId}", produces = "application/json;charset=UTF-8")
    @ResponseBody
    public String writeOcm(@PathVariable("nodeId") String nodeId) {
        log.info("Write OCM Received nodeId: {}", nodeId);
        Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        Node newNode = new NodeBuilder().setNodeId(node.getNodeId()).addAugmentation(Node1.class,
                new Node1Builder().setPhysical(new PhysicalBuilder()
                        .setOCMGripGroups(nodeAttr.getOCMGripGroups())
                        .build())
                    .build())
            .build();

        return write2Ne(newNode);
    }

    private String write2Ne(Node node) {
        ConfigNeOutput output;
        try {
            output = neMgr.configNe(node);
            log.info("adapter output \n{}", output);
            return output.toString();
        } catch (CommonException e) {
            return String.format("write ne error %s", e);
        }
    }
}