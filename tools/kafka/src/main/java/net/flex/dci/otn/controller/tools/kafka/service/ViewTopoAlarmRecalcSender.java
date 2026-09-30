package net.flex.dci.otn.controller.tools.kafka.service;

import java.util.List;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.view.ViewTopoAlarmRecalcMsg;
import net.flex.dci.otn.controller.tools.kafka.constants.KafkaTopics;

/**
 * 2026/2/21
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class ViewTopoAlarmRecalcSender {

    @Setter
    public static PublishService publishService;

    public static void sendAlarmRecalcMsg(ViewTopoAlarmRecalcMsg alarmRecalcMsg) {
        log.debug("publish current view topo alarm recalc  message:{}", alarmRecalcMsg);
        String key = extractKey(alarmRecalcMsg);
        publishService.send(KafkaTopics.VIEW_TOPO_ALARM_RECALC_TOPIC, key, alarmRecalcMsg);
    }

    private static String extractKey(ViewTopoAlarmRecalcMsg msg) {
        if (msg.getViewNodeIds() != null && !msg.getViewNodeIds().isEmpty()) {
            return msg.getViewNodeIds().get(0);
        }
        if (msg.getViewLinkIds() != null && !msg.getViewLinkIds().isEmpty()) {
            return msg.getViewLinkIds().get(0);
        }
        return "VIEW_TOPO_RECALC";
    }

    public static void sendAlarmRecalcMsg(ViewTopoAlarmRecalcMsg alarmRecalcMsg, String key) {
        log.debug("publish current view topo alarm recalc message with key: {}, message:{}", key,
                alarmRecalcMsg);
        publishService.send(
                KafkaTopics.VIEW_TOPO_ALARM_RECALC_TOPIC, key, alarmRecalcMsg);
    }

    private static int getSize(List<String> list) {
        return list == null ? 0 : list.size();
    }

    public static void initializeTopics() {
        publishService.newTopic(KafkaTopics.VIEW_TOPO_ALARM_RECALC_TOPIC);
    }
}
