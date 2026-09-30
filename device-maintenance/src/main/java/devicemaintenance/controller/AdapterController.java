package devicemaintenance.controller;

import devicemaintenance.integration.SftpServerClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.HashMap;
import java.util.Map;

/**
 * 适配器控制器
 * 提供基本的健康检查接口和SFTP服务管理功能
 */
@RestController
@RequestMapping("/api/adapter")
@Validated
@Slf4j
public class AdapterController {

    private final SftpServerClient sftpServerClient;

    @Autowired
    public AdapterController(SftpServerClient sftpServerClient) {
        this.sftpServerClient = sftpServerClient;
    }

    /**
     * 健康检查接口 - 类似于ping
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Device Maintenance Adapter");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    /**
     * 获取SFTP管理微服务信息
     */
    @GetMapping("/sftp/info")
    public ResponseEntity<Map<String, Object>> getSftpServerInfo() {
        log.info("Getting SFTP server information");
        
        try {
            SftpServerClient.SftpServerResponse response = sftpServerClient.getSftpServerInfo();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.isSuccess());
            result.put("message", response.getMessage());
            result.put("data", response.getData());
            result.put("timestamp", response.getTimestamp());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting SFTP server info", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to get SFTP management service information: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 查询所有SFTP服务器配置
     */
    @GetMapping("/sftp/servers")
    public ResponseEntity<Map<String, Object>> getAllSftpServers() {
        log.info("Getting all SFTP server configurations");
        
        try {
            SftpServerClient.SftpServerResponse response = sftpServerClient.getAllSftpServers();
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.isSuccess());
            result.put("message", response.getMessage());
            result.put("data", response.getData());
            result.put("timestamp", response.getTimestamp());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting all SFTP servers", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to query all SFTP server configurations: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 查询特定SFTP服务器配置
     */
    @GetMapping("/sftp/servers/{serverName}")
    public ResponseEntity<Map<String, Object>> getSftpServer(
            @PathVariable @NotBlank(message = "Server name cannot be blank") String serverName) {
        
        log.info("Getting SFTP server configuration: {}", serverName);
        
        try {
            SftpServerClient.SftpServerResponse response = sftpServerClient.getSftpServer(serverName);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.isSuccess());
            result.put("message", response.getMessage());
            result.put("data", response.getData());
            result.put("timestamp", response.getTimestamp());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting SFTP server: {}", serverName, e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to query SFTP server configuration: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 创建SFTP服务器
     */
    @PostMapping("/sftp/create")
    public ResponseEntity<Map<String, Object>> createSftpServer(
            @Valid @RequestBody CreateSftpServerRequest request) {
        
        log.info("Creating SFTP server: {}", request.getServerName());
        
        try {
            SftpServerClient.SftpServerResponse response = sftpServerClient.createSftpServer(
                request.getServerName(), 
                request.getHost(), 
                request.getPort(), 
                request.getUsername(), 
                request.getPassword()
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.isSuccess());
            result.put("message", response.getMessage());
            result.put("data", response.getData());
            result.put("timestamp", response.getTimestamp());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error creating SFTP server", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to create SFTP server: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 更新SFTP服务器
     */
    @PutMapping("/sftp/update")
    public ResponseEntity<Map<String, Object>> updateSftpServer(
            @Valid @RequestBody UpdateSftpServerRequest request) {
        
        log.info("Updating SFTP server: {}", request.getServerName());
        
        try {
            SftpServerClient.SftpServerResponse response = sftpServerClient.updateSftpServer(
                request.getServerName(), 
                request.getHost(), 
                request.getPort(), 
                request.getUsername(), 
                request.getPassword()
            );
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.isSuccess());
            result.put("message", response.getMessage());
            result.put("data", response.getData());
            result.put("timestamp", response.getTimestamp());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error updating SFTP server", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to update SFTP server: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    /**
     * 删除SFTP服务器
     */
    @DeleteMapping("/sftp/delete/{serverName}")
    public ResponseEntity<Map<String, Object>> deleteSftpServer(
            @PathVariable @NotBlank(message = "Server name cannot be blank") String serverName) {
        
        log.info("Deleting SFTP server: {}", serverName);
        
        try {
            SftpServerClient.SftpServerResponse response = sftpServerClient.deleteSftpServer(serverName);
            
            Map<String, Object> result = new HashMap<>();
            result.put("success", response.isSuccess());
            result.put("message", response.getMessage());
            result.put("data", response.getData());
            result.put("timestamp", response.getTimestamp());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error deleting SFTP server", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Failed to delete SFTP server: " + e.getMessage());
            errorResponse.put("timestamp", System.currentTimeMillis());
            
            return ResponseEntity.ok(errorResponse);
        }
    }

    // DTO类定义
    public static class CreateSftpServerRequest {
        @NotBlank(message = "Server name cannot be blank")
        private String serverName;
        
        @NotBlank(message = "Host cannot be blank")
        private String host;
        
        @NotNull(message = "Port cannot be null")
        @Min(value = 1, message = "Port must be greater than 0")
        @Max(value = 65535, message = "Port must be less than 65536")
        private Integer port;
        
        @NotBlank(message = "Username cannot be blank")
        private String username;
        
        @NotBlank(message = "Password cannot be blank")
        private String password;

        // Getters and Setters
        public String getServerName() { return serverName; }
        public void setServerName(String serverName) { this.serverName = serverName; }
        
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        
        public Integer getPort() { return port; }
        public void setPort(Integer port) { this.port = port; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class UpdateSftpServerRequest extends CreateSftpServerRequest {
        // 继承所有字段，用于更新操作
    }
}
