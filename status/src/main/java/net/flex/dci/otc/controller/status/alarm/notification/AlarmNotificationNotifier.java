/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.alarm.notification;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.serialization.JsonUtil;
import net.flex.dci.otn.controller.tools.kafka.service.PublishService;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import net.flex.dci.otn.db.jpa.service.dao.AlarmDaoService;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmNotification;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmNotificationBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.alarm.notification.NewAlarm;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.alarm.notification.NewAlarmBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.alarm.notification.NewAlarmKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yangtools.yang.common.QName;
import org.opendaylight.yangtools.yang.model.api.NotificationDefinition;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

@Component
@RequiredArgsConstructor
public class AlarmNotificationNotifier {

    private final AlarmDaoService alarmDaoService;

    private final PublishService publishService;

    private final JsonUtil jsonUtil;

    public void notify(List<String> alarmIdList, List<String> clearedIdList) {
        try {
            if (!CollectionUtils.isEmpty(alarmIdList)) {
                AlarmNotificationBuilder alarmBuilder = new AlarmNotificationBuilder();
                List<AlarmRecord> alarmList = alarmDaoService.getAlarmList(alarmIdList);
                alarmBuilder.setNewAlarm(this.adaptAlarmList(alarmList));
                sendToKafka(alarmBuilder.build());
            }

            if (!CollectionUtils.isEmpty(clearedIdList)) {
                AlarmNotificationBuilder alarmBuilder = new AlarmNotificationBuilder();
                alarmBuilder.setRemovedAlarmId(clearedIdList);
                sendToKafka(alarmBuilder.build());
            }
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.ALARM_ERROR,
                    "failed to send alarm notification " + e.getMessage(), e);
        }
    }

    private void sendToKafka(AlarmNotification alarmNotification) {
        QName qName = AlarmNotification.QNAME;
        NotificationDefinition notificationDefinition = jsonUtil
                .getNotificationDefinition(qName.getNamespace().toString(),
                        qName.getLocalName());
        String xml = jsonUtil.fromDataObjectToXml(notificationDefinition, alarmNotification);
        publishService.sendStringMessage("alarmNotifyTopic", xml);
    }

    private List<NewAlarm> adaptAlarmList(List<AlarmRecord> alarmList) throws Exception {
        List<NewAlarm> retList = new ArrayList<>();
        if (alarmList != null) {
            for (AlarmRecord alarm : alarmList) {
                NewAlarmBuilder newAlarmBuilder = new NewAlarmBuilder();
                newAlarmBuilder.setKey(new NewAlarmKey(alarm.getAlarmId()));
                newAlarmBuilder.setAlarmGroup(alarm.getAlarmGroup());
                newAlarmBuilder.setAlarmId(alarm.getAlarmId());
                newAlarmBuilder.setAlarmText(alarm.getAlarmText());
                newAlarmBuilder.setAlarmTypeId(alarm.getAlarmTypeId());
                newAlarmBuilder.setCreationReceivedTime(
                        BigInteger.valueOf(alarm.getNmlReceivedTime()));
                newAlarmBuilder.setCreationTime(BigInteger.valueOf(alarm.getCreationTime()));
                newAlarmBuilder.setNeId(alarm.getNeId());
                newAlarmBuilder.setNmlKey(alarm.getNmlKey());
                newAlarmBuilder.setNmlKeyName(alarm.getNmlKeyName());
                newAlarmBuilder.setResourceRef(alarm.getResourceRef());
                newAlarmBuilder.setSa(alarm.getSa());
                newAlarmBuilder.setServerity(alarm.getSeverity() != null ? AlarmSeverity
                        .forValue(alarm.getSeverity().getIntValue()) : null);
                retList.add(newAlarmBuilder.build());
            }
        }
        return retList;
    }

}
