package net.flex.dci.otn.controller.schedule.task.detail.controller;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Arrays;

public class MysqlAux {
    private final String jdbcUrl;

    public String detectPrimary(List<String> hosts, String username, String password) {
        for (String host : hosts) {
            String hostOnly = host.contains(":") ? host.split(":")[0] : host;
            int port = host.contains(":") ? Integer.parseInt(host.split(":")[1]) : 3306;

            String url = String.format("jdbc:mysql://%s:%d/mysql", hostOnly, port);

            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                boolean readOnly = conn.isReadOnly();
                if (!readOnly) {
                    return host;
                }
            } catch (SQLException e) {
                // ignore and try next
            }
        }
        throw new RuntimeException("No writable MySQL primary found in cluster");
    }

    public MysqlAux(String jdbcUrl) {
        if (!jdbcUrl.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("Invalid MySQL JDBC URL: " + jdbcUrl);
        }
        this.jdbcUrl = jdbcUrl.substring("jdbc:mysql://".length());
    }

    public List<String> getHosts() {
        String hostsPart = jdbcUrl.substring(0, jdbcUrl.indexOf("/"));
        return Arrays.asList(hostsPart.split(","));
    }

    public String getDatabase() {
        int start = jdbcUrl.indexOf("/") + 1;
        int end = jdbcUrl.contains("?") ? jdbcUrl.indexOf("?") : jdbcUrl.length();
        return jdbcUrl.substring(start, end);
    }

    public int getPort(String host) {
        if (host.contains(":")) {
            return Integer.parseInt(host.split(":")[1]);
        } else {
            return 3306;
        }
    }

    public String getHostOnly(String host) {
        return host.contains(":") ? host.split(":")[0] : host;
    }
}
