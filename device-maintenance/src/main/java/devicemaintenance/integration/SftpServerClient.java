package devicemaintenance.integration;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zkclient4boot.refactor.utils.DciInstancesUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * SFTP服务器客户端
 * 用于与sftpserver微服务通信，管理SFTP服务器信息
 */
@Component
@Slf4j
public class SftpServerClient {

    private final RestTemplate restTemplate;

    @Value("${sftp-server.base-url:}")
    private String sftpServerBaseUrl;

    @Autowired
    public SftpServerClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 获取SFTP服务器的基础URL
     * 优先使用配置文件中的URL，如果没有配置则通过服务发现获取
     */
    private String getSftpServerBaseUrl() {
        // 1. 优先使用配置文件中的URL
        if (sftpServerBaseUrl != null && !sftpServerBaseUrl.trim().isEmpty()) {
            log.debug("Using configured SFTP server URL: {}", sftpServerBaseUrl);
            return sftpServerBaseUrl.trim();
        }
        
        // 2. 尝试通过服务发现获取
        Optional<InstanceDetails> instance = findSftpInstance("sftpserver");
        if (!instance.isPresent()) {
            instance = findSftpInstance("ftpServer");
        }
        if (instance.isPresent()) {
            InstanceDetails detail = instance.get();
            String host = firstNonBlank(detail.getHostIp(), detail.getMyIp());
            int port = detail.getPort();
            if (host != null && port > 0) {
                String discoveredUrl = "http://" + host + ":" + port;
                log.info("Using discovered SFTP server URL: {}", discoveredUrl);
                return discoveredUrl;
            }
        } else {
            log.warn("Service discovery returned empty result for sftpserver");
        }
        
        // 3. 如果都失败，提供详细的错误信息
        String errorMsg = String.format(
            "无法获取SFTP服务器地址。请检查: " +
            "1) 配置文件中是否设置了 sftp-server.base-url; " +
            "2) ZooKeeper服务发现是否可用; " +
            "3) sftpserver服务是否在ZooKeeper中注册。" +
            "当前配置: sftp-server.base-url=%s", 
            sftpServerBaseUrl
        );
        throw new RuntimeException(errorMsg);
    }

    private Optional<InstanceDetails> findSftpInstance(String serviceName) {
        List<String> aliases = buildAliases(serviceName);
        for (String alias : aliases) {
            List<InstanceDetails> details = DciInstancesUtils.getStateInstancesByModuleName(alias);
            if (details != null) {
                for (InstanceDetails detail : details) {
                    if (detail != null && detail.getPort() > 0) {
                        return Optional.of(detail);
                    }
                }
            }
        }
        return Optional.empty();
    }

    private List<String> buildAliases(String serviceName) {
        Set<String> aliases = new LinkedHashSet<>();
        if (serviceName == null || serviceName.trim().isEmpty()) {
            return new ArrayList<>(aliases);
        }
        String trimmed = serviceName.trim();
        aliases.add(trimmed);
        aliases.add(trimmed.toLowerCase(Locale.ROOT));
        aliases.add(trimmed.toUpperCase(Locale.ROOT));
        aliases.add(trimmed.replace("_", "").replace("-", ""));
        aliases.add(camelize(trimmed, "-"));
        aliases.add(camelize(trimmed, "_"));
        return new ArrayList<>(aliases);
    }

    private String camelize(String value, String delimiter) {
        if (!value.contains(delimiter)) {
            return capitalize(value);
        }
        StringBuilder builder = new StringBuilder();
        String[] parts = value.split(delimiter);
        for (String part : parts) {
            if (!part.isEmpty()) {
                builder.append(capitalize(part));
            }
        }
        return builder.toString();
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() == 1) {
            return value.toUpperCase(Locale.ROOT);
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * 获取SFTP服务器信息
     */
    public SftpServerResponse getSftpServerInfo() {
        try {
            String baseUrl = getSftpServerBaseUrl();
            log.info("Getting SFTP server info from: {}", baseUrl);
            
            // 调用健康检查接口来验证服务可用性
            String healthUrl = baseUrl + "/actuator/health";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<Map> response = restTemplate.exchange(
                healthUrl, HttpMethod.GET, entity, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> healthData = response.getBody();
                
                // 构建SFTP服务器信息响应
                Map<String, Object> sftpInfo = new HashMap<>();
                sftpInfo.put("serverUrl", baseUrl);
                sftpInfo.put("status", healthData != null ? healthData.get("status") : "UP");
                sftpInfo.put("serviceType", "SFTP Server Manager");
                sftpInfo.put("version", "1.0.0-SNAPSHOT");
                sftpInfo.put("description", "SFTP服务器管理微服务");
                sftpInfo.put("supportedOperations", new String[]{
                    "create-ftp-server", "update-ftp-server", "delete-ftp-server"
                });
                
                return SftpServerResponse.success(sftpInfo, "SFTP服务器信息获取成功");
            } else {
                return SftpServerResponse.failure("SFTP服务器不可用", response.getStatusCode().value());
            }
            
        } catch (Exception e) {
            log.error("Failed to get SFTP server info", e);
            return SftpServerResponse.failure("获取SFTP服务器信息失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 创建SFTP服务器
     */
    public SftpServerResponse createSftpServer(String serverName, String host, int port, 
                                              String username, String password) {
        try {
            String baseUrl = getSftpServerBaseUrl();
            log.info("Creating SFTP server: {} at {}:{}", serverName, host, port);
            
            String createUrl = baseUrl + "/restconf/operations/ftp-server:create-ftp-server";
            
            // 构建请求体 (RESTCONF格式)
            Map<String, Object> input = new HashMap<>();
            input.put("name", serverName);
            input.put("host", host);
            input.put("port", port);
            input.put("username", username);
            input.put("password", password);
            input.put("protocol", "SFTP");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", input);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                createUrl, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return SftpServerResponse.success(response.getBody(), "SFTP服务器创建成功");
            } else {
                return SftpServerResponse.failure("SFTP服务器创建失败", response.getStatusCode().value());
            }
            
        } catch (Exception e) {
            log.error("Failed to create SFTP server", e);
            return SftpServerResponse.failure("创建SFTP服务器失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 更新SFTP服务器
     */
    public SftpServerResponse updateSftpServer(String serverName, String host, int port, 
                                              String username, String password) {
        try {
            String baseUrl = getSftpServerBaseUrl();
            log.info("Updating SFTP server: {} at {}:{}", serverName, host, port);
            
            String updateUrl = baseUrl + "/restconf/operations/ftp-server:update-ftp-server";
            
            // 构建请求体 (RESTCONF格式)
            Map<String, Object> input = new HashMap<>();
            input.put("name", serverName);
            input.put("host", host);
            input.put("port", port);
            input.put("username", username);
            input.put("password", password);
            input.put("protocol", "SFTP");
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", input);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                updateUrl, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return SftpServerResponse.success(response.getBody(), "SFTP服务器更新成功");
            } else {
                return SftpServerResponse.failure("SFTP服务器更新失败", response.getStatusCode().value());
            }
            
        } catch (Exception e) {
            log.error("Failed to update SFTP server", e);
            return SftpServerResponse.failure("更新SFTP服务器失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 查询所有SFTP管理微服务配置
     */
    public SftpServerResponse getAllSftpServers() {
        try {
            String baseUrl = getSftpServerBaseUrl();
            log.info("Getting all SFTP servers from: {}", baseUrl);
            
            String queryUrl = baseUrl + "/restconf/config/ftp-server:ftp-servers";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                queryUrl, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return SftpServerResponse.success(response.getBody(), "SFTP管理微服务配置查询成功");
            } else {
                return SftpServerResponse.failure("SFTP管理微服务配置查询失败", response.getStatusCode().value());
            }
            
        } catch (Exception e) {
            log.error("Failed to get all SFTP servers", e);
            return SftpServerResponse.failure("查询SFTP管理微服务配置失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 查询特定SFTP服务器配置
     */
    public SftpServerResponse getSftpServer(String serverName) {
        try {
            String baseUrl = getSftpServerBaseUrl();
            log.info("Getting SFTP server '{}' from: {}", serverName, baseUrl);
            
            String queryUrl = baseUrl + "/restconf/config/ftp-server:ftp-servers/ftp-server/" + serverName;
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                queryUrl, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return SftpServerResponse.success(response.getBody(), "SFTP服务器配置查询成功");
            } else {
                return SftpServerResponse.failure("SFTP服务器配置查询失败", response.getStatusCode().value());
            }
            
        } catch (Exception e) {
            log.error("Failed to get SFTP server: {}", serverName, e);
            return SftpServerResponse.failure("查询SFTP服务器配置失败: " + e.getMessage(), 500);
        }
    }

    /**
     * 删除SFTP服务器
     */
    public SftpServerResponse deleteSftpServer(String serverName) {
        try {
            String baseUrl = getSftpServerBaseUrl();
            log.info("Deleting SFTP server: {}", serverName);
            
            String deleteUrl = baseUrl + "/restconf/operations/ftp-server:delete-ftp-server";
            
            // 构建请求体 (RESTCONF格式)
            Map<String, Object> input = new HashMap<>();
            input.put("name", serverName);
            
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("input", input);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                deleteUrl, HttpMethod.POST, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                return SftpServerResponse.success(response.getBody(), "SFTP服务器删除成功");
            } else {
                return SftpServerResponse.failure("SFTP服务器删除失败", response.getStatusCode().value());
            }
            
        } catch (Exception e) {
            log.error("Failed to delete SFTP server", e);
            return SftpServerResponse.failure("删除SFTP服务器失败: " + e.getMessage(), 500);
        }
    }

    /**
     * SFTP服务器响应封装类
     */
    public static class SftpServerResponse {
        private final boolean success;
        private final Object data;
        private final String message;
        private final int statusCode;
        private final long timestamp;

        private SftpServerResponse(boolean success, Object data, String message, int statusCode) {
            this.success = success;
            this.data = data;
            this.message = message;
            this.statusCode = statusCode;
            this.timestamp = System.currentTimeMillis();
        }

        public static SftpServerResponse success(Object data, String message) {
            return new SftpServerResponse(true, data, message, 200);
        }

        public static SftpServerResponse failure(String message, int statusCode) {
            return new SftpServerResponse(false, null, message, statusCode);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public Object getData() { return data; }
        public String getMessage() { return message; }
        public int getStatusCode() { return statusCode; }
        public long getTimestamp() { return timestamp; }
    }
}
