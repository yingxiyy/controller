package net.flex.dci.otc.controller.notifier.utils;

/**
 * @version 1.0
 * @date 2022/4/2 14:20
 */
public class DateTimeUtils {

    private static final String DCI_TIMESTAMP_FORMATTER = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";

    public static String convert2Date(Long timestamp) {
        return timeStamp2Date(timestamp, DCI_TIMESTAMP_FORMATTER);
    }

    private static String timeStamp2Date(Long timestamp, String formats) {
        String date = new java.text.SimpleDateFormat(formats).format(new java.util.Date(timestamp));
        return date;
    }
}
