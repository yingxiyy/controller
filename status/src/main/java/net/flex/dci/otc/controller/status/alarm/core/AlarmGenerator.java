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
import net.flex.dci.otc.common.model.type.ActionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.controller.status.alarm.notification.AlarmNotificationNotifier;
import net.flex.dci.otc.controller.status.configuration.DciStatusConfiguration;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import net.flex.dci.otn.db.jpa.service.dao.AlarmDaoService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AlarmGenerator {

    @Autowired
    private AlarmDaoService alarmDaoService;


    @Autowired
    private DciStatusConfiguration alarmConfig;

    @Autowired
    private AlarmNotificationNotifier alarmNotificationNotifier;


    public void generateTunnelAlarm(Tunnel tunnel, AlarmSeverity alarmSeverity) {
        AlarmSeverity oldAs = tunnel.getAlarmState();
        this.processLinkAlarm(tunnel.getTunnelId().getValue(), oldAs, alarmSeverity,
                LinkType.OchTunnel, null);
    }

    public void generateLinkAlarm(Link link, AlarmSeverity alarmSeverity) {
        this.generateLinkAlarm(link, alarmSeverity, null);
    }

    public void generateLinkAlarm(Link link, AlarmSeverity alarmSeverity, String alarmText) {
        AlarmSeverity oldAs = null;
        LinkType linkType = null;
        if (link.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                != null) {
            linkType = link.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                    .getPhysical().getLinkType();
            oldAs = link.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                    .getPhysical().getAlarmState();
        } else if (link.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                != null) {
            linkType = LinkType.SiteLink;
            oldAs = link.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite().getAlarmState();
        } else if (link.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                != null) {
            linkType = LinkType.OchLink;
            oldAs = link.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                    .getOch().getAlarmState();
        } else if (link.getAugmentation(
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1.class)
                != null) {
            linkType = LinkType.OchTunnel;
            oldAs = link.getAugmentation(
                            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Link1.class)
                    .getView().getAlarmState();
        }
        if (oldAs == null) {
            oldAs = AlarmSeverity.Cleared;
        }
        this.processLinkAlarm(link.getLinkId().getValue(), oldAs, alarmSeverity, linkType,
                alarmText);
    }

    public void generateNeDownAlarm(String neId, boolean isClear) {
        List<String> updateIdList = new ArrayList<>();
        List<String> clearIdList = new ArrayList<>();
        String alarmText = "NE_Connection_Failure;Controller finds NE out of service.";
        boolean isSuccess = false;
        AlarmRecord alarm = null;
        String alarmId = neId + ";isolation";
        alarm = alarmDaoService.getAlarm(alarmId);
        if (isClear) {
            if (alarm != null) {
                clearIdList.add(alarm.getAlarmId());
                alarmDaoService.purgeAlarms(clearIdList, ActionType.Close);
            }
        } else {
            boolean isUpdate = true;
            if (alarm == null) {
                alarm = new AlarmRecord();
                alarm.setAlarmId(alarmId);
                isUpdate = false;
            }
            alarm.setAlarmText(alarmText);
            alarm.setNmlKey(neId);
            alarm.setResourceRef(neId);
            alarm.setIp(this.getNeIp(neId));
            alarm.setComponentRef(neId);
            alarm.setAlarmTypeId("NE");
            alarm.setAlarmGroup("NE_OOS");
            alarm.setNeId("");
            alarm.setSeverity(AlarmSeverity.Major);
            alarm.setSa(false);
            if (!isUpdate) {
                alarm.setCreationTime(System.currentTimeMillis());
                alarm.setNmlReceivedTime(System.currentTimeMillis());
            }
            alarmDaoService.saveAlarm(alarm);
            updateIdList.add(alarm.getAlarmId());
        }
        alarmNotificationNotifier.notify(updateIdList, clearIdList);
    }

    private String getNeIp(String neId) {
        PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
        Physical phy = phyNodeDao.getOpPhysicalByNode(neId);
        if (phy != null && phy.getIp() != null) {
            return phy.getIp();
        } else {
            return null;
        }
    }

    private void processLinkAlarm(String id, AlarmSeverity oldAs, AlarmSeverity newAs,
            LinkType linkType, String text) {
        if (newAs != null) {
            boolean isShowLinkAlarm = alarmConfig.isShowLinkAlarm();
            log.debug("link {} process alarm old {} new {} alarmText [{}] flag {}", id, oldAs,
                    newAs, text, isShowLinkAlarm);
            if (!isShowLinkAlarm) {
                return;
            }
            boolean isSuccess = false;
            String alarmId = null;
            List<String> updateIdList = new ArrayList<>();
            List<String> clearIdList = new ArrayList<>();
            if (text != null) {
                if (text.startsWith("OMS_NOTAUTO_SWITCH")) {
                    alarmId = id + ";"
                            + "OMS_NOTAUTO_SWITCH";
                } else if (text.startsWith("OMS_PATH_SEPARATION")) {
                    alarmId = id + ";"
                            + "OMS_PATH_SEPARATION";
                } else if (text.startsWith("OMS_Degrade")) {
                    alarmId = id + ";" + "OMS_DEG";
                } else {
                    alarmId = id + ";" + "link alarm";
                }
            } else {
                alarmId = id + ";" + "link alarm";
            }
            AlarmRecord alarm = null;
            alarm = alarmDaoService.getAlarm(alarmId);
            if (newAs == AlarmSeverity.Cleared) {
                if (alarm != null) {
                    clearIdList.add(alarm.getAlarmId());
                    alarmDaoService.purgeAlarms(clearIdList, ActionType.Close);
                }
            } else {
                String alarmGroup = null;
                String alarmText = null;
                boolean isUpdate = true;
                if (alarm == null) {
                    alarm = new AlarmRecord();
                    alarm.setAlarmId(alarmId);
                    alarm.setAlarmTypeId("LINK");
                    isUpdate = false;
                }
                if (linkType == LinkType.OmsLink) {
                    alarmGroup = "Fiber_OMS";
                } else if (linkType == LinkType.OtsLink) {
                    alarmGroup = "Fiber_OTS";
                } else if (linkType == LinkType.OsLink) {
                    alarmGroup = "Fiber_OCH";
                } else if (linkType == LinkType.CableLink) {
                    alarmGroup = "Fiber_MGT";
                } else if (linkType == LinkType.OchTunnel) {
                    alarmGroup = "CSC";
                } else if (linkType == LinkType.SiteLink) {
                    alarmGroup = "OMS";
                }
                if (newAs == AlarmSeverity.Critical) {
                    alarm.setSa(true);
                    if (linkType == LinkType.OmsLink) {
                        alarmText = "Fiber_OMS_Failure;Operation status of any port of this fiber is INACTIVE.";
                    } else if (linkType == LinkType.OtsLink) {
                        alarmText = "Fiber_OTS_Failure;Operation status of any port of this fiber is INACTIVE.";
                    } else if (linkType == LinkType.OsLink) {
                        alarmText = "Fiber_OCH_Failure;Operation status of any port of this fiber is INACTIVE.";
                    } else if (linkType == LinkType.CableLink) {
                        alarmText = "Fiber_MGT_Failure;Operation status of any port of this fiber is INACTIVE.";
                    } else if (linkType == LinkType.SiteLink) {
                        alarmText = "OMS_Failure;Operation status of any port in the entire opName of this Optical Multiplex Section is INACTIVE.";
                    } else if (linkType == LinkType.OchTunnel) {
                        alarmText = "CSC_Failure;Operation status of any port in the entire opName of this Client Signal Connection is INACTIVE.";
                    } else {
                        alarmText = "Link is broken";
                    }
                } else if (newAs == AlarmSeverity.Major) {
                    alarm.setSa(true);
                } else if (newAs == AlarmSeverity.Minor) {
                    alarm.setSa(false);
                    alarmText = "Link abnormal";
                }

                if (text != null) {
                    alarmText = text;
                }
                if (alarmText != null) {
                    if (alarmText.startsWith("OMS_Degrade")) {
                        alarmGroup = "OMS_DEG";
                        alarm.setSa(true);
                    } else if (alarmText.startsWith("OMS_NOTAUTO_SWITCH")) {
                        alarmGroup = "OMS_NOTAUTO";
                        alarm.setSa(true);
                    } else if (alarmText.startsWith("OMS_PATH_SEPARATION")) {
                        alarmGroup = "OMS_PATH_SEP";
                        alarm.setSa(true);
                    }
                }
                alarm.setAlarmText(alarmText);
                alarm.setSeverity(newAs != null ? newAs
                        : AlarmSeverity.Cleared);
                alarm.setNmlKey(id);
                alarm.setAlarmGroup(alarmGroup);
                alarm.setResourceRef(id);
                alarm.setNeId("");
                alarm.setComponentRef("");
                alarm.setIp(null);
                if (!isUpdate) {
                    alarm.setCreationTime(System.currentTimeMillis());
                    alarm.setNmlReceivedTime(System.currentTimeMillis());

                }
                alarmDaoService.saveAlarm(alarm);
                updateIdList.add(alarm.getAlarmId());
            }
            isSuccess = true;
            alarmNotificationNotifier.notify(updateIdList, clearIdList);
        }
    }

    public void generateAdapterAlarm(String adapterId, AlarmSeverity alarmSeverity,
            boolean isClear) {
        log.debug("generate adapter {} alarm severity {} isClear {}", adapterId, alarmSeverity,
                isClear);
        boolean isSuccess = false;
        List<String> updateIdList = new ArrayList<>();
        List<String> clearIdList = new ArrayList<>();

        String alarmText = null;
        String alarmId = null;
        if (alarmSeverity == AlarmSeverity.Critical) {
            alarmId = "adapter isolation;" + adapterId;
            alarmText = "communication isolation";
        } else if (alarmSeverity == AlarmSeverity.Minor) {
            alarmId = "adapter communication broken;" + adapterId;
            alarmText = "communication broken";
        } else {
            return;
        }

        AlarmRecord alarm = alarmDaoService.getAlarm(alarmId);
        if (isClear) {
            if (alarm != null) {
                clearIdList.add(alarm.getAlarmId());
                alarmDaoService.purgeAlarms(clearIdList, ActionType.Close);
            }
        } else {
            boolean isUpdate = true;
            if (alarm == null) {
                alarm = new AlarmRecord();
                alarm.setAlarmId(alarmId);
                isUpdate = false;
            }

            alarm.setAlarmText(alarmText);
            alarm.setSeverity(
                    alarmSeverity != null ? alarmSeverity
                            : AlarmSeverity.Cleared);
            alarm.setNmlKey(adapterId);
            alarm.setResourceRef(adapterId);
            alarm.setNeId("");
            alarm.setComponentRef("");
            if (!isUpdate) {
                alarm.setCreationTime(System.currentTimeMillis());
                alarm.setNmlReceivedTime(System.currentTimeMillis());
                alarmDaoService.saveAlarm(alarm);
            }
            updateIdList.add(alarm.getAlarmId());
        }
        isSuccess = true;
        alarmNotificationNotifier.notify(updateIdList, clearIdList);
    }

    public void generateTelemetryServerAlarm(String telemetryServerName, boolean isClear) {
        log.debug("generate alarm for telemetry server {} isClear {}", telemetryServerName,
                isClear);
        boolean isSuccess = false;
        List<String> updateIdList = new ArrayList<>();
        List<String> clearIdList = new ArrayList<>();
        String alarmText = null;
        String alarmId = null;
        alarmId = "telemetry server isolation;" + telemetryServerName;
        alarmText = "telemetry server isolation";
        AlarmRecord alarm = alarmDaoService.getAlarm(alarmId);
        if (isClear) {
            if (alarm != null) {
                clearIdList.add(alarm.getAlarmId());
                alarmDaoService.purgeAlarms(clearIdList, ActionType.Close);
            }
        } else {
            boolean isUpdate = true;
            if (alarm == null) {
                alarm = new AlarmRecord();
                alarm.setAlarmId(alarmId);
                isUpdate = false;
            }

            alarm.setAlarmText(alarmText);
            alarm.setSeverity(AlarmSeverity.Critical);
            alarm.setNmlKey(telemetryServerName);
            alarm.setResourceRef(telemetryServerName);
            alarm.setNeId("");
            alarm.setComponentRef("");
            if (!isUpdate) {
                alarm.setCreationTime(System.currentTimeMillis());
                alarm.setNmlReceivedTime(System.currentTimeMillis());
            }
            alarmDaoService.saveAlarm(alarm);
            updateIdList.add(alarm.getAlarmId());
        }
        isSuccess = true;
        alarmNotificationNotifier.notify(updateIdList, clearIdList);
    }

    public void generateSynchronizationAlarm(String neId, boolean isClear) {
        log.debug("generate synchronization alarm for ne {} isClear {}", neId, isClear);
        boolean isSuccess = false;
        List<String> updateIdList = new ArrayList<>();
        List<String> clearIdList = new ArrayList<>();
        AlarmRecord alarm = null;
        String alarmText = null;
        String alarmId = null;
        alarmId = "ne synchronization failure;" + neId;
        alarmText = "NE_Synchronaization_Failure;Controller can't synchronize the NE correctly.";
        alarm = alarmDaoService.getAlarm(alarmId);
        if (isClear) {
            if (alarm != null) {
                clearIdList.add(alarm.getAlarmId());
                alarmDaoService.purgeAlarms(clearIdList, ActionType.Close);
            }
        } else {
            boolean isUpdate = true;
            if (alarm == null) {
                alarm = new AlarmRecord();
                alarm.setAlarmId(alarmId);
                isUpdate = false;
            }

            alarm.setAlarmText(alarmText);
            alarm.setSeverity(AlarmSeverity.SynchronizationException);
            alarm.setNmlKey(neId);
            alarm.setAlarmGroup("NE_SYNC");
            alarm.setResourceRef(neId);
            alarm.setNeId("");
            alarm.setAlarmTypeId("NE");
            alarm.setComponentRef("");
            alarm.setSa(false);
            if (!isUpdate) {
                alarm.setCreationTime(System.currentTimeMillis());
                alarm.setNmlReceivedTime(System.currentTimeMillis());
            }
            alarmDaoService.saveAlarm(alarm);
            updateIdList.add(alarm.getAlarmId());
        }
        isSuccess = true;
        alarmNotificationNotifier.notify(updateIdList, clearIdList);
    }
}
