package net.flex.dci.otn.controller.schedule.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * 2026/6/2
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@Slf4j
public class MysqlConnectionInfo implements Serializable {

    private String host;

    private String database;

    private Integer port = 3306;

    public static MysqlConnectionInfo parseUrl(String url) {
        if (url == null || !url.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("Invalid MySQL JDBC URL: " + url);
        }
        String withoutPrefix = url.substring("jdbc:mysql://".length());
        int slashIndex = -1;
        int bracketCount = 0;
        for (int i = 0; i < withoutPrefix.length(); i++) {
            char c = withoutPrefix.charAt(i);
            if (c == '[') {
                bracketCount++;
            } else if (c == ']') {
                bracketCount--;
            } else if (c == '/' && bracketCount == 0) {
                slashIndex = i;
                break;
            }
        }
        if (slashIndex == -1) {
            throw new IllegalArgumentException("Missing database name in URL");
        }
        String hostPortPart = withoutPrefix.substring(0, slashIndex);
        String rest = withoutPrefix.substring(slashIndex + 1);
        String database = rest.split("\\?")[0];
        if (database.isEmpty()) {
            throw new IllegalArgumentException("Empty database name");
        }

        String host;
        int port = 3306;
        if (hostPortPart.startsWith("[") && hostPortPart.contains("]")) {
            int closeBracket = hostPortPart.indexOf(']');
            host = hostPortPart.substring(1, closeBracket);
            String afterBracket = hostPortPart.substring(closeBracket + 1);
            if (afterBracket.startsWith(":")) {
                String portStr = afterBracket.substring(1);
                if (!portStr.isEmpty()) {
                    try {
                        port = Integer.parseInt(portStr);
                    } catch (NumberFormatException e) {
                        // 端口解析失败，使用默认值
                        log.error("failed to parse the port,use default instead");
                    }
                }
            }
        } else {
            if (hostPortPart.contains(":")) {
                String[] parts = hostPortPart.split(":");
                host = parts[0];
                if (parts.length > 1) {
                    try {
                        port = Integer.parseInt(parts[1]);
                    } catch (NumberFormatException e) {
                        // 忽略，使用默认端口
                        log.error("failed to parse the port,use default instead");
                    }
                }
            } else {
                host = hostPortPart;
            }
        }

        return MysqlConnectionInfo.builder()
                .host(host)
                .port(port)
                .database(database)
                .build();

    }
}
