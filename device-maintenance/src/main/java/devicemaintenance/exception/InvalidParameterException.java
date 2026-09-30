package devicemaintenance.exception;

/**
 * 参数无效异常
 * 
 * 用于表示请求参数不合法、缺失或格式错误
 */
public class InvalidParameterException extends DeviceMaintenanceException {

    private static final long serialVersionUID = 1L;

    public InvalidParameterException(String message) {
        super("INVALID_PARAMETER", message);
    }

    public InvalidParameterException(String message, Throwable cause) {
        super("INVALID_PARAMETER", message, cause);
    }
}

