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
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.ViewLinkDao;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.View;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.ViewBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ViewLinkUpdater implements AlarmUpdater {

//    @Autowired
//    private MongoDao mongoDao;
//
//    @Autowired
//    private MongoDaoUtil mongoDaoUtil;\

    @Autowired
    private ViewLinkDao viewLinkDao;
    @Autowired
    private SiteLinkDao siteLinkDao;

    public void refreshAlarmStatus(String viewLinkId) {
        Link viewLink = viewLinkDao.getViewLinkById(viewLinkId);
        this.refreshAlarmStatus(viewLink);
    }

    public void refreshAlarmStatus(Link viewLink) {
        log.debug("refreshAlarmStatus for view link {}", viewLink.getLinkId().getValue());
        this.refreshChild(viewLink);
        this.updateLinkAlarmStatus(viewLink);
    }

    private void refreshChild(Link viewLink) {
        List<SupportingLink> supportingLinkList = viewLink.getSupportingLink();
        if (supportingLinkList != null) {
            for (SupportingLink supportingLink : supportingLinkList) {
                String siteLinkId = supportingLink.getLinkRef().getValue();
                Link siteLink = siteLinkDao.getSiteLinkById(siteLinkId);
                SiteLinkUpdater siteLinkUpdater = new SiteLinkUpdater();
                siteLinkUpdater.refreshAlarmStatus(siteLink);
            }
        }
    }

    private boolean shouldLinkUpdate(Link link, String changedSiteLinkId) {
        List<SupportingLink> supportingLinkList = link.getSupportingLink();
        if (supportingLinkList != null) {
            for (SupportingLink supportingLink : supportingLinkList) {
                String siteLinkId = supportingLink.getLinkRef().getValue();
                if (siteLinkId.equals(changedSiteLinkId)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void updateAlarmStatus(String id) {
        Link viewLink = viewLinkDao.getViewLinkById(id);
        updateLinkAlarmStatus(viewLink);
    }

    private void updateLinkAlarmStatus(Link link) {
        log.debug("updateLinkAlarmStatus for view link {}", link.getLinkId().getValue());
        List<SupportingLink> supportingLinkList = link.getSupportingLink();
        AlarmSeverity alarmSeverity = AlarmSeverity.Cleared;
        if (supportingLinkList != null) {
            for (SupportingLink supportingLink : supportingLinkList) {
                String siteLinkId = supportingLink.getLinkRef().getValue();
//                InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site> iid = InstanceIdentifier
//                        .create(
//                                NetworkTopology.class)
//                        .child(Topology.class,
//                                new TopologyKey(new TopologyId(SiteTopology.QNAME.getLocalName())))
//                        .child(Link.class, new LinkKey(new LinkId(siteLinkId)))
//                        .augmentation(
//                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
//                        .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site.class);
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site site =
                        siteLinkDao.getSiteLinkAttributeSite(siteLinkId);
                log.debug("Get supporting siteLink {} with alarmStatus {}", siteLinkId,
                        site.getAlarmState());
                if (site.getAlarmState() != null
                        && alarmSeverity.compareTo(site.getAlarmState()) < 0) {
                    alarmSeverity = site.getAlarmState();
                }
            }
        }

//        InstanceIdentifier<View> viewOpId = InstanceIdentifier.create(NetworkTopology.class)
//                .child(Topology.class, new TopologyKey(
//                        new TopologyId("site-" + ViewTopology.QNAME.getLocalName())))
//                .child(Link.class, link.getKey())
//                .augmentation(Link1.class).child(View.class);

        AlarmSeverity oldLinkSeverity = this.getLinkSeverity(link);
        if (oldLinkSeverity != alarmSeverity) {
            ViewBuilder viewBuilder = new ViewBuilder();
            viewBuilder.setAlarmState(alarmSeverity);
//			AlarmGenerator.getInstance().generateLinkAlarm(link, alarmSeverity);
//            mongoDao.saveData(viewOpId, viewBuilder.build(), DataStoreType.OPERATIONAL,
//                    YangOperationType.MERGE);
            viewLinkDao.saveViewLinkAttributeView(link.getLinkId().getValue(),
                    viewBuilder.build());
            log.info("View link {} alarm severity change from {} to {}",
                    link.getLinkId().getValue(), oldLinkSeverity, alarmSeverity);
        }

    }

    private AlarmSeverity getLinkSeverity(Link link) {
        AlarmSeverity ret = AlarmSeverity.Cleared;
        if (link.getAugmentation(Link1.class) != null) {
            View view = link.getAugmentation(Link1.class).getView();
            if (view != null && view.getAlarmState() != null) {
                ret = view.getAlarmState();
            }
        }
        return ret;
    }
}
