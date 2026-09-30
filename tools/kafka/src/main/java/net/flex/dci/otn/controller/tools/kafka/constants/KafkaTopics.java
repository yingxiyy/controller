package net.flex.dci.otn.controller.tools.kafka.constants;

import net.flex.dci.otc.common.util.Constant;

/**
 * @version 1.0
 * @date 2022/5/5 14:36
 */
public class KafkaTopics {

    public static final String DCI_ALARM_TOPIC = "dci-alarm";

    public static final String BROADCAST_TOPIC = "broadcastMessageTopic";

    public static final String NE_STATUS_TOPIC = "ne-status";

    public static final String TASK_INFO_TOPIC = Constant.TASKINFO_TOPIC;

    public static final String OBJECT_NOTIFICATION_TOPIC = "objectUpdateTopic";

    public static final String ELEMENT_CHANGE_TOPIC = "element-change";

    public static final String DCI_APP_ALARM_TOPIC = "dci-app-alarm";

    public static final String NE_CONN_STATUS_TOPIC = "ne-conn-status";

    public static final String STATUS_EVENT_TOPIC = "status-events";

    public static final String NE_OPERATION_TOPIC = "ne.ops.json";

    public static final String VIEW_TOPO_ALARM_RECALC_TOPIC = "view-topo-migration-alarm-recalc";
}
