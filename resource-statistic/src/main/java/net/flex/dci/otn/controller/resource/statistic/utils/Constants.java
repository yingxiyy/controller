package net.flex.dci.otn.controller.resource.statistic.utils;

import java.time.format.DateTimeFormatter;

/**
 * @version 1.0
 * @date 10/30/2025 4:52 PM
 */
public class Constants {

    public static String SOFTWARE_VERSION = "software-version";

    public static String PART_NO = "part-no";

    public static String FIRMWARE_VERSION = "firmware-version";

    public static String YANG_VERSION = "ne.yang-version";

    public static String MFG_DATE = "mfg-date";

    public static String SITE_INFO_ID = "siteInfoId";

    public static String DOT = ",";

    public static String DEFAULT_USER = "system";

    public static String DEFAULT_SUBNET_ID = "global_root_node";


    public static Integer BATCH_TASK_TIMEOUT = 10;

    public static DateTimeFormatter FILE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String EXCEL_SUFFIX = "xlsx";

    public static String CSV_SUFFIX = "csv";

    public static String TASK_FILE_NAME = "taskInfo.json";

    public static String LEG_REQUIRED = "leg-required";

    public static String QUERY_TASK_PREFIX = "QUERY_";

    public static String NE_TYPE = "neType";

    public static class Sheet {

        public static String LLDP = "LLDP";
        public static String TUNNEL = "TUNNEL";

        public static String PERFORMANCE = "PERFORMANCE";

        public static String CARD = "CARD";

        public static String TRANSCEIVER = "TRANSCEIVER";

        public static String DEFAULT = "default";
    }

    public static long BATCH_SIZE = 500L;

    public static String QUERY_PREFIX = "QUERY_";

    public static String SUBNET = "SUBNET";
}
