package net.flex.dci.otc.controller.status.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @version 1.0
 * @date 2022/4/5 15:22
 */
public class Constants {


    public static final String MPO_SUFFIX = "MPO";

    public static final String COLON = ":";

    public static final int NODE_ORDER = -1;

    public static final int VIEW_NODE_ORDER = 0;

    public static final int SITE_NODE_ORDER = 1;

    public static final int PHY_LINK_ORDER = 2;

    public static final int SITE_LINK_ORDER = 3;

    public static final int OCH_LINK_ORDER = 4;

    public static final int TUNNEL_ORDER = 5;

    public static final String SITE_LINK_MODEL_KEY = "model";
    public static final String DISK_USAGE = "SYSTEM_DISK";

    public static final String DISK = "DISK";

    public static final List<String> SITE_LINK_PROTECT_MODELS = new ArrayList<String>() {
        {
            add("3");

            add("4");

            add("5");

            add("6");
        }
    };

    public static final String SITE_LINK_PATTERN = "SiteLink*";

    public static final String CHASSIS = "CHASSIS";

    public static final int REF_PORT_ALARM_LENGTH = 4;

    public static final int REF_EQUIP_ALARM_LENGTH = 3;

    public static final int REF_PHY_NODE_ALARM_LENGTH = 2;

    public static final String APS_PREFIX = "APS";


    public static final String TRANSCEIVER = "TRANSCEIVER";


    public static String UTF8_encoding = "UTF8";

    public static final int DEFAULT_ALARM_REFRESH_THREAD_POOL_SIZE = 10;

    public static String VIRTUAL = "VIRTUAL";

    public static String OD_PREFIX = "OD:";

    public static String TD_PREFIX = "TD:";

    public static String PHY_LINK_PREFIX = "PHY_LINK_LOCK::";

    public static String OCH_LINK_PREFIX = "OCH_LINK_LOCK::";

    public static String TUNNEL_PREFIX = "TUNNEL_LOCK::";

    public static final String SITE_LINK_PREFIX = "SITE_LINK_LOCK::";

    public static final String LINE_PORT_SUFFIX = "-LINE";

    public static final String SIG_PORT_SUFFIX = "-SIG";

    public static final String LINE_PORT_REGEX = "-LINE$";

    public static final String SIG_PORT_REGEX = "-SIG$";

    public static final String EMPTY = "__EMPTY__";

    public static final String ALARM_HASH_CNT_KEY = "alarms:cnt:";

    public static final String ALARM_HASH_NE_CNT_KEY = "alarms:cnt:ne:";

    public static final String ALARM_KEY_PATTERN = "alarms:*";

}
