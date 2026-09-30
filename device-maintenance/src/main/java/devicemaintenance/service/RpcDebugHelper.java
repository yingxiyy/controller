package devicemaintenance.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import devicemaintenance.entity.DeviceTask;
import devicemaintenance.integration.NeMgrIntegrationService;
import devicemaintenance.repository.DeviceTaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * RPC 调试帮助类
 * 统一处理所有 RPC 操作的 debug payload 保存逻辑
 * 
 * 职责：
 * 1. 保存 RPC 请求参数
 * 2. 保存 RPC 响应结果
 * 3. 保存错误信息和堆栈跟踪
 * 4. 统一的 JSON 序列化
 */
@Component
@Slf4j
public class RpcDebugHelper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private DeviceTaskRepository deviceTaskRepository;

    /**
     * 保存 RPC 成功响应的 debug payload
     * 
     * @param task DeviceTask 实体
     * @param debugPayload 完整的 debug payload（来自 RpcResult.getDebugPayload()）
     * @param debug 是否启用调试模式
     *              - true: 使用 INFO 级别日志
     *              - false: 使用 DEBUG 级别日志
     *              注意：无论 debug 是何值，只要 debugPayload 存在就会保存到数据库
     */
    public void saveSuccessDebugPayload(
            DeviceTask task, 
            Map<String, Object> debugPayload,
            boolean debug) {
        
        // ✅ 如果 debugPayload 已经存在（说明 RPC 调用时已经生成），就保存它
        // debug 参数控制日志级别，但不影响是否保存
        try {
            if (debugPayload != null && !debugPayload.isEmpty()) {
                String debugJson = objectMapper.writeValueAsString(debugPayload);
                task.setDebugPayload(debugJson);
                
                // ✅ 使用 debug 参数控制日志级别
                if (debug) {
                    log.info("✅ Debug payload saved: {} bytes", debugJson.length());
                } else {
                    log.debug("Debug payload saved: {} bytes", debugJson.length());
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Failed to save debug payload: {}", e.getMessage());
        }
    }

    /**
     * 保存 RPC 返回失败的 debug payload（RPC 调用成功，但返回 result=fail）
     * 
     * @param task DeviceTask 实体
     * @param errorMessage 错误消息（不使用，保留向后兼容）
     * @param requestParams 请求参数 Map（如果是完整的 debugPayload，可能已包含 rpc-response）
     * @param rpcResponse RPC 响应（可能为 null）
     */
    public void saveRpcFailureDebugPayload(
            DeviceTask task,
            String errorMessage,
            Map<String, Object> requestParams,
            Object rpcResponse) {
        
        try {
            Map<String, Object> debugPayload;
            
            // ✅ 如果 requestParams 已经包含 rpc-response，直接使用
            if (requestParams != null && requestParams.containsKey("rpc-response")) {
                debugPayload = new HashMap<>(requestParams);
            } else {
                // ✅ 否则，构建新的 debugPayload
                debugPayload = new HashMap<>();
                
                // 添加请求参数
                if (requestParams != null) {
                    debugPayload.putAll(requestParams);
                }
                
                // 添加 RPC 响应（RPC 调用成功，只是业务操作失败）
                if (rpcResponse != null) {
                    debugPayload.put("rpc-response", rpcResponse);
                }
            }
            
            // ⚠️ 不添加 error 字段，因为 RPC 调用本身是成功的
            // RPC 返回 result=fail 只是业务操作失败，不是错误/异常
            
            debugPayload.put("timestamp", System.currentTimeMillis());
            
            String debugJson = objectMapper.writeValueAsString(debugPayload);
            task.setDebugPayload(debugJson);
            log.info("✅ RPC failure debug payload saved: {} bytes", debugJson.length());
            
        } catch (Exception e) {
            log.error("❌ Failed to save RPC failure debug payload: {}", e.getMessage(), e);
            // Fallback（只保存基本信息）
            try {
                Map<String, Object> fallbackPayload = new HashMap<>();
                fallbackPayload.put("serialization-error", e.getMessage());
                fallbackPayload.put("timestamp", System.currentTimeMillis());
                if (requestParams != null) {
                    fallbackPayload.put("request-params", requestParams);
                }
                String fallbackJson = objectMapper.writeValueAsString(fallbackPayload);
                task.setDebugPayload(fallbackJson);
                log.info("✅ Fallback debug payload saved");
            } catch (Exception fallbackError) {
                log.error("❌❌ Even fallback debug payload failed: {}", fallbackError.getMessage());
            }
        }
    }

    /**
     * 保存异常的 debug payload（真正的异常，包含堆栈跟踪）
     * 
     * @param task DeviceTask 实体
     * @param exception 异常对象
     * @param requestParams 请求参数 Map
     * @param rpcResponse RPC 响应（可能为 null）
     */
    public void saveFailureDebugPayload(
            DeviceTask task,
            Exception exception,
            Map<String, Object> requestParams,
            Object rpcResponse) {
        
        try {
            Map<String, Object> debugPayload = new HashMap<>();
            
            // ✅ 请求参数
            if (requestParams != null) {
                debugPayload.put("request-params", requestParams);
            }
            
            // ✅ RPC 响应（如果有）
            if (rpcResponse != null) {
                debugPayload.put("rpc-response", rpcResponse);
            }
            
            // ✅ 错误信息
            Map<String, Object> errorInfo = new HashMap<>();
            errorInfo.put("exception-type", exception.getClass().getName());
            errorInfo.put("error-message", exception.getMessage());
            
            // ✅ 堆栈跟踪
            StackTraceElement[] stackTrace = exception.getStackTrace();
            if (stackTrace != null && stackTrace.length > 0) {
                String[] stackTraceStrings = new String[Math.min(10, stackTrace.length)];
                for (int i = 0; i < stackTraceStrings.length; i++) {
                    stackTraceStrings[i] = stackTrace[i].toString();
                }
                errorInfo.put("stack-trace", stackTraceStrings);
            }
            
            debugPayload.put("error", errorInfo);
            debugPayload.put("timestamp", System.currentTimeMillis());
            
            String debugJson = objectMapper.writeValueAsString(debugPayload);
            task.setDebugPayload(debugJson);
            log.info("✅ Failure debug payload saved: {} bytes", debugJson.length());
            
        } catch (Exception e) {
            log.error("❌ Failed to save failure debug payload: {}", e.getMessage(), e);
            // 至少保存一个简单的错误信息
            try {
                Map<String, Object> fallbackPayload = new HashMap<>();
                fallbackPayload.put("error-message", exception.getMessage());
                fallbackPayload.put("serialization-error", e.getMessage());
                fallbackPayload.put("timestamp", System.currentTimeMillis());
                if (requestParams != null) {
                    fallbackPayload.put("request-params", requestParams);
                }
                String fallbackJson = objectMapper.writeValueAsString(fallbackPayload);
                task.setDebugPayload(fallbackJson);
                log.info("✅ Fallback debug payload saved");
            } catch (Exception fallbackError) {
                log.error("❌❌ Even fallback debug payload failed: {}", fallbackError.getMessage());
            }
        }
    }

    /**
     * 创建请求参数 Map（通用工厂方法）
     */
    public Map<String, Object> createRequestParams(String deviceId, String operation) {
        Map<String, Object> params = new HashMap<>();
        params.put("device-id", deviceId);
        params.put("operation", operation);
        params.put("timestamp", System.currentTimeMillis());
        return params;
    }

    /**
     * 添加参数到 Map
     */
    public void addParam(Map<String, Object> params, String key, Object value) {
        if (value != null) {
            params.put(key, value);
        }
    }

    /**
     * 保存预验证失败的 debug payload
     * 
     * @param task DeviceTask 实体
     * @param errorMessage 错误信息
     * @param requestParams 请求参数
     */
    public void savePreValidationFailurePayload(
            DeviceTask task,
            String errorMessage,
            Map<String, Object> requestParams) {
        
        try {
            Map<String, Object> debugPayload = new HashMap<>();
            
            if (requestParams != null) {
                debugPayload.put("request-params", requestParams);
            }
            
            debugPayload.put("error", errorMessage);
            debugPayload.put("stage", "pre-validation");
            debugPayload.put("timestamp", System.currentTimeMillis());
            
            String debugJson = objectMapper.writeValueAsString(debugPayload);
            task.setDebugPayload(debugJson);
            log.info("✅ Pre-validation failure debug payload saved");
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to save pre-validation debug payload: {}", e.getMessage());
        }
    }

    /**
     * 保存设备友好名称（如果存在）
     * 统一处理设备友好名称的保存逻辑，消除重复代码
     * 
     * @param task DeviceTask 实体
     * @param result RPC 结果（包含设备友好名称）
     */
    public void saveFriendlyNameIfPresent(
            DeviceTask task, 
            NeMgrIntegrationService.RpcResult result) {
        
        if (result != null && result.getDeviceFriendlyName() != null) {
            task.setDeviceName(result.getDeviceFriendlyName());
            deviceTaskRepository.save(task);
            log.info("  ✓ Saved device friendly name: {}", result.getDeviceFriendlyName());
        }
    }

    /**
     * 保存 debug payload 并持久化到数据库
     * 统一处理 debug payload 的保存和持久化逻辑，消除重复代码
     * 
     * @param task DeviceTask 实体
     * @param debugPayload 完整的 debug payload（来自 RpcResult.getDebugPayload()）
     * @param debug 是否启用调试模式
     */
    public void saveDebugPayloadAndPersist(
            DeviceTask task, 
            Map<String, Object> debugPayload,
            boolean debug) {
        
        // 先保存 debug payload 到 task 对象
        saveSuccessDebugPayload(task, debugPayload, debug);
        
        // 如果 debugPayload 被设置（无论 debug 是否为 true），都需要持久化到数据库
        // 因为即使 debug=false，debugPayload 也已经生成并设置到 task 对象中了
        if (task.getDebugPayload() != null) {
            deviceTaskRepository.save(task);
        }
    }
}
