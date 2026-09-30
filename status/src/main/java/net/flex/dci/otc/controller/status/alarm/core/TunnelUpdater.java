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
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.topology.type.SiteTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Topology1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.base.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.base.attributes.SourceTp;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TunnelUpdater implements AlarmUpdater {

    @Autowired
    private TunnelDao tunnelDao;
    @Autowired
    private OchLinkDao ochLinkDao;

    @Autowired
    private AlarmGenerator alarmGenerator;

    public void updateAlarmStatus() {
        List<Tunnel> tunnelList = tunnelDao.listTunnels();
        if (tunnelList != null) {
            for (Tunnel tunnel : tunnelList) {
                if (this.shouldTunnelUpdate(tunnel)) {
                    this.updateTunnelAlarmStatus(tunnel);
                }
            }
        }
    }

    public void refreshAlarmStatus(String tunnelId) {
        this.refreshAlarmStatus(this.getTunnel(tunnelId));
    }

    private Tunnel getTunnel(String tunnelId) {
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        return tunnel;
    }

    public void refreshAlarmStatus(Tunnel tunnel) {
        log.debug("refreshAlarmStatus for tunnel {}", tunnel.getTunnelId().getValue());
        if (tunnel.getImplementState() == ImplementState.Allocate) {
            if (tunnel.getAlarmState() != AlarmSeverity.Cleared) {
                TunnelBuilder tunnelBuilder = new TunnelBuilder();
                tunnelBuilder.setKey(tunnel.getKey());
                tunnelBuilder.setAlarmState(AlarmSeverity.Cleared);
                alarmGenerator.generateTunnelAlarm(tunnel, AlarmSeverity.Cleared);
//                mongoDaoUtil.saveTunnel(tunnel.getTunnelId().getValue(), tunnelBuilder.build(),
//                        DataStoreType.OPERATIONAL,
//                        YangOperationType.MERGE);
                tunnelDao.saveTunnel(tunnelBuilder.build());
                log.info("Tunnel {} alarm severity change from {} to {}",
                        tunnel.getTunnelId().getValue(), tunnel.getAlarmState(),
                        AlarmSeverity.Cleared);
            }
        } else {
            this.refreshChild(tunnel);
            this.updateTunnelAlarmStatus(tunnel);
        }

    }

    private void refreshChild(Tunnel tunnel) {
//        List<SupportingLink> supportingLinkList = tunnel.getSupportingLink();
//        if (supportingLinkList != null) {
//            for (SupportingLink supportingLink : supportingLinkList) {
//                String ochLinkId = supportingLink.getLinkRef().getValue();
//                OchLinkUpdater ochLinkUpdater = new OchLinkUpdater();
//                InstanceIdentifier<Link> iid = Constant.OCH_TOPO_IID
//                        .child(Link.class, new LinkKey(new LinkId(ochLinkId)));
//                Link ochLink = MdsalOperation.getInstance()
//                        .read(LogicalDatastoreType.OPERATIONAL, iid);
//                ochLinkUpdater.refreshAlarmStatus(ochLink);
//            }
//        }
    }

    private boolean shouldTunnelUpdate(
            Tunnel tunnel) {
        if (tunnel.getImplementState() != ImplementState.Implement) {
            log.debug("Tunnel {} is not implemented", tunnel.getTunnelId().getValue());
            return false;
        }
        String tunnelId = tunnel.getTunnelId().getValue();
//        if (StatusContext.getInstance().getTunnelIdSet().contains(tunnelId)) {
//            return true;
//        }
//        if (this.isTunnelTpUpdated(tunnel)) {
//            log.debug("should update tunnel {} by tp change", tunnelId);
//            return true;
//        }
        List<SupportingLink> supportingLinkList = tunnel.getSupportingLink();
        if (supportingLinkList != null) {
            for (SupportingLink supportingLink : supportingLinkList) {
                String ochLinkId = supportingLink.getLinkRef().getValue();
//                if (StatusContext.getInstance().getOchLinkIdSet().contains(ochLinkId)) {
//                    logger.debug("should update tunnel {} by ochLink change", tunnelId);
//                    return true;
//                }
            }
        }
        return false;
    }

    private boolean isTunnelTpUpdated(Tunnel tunnel, String neId, Set<String> keySet) {
//        Set<String> keySet = (Set<String>) StatusContext.getInstance().getContext()
//                .get(Constant.NE_NML_KEY_SET);
//        String neId = StatusContext.getInstance().getNeId();
        List<SourceTp> stps = tunnel.getSourceTp();
        List<DestinationTp> dtps = tunnel.getDestinationTp();
        if (keySet == null) {
            if (neId != null) {
                for (SourceTp stp : stps) {
                    if (stp.getTpRef().getValue().startsWith(neId)) {
                        return true;
                    }
                }
                for (DestinationTp dtp : dtps) {
                    if (dtp.getTpRef().getValue().startsWith(neId)) {
                        return true;
                    }
                }
            }
        } else {
            for (SourceTp stp : stps) {
                for (String nmlKey : keySet) {
                    if (stp.getTpRef().getValue().startsWith(nmlKey)) {
                        return true;
                    }
                }
            }
            for (DestinationTp dtp : dtps) {
                for (String nmlKey : keySet) {
                    if (dtp.getTpRef().getValue().startsWith(nmlKey)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private List<String> getTpNmlKey(String tpId) {
        String equipId = tpId.substring(0, tpId.lastIndexOf("#"));
        String neId = tpId.substring(0, equipId.lastIndexOf("#"));
        List<String> idList = new ArrayList<String>();
        idList.add(tpId);
        idList.add(equipId);
        idList.add(neId);
        return idList;

    }

    public void updateAlarmStatus(String id) {
        Tunnel tunnel = tunnelDao.getTunnelById(id);
        updateTunnelAlarmStatus(tunnel);
    }

    private void updateTunnelAlarmStatus(
            Tunnel tunnel) {
        log.debug("Update tunnel alarmStatus for tunnel {}", tunnel.getTunnelId().getValue());
        List<SupportingLink> supportingLinkList = tunnel.getSupportingLink();
        AlarmSeverity alarmSeverity = AlarmSeverity.Cleared;
        String tunnelId = tunnel.getTunnelId().getValue();
        if (supportingLinkList != null) {
            for (SupportingLink supportingLink : supportingLinkList) {
                String linkId = supportingLink.getLinkRef().getValue();
                AlarmSeverity currentStatus = AlarmSeverity.Cleared;
                currentStatus = this.getOchLinkStatus(linkId);
                log.debug("Get supporting ochLink {} alarmStatus {}", linkId, currentStatus);
                if (alarmSeverity.compareTo(currentStatus) < 0) {
                    alarmSeverity = currentStatus;
                }
            }
        }
        log.debug("Update tunnel alarmStatus for tunnel {}, supporting link alarm status {}",
                alarmSeverity);
        List<SourceTp> stps = tunnel.getSourceTp();
        List<String> nmlKeys = new ArrayList<>();
        if (stps != null) {
            for (SourceTp stp : stps) {
                nmlKeys.addAll(this.getTpNmlKey(stp.getTpRef().getValue()));
            }
        }
        List<DestinationTp> dtps = tunnel.getDestinationTp();
        if (dtps != null) {
            for (DestinationTp dtp : dtps) {
                nmlKeys.addAll(this.getTpNmlKey(dtp.getTpRef().getValue()));
            }
        }
        AlarmCalculator alarmCalculator = new AlarmCalculator();
        AlarmSeverity tpSeverity = alarmCalculator.calculateSeverity(tunnelId, nmlKeys);
        log.debug("Update tunnel alarmStatus for tunnel {}, tp alarm status {}", tunnelId,
                tpSeverity);
        if (alarmSeverity.compareTo(tpSeverity) < 0) {
            alarmSeverity = tpSeverity;
        }

        InstanceIdentifier<Tunnel> tunnelOpId = InstanceIdentifier.create(NetworkTopology.class)
                .child(Topology.class,
                        new TopologyKey(new TopologyId(SiteTopology.QNAME.getLocalName())))
                .augmentation(Topology1.class)
                .child(Tunnel.class,
                        tunnel.getKey());
        TunnelBuilder tunnelBuilder = new TunnelBuilder();
        tunnelBuilder.setKey(tunnel.getKey());
        AlarmSeverity oldTunnelSeverity = AlarmSeverity.Cleared;
        if (tunnel.getAlarmState() != null) {
            oldTunnelSeverity = tunnel.getAlarmState();
        }
        log.debug(
                "Update tunnel alarmStatus for tunnel {} old alarmStatus {}, new alarmStatus {}",
                tunnel.getTunnelId().getValue(), oldTunnelSeverity, alarmSeverity);

        if (oldTunnelSeverity != alarmSeverity || alarmSeverity == AlarmSeverity.Cleared) {
            tunnelBuilder.setAlarmState(alarmSeverity);
//            mongoDaoUtil.saveTunnel(tunnel.getTunnelId().getValue(), tunnelBuilder.build(),
//                    DataStoreType.OPERATIONAL, YangOperationType.MERGE);
            tunnelDao.saveTunnel(tunnelBuilder.build());
            alarmGenerator.generateTunnelAlarm(tunnel, alarmSeverity);
            log.info("Update tunnel alarmStatus for tunnel {} change from {} to {}",
                    tunnel.getTunnelId().getValue(), oldTunnelSeverity, alarmSeverity);
        }
    }

    private AlarmSeverity getOchLinkStatus(String linkId) {
//        InstanceIdentifier<Och> iid = InstanceIdentifier.create(NetworkTopology.class)
//                .child(Topology.class,
//                        new TopologyKey(new TopologyId(OchTopology.QNAME.getLocalName())))
//                .child(Link.class, new LinkKey(new LinkId(linkId)))
//                .augmentation(
//                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
//                .child(Och.class);
        Och och = ochLinkDao.getLinkAttributeOch(linkId);
        if (och != null && och.getAlarmState() != null) {
            return och.getAlarmState();
        } else {
            return AlarmSeverity.Cleared;
        }
    }
}
