/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.alarm.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.util.SiteLinkUtil;
import net.flex.dci.otc.mongo.dao.CrossConnectionsDao;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.Aps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Secondary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SiteLinkUpdater implements AlarmUpdater {

    private boolean isAutoMode = true;

    private AlarmSeverity neSeverity = null;

    @Autowired
    private SiteLinkDao siteLinkDao;
    @Autowired
    private PhyLinkDao phyLinkDao;
    @Autowired
    private CrossConnectionsDao connectionsDao;

    @Autowired
    private AlarmGenerator alarmGenerator;

    @Autowired
    private SiteLinkUtil siteLinkUtil;

//    @Autowired
//    private OnChangeService onChangeService;

    public void refreshAlarmStatus(String linkId) {
        Link siteLink = siteLinkDao.getSiteLinkById(linkId);
        this.refreshAlarmStatus(siteLink);
        ViewLinkUpdater viewLinkUpdater = new ViewLinkUpdater();
        viewLinkUpdater.updateAlarmStatus(linkId);
    }

    public void refreshAlarmStatus(Link siteLink) {
        log.debug("refreshAlarmStatus for site link {}", siteLink.getLinkId().getValue());
        this.refreshChild(siteLink);
        this.updateAlarmStatus(siteLink);
    }

    private void refreshChild(Link link) {
        List<SupportingLink> supportingLinkList = link.getSupportingLink();
        if (supportingLinkList != null) {
            for (SupportingLink supportingLink : supportingLinkList) {
                String linkId = supportingLink.getLinkRef().getValue();
//                InstanceIdentifier<Link> iid = InstanceIdentifier.create(NetworkTopology.class)
//                        .child(Topology.class, new TopologyKey(
//                                new TopologyId(OtnPhyTopology.QNAME.getLocalName())))
//                        .child(Link.class, new LinkKey(new LinkId(linkId)));
                Link phyLink = phyLinkDao.getPhyLinkById(linkId);
                PhyLinkUpdater phyLinkUpdater = new PhyLinkUpdater();
                phyLinkUpdater.refreshAlarmStatus(phyLink);
            }
        }
    }

    private boolean shouldLinkUpdate(Link link) {
        if (!this.isLinkImplemnt(link)) {
            log.debug("siteLink {} is not implmeneted", link.getLinkId().getValue());
            return false;
        }
        List<SupportingLink> supportingLinkList = link.getSupportingLink();
        if (supportingLinkList != null) {
            for (SupportingLink supportingLink : supportingLinkList) {
                String phyLinkId = supportingLink.getLinkRef().getValue();
//                if (StatusContext.getInstance().getPhyLinkIdSet().contains(phyLinkId)) {
//                    return true;
//                }
            }
        }
        return false;
    }

    private boolean isLinkImplemnt(Link link) {
        Link1 link1 = link
                .getAugmentation(
                        Link1.class);
        if (link1 != null) {
            Site site = link1.getSite();
            return site != null && site.getImplementState() == ImplementState.Implement;
        }
        return false;
    }


    public void updateAlarmStatus(String id) {
        Link link = siteLinkDao.getSiteLinkById(id);
        updateAlarmStatus(link);
    }

    private void updateAlarmStatus(Link link) {
        log.debug("Update siteLink alarmStatus for site link {}", link.getLinkId().getValue());
        Properties properties = link.getAugmentation(Link1.class).getSite().getProperties();
        List<Property> propList = properties.getProperty();
        List<String> modelValList = new ArrayList<>();
        modelValList.add("3");
        modelValList.add("4");
        modelValList.add("5");
        boolean isProtectMode = false;
        for (Property prop : propList) {
            if (prop.getName().equals("model") && modelValList.contains(prop.getValue())) {
                log.debug("Update siteLink alarmStatus for mode {}", prop.getValue());
                isProtectMode = true;
                break;
            }
        }
        AlarmSeverity alarmSeverity = null;
        if (isProtectMode) {
            alarmSeverity = this.calculateSeverityForProtectMode(link);
            if (alarmSeverity == null) {
                return;
            }
        } else {
            alarmSeverity = this.calculateSeverityForNoProtectMode(link);
        }
        AlarmSeverity oldLinkSeverity = this.getLinkSeverity(link);
        log.debug(
                "Update siteLink alarmStatus for site link {}, old alarmStatus {} , new AlarmStatus {}",
                link.getLinkId().getValue(), oldLinkSeverity, alarmSeverity);
        if (oldLinkSeverity != alarmSeverity || alarmSeverity == AlarmSeverity.Cleared) {
            SiteBuilder siteBuilder = new SiteBuilder();
            siteBuilder.setAlarmState(alarmSeverity);
//            mongoDao.saveData(siteOpId, siteBuilder.build(), DataStoreType.OPERATIONAL,
//                    YangOperationType.MERGE);
            siteLinkDao.updateSiteLinkAttributeSite(link.getLinkId().getValue(),
                    siteBuilder.build());
            if (alarmSeverity == AlarmSeverity.Critical) {
                alarmGenerator.generateLinkAlarm(link, AlarmSeverity.Critical);
            } else {
                alarmGenerator.generateLinkAlarm(link, AlarmSeverity.Cleared);
            }
//            onChangeService.onSiteLinkChanged(link.getLinkId().getValue(), StatusType.ALARM_TYPE);
            log.debug("Update siteLink alarmStatus for site link {} change from {} to {}",
                    link.getLinkId().getValue(), oldLinkSeverity, alarmSeverity);
        }
    }

    /**
     * 1: primary 2: secondary 3: primary + secondary
     *
     * @param link
     * @return
     */
    private int getProtectPathType(Link link) {
        Site site = link.getAugmentation(Link1.class).getSite();
        Route route = site.getExplictRoute().getRoute().get(0);
        List<CrossConnections> xcs = route.getPrimary().getCrossConnections();
        boolean isContainPrimary = false;
        boolean isContainSecondary = false;
        int xcCnt = 0;
        if (xcs != null) {
            for (CrossConnections xc : xcs) {
                String tpRef = xc.getSourceTp().get(0).getTpRef().getValue();
                String[] ids = tpRef.split("#");
                NodeId nodeId = new NodeId(ids[0] + "#" + ids[1]);

//                InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> path = InstanceIdentifier
//                        .builder(NetworkTopology.class)
//                        .child(Topology.class, new TopologyKey(
//                                new TopologyId(OtnPhyTopology.QNAME.getLocalName())))
//                        .child(Node.class, new NodeKey(nodeId))
//                        .augmentation(
//                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
//                        .child(Physical.class)
//                        .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections.class,
//                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsKey(
//                                        xc.getCrossConnectionId()))
//                        .build();
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections phyXc =
                        connectionsDao.getOpXC(nodeId.getValue(),
                                xc.getCrossConnectionId().getValue());
//                        (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections)
//                        mongoDao
//                                .readData(path, DataStoreType.OPERATIONAL);
                Aps aps = phyXc.getAps();
                if (aps != null) {
                    if (aps.getActivePath() == ApsPath.PRIMARY) {
                        isContainPrimary = true;
                        xcCnt++;
                    } else if (aps.getActivePath() == ApsPath.SECONDARY) {
                        isContainSecondary = true;
                        xcCnt++;
                    }

                }
            }

        }
        if (xcCnt != 2) {
            return -1;
        }
        if (isContainPrimary && !isContainSecondary) {
            return 1;
        } else if (!isContainPrimary && isContainSecondary) {
            return 2;
        } else if (isContainPrimary && isContainSecondary) {
            return 3;
        }
        return -1;
    }

    private void getSwitchPort(Link link, Set<String> switchPorts, Set<String> selectPort) {
        Site site = link.getAugmentation(Link1.class).getSite();
        Route route = site.getExplictRoute().getRoute().get(0);
        List<CrossConnections> xcs = route.getPrimary().getCrossConnections();
        if (xcs != null) {
            for (CrossConnections xc : xcs) {
                String tpRef = xc.getSourceTp().get(0).getTpRef().getValue();
                String[] ids = tpRef.split("#");
                NodeId nodeId = new NodeId(ids[0] + "#" + ids[1]);

//                InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> path = InstanceIdentifier
//                        .builder(NetworkTopology.class)
//                        .child(Topology.class, new TopologyKey(
//                                new TopologyId(OtnPhyTopology.QNAME.getLocalName())))
//                        .child(Node.class, new NodeKey(nodeId))
//                        .augmentation(
//                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1.class)
//                        .child(Physical.class)
//                        .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections.class,
//                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsKey(
//                                        xc.getCrossConnectionId()))
//                        .build();
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections phyXc =
                        connectionsDao.getOpXC(nodeId.getValue(),
                                xc.getCrossConnectionId().getValue());
//                        (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections) mongoDao
//                        .readData(path, DataStoreType.OPERATIONAL);
                Aps aps = phyXc.getAps();
                if (aps != null) {
                    if (xc.getDestinationTp() != null) {
                        for (DestinationTp tp : xc.getDestinationTp()) {
                            switchPorts.add(tp.getTpRef().getValue());
                        }
                    }
                    if (aps.getActivePath() == ApsPath.PRIMARY) {
                        selectPort.add(xc.getDestinationTp().get(0).getTpRef().getValue());
                    } else if (aps.getActivePath() == ApsPath.SECONDARY) {
                        selectPort.add(xc.getDestinationTp().get(1).getTpRef().getValue());
                    }
                    if (aps.getForceToPort() != null
                            && aps.getForceToPort() != ApsPath.NONE) {
                        isAutoMode = false;
                    }
                }
            }
        }
    }

    private AlarmSeverity calculateSeverityForProtectMode(Link link) {
        Site site = link.getAugmentation(Link1.class).getSite();
        Route route = site.getExplictRoute().getRoute().get(0);
        String linkId = link.getLinkId().getValue();
        AlarmSeverity alarmSeverity = AlarmSeverity.Cleared;
        Set<String> switchPorts = new HashSet<String>();
        Set<String> selectPort = new HashSet<String>();
        this.getSwitchPort(link, switchPorts, selectPort);
        AlarmSeverity primaryAS = this
                .calculatePrimaryRoute(linkId, route.getPrimary(), route.getSecondary());
        log.debug("Update siteLink alarmStatus {} primary alarm status {}", linkId, primaryAS);
        AlarmSeverity secondaryAS = this
                .calculateSecondaryRoute(linkId, route.getPrimary(), route.getSecondary(),
                        switchPorts);
        log.debug("Update siteLink alarmStatus {} secondary alarm status {}", linkId,
                secondaryAS);
        int pathType = this.getProtectPathType(link);
        log.debug("Update siteLink alarmStatus {} pathType {}", linkId, pathType);
        if (pathType == -1) {
            return null;
        }
        if (pathType == 1) {
            alarmSeverity = primaryAS;
        } else if (pathType == 2) {
            alarmSeverity = secondaryAS;
        } else if (pathType == 3) {
            alarmSeverity = this.calculateMixRoute(linkId, route.getPrimary(), route.getSecondary(),
                    switchPorts, selectPort);
        }
        this.neSeverity = alarmSeverity;

        if (alarmSeverity == AlarmSeverity.Major) {
            alarmGenerator.generateLinkAlarm(link, AlarmSeverity.Major,
                    "OMS_Degrade;For OMSP, if operation status of any port in one single opName of this Optical Multiplex Section is INACTIVE.");
        } else {
            alarmGenerator.generateLinkAlarm(link, AlarmSeverity.Cleared,
                    "OMS_Degrade;For OMSP, if operation status of any port in one single opName of this Optical Multiplex Section is INACTIVE.");
        }

        if (!isAutoMode) {
            log.debug("Update siteLink alarmStatus {} severity {} for OMS_NOTAUTO_SWITCH",
                    linkId, AlarmSeverity.Major);
            if (alarmSeverity.compareTo(AlarmSeverity.Major) < 0) {
                alarmSeverity = AlarmSeverity.Major;
            }
            alarmGenerator.generateLinkAlarm(link, AlarmSeverity.Major,
                    "OMS_NOTAUTO_SWITCH;For OMSP, any end of this OMS works in force or manual mode.");
        } else {
            alarmGenerator.generateLinkAlarm(link, AlarmSeverity.Cleared,
                    "OMS_NOTAUTO_SWITCH;For OMSP, any end of this OMS works in force or manual mode.");
        }

        if (pathType == 3) {
            log.debug("Update siteLink alarmStatus {} severity {} for OMS_PATH_SEPARATION",
                    linkId, AlarmSeverity.Minor);
            if (alarmSeverity.compareTo(AlarmSeverity.Minor) < 0) {
                alarmSeverity = AlarmSeverity.Minor;
            }
            alarmGenerator.generateLinkAlarm(link, AlarmSeverity.Minor,
                    "OMS_PATH_SEPARATION;For OMSP, A->Z and Z->A work on different paths.");
        } else {
            alarmGenerator.generateLinkAlarm(link, AlarmSeverity.Cleared,
                    "OMS_PATH_SEPARATION;For OMSP, A->Z and Z->A work on different paths.");
        }

        return alarmSeverity;
    }

    private AlarmSeverity calculatePrimaryRoute(String linkId, Primary primary,
            Secondary secondary) {
        List<String> tps = siteLinkUtil.getPrimaryRouteTps(primary);
        AlarmSeverity alarmSeverity = this.calculateAlarmSeverityByTp(linkId, tps);
        if (alarmSeverity != AlarmSeverity.Critical && alarmSeverity != AlarmSeverity.Unknown) {
            tps = new ArrayList<>();
            AlarmCalculator alarmCalculator = new AlarmCalculator();
            tps.addAll(siteLinkUtil.getPrimaryRouteTps(primary));
            tps.addAll(siteLinkUtil.getSecondaryRouteTps(secondary));
            alarmSeverity = alarmCalculator.calculateSeverityBelowCritical(linkId, tps);
            if (alarmSeverity == null) {
                alarmSeverity = AlarmSeverity.Cleared;
            }
        }
        log.debug("Update siteLink alarm {} primary alarm status {}", linkId, alarmSeverity);
        return alarmSeverity;
    }

    private AlarmSeverity calculateSecondaryRoute(String linkId, Primary primary,
            Secondary secondary, Set<String> switchPorts) {
        List<String> tps = siteLinkUtil.getSecondaryRouteTps(secondary);
        tps.addAll(siteLinkUtil.getCommonTps(primary, switchPorts));
        AlarmSeverity alarmSeverity = this.calculateAlarmSeverityByTp(linkId, tps);
        if (alarmSeverity != AlarmSeverity.Critical && alarmSeverity != AlarmSeverity.Unknown) {
            tps = new ArrayList<>();
            AlarmCalculator alarmCalculator = new AlarmCalculator();
            tps.addAll(siteLinkUtil.getPrimaryRouteTps(primary));
            tps.addAll(siteLinkUtil.getSecondaryRouteTps(secondary));
            alarmSeverity = alarmCalculator.calculateSeverityBelowCritical(linkId, tps);
            if (alarmSeverity == null) {
                alarmSeverity = AlarmSeverity.Cleared;
            }
        }
        log.debug("Update siteLink alarm {} secondary alarm status {}", linkId, alarmSeverity);
        return alarmSeverity;
    }

    private AlarmSeverity calculateAlarmSeverityByTp(String linkId, List<String> tps) {
        AlarmSeverity alarmSeverity = null;
        if (tps != null) {
            List<String> nmlKeys = new ArrayList<>();
            for (String tpId : tps) {
                nmlKeys.addAll(this.getNmlKeyForTp(tpId));
            }
            AlarmCalculator alarmCalculator = new AlarmCalculator();
            alarmSeverity = alarmCalculator.calculateSeverity(linkId, nmlKeys);
        }
        if (alarmSeverity == null) {
            alarmSeverity = AlarmSeverity.Cleared;
        }
        return alarmSeverity;
    }


    private AlarmSeverity calculateMixRoute(String linkId, Primary primary, Secondary secondary,
            Set<String> switchPorts, Set<String> selectedPort) {
        AlarmCalculator alarmCalculator = new AlarmCalculator();
        AlarmSeverity alarmSeverity = null;
        List<String> tps = new ArrayList<>();
        tps.addAll(selectedPort);
        tps.addAll(siteLinkUtil.getCommonTps(primary, switchPorts));
        List<String> nmlKeys = new ArrayList<>();
        for (String tpId : tps) {
            nmlKeys.addAll(this.getNmlKeyForTp(tpId));
        }
        alarmSeverity = alarmCalculator.calculateSeverity(linkId, nmlKeys);
        if (alarmSeverity == AlarmSeverity.Critical || alarmSeverity == AlarmSeverity.Unknown) {
            return alarmSeverity;
        } else {
            tps = new ArrayList<>();
            tps.addAll(siteLinkUtil.getPrimaryRouteTps(primary));
            tps.addAll(siteLinkUtil.getSecondaryRouteTps(secondary));
            alarmSeverity = alarmCalculator.calculateSeverityBelowCritical(linkId, tps);
            if (alarmSeverity == null) {
                alarmSeverity = AlarmSeverity.Cleared;
            }
            return alarmSeverity;
        }
    }

    private List<String> getNmlKeyForTp(String tpId) {
        String equipId = tpId.substring(0, tpId.lastIndexOf("#"));
        String neId = tpId.substring(0, equipId.lastIndexOf("#"));
        List<String> nmlKeyList = new ArrayList<String>();
        nmlKeyList.add(tpId);
        nmlKeyList.add(equipId);
        nmlKeyList.add(neId);
        return nmlKeyList;
    }

    private AlarmSeverity calculateSeverityForNoProtectMode(Link link) {
        List<SupportingLink> supportingLinkList = link.getSupportingLink();
        AlarmSeverity alarmSeverity = AlarmSeverity.Cleared;
        if (supportingLinkList != null) {
            for (SupportingLink supportingLink : supportingLinkList) {
                String phyLinkId = supportingLink.getLinkRef().getValue();
//                InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical> iid = InstanceIdentifier
//                        .create(NetworkTopology.class)
//                        .child(Topology.class, new TopologyKey(
//                                new TopologyId(OtnPhyTopology.QNAME.getLocalName())))
//                        .child(Link.class, new LinkKey(new LinkId(phyLinkId)))
//                        .augmentation(
//                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
//                        .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical.class);
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical phy =
                        phyLinkDao.getLinkPhysical(phyLinkId);
//                        (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical) mongoDao
//                        .readData(iid, DataStoreType.OPERATIONAL);
                AlarmSeverity as = AlarmSeverity.Cleared;
                if (phy != null && phy.getAlarmState() != null) {
                    as = phy.getAlarmState();
                }
                log.debug("Get supporting phyLink {} alarmStatus {}",
                        link.getLinkId().getValue(), phyLinkId, as);
                if (alarmSeverity.compareTo(as) < 0) {
                    alarmSeverity = as;
                }
            }
        }
        this.neSeverity = alarmSeverity;
        return alarmSeverity;
    }

    private AlarmSeverity getLinkSeverity(Link link) {
        AlarmSeverity ret = AlarmSeverity.Cleared;
        if (link.getAugmentation(Link1.class) != null) {
            Site site = link.getAugmentation(Link1.class).getSite();
            if (site != null && site.getAlarmState() != null) {
                ret = site.getAlarmState();
            }
        }
        return ret;
    }
}
