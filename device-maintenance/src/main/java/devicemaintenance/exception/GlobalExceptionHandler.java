package devicemaintenance.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理资源不存在异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(
            ResourceNotFoundException ex, 
            HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        
        HttpStatus status = HttpStatus.NOT_FOUND;
        ErrorResponse error = ErrorResponse.builder()
                .error(ex.getErrorCode())
                .message(ex.getMessage())
                .status(status.value())
                .timestamp(formatTimestamp(LocalDateTime.now()))
                .path(request.getRequestURI())
                .build();
                
        return ResponseEntity.status(status).body(error);
    }

    /**
     * 处理参数无效异常
     */
    @ExceptionHandler(InvalidParameterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParameterException(
            InvalidParameterException ex, 
            HttpServletRequest request) {
        log.warn("Invalid parameter: {}", ex.getMessage());
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse error = ErrorResponse.builder()
                .error(ex.getErrorCode())
                .message(ex.getMessage())
                .status(status.value())
                .timestamp(formatTimestamp(LocalDateTime.now()))
                .path(request.getRequestURI())
                .build();
                
        return ResponseEntity.status(status).body(error);
    }

    /**
     * 处理批次状态异常
     */
    @ExceptionHandler(BatchStateException.class)
    public ResponseEntity<ErrorResponse> handleBatchStateException(
            BatchStateException ex, 
            HttpServletRequest request) {
        log.warn("Batch state error: {}", ex.getMessage());
        
        HttpStatus status = HttpStatus.CONFLICT;
        ErrorResponse error = ErrorResponse.builder()
                .error(ex.getErrorCode())
                .message(ex.getMessage())
                .status(status.value())
                .timestamp(formatTimestamp(LocalDateTime.now()))
                .path(request.getRequestURI())
                .build();
                
        return ResponseEntity.status(status).body(error);
    }

    /**
     * 处理操作不允许异常
     */
    @ExceptionHandler(OperationNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleOperationNotAllowedException(
            OperationNotAllowedException ex, 
            HttpServletRequest request) {
        log.warn("Operation not allowed: {}", ex.getMessage());
        
        HttpStatus status = HttpStatus.FORBIDDEN;
        ErrorResponse error = ErrorResponse.builder()
                .error(ex.getErrorCode())
                .message(ex.getMessage())
                .status(status.value())
                .timestamp(formatTimestamp(LocalDateTime.now()))
                .path(request.getRequestURI())
                .build();
                
        return ResponseEntity.status(status).body(error);
    }

    /**
     * 处理设备维护业务异常（通用）
     */
    @ExceptionHandler(DeviceMaintenanceException.class)
    public ResponseEntity<ErrorResponse> handleDeviceMaintenanceException(
            DeviceMaintenanceException ex, 
            HttpServletRequest request) {
        log.error("Device maintenance error: {}", ex.getMessage(), ex);
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse error = ErrorResponse.builder()
                .error(ex.getErrorCode() != null ? ex.getErrorCode() : "INTERNAL_ERROR")
                .message(ex.getMessage())
                .status(status.value())
                .timestamp(formatTimestamp(LocalDateTime.now()))
                .path(request.getRequestURI())
                .build();
                
        return ResponseEntity.status(status).body(error);
    }

    /**
     * 处理参数验证异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> handleValidationException(
            Exception ex, 
            HttpServletRequest request) {
        log.warn("Validation error: {}", ex.getMessage());
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse error = ErrorResponse.builder()
                .error("VALIDATION_ERROR")
                .message("Request parameter validation failed")
                .status(status.value())
                .timestamp(formatTimestamp(LocalDateTime.now()))
                .path(request.getRequestURI())
                .build();
                
        return ResponseEntity.status(status).body(error);
    }

    /**
     * 处理约束违反异常
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex, 
            HttpServletRequest request) {
        log.warn("Constraint violation: {}", ex.getMessage());
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse error = ErrorResponse.builder()
                .error("CONSTRAINT_VIOLATION")
                .message("Data constraint violation")
                .status(status.value())
                .timestamp(formatTimestamp(LocalDateTime.now()))
                .path(request.getRequestURI())
                .build();
                
        return ResponseEntity.status(status).body(error);
    }

    /**
     * 处理参数不合法异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(
            IllegalArgumentException ex, 
            HttpServletRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        
        HttpStatus status = HttpStatus.BAD_REQUEST;
        ErrorResponse error = ErrorResponse.builder()
                .error("INVALID_PARAMETER")
                .message(ex.getMessage())
                .status(status.value())
                .timestamp(formatTimestamp(LocalDateTime.now()))
                .path(request.getRequestURI())
                .build();
                
        return ResponseEntity.status(status).body(error);
    }

    /**
     * 处理状态不合法异常
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(
            IllegalStateException ex, 
            HttpServletRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        
        HttpStatus status = HttpStatus.CONFLICT;
        ErrorResponse error = ErrorResponse.builder()
                .error("ILLEGAL_STATE")
                .message(ex.getMessage())
                .status(status.value())
                .timestamp(formatTimestamp(LocalDateTime.now()))
                .path(request.getRequestURI())
                .build();
                
        return ResponseEntity.status(status).body(error);
    }

    /**
     * 处理空指针异常
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointerException(
            NullPointerException ex, 
            HttpServletRequest request) {
        log.error("Null pointer exception: {}", ex.getMessage(), ex);
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse error = ErrorResponse.builder()
                .error("NULL_POINTER")
                .message("Internal error: null pointer exception")
                .status(status.value())
                .timestamp(formatTimestamp(LocalDateTime.now()))
                .path(request.getRequestURI())
                .build();
                
        return ResponseEntity.status(status).body(error);
    }

    /**
     * 处理 REST 客户端异常（外部服务调用失败）
     */
    @ExceptionHandler(org.springframework.web.client.RestClientException.class)
    public ResponseEntity<ErrorResponse> handleRestClientException(
            org.springframework.web.client.RestClientException ex, 
            HttpServletRequest request) {
        log.error("REST client error: {}", ex.getMessage(), ex);
        
        HttpStatus status = HttpStatus.BAD_GATEWAY;
        ErrorResponse error = ErrorResponse.builder()
                .error("EXTERNAL_SERVICE_ERROR")
                .message("External service call failed: " + ex.getMessage())
                .status(status.value())
                .timestamp(formatTimestamp(LocalDateTime.now()))
                .path(request.getRequestURI())
                .build();
                
        return ResponseEntity.status(status).body(error);
    }

    /**
     * 处理其他未知异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknownException(
            Exception ex, 
            HttpServletRequest request) {
        log.error("Unknown error occurred: {}", ex.getMessage(), ex);
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ErrorResponse error = ErrorResponse.builder()
                .error("INTERNAL_ERROR")
                .message("Internal error: " + ex.getMessage())
                .status(status.value())
                .timestamp(formatTimestamp(LocalDateTime.now()))
                .path(request.getRequestURI())
                .build();
                
        return ResponseEntity.status(status).body(error);
    }
    
    /**
     * 格式化时间戳为人类可读格式
     */
    private String formatTimestamp(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }

    /**
     * 错误响应对象
     * 
     * 格式说明：
     * - error: 错误类型（如 "INTERNAL_ERROR", "VALIDATION_ERROR" 等）
     * - message: 具体错误内容
     * - status: HTTP 状态码
     * - timestamp: 人类可读格式的时间戳（如 "2025-11-03 14:30:45"）
     * - path: 当前报错的接口路径
     */
    public static class ErrorResponse {
        private String error;      // 错误类型
        private String message;    // 具体错误内容
        private Integer status;    // HTTP 状态码
        private String timestamp;  // 人类可读格式的时间戳
        private String path;       // 接口路径

        public static ErrorResponseBuilder builder() {
            return new ErrorResponseBuilder();
        }

        // Getters and Setters
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
        
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }

        public static class ErrorResponseBuilder {
            private ErrorResponse response = new ErrorResponse();

            public ErrorResponseBuilder error(String error) {
                response.setError(error);
                return this;
            }

            public ErrorResponseBuilder message(String message) {
                response.setMessage(message);
                return this;
            }

            public ErrorResponseBuilder status(Integer status) {
                response.setStatus(status);
                return this;
            }

            public ErrorResponseBuilder timestamp(String timestamp) {
                response.setTimestamp(timestamp);
                return this;
            }

            public ErrorResponseBuilder path(String path) {
                response.setPath(path);
                return this;
            }

            public ErrorResponse build() {
                // ✅ 合并 error 和 message：error = error + ":" + message
                // message 保持不变
                if (response.error != null && response.message != null) {
                    response.error = response.error + ": " + response.message;
                }
                return response;
            }
        }
    }
}
