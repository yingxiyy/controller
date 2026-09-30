/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.alarm.core;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRackBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SiteNodeUpdater implements AlarmUpdater {

    @Autowired
    private SiteNodeDao siteNodeDao;
    @Autowired
    private PhyNodeDao phyNodeDao;

//    @Autowired
//    private OnChangeService onChangeService;

    public void updateAlarmStatus() {
        List<Node> siteNodeList = siteNodeDao.listSiteNodes();
        if (siteNodeList != null) {
            for (Node siteNode : siteNodeList) {
                if (this.shouldSiteUpdate(siteNode)) {
                    this.updateAlarmStatus(siteNode);
                }
            }
        }
    }


    public void refreshAlarmStatus(String nodeId) {
        Node siteNode = siteNodeDao.getSiteNodeById(nodeId);
        this.refreshAlarmStatus(siteNode);
    }

    public void refreshAlarmStatus(Node siteNode) {
        log.debug("refreshAlarmStatus for site node {}", siteNode.getNodeId().getValue());
        this.updateAlarmStatus(siteNode);
    }

    private boolean shouldSiteUpdate(Node siteNode) {
//		if(!this.isNodeImplemnt(siteNode)) {
//			return false;
//		}
//        String neId = StatusContext.getInstance().getNeId();
//        List<SupportingNode> neList = siteNode.getSupportingNode();
//        if (neList != null && neList.size() > 0) {
//            for (SupportingNode ne : neList) {
//                if (ne.getNodeRef().getValue().equals(neId)) {
//                    return true;
//                }
//            }
//        }
//        return false;
        return false;
    }

    private boolean isNodeImplemnt(Node node) {
        Node1 node1 =
                node.getAugmentation(Node1.class);
        if (node1 != null) {
            Site site = node1.getSite();
            if (site != null
                    && site.getImplementState() == ImplementState.Implement) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void updateAlarmStatus(String id) {
        Node node = siteNodeDao.getSiteNodeById(id);
        updateAlarmStatus(node);
    }

    private void updateAlarmStatus(Node siteNode) {
        log.debug("updateAlarmStatus for site node {}", siteNode.getNodeId().getValue());
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setKey(siteNode.getKey());
        nodeBuilder.setNodeId(siteNode.getNodeId());

        SiteBuilder siteBuilder = new SiteBuilder();
        this.buildRackAlarmStatus(siteNode, siteBuilder);
        boolean isAlarmChanged = this.buildSiteAlarmStatus(siteNode, siteBuilder);
        Node1Builder node1Builder = new Node1Builder();
        node1Builder.setSite(siteBuilder.build());
        nodeBuilder.addAugmentation(Node1.class, node1Builder.build());
//        mongoDaoUtil.saveSiteNode(siteNode.getNodeId().getValue(), nodeBuilder.build(),
//                DataStoreType.OPERATIONAL,
//                YangOperationType.MERGE);
        siteNodeDao.saveSiteNode(nodeBuilder.build());
//        if (isAlarmChanged) {
//            onChangeService
//                    .onSiteNodeChanged(siteNode.getNodeId().getValue(), StatusType.ALARM_TYPE);
//        }

    }

    private void buildRackAlarmStatus(Node siteNode, SiteBuilder siteBuilder) {
        Site site = siteNode.getAugmentation(Node1.class).getSite();
        List<SupportingRack> rackList = site.getSupportingRack();
        if (rackList != null && rackList.size() > 0) {
            List<SupportingRack> rackAlarmList = new ArrayList<SupportingRack>();
            for (SupportingRack rack : rackList) {
                log.debug("Update rack alarmStatus for rack {}", rack.getRackId());
                List<SupportingNe> neList = rack.getSupportingNe();
                AlarmSeverity alarmSererity = AlarmSeverity.Cleared;
                if (neList != null) {
                    for (SupportingNe ne : neList) {
                        String neId = ne.getNodeRef().getValue();
//                        InstanceIdentifier<Physical> iid = InstanceIdentifier.create(
//                                        NetworkTopology.class)
//                                .child(Topology.class, new TopologyKey(
//                                        new TopologyId(OtnPhyTopology.QNAME.getLocalName())))
//                                .child(Node.class, new NodeKey(new NodeId(neId)))
//                                .augmentation(
//                                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
//                                .child(Physical.class);
                        Physical phy = phyNodeDao.getOpPhysicalByNode(neId);
                        AlarmSeverity as;
                        if (phy.getAlarmState() == null) {
                            as = AlarmSeverity.Cleared;
                        } else {
                            as = phy.getAlarmState();
                        }
                        if (alarmSererity.compareTo(as) < 0) {
                            alarmSererity = as;
                        }
                        log.debug("Get supportin ne {} alarmStatus {}", neId, as);
                    }
                }
                SupportingRackBuilder rackBuilder = new SupportingRackBuilder();
                rackBuilder.setKey(rack.getKey());
                rackBuilder.setRackId(rack.getRackId());
                rackBuilder.setAlarmState(alarmSererity);
                log.debug("Update rack alarmStatus for rack {} with alarmStatus {}",
                        rack.getRackId(), alarmSererity);
                rackAlarmList.add(rackBuilder.build());
            }
            siteBuilder.setSupportingRack(rackAlarmList);
        }
    }

    private boolean buildSiteAlarmStatus(Node siteNode, SiteBuilder siteBuilder) {
        log.debug("Update siteNode alarmStatus {}", siteNode.getNodeId().getValue());
        List<SupportingNode> supportingNode = siteNode.getSupportingNode();
        AlarmSeverity alarmSererity = AlarmSeverity.Cleared;
        if (supportingNode != null) {
            for (SupportingNode sn : supportingNode) {
                String neId = sn.getNodeRef().getValue();
//                InstanceIdentifier<Physical> iid = InstanceIdentifier.create(NetworkTopology.class)
//                        .child(Topology.class, new TopologyKey(
//                                new TopologyId(OtnPhyTopology.QNAME.getLocalName())))
//                        .child(Node.class, new NodeKey(new NodeId(neId)))
//                        .augmentation(
//                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
//                        .child(Physical.class);
                Physical phy = phyNodeDao.getOpPhysicalByNode(neId);
                log.debug("Get suppporting phyNe {} with alarmStatus {}", neId,
                        phy.getAlarmState());
                if (phy.getAlarmState() != null
                        && alarmSererity.compareTo(phy.getAlarmState()) < 0) {
                    alarmSererity = phy.getAlarmState();
                }
            }
        }
        AlarmSeverity oldAS = this.getNodeAlarmStatus(siteNode);
        siteBuilder.setAlarmState(alarmSererity);
        log.debug("Update siteNode alarmStatus {} , old alarmStatus {} , new AlarmStatus {}",
                siteNode.getNodeId().getValue(), oldAS, alarmSererity);
        if (oldAS != alarmSererity) {
            log.debug(
                    "Update siteNode alarmStatus {} change from old alarmStatus {} to new AlarmStatus {}",
                    siteNode.getNodeId().getValue(), oldAS, alarmSererity);
            return true;
        } else {
            return false;
        }
    }

    private AlarmSeverity getNodeAlarmStatus(Node siteNode) {
        Node1 node1 = siteNode.getAugmentation(Node1.class);
        if (node1 != null && node1.getSite() != null && node1.getSite().getAlarmState() != null) {
            return node1.getSite().getAlarmState();
        }
        return AlarmSeverity.Cleared;
    }


}
