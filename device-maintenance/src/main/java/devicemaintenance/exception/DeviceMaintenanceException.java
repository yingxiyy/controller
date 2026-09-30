package devicemaintenance.exception;

/**
 * 设备维护异常基类
 */
public class DeviceMaintenanceException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    private final String errorCode;
    
    public DeviceMaintenanceException(String message) {
        super(message);
        this.errorCode = "DEVICE_MAINTENANCE_ERROR";
    }
    
    public DeviceMaintenanceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public DeviceMaintenanceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "DEVICE_MAINTENANCE_ERROR";
    }
    
    public DeviceMaintenanceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
