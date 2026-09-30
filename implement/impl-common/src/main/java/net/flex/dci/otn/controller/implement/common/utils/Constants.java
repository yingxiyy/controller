package net.flex.dci.otn.controller.implement.common.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Constants {

    public static final String LAG_SUFFIX = "MPO";
    public static final String MPO_PREFIX = "MPO";
    public static final int LAG_SIZE = 8;
    public static final String SITE_PREFIX = "Site-";
    public static final String SITE_SPLITTER = "-Site-";
    public static final String POUND = "#";
    public static final String COLON = ":";
    public static final String AT = "@";
    public static final String BLANK = "";

    public static final String LINK_SEPARATOR = "--";

    public static final int SCHEDULE_DELAY = 5;
    public static final int SCHEDULE_PERIOD = 5;

    public static final int LOCK_TIME_OUT = 60;

    public static final String MUX_PORT_SUFFIX = "D";

    public static final String CUSTOM_INFO = "custom-info";

    public static final String DESCRIPTION = "description";

    public static final int CUSTOM_INFO_LENGTH = 128;

    public static final int DESCRIPTION_LENGTH = 128;

    public static final String SUCCESSFULLY = " successfully.";

    public static final String FAILED = "OPERATION FAIL";

    public static final String CONFIG_FAILED = "config fail";

    public static final String REASON_IS = " THE reason is:";

    public static final String RUN_AS_ASYNC = "run as async";

    public static final String SWITCH_CU_TITLE = "SWITCH_CU_";

    public static final String UPLOAD_NE_HISTORY_PM = "UPLOAD_NE_HISTORY_PM_";

    public static final String INTERVAL = "INTERVAL";

    public static class APS_ELEMENT {

        public static final String FORCE_TO_PORT = "force-to-port";

        public static final String FORCE_TO_PORT_TARGET_PATH = "force-to-port-target-path.path";

        public static final String FORCE_TO_PORT_TARGET_INDEX = "force-to-port-target-path.index";

        public static final String ACTIVE_PATH_PATH = "active-path.path";

        public static final String ACTIVE_PATH_INDEX = "active-path.index";

        public static final String AUTO_SWITCH_TEMPLATE = "%s_%s/%s";

        public static final String NONE_AUTO_SWITCH_TEMPLATE = "%s_%s_%s/%s";

        public static final String RESTORE_PATH_TEMPLATE = "RESTORE_PATH_%s/%s/MEMBER_%s";
        public static final String APS_COMMAND_MASSAGE_TEMPLATE = "";

        public static final String BATCH_APS_SWITCH_TEMPLATE = "BATCH_SWITCH_%d_TUNNELS_%s";

        public static final String BATCH_APS_SWITCH_START = "Starting batch tunnel switch operation %s ";

        public static final String BATCH_APS_SWITCH_RESULT = "Results: Success(%d) Failed(%d) Total(%d) Success rate(%.1f%%)";

        public static final String TUNNEL_AUTO_SWITCH_TEMPLATE = "%s_%s/%s::%s";

        public static final String TUNNEL_NONE_AUTO_SWITCH_TEMPLATE = "%s_%s_%s/%s::%s";

        public static final String CONFIG_APS_TOPIC_TEMPLATE = "APS_CONFIGURATION_%s/%s::%s";

        public static final String CONFIG_APS_MESSAGE = "config aps %s/%s";

        public static final String PATH_ALREADY_SET = "target path already set";

        public static final String EXCEPTION_REASON_PREFIX = "Exception is: ";

        public static final String START = "start";

        public static final String FINISH = "finish";

        public static final List<String> APS_GROUP_MEMBER_PREFIX = Collections.unmodifiableList(
                Arrays.asList("A.", "B.", "C."));

        public static final String PATH_CONDITION_SUFFIX = ".path-condition";

    }


    public static class PHYSICAL_ELEMENT {

        public static final String BATCH_UPDATE_NODE_PHYSICAL = "BATCH_UPDATE_NE_%d_%s";

        public static final String UPDATE_NODE_FRIENDLY_NAME = "NODE_NAME_UPDATE: %s→%s";

        public static final String UPDATE_NODE_LOGIN_INFO = "Update node login info: %s";
    }

}
