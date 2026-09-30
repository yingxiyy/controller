package net.flex.dci.otn.controller.cli.utils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

/**
 *
 * @version 1.0
 * @date 9/17/2025 2:23 PM
 */
@Slf4j
public class CliServiceUtils {

    public static final String SESSION_ID = "sessionId";

    public static final List<String> CLI_SERVICE_MODULE = Arrays.asList("cli-service",
            "cliService");

    public static final Pattern IPV4_PATTERN = Pattern.compile(
            "\\b((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\b");

    public static boolean isValidIp(String ip) {
        log.debug("validate ip:{}", ip);
        return IPV4_PATTERN.matcher(ip).matches();
    }

    public static String getSessionIdFromSSHId(String sessionId) {
        return sessionId.replaceAll("-ssh", "");
    }

    public static String getSshSessionId(String sessionId) {
        return sessionId + "-ssh";
    }
}
