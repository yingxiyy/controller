package net.flex.dci.otc.controller.ne.manager.utils;

/**
 * @version 1.0
 * @date 2022/1/28 10:34
 */

public class NeManagerConstants {

    public static final String SCHEDULE_JOB_PREFIX = "NE_SYNC_TASK_";


    public static final String ABNORMAL_OPER_STATUS = "abnormal-oper-status";

    public static final String MISALIGN = "misAlign";

    public static final String INSTALL_NONE = "NONE";

    public static final String BLANK = "BLANK";

    public static final String BLANK_SLOT_PREFIX = "SLOT";

    public static final String LINECARD = "LINECARD";

    public static final String TRANSCEIVER = "TRANSCEIVER";

    public static final String PANEL = "PANEL";

    public static final String CHASSIS = "CHASSIS";

    public static final String MUXPANEL = "MAXPANEL";

    public static final String MUX = "MUX";

    public static final String MPO = "MPO";

    public static final String CROSS_CONNECTION = "CROSSCONNECTION";

    public static final String LINK = "LINK";

    public static final String CARD = "CARD";

    public static final String ELEMENT_SPECIAL = "element";

    public static final String SPECIAL_EQUIPMENT = "equipment";

    public static final String BOARD = "board";

    public static final String NON_BUSINESS = "nonBusiness";

    public static final String TYPE = "type";
    public static final String TWO_UNIT = "two.unit";

    public static final String COMMA = ",";

    public static final String SLOT_PREFIX = "SLOT";

    public static final String HOSTNAME = "hostName";

    public static final String YANG_MODEL = "yang-model";

    public static final String NE_YANG_VERSION = "ne.yang-version";

    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    public static final String TIMEZONE = "timezone";
    public static final Long DEFAULT_MINUTES_INTERVAL = 2L;
    public static final String CLEAR_SUCCESS = "cleared";

    public static final String OBJ_CHANGE = "OBJ_CHANGE";

    public final static String CURRENT_SOFTWARE_VERSION = "software-version";

    public final static String PROPERTY_UNKNOWN = "--";

    public static final class DefaultSystemConfig {

        public static final String DEFAULT_HOST = "127.0.0.1";

        public static final String TIME_ZONE = "timezone";

        public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";
    }

    public static final class DefaultRack {

        public static final String MANUAL_DEFAULT_RACK_PREFIX = "MANUAL_RACK_";

        public static final String MANUAL_DEFAULT_RACK_ID_PREFIX = "Rack-MANUAL-RACK-";

        public static final int DEFAULT_START_RACK = 36;
    }

    public static final class TelemetryConfig {

        public static final String TELEMETRY_SENSOR_GROUP_FILE = "telemetrySensorGroup.json";

        public static final String TELEMETRY_MANAGER_CONFIG_FILE = "telemetryMgrConfig.json";
    }

    public static final class NeImplementState {

        public static final String NE_STATE_CHANGE = "NE_STATE_CHANGE";
        public static final String IMPLEMENT = "implement";
        public static final String ALLOCATE = "allocate";
    }

    public static final class NeConnStatus {

        public static final String CONN_STATUS = "CONN_STATUS";

        public static final String DISCONNECT_STATE = "DISCONNECTED";

        public static final String SYNCHRONIZE_STATE = "SYNC_OK";

        public static final String CONNECTED_STATE = "CONNECTED";
    }

    public static final class NeResourceLock {

        public static final String NE_RESOURCE_LOCK_PATH = "/ne-resources/%s/lock";
    }

    public static final class NeNtpVersion {

        public static final String VENDOR_HUAWEI = "HUAWEI";
    }

    public static final class Northbound {

        public static final String TELEMETRY_SENSOR_GROUP_PREFIX = "NorthBound-";

    }

    public static final class RegisterError {

        public static final String UNREACHABLE_ERROR = "No route to host";
        public static final String LOGIN_FAILED_ERROR = "Authentication failed";
        public static final String CONNECTION_TIME_OUT_ERROR = "connect timed out";
    }

    public static final String MODULE = "neMgr";

}