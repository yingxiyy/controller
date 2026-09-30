/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.alarm.controller;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import net.flex.dci.otn.db.jpa.entity.AlarmHistoryRecord;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.ArchiveType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.springframework.stereotype.Component;

@Component
public class AlarmOutputAdapter {


    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.current.alarms.output.Alarm> adapterAlarmList(
            List<AlarmRecord> alarmList) throws Exception {
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.current.alarms.output.Alarm> retList =
                new ArrayList<>();
        if (alarmList != null) {
            for (AlarmRecord alarm : alarmList) {
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.current.alarms.output.AlarmBuilder builder =
                        new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.current.alarms.output.AlarmBuilder();
                builder.setAlarmId(alarm.getAlarmId());
                builder.setServerity(alarm.getSeverity() != null ? AlarmSeverity
                        .forValue(alarm.getSeverity().getIntValue()) : null);
                builder.setResourceRef(alarm.getResourceRef());
                builder.setAlarmText(alarm.getAlarmText());
                builder.setAlarmGroup(alarm.getAlarmGroup());
                builder.setAlarmTypeId(alarm.getAlarmTypeId());
                builder.setCreationTime(BigInteger.valueOf(alarm.getCreationTime()));
                builder.setCreationReceivedTime(
                        BigInteger.valueOf(alarm.getNmlReceivedTime()));
//				builder.setClearTime()
//				builder.setClearReceivedTime()
//				builder.setArchiveTime(value)
//				builder.setArchiveType(value)
                builder.setNeId(alarm.getNeId());
                builder.setNmlKey(alarm.getNmlKey());
                builder.setNmlKeyName(
                        alarm.getNmlKeyName() != null ? alarm.getNmlKeyName() : alarm.getNmlKey());
                builder.setSa(alarm.getSa());
                builder.setEquipmentRef(alarm.getComponentRef());
                builder.setAlarmIndex(alarm.getIndex());
                retList.add(builder.build());
            }
        }
        return retList;
    }

    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.history.alarms.output.Alarm> adapterHistoryAlarmList(
            List<AlarmHistoryRecord> alarmList) throws Exception {
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.history.alarms.output.Alarm> retList = new ArrayList<>();
        if (alarmList != null) {
            for (AlarmHistoryRecord alarm : alarmList) {
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.history.alarms.output.AlarmBuilder builder =
                        new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.history.alarms.output.AlarmBuilder();
                builder.setAlarmId(alarm.getAlarmId());
                builder.setServerity(alarm.getSeverity() != null ? AlarmSeverity
                        .forValue(alarm.getSeverity().getIntValue()) : null);
                builder.setResourceRef(alarm.getResourceRef());
                builder.setAlarmText(alarm.getAlarmText());
                builder.setAlarmGroup(alarm.getAlarmGroup());
                builder.setAlarmTypeId(alarm.getAlarmTypeId());
                builder.setCreationTime(BigInteger.valueOf((alarm.getCreationTime())));
                builder.setCreationReceivedTime(
                        BigInteger.valueOf(alarm.getNmlReceivedTime()));
                builder.setClearTime(BigInteger.valueOf(alarm.getClearedTime()));
                builder.setArchiveTime(BigInteger.valueOf(alarm.getArchivedTime()));
                builder.setArchiveType(alarm.getActionType() != null ? ArchiveType
                        .forValue(alarm.getActionType().getIntValue()) : null);
                builder.setNeId(alarm.getNeId());
                builder.setNmlKey(alarm.getNmlKey());
                builder.setNmlKeyName(
                        alarm.getNmlKeyName() != null ? alarm.getNmlKeyName() : alarm.getNmlKey());
                builder.setSa(alarm.getSa());
                builder.setEquipmentRef(alarm.getComponentRef());
                builder.setAlarmIndex(alarm.getIndex());
                retList.add(builder.build());
            }
        }
        return retList;
    }

    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.alarms.output.Alarm> adapterHistoryAlarmListForAll(
            List<AlarmHistoryRecord> alarmList) throws Exception {
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.alarms.output.Alarm> retList = new ArrayList<>();
        if (alarmList != null) {
            for (AlarmHistoryRecord alarm : alarmList) {
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.alarms.output.AlarmBuilder builder =
                        new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.get.alarms.output.AlarmBuilder();
                builder.setAlarmId(alarm.getAlarmId());
                builder.setServerity(alarm.getSeverity() != null ? AlarmSeverity
                        .forValue(alarm.getSeverity().getIntValue()) : null);
                builder.setResourceRef(alarm.getResourceRef());
                builder.setAlarmText(alarm.getAlarmText());
                builder.setAlarmGroup(alarm.getAlarmGroup());
                builder.setAlarmTypeId(alarm.getAlarmTypeId());
                builder.setCreationTime(BigInteger.valueOf(alarm.getCreationTime()));
                builder.setCreationReceivedTime(
                        BigInteger.valueOf(alarm.getNmlReceivedTime()));
                builder.setClearTime(BigInteger.valueOf(alarm.getClearedTime()));
                builder.setArchiveTime(BigInteger.valueOf(alarm.getArchivedTime()));
                builder.setArchiveType(alarm.getActionType() != null ? ArchiveType
                        .forValue(alarm.getActionType().getIntValue()) : null);
                builder.setNeId(alarm.getNeId());
                if (alarm.getClearedTime() != null || alarm.getArchivedTime() != null) {
                    builder.setSearchType("history");
                } else {
                    builder.setSearchType("current");
                }
                builder.setNmlKey(alarm.getNmlKey());
                builder.setNmlKeyName(
                        alarm.getNmlKeyName() != null ? alarm.getNmlKeyName() : alarm.getNmlKey());
                builder.setSa(alarm.getSa());
                builder.setEquipmentRef(alarm.getComponentRef());
                builder.setAlarmIndex(alarm.getIndex());
                retList.add(builder.build());
            }
        }
        return retList;
    }
}
