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
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.topology.type.OchTopology;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OchLinkUpdater implements AlarmUpdater {


    //    @Autowired
//    private OnChangeService onChangeService;
    @Autowired
    private OchLinkDao ochLinkDao;
    @Autowired
    private PhyLinkDao phyLinkDao;
    @Autowired
    private SiteLinkDao siteLinkDao;


    public void refreshAlarmStatus(String linkId) {
        Link ochLink = ochLinkDao.getOchLinkByLinkId(linkId);
        this.refreshAlarmStatus(ochLink);
    }

    public void refreshAlarmStatus(Link link) {
        log.debug("refreshAlarmStatus for och link {}", link.getLinkId().getValue());
        this.refreshChild(link);
        this.updateLinkAlarmStatus(link);
    }

    private void refreshChild(Link link) {
        List<SupportingLink> supportingLinkList = link.getSupportingLink();
        if (supportingLinkList != null) {
            for (SupportingLink supportingLink : supportingLinkList) {
                String linkId = supportingLink.getLinkRef().getValue();
                Link phyLink = this.getPhyLink(linkId);
                if (phyLink != null) {
                    PhyLinkUpdater phyLinkUpdater = new PhyLinkUpdater();
                    phyLinkUpdater.refreshAlarmStatus(phyLink);
                } else {
                    Link siteLink = this.getSiteLink(linkId);
                    if (siteLink != null) {
                        SiteLinkUpdater siteLinkUpdater = new SiteLinkUpdater();
                        siteLinkUpdater.refreshAlarmStatus(siteLink);
                    }
                }
            }
            ViewLinkUpdater viewLinkUpdater = new ViewLinkUpdater();
        }
    }

    private boolean isLinkImplemnt(Link link) {
        Link1 link1 =
                link.getAugmentation(Link1.class);
        if (link1 != null) {
            Och och = link1.getOch();
            if (och != null
                    && och.getImplementState() == ImplementState.Implement) {
                return true;
            }
        }
        return false;
    }


    public void updateAlarmStatus(String id) {
        Link link = ochLinkDao.getOchLinkByLinkId(id);
        updateLinkAlarmStatus(link);
    }

    private void updateLinkAlarmStatus(Link link) {
        log.debug("Update ochLink alarmStatus for och link {}", link.getLinkId().getValue());
        List<SupportingLink> supportingLinkList = link.getSupportingLink();
        AlarmSeverity alarmSeverity = AlarmSeverity.Cleared;
        if (supportingLinkList != null) {
            for (SupportingLink supportingLink : supportingLinkList) {
                String linkId = supportingLink.getLinkRef().getValue();
                AlarmSeverity as = this.getSiteLinkStatus(linkId);
                if (as == null) {
                    as = this.getPhyLinkStatus(linkId);
                } else {
                    log.debug("Get supporting siteLink {} alarmStatus {}", linkId, as);
                }
                if (as == null) {
                    as = AlarmSeverity.Cleared;
                    log.debug("Get supporting link {} alarmStatus {}", linkId, as);
                } else {
                    log.debug("Get supporting phyLink {} alarmStatus {}", linkId, as);
                }
                if (alarmSeverity.compareTo(as) < 0) {
                    alarmSeverity = as;
                }
            }
        }

        InstanceIdentifier<Och> ochOpId = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class,
                        new TopologyKey(new TopologyId(OchTopology.QNAME.getLocalName())))
                .child(Link.class, link.getKey())
                .augmentation(Link1.class).child(Och.class);
        AlarmSeverity oldLinkSeverity = this.getLinkSeverity(link);
        log.debug(
                "Update ochLink alarmStatus for och link {},old alarmStatus {} , new alarmStatus {}",
                link.getLinkId().getValue(), oldLinkSeverity, alarmSeverity);
        if (oldLinkSeverity != alarmSeverity) {
            OchBuilder ochBuilder = new OchBuilder();
            ochBuilder.setAlarmState(alarmSeverity);
//            mongoDao.saveData(ochOpId, ochBuilder.build(), DataStoreType.OPERATIONAL,
//                    YangOperationType.MERGE);
            ochLinkDao.updateLinkAttributeOch(link, ochBuilder.build());
            log.info("Update ochLink alarmStatus for och link {} change from {} to {}",
                    link.getLinkId().getValue(), oldLinkSeverity, alarmSeverity);
        }

    }

    private Link getPhyLink(String linkId) {
        return phyLinkDao.getPhyLinkById(linkId);
    }

    private Link getSiteLink(String linkId) {
        return siteLinkDao.getSiteLinkById(linkId);
    }

    private AlarmSeverity getPhyLinkStatus(String linkId) {
//        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical> iid = InstanceIdentifier
//                .create(NetworkTopology.class)
//                .child(Topology.class,
//                        new TopologyKey(new TopologyId(OtnPhyTopology.QNAME.getLocalName())))
//                .child(Link.class, new LinkKey(new LinkId(linkId)))
//                .augmentation(
//                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
//                .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical.class);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical phy = phyLinkDao.getLinkPhysical(
                linkId);
//                (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical) mongoDao
//                        .readData(iid, DataStoreType.OPERATIONAL);
        if (phy != null && phy.getAlarmState() != null) {
            return phy.getAlarmState();
        } else {
            return null;
        }
    }

    private AlarmSeverity getSiteLinkStatus(String linkId) {
//        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site> iid = InstanceIdentifier
//                .create(NetworkTopology.class)
//                .child(Topology.class,
//                        new TopologyKey(new TopologyId(SiteTopology.QNAME.getLocalName())))
//                .child(Link.class, new LinkKey(new LinkId(linkId)))
//                .augmentation(
//                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
//                .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site.class);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site site = siteLinkDao.getSiteLinkAttributeSite(
                linkId);
//                (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site)
//                        mongoDao
//                                .readData(iid, DataStoreType.OPERATIONAL);
        if (site != null && site.getAlarmState() != null) {
            return site.getAlarmState();
        } else {
            return null;
        }
    }

    private AlarmSeverity getLinkSeverity(Link link) {
        AlarmSeverity ret = AlarmSeverity.Cleared;
        if (link.getAugmentation(Link1.class) != null) {
            Och och = link.getAugmentation(Link1.class).getOch();
            if (och != null && och.getAlarmState() != null) {
                ret = och.getAlarmState();
            }
        }
        return ret;
    }
}
