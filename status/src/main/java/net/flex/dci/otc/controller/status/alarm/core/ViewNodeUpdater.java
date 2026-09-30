/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.alarm.core;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.mongo.dao.ViewNodeDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1Builder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.ViewBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.node.attributes.SupportingNode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ViewNodeUpdater implements AlarmUpdater {

//    @Autowired
//    private MongoDao mongoDao;
//
//    @Autowired
//    private MongoDaoUtil mongoDaoUtil;

    @Autowired
    private ViewNodeDao viewNodeDao;
    @Autowired
    private SiteNodeDao siteNodeDao;

    @Override
    public void updateAlarmStatus(String id) {
        Node viewNode = viewNodeDao.getViewNodeById(id);
        updateAlarmStatus(viewNode);
    }

    private void updateAlarmStatus(Node viewNode) {
        log.debug("Update viewNode alarmStatus for view node {}",
                viewNode.getNodeId().getValue());
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setKey(viewNode.getKey());
        nodeBuilder.setNodeId(viewNode.getNodeId());

        ViewBuilder viewBuilder = new ViewBuilder();
        List<SupportingNode> supportingNode = viewNode.getSupportingNode();
        AlarmSeverity alarmSererity = AlarmSeverity.Cleared;
        if (supportingNode != null) {
            for (SupportingNode sn : supportingNode) {
                String siteNodeId = sn.getNodeRef().getValue();
//                InstanceIdentifier<Site> iid = InstanceIdentifier.create(NetworkTopology.class)
//                        .child(Topology.class,
//                                new TopologyKey(new TopologyId(SiteTopology.QNAME.getLocalName())))
//                        .child(Node.class, new NodeKey(new NodeId(siteNodeId)))
//                        .augmentation(
//                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
//                        .child(Site.class);
                Site site = siteNodeDao.getSiteNodeAttributeSite(siteNodeId);
                AlarmSeverity as = AlarmSeverity.Cleared;
                if (site != null && site.getAlarmState() != null) {
                    as = site.getAlarmState();
                }
                log.debug("Get supporting siteNode {} alarmStatus {}", siteNodeId, as);
                if (alarmSererity.compareTo(as) < 0) {
                    alarmSererity = as;
                }
            }
        }

        AlarmSeverity oldAs = this.getNodeAlarmStatus(viewNode);
        log.debug(
                "Update viewNode alarmStatus for view node {} , old AlarmStatus {} , new AlarmStatus {}",
                viewNode.getNodeId().getValue(), oldAs, alarmSererity);
        if (oldAs != alarmSererity) {
            viewBuilder.setAlarmState(alarmSererity);
            Node1Builder node1Builder = new Node1Builder();
            node1Builder.setView(viewBuilder.build());
            nodeBuilder.addAugmentation(
                    org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1.class,
                    node1Builder.build());
//            mongoDaoUtil.saveViewNode(viewNode.getNodeId().getValue(), nodeBuilder.build(),
//                    DataStoreType.OPERATIONAL, YangOperationType.MERGE);
            viewNodeDao.saveViewNode(nodeBuilder.build());
            log.info("Update viewNode alarmStatus for view node {} change from {} to {}",
                    viewNode.getNodeId().getValue(), oldAs, alarmSererity);
        }

    }

    private AlarmSeverity getNodeAlarmStatus(Node viewNode) {
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1 node1 = viewNode
                .getAugmentation(
                        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1.class);
        if (node1.getView() != null && node1.getView().getAlarmState() != null) {
            return node1.getView().getAlarmState();
        }
        return AlarmSeverity.Cleared;
    }

}
