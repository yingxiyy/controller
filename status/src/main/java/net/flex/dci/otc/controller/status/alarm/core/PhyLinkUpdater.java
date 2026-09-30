/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.alarm.core;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class PhyLinkUpdater implements AlarmUpdater {

    @Autowired
    private PhyLinkDao phyLinkDao;

    @Autowired
    private AlarmCalculator alarmCalculator;

    @Autowired
    private AlarmGenerator alarmGenerator;

//    @Autowired
//    private OnChangeService onChangeService;

    public void refreshAlarmStatus(String linkId) {
        Link phyLink = phyLinkDao.getPhyLinkById(linkId);
        this.refreshAlarmStatus(phyLink);
    }

    public void refreshAlarmStatus(Link phyLink) {
        log.debug("refreshAlarmStatus for phy link {}", phyLink.getLinkId().getValue());
        this.updateLinkAlarmStatus(phyLink);
    }

    private boolean shouldLinkUpdate(Link link, String neId, Set<String> nmlKeySet) {
        if (!this.isLinkImplement(link)) {
            log.debug("phyLink {} is not implemented.", link.getLinkId().getValue());
            return false;
        }
        String srcNeId = link.getSource().getSourceNode().getValue();
        String destNeId = link.getDestination().getDestNode().getValue();
        if (srcNeId.equals(neId) || destNeId.equals(neId)) {
            if (nmlKeySet != null) {
                return shouldLinkUpdate(link, nmlKeySet);
            } else {
                return true;
            }

        } else {
            return false;
        }
    }

    private boolean shouldLinkUpdate(Link link, Set<String> keySet) {
        for (String nmlKey : keySet) {
            if (link.getSource().getSourceTp().getValue().startsWith(nmlKey)) {
                return true;
            }
            if (link.getDestination().getDestTp().getValue().startsWith(nmlKey)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLinkImplement(Link link) {
        Link1 link1 = link.getAugmentation(Link1.class);
        if (link1 != null) {
            Physical phy = link1.getPhysical();
            if (phy != null
                    && phy.getImplementState() == ImplementState.Implement) {
                return true;
            }
        }
        return false;
    }


    private List<String> getNmlKeyForTp(String tpId) {
        String equipId = tpId.substring(0, tpId.lastIndexOf("#"));
        List<String> nmlKeyList = new ArrayList<String>();
        nmlKeyList.add(tpId);
        nmlKeyList.add(equipId);
        String strArr[] = tpId.split("#");
        String neId = strArr[0] + "#" + strArr[1];
        nmlKeyList.add(neId);
        String equipSegment = strArr[2];
        String tpSegment = strArr[3];

        return nmlKeyList;
    }

    public void updateAlarmStatus(String id) {
        Link phyLink = phyLinkDao.getPhyLinkById(id);
        updateLinkAlarmStatus(phyLink);
    }

    public void updateLinkAlarmStatus(Link link) {
        log.debug("Update phyLink {} alarm status for link", link.getLinkId().getValue());
        AlarmSeverity oldLinkSeverity = this.getLinkSeverity(link.getLinkId().getValue(), "link");
        List<String> nmlKeyList = new ArrayList<>();
        nmlKeyList.addAll(this.getNmlKeyForTp(link.getSource().getSourceTp().getValue()));
        nmlKeyList.addAll(this.getNmlKeyForTp(link.getDestination().getDestTp().getValue()));
        AlarmSeverity alarmSeverity = alarmCalculator
                .calculateSeverity(link.getLinkId().getValue(), nmlKeyList);
        log.debug("Update phyLink alarm status {} old alarmStatus {} , new alarmStatus {}",
                link.getLinkId().getValue(), oldLinkSeverity, alarmSeverity);

//        InstanceIdentifier<Physical> iid = InstanceIdentifier.create(NetworkTopology.class)
//                .child(Topology.class,
//                        new TopologyKey(new TopologyId(OtnPhyTopology.QNAME.getLocalName())))
//                .child(Link.class, link.getKey())
//                .augmentation(
//                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
//                .child(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical.class);

        if (oldLinkSeverity != alarmSeverity || alarmSeverity == AlarmSeverity.Cleared) {
            PhysicalBuilder phyBuilder = new PhysicalBuilder();
            String linkId = link.getLinkId().getValue();
            phyBuilder.setAlarmState(alarmSeverity);
//            mongoDao.saveData(iid, phyBuilder.build(), DataStoreType.OPERATIONAL,
//                    YangOperationType.MERGE);
            phyLinkDao.updateLinkPhysical(link.getLinkId().getValue(), phyBuilder.build());
            alarmGenerator.generateLinkAlarm(link, alarmSeverity);
            log.debug("Update phyLink alarm status {} change from {} to {}",
                    link.getLinkId().getValue(), oldLinkSeverity, alarmSeverity);
//            onChangeService.onPhyLinkChanged(linkId, StatusType.ALARM_TYPE);
        }
    }

    private AlarmSeverity getLinkSeverity(String linkId, String type) {
        AlarmSeverity ret = AlarmSeverity.Cleared;
        Link link = phyLinkDao.getPhyLinkById(linkId);

        if (link.getAugmentation(Link1.class) != null) {
            Physical phy = link.getAugmentation(Link1.class).getPhysical();
            if (type.equals("link")) {
                if (phy != null && phy.getAlarmState() != null) {
                    ret = phy.getAlarmState();
                }
            } else if (type.equals("srcTp")) {
                if (phy != null && phy.getSourceTpAlarmStatus() != null) {
                    ret = phy.getSourceTpAlarmStatus();
                }
            } else if (type.equals("destTp")) {
                if (phy != null && phy.getDestTpAlarmStatus() != null) {
                    ret = phy.getDestTpAlarmStatus();
                }
            }
        }
        return ret;
    }

}
