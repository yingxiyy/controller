package devicemaintenance.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 软件下载请求对象
 */
public class SoftwareDownloadRequest {
    
    @NotBlank(message = "设备ID不能为空")
    private String deviceId;
    
    @NotBlank(message = "软件文件路径不能为空")
    private String softwareFilePath;
    
    private String deviceName;
    
    private String sftpServerName;
    
    // 构造方法
    public SoftwareDownloadRequest() {
    }
    
    public SoftwareDownloadRequest(String deviceId, String softwareFilePath) {
        this.deviceId = deviceId;
        this.softwareFilePath = softwareFilePath;
    }
    
    // 验证方法
    public void validate() {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be empty");
        }
        if (softwareFilePath == null || softwareFilePath.trim().isEmpty()) {
            throw new IllegalArgumentException("Software file path cannot be empty");
        }
    }
    
    // Getters and Setters
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getSoftwareFilePath() {
        return softwareFilePath;
    }
    
    public void setSoftwareFilePath(String softwareFilePath) {
        this.softwareFilePath = softwareFilePath;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public String getSftpServerId() {
        return sftpServerName;
    }
    
    public void setSftpServerId(String sftpServerName) {
        this.sftpServerName = sftpServerName;
    }
    
    @Override
    public String toString() {
        return "SoftwareDownloadRequest{" +
                "deviceId='" + deviceId + '\'' +
                ", softwareFilePath='" + softwareFilePath + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", sftpServerName='" + sftpServerName + '\'' +
                '}';
    }
}
