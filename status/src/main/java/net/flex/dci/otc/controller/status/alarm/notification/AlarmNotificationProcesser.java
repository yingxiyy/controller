/// *
// *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
// *
// *  This program and the accompanying materials are made available under the
// *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
// *  and is available at http://www.eclipse.org/legal/epl-v10.html
// */
//
//package net.flex.dci.otc.controller.status.alarm.notification;
//
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
//import lombok.extern.slf4j.Slf4j;
//import net.flex.dci.otc.common.model.type.YangObjectType;
//
//import net.flex.dci.otc.mongo.dao.PhyLinkDao;
//import net.flex.dci.otc.mongo.dao.SiteNodeDao;
//import net.flex.dci.otc.serialization.JsonUtil;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.NeAlarmNotification;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ne.alarm.notification.AlarmDelete;
//import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.rev180719.ne.alarm.notification.AlarmUpdate;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
//import org.opendaylight.yangtools.yang.common.QName;
//import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//@Component
//@Slf4j
//public class AlarmNotificationProcesser {
//
//    @Autowired
//    private JsonUtil jsonUtil;
//
//    //    @Autowired
////    private MongoDaoUtil mongoDaoUtil;
//    @Autowired
//    private PhyLinkDao phyLinkDao;
//    @Autowired
//    private SiteNodeDao siteNodeDao;
//
//    @Autowired
//    private StatusSendService statusSendService;
//
//    @Autowired
//    private AlarmNotificationNotifier alarmNotificationNotifier;
//
//    public void process(String xmlNotification) {
//        try {
//            QName qName = NeAlarmNotification.QNAME;
//            NotificationDefinition notificationDefinition = jsonUtil
//                    .getNotificationDefinition(qName.getNamespace().toString(),
//                            qName.getLocalName());
//            NeAlarmNotification alarmNotification = (NeAlarmNotification) jsonUtil
//                    .fromXmlToDataObject(xmlNotification, notificationDefinition);
//            sendNotification(alarmNotification);
//            processPhyLink(alarmNotification);
//            processSiteNode(alarmNotification);
//        } catch (Exception e) {
//            log.error("Failed to process alarm notification: {}", xmlNotification, e);
//        }
//    }
//
//    private void sendNotification(NeAlarmNotification notification) throws Exception {
//        boolean isSendNotif = false;
//        List<String> alarmIdList = new ArrayList<>();
//        List<String> clearedIdList = new ArrayList<>();
//        if (notification.getAlarmUpdate() != null
//                && notification.getAlarmUpdate().size() > 0) {
//            List<AlarmUpdate> alarmUpdateList = notification.getAlarmUpdate();
//            for (AlarmUpdate alarmUpdate : alarmUpdateList) {
//                alarmIdList.add(alarmUpdate.getAlarmId());
//            }
//            isSendNotif = true;
//        }
//        if (notification.getAlarmDelete() != null
//                && notification.getAlarmDelete().size() > 0) {
//            List<AlarmDelete> alarmDeleteList = notification.getAlarmDelete();
//            for (AlarmDelete alarmDelete : alarmDeleteList) {
//                clearedIdList.add(alarmDelete.getAlarmId());
//            }
//            isSendNotif = true;
//        }
//        if (isSendNotif) {
//            alarmNotificationNotifier.notify(alarmIdList, clearedIdList);
//        }
//    }
//
//    private void processPhyLink(NeAlarmNotification alarmNotification) {
//        List<Link> links = phyLinkDao.listPhyLinks();
//        Set<String> linkIds = new HashSet<>();
//        if (alarmNotification.getAlarmUpdate() != null && !alarmNotification.getAlarmUpdate()
//                .isEmpty()) {
//            for (AlarmUpdate alarmUpdate : alarmNotification.getAlarmUpdate()) {
//                for (Link link : links) {
//                    if (shouldLinkUpdate(link, alarmNotification.getNodeId().getValue(),
//                            Collections.singleton(alarmUpdate.getNmlkey()))) {
//                        linkIds.add(link.getLinkId().getValue());
//                    }
//                }
//
//            }
//        }
//        if (alarmNotification.getAlarmDelete() != null && !alarmNotification.getAlarmDelete()
//                .isEmpty()) {
//            for (AlarmDelete alarmDelete : alarmNotification.getAlarmDelete()) {
//                for (Link link : links) {
//                    if (shouldLinkUpdate(link, alarmNotification.getNodeId().getValue(),
//                            Collections.singleton(alarmDelete.getNmlkey()))) {
//                        linkIds.add(link.getLinkId().getValue());
//                    }
//                }
//            }
//        }
//        for (String linkId : linkIds) {
//            statusSendService.sendMessage(linkId, YangObjectType.PhyLink, StatusType.ALARM_TYPE);
//        }
//    }
//
//    private void processSiteNode(NeAlarmNotification alarmNotification) {
//        List<Node> nodes = siteNodeDao.listSiteNodes();
//        Set<String> nodeIds = new HashSet<>();
//        for (Node node : nodes) {
//            if (alarmNotification.getNodeId().getValue()
//                    .startsWith(node.getNodeId().getValue())) {
//                statusSendService.sendMessage(node.getNodeId().getValue(), YangObjectType.SiteNode,
//                        StatusType.ALARM_TYPE);
//                break;
//            }
//        }
//    }
//
//    private boolean shouldLinkUpdate(Link link, String neId, Set<String> nmlKeySet) {
//        String srcNeId = link.getSource().getSourceNode().getValue();
//        String destNeId = link.getDestination().getDestNode().getValue();
//        if (srcNeId.equals(neId) || destNeId.equals(neId)) {
//            if (nmlKeySet != null) {
//                return shouldLinkUpdate(link, nmlKeySet);
//            } else {
//                return true;
//            }
//
//        } else {
//            return false;
//        }
//    }
//
//    private boolean shouldLinkUpdate(Link link, Set<String> keySet) {
//        for (String nmlKey : keySet) {
//            if (link.getSource().getSourceTp().getValue().startsWith(nmlKey)) {
//                return true;
//            }
//            if (link.getDestination().getDestTp().getValue().startsWith(nmlKey)) {
//                return true;
//            }
//        }
//        return false;
//    }
//}
