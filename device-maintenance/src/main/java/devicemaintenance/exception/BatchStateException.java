package devicemaintenance.exception;

/**
 * 批次状态异常
 * 
 * 用于表示批次状态不允许执行特定操作
 */
public class BatchStateException extends DeviceMaintenanceException {

    private static final long serialVersionUID = 1L;

    public BatchStateException(String message) {
        super("BATCH_STATE_ERROR", message);
    }

    public BatchStateException(String message, Throwable cause) {
        super("BATCH_STATE_ERROR", message, cause);
    }
}

