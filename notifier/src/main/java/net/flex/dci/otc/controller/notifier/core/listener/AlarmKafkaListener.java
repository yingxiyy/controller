package net.flex.dci.otc.controller.notifier.core.listener;

import com.alibaba.fastjson.JSON;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.aspect.Log;
import net.flex.dci.otc.common.model.alarm.Alarm;
import net.flex.dci.otc.common.model.alarm.CtrlAlarm;
import net.flex.dci.otc.controller.notifier.core.domain.alarm.AlarmDetail;
import net.flex.dci.otc.controller.notifier.core.domain.alarm.AlarmDetailNotification;
import net.flex.dci.otc.controller.notifier.core.domain.alarm.AlarmNotification;
import net.flex.dci.otc.controller.notifier.core.domain.alarm.AlarmNotificationBody;
import net.flex.dci.otc.controller.notifier.core.websocket.WebSocketServerManager;
import net.flex.dci.otc.controller.notifier.utils.DateTimeUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@ConditionalOnClass({WebMvcConfigurer.class, WebSocketServerManager.class})
@Component
@Slf4j
public class AlarmKafkaListener implements MessageListener<String, String> {


    @Autowired
    private WebSocketServerManager webSocketServerManager;


    public AlarmKafkaListener() {

    }

    @Override
    @Log
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("start to handle the record :{}", record);
        CtrlAlarm alarm = JSON.parseObject(record.value(), CtrlAlarm.class);
        AlarmDetailNotification alarmNotification = buildAlarmNotification(alarm);
        sendToUI(alarmNotification);
    }

    private void sendToUI(AlarmDetailNotification alarmNotification) {
        log.debug("send alarm notification to the ui, notification :{}", alarmNotification);
        String alarmJson = JSON.toJSONString(alarmNotification);
        try {
            webSocketServerManager.sendAlarmChangeMessage(alarmJson);
        } catch (Exception e) {
            log.error("failed to send the alarm to the ui,error :{}", e.getMessage(), e);
        }

    }

    /**
     * format the alarm to websocket notification
     *
     * @param alarm
     * @return
     */
    private AlarmDetailNotification buildAlarmNotification(CtrlAlarm alarm) {
        log.info("build alarm detail the alarm is:{}", alarm);
        List<AlarmDetail> newAlarms = alarm.getNewAlarms().stream()
                .map(this::convert2AlarmDetail).collect(Collectors.toList());
        Long eventTimestamp = alarm.getEventTime();
        List<String> clearAlarmIds = alarm.getClearAlarms().stream().map(Alarm::getId).collect(
                Collectors.toList());
        List<AlarmDetail> removeAlarms = alarm.getClearAlarms().stream()
                .map(this::convert2AlarmDetail).collect(
                        Collectors.toList());
        return AlarmDetailNotification.builder().alarmNotification(
                        AlarmNotification.builder().notificationBody(AlarmNotificationBody.builder()
                                .newAlarm(newAlarms.isEmpty() ? null : newAlarms)
                                .removeAlarmId(clearAlarmIds.isEmpty() ? null : clearAlarmIds)
                                .removeAlarm(removeAlarms)
                                .build()).eventTime(DateTimeUtils.convert2Date(eventTimestamp)).build())
                .build();
    }

    private AlarmDetail convert2AlarmDetail(Alarm alarm) {
        AlarmDetail alarmDetail = AlarmDetail.builder()
                .alarmGroup(alarm.getGroup())
                .alarmTypeId(alarm.getAlarmTypeId())
                .neIp(alarm.getIp())
                .neId(alarm.getNeId())
                .sa(alarm.getServiceAffect())
                .alarmText(alarm.getText())
                .componentRef(alarm.getComponent())
                .nmlKey(alarm.getToopKey())
                .alarmId(alarm.getId())
                .nmlKeyName(alarm.getToopKeyName())
                .neId(alarm.getNeId())
                .creationTime(alarm.getTimeCreated())
                .severity(alarm.getSeverity() == null ? null : alarm.getSeverity().toLowerCase())
                .resourceRef(alarm.getResource())
                .index(alarm.getIndex())
                .build();
        return alarmDetail;
    }
}
