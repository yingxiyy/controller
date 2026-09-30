package net.flex.dci.otn.controller.allocate.nbi.impl.repair;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otn.controller.allocate.link.och.OchLinkFromTunnelRemover;
import net.flex.dci.otn.controller.allocate.link.site.SiteLinkOchUpdater;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class DBRepair {
    ChangedObject changedObject = new ChangedObject();

    PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
    MultipleTransaction transaction = SpringBeanFinder.getBean(MultipleTransaction.class);

    public void tpBusyStateRepair() {
        List<Node> nodeList = phyNodeDao.listConfigPhyNodes();
        nodeList.forEach(node-> {
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            AtomicBoolean changed = new AtomicBoolean(false);
            List<TerminationPoint> newTpList = node.getTerminationPoint().stream().map(tp -> {
                String tpId = tp.getTpId().getValue();
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr
                        = tp.getAugmentation(TerminationPoint1.class).getPhysical();

                if (tpAttr.getOtuClient() != null) {
                    return tp;
                }

                if (tpAttr.getConnectionStatus().equals(ConnectionStatus.Busy)) {
                    if (!nodeAttr.getInternalLinks().stream().anyMatch(il -> il.getLinkRef().contains(tpId))) {
                        TerminationPoint newTp = new TerminationPointBuilder(tp).addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                        .setPhysical(new PhysicalBuilder(tpAttr)
                                                .setConnectionStatus(ConnectionStatus.Idle)
                                                .build())
                                        .build())
                                .build();

                        log.debug("changed tp to idle {}", tpId);
                        changed.set(true);
                        return newTp;
                    }
                } else { //idle
                    if (nodeAttr.getInternalLinks().stream().anyMatch(il -> il.getLinkRef().contains(tpId))) {
                        TerminationPoint newTp = new TerminationPointBuilder(tp).addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                        .setPhysical(new PhysicalBuilder(tpAttr)
                                                .setConnectionStatus(ConnectionStatus.Busy)
                                                .build())
                                        .build())
                                .build();

                        log.debug("changed tp to busy {}", tpId);
                        changed.set(true);
                        return newTp;
                    }
                }
                return tp;
            }).collect(Collectors.toList());

            Node newNode;
            if (changed.get()) {
                newNode = new NodeBuilder(node).setTerminationPoint(newTpList).build();
                changedObject.addChangedPhyNode(newNode);
            }
        });

        transaction.save(changedObject);
    }

    public String removeOch(String ochLinkId) {
        Link ochLink = changedObject.getChangedOchLink(ochLinkId);
        OchLinkFromTunnelRemover remover = new OchLinkFromTunnelRemover(changedObject, null, ochLinkId);
        remover.removeOch(ochLink);
        return "\ndone\n";
    }
}
