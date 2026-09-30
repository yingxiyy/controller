package devicemaintenance.dto;

import java.time.LocalDateTime;

/**
 * 软件下载响应对象
 */
public class SoftwareDownloadResponse {
    
    private String taskId;
    private String deviceId;
    private String deviceName;
    private String status;
    private String message;
    private LocalDateTime createTime;
    
    // 构造方法
    public SoftwareDownloadResponse() {
        this.createTime = LocalDateTime.now();
    }
    
    public SoftwareDownloadResponse(String taskId, String deviceId, String status) {
        this.taskId = taskId;
        this.deviceId = deviceId;
        this.status = status;
        this.createTime = LocalDateTime.now();
    }
    
    // 静态工厂方法
    public static SoftwareDownloadResponse success(String taskId, String deviceId, String message) {
        SoftwareDownloadResponse response = new SoftwareDownloadResponse(taskId, deviceId, "SUCCESS");
        response.setMessage(message);
        return response;
    }
    
    public static SoftwareDownloadResponse failed(String deviceId, String message) {
        SoftwareDownloadResponse response = new SoftwareDownloadResponse(null, deviceId, "FAILED");
        response.setMessage(message);
        return response;
    }
    
    public static SoftwareDownloadResponse inProgress(String taskId, String deviceId, String message) {
        SoftwareDownloadResponse response = new SoftwareDownloadResponse(taskId, deviceId, "IN_PROGRESS");
        response.setMessage(message);
        return response;
    }
    
    // Getters and Setters
    public String getTaskId() {
        return taskId;
    }
    
    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getDeviceName() {
        return deviceName;
    }
    
    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public LocalDateTime getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
    
    @Override
    public String toString() {
        return "SoftwareDownloadResponse{" +
                "taskId='" + taskId + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", status='" + status + '\'' +
                ", message='" + message + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
