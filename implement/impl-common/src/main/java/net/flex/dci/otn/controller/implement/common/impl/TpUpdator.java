package net.flex.dci.otn.controller.implement.common.impl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.AdapterDao;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.implement.common.utils.MergeData;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.ne.result.FailObj;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.update.termination.point.input.Tps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TpUpdator {

    @Autowired
    private MultipleTransaction multipleTransaction;
    @Autowired
    private NeManagerRpc neManagerRpc;
    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    private AdapterDao adapterDao;

    @Autowired
    private TerminationPointDao terminationPointDao;

    @Autowired
    private OchLinkDao ochLinkDao;

//  public TpUpdator() {
//    multipleTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
//    neManagerRpc = SpringBeanFinder.getBean(NeManagerRpc.class);
//    phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
//  }

    /**
     * method to config termination point
     *
     * @param tp
     */
    synchronized public void configTerminationPoint(Tps tp) {
        log.debug("start config tp {}", tp.getTpId().getValue());

        ZkResourceLock lock = new ZkResourceLock();
        try {
            String nodeId = PhysicalTpIdNamingRule.getNodeId(tp.getTpId().getValue());
            lock.addResource(nodeId);

            checkingUpdateAttr(tp);
            TerminationPoint configTp = terminationPointDao.getConfigPhyTpByNeIdAndTpId(nodeId,
                    tp.getTpId().getValue());
            TerminationPoint updateTerminationPoint = updateTerminationPoint(tp);
            if (configTp != null) {
                ChangedObject changedObject = new ChangedObject();
                changedObject.addChangedPhyNode(updateTP2Node(tp, nodeId));
                multipleTransaction.save(changedObject);
            }
            write2Ne(updateTerminationPoint, nodeId);
        } catch (Exception e) {
            throw e;
        } finally {
            lock.unlock();
        }
    }

    //checking does this is used to update frequency of L port
    private void checkingUpdateAttr(Tps tp) {
        if (tp.getPhysical() != null && tp.getPhysical().getPortType() != null &&
                tp.getPhysical().getPortType().equals(PortType.OTULine)
                && tp.getPhysical().getOtuLine() != null) {
            if (tp.getPhysical().getOtuLine().getCentralFrequency() != null) {
                List<Link> ochList = ochLinkDao.queryWithTp(tp.getTpId().getValue());
                if (ochList.size() > 0) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "the TP has used in Tunnel, please change frequency at Tunnel");
                }
            }
        }
    }

    public void write2Ne(TerminationPoint updateTerminationPoint, String nodeId)
            throws CommonException {

//        boolean isRegistered = phyNodeDao.existsOpNode(nodeId);
        boolean isRegistered = adapterDao.getAdapterByNeId(nodeId) != null;
        if (!isRegistered) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the node is not accessable");
        }
        log.debug("start to write to ne ,ne nodeId :{}", nodeId);
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setNodeId(new NodeId(nodeId));
        nodeBuilder.setKey(new NodeKey(NodeId.getDefaultInstance(nodeId)));
        nodeBuilder.addAugmentation(Node1.class, new Node1Builder()
                .setPhysical(
                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder().build())
                .build());
        nodeBuilder.setTerminationPoint(Collections.singletonList(updateTerminationPoint));

        ConfigNeOutput result = neManagerRpc.configNe(nodeBuilder.build());
        if (result.getFailObj() != null && result.getFailObj().getObject() != null) {
            throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                    convert(result.getFailObj()));
        }
    }

    private Node updateTP2Node(Tps tp, String nodeId) {
        Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
        List<TerminationPoint> tps = node.getTerminationPoint();

        TerminationPointBuilder newTpBuilder;
        Optional<TerminationPoint> optTp = tps.stream()
                .filter(tpi -> tpi.getTpId().equals(tp.getTpId())).findAny();
        if (optTp.isPresent()) {
            TerminationPoint1 dbTp = optTp.get().getAugmentation(TerminationPoint1.class);
            PhysicalBuilder dbPhyBuilder = new PhysicalBuilder(dbTp.getPhysical());

            TerminationPoint1 newTp1 = mergeTpPhysaicalAttr(tp.getPhysical(), dbPhyBuilder);
//            phyBuilder.fieldsFrom(tp.getPhysical());  //this will overwrite all dbTp with UI provided TP
//            TerminationPoint1 newTp1 = new TerminationPoint1Builder(tp1).setPhysical(
//                    phyBuilder.build()).build();
            newTpBuilder = new TerminationPointBuilder(optTp.get()).addAugmentation(
                    TerminationPoint1.class, newTp1);
            tps.removeIf(tpi -> tpi.getTpId().equals(tp.getTpId()));
        } else {
            log.error("impossible, setting a TP, but the TP cannot find in cfgNode!");
            newTpBuilder = new TerminationPointBuilder().setTpId(new TpId(tp.getTpId().getValue()))
                    .setKey(new TerminationPointKey(new TpId(tp.getTpId().getValue())))
                    .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                            .setPhysical(tp.getPhysical()).build());
        }

        tps.add(newTpBuilder.build());
        return new NodeBuilder(node).setTerminationPoint(tps).build();
    }

    private TerminationPoint1 mergeTpPhysaicalAttr(Physical source, PhysicalBuilder target) {
        MergeData.merge(source, target);
        return new TerminationPoint1Builder().setPhysical(target.build()).build();
    }

    private TerminationPoint updateTerminationPoint(Tps tp) {
        TerminationPoint newTp = new TerminationPointBuilder()
                .setTpId(new TpId(tp.getTpId().getValue()))
                .setKey(new TerminationPointKey(new TpId(tp.getTpId().getValue())))
                .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                        .setPhysical(tp.getPhysical()).build())
                .build();
        return newTp;
    }


    private String convert(FailObj failObj) {
        StringBuffer sb = new StringBuffer();
        if (failObj != null && failObj.getObject() != null) {
            for (org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.config.object.ids.Object
                    obj : failObj.getObject()) {
                sb.append(obj.getObjectType().name() + ": ");
                sb.append(obj.getObjectId() + ", ");
                sb.append(obj.getMessageInfo() + "\n");
            }
        }
        return sb.toString();
    }


}
