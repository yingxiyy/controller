package net.flex.dci.otn.controller.sftp.manager.utils;

import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;

/**
 *
 * @version 1.0
 * @date 9/23/2025 1:10 PM
 */
public class FtpManagerUtil {

    public static final String RESULT_SUCCESS = "success";

    public static void notify(String title, String message, boolean error) {
        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title(title)
                        .message(message)
                        .error(error)
                        .build()
        );
    }

    public static String extractFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastSeparator >= 0 && lastSeparator < filePath.length() - 1) {
            return filePath.substring(lastSeparator + 1);
        }

        return filePath;
    }

    public static String extractDirectoryPath(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }

        int lastSeparator = Math.max(filePath.lastIndexOf('/'), filePath.lastIndexOf('\\'));
        if (lastSeparator > 0) {
            return filePath.substring(0, lastSeparator);
        }

        return ".";
    }
}
