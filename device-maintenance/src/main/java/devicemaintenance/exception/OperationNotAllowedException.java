package devicemaintenance.exception;

/**
 * 操作不允许异常
 * 
 * 用于表示当前上下文不允许执行请求的操作
 */
public class OperationNotAllowedException extends DeviceMaintenanceException {

    private static final long serialVersionUID = 1L;

    public OperationNotAllowedException(String message) {
        super("OPERATION_NOT_ALLOWED", message);
    }

    public OperationNotAllowedException(String message, Throwable cause) {
        super("OPERATION_NOT_ALLOWED", message, cause);
    }
}

