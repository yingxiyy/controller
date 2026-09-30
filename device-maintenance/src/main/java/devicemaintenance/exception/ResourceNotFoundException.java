package devicemaintenance.exception;

/**
 * 资源不存在异常
 * 
 * 用于表示请求的资源（批次、设备、文件等）不存在
 */
public class ResourceNotFoundException extends DeviceMaintenanceException {

    private static final long serialVersionUID = 1L;

    public ResourceNotFoundException(String message) {
        super("RESOURCE_NOT_FOUND", message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super("RESOURCE_NOT_FOUND", message, cause);
    }
}

