package devicemaintenance.integration;

import static devicemaintenance.utils.FtpUtils.buildDatabaseSftpServer;
import static devicemaintenance.utils.FtpUtils.buildSftpServer;

import devicemaintenance.utils.DeviceMaintenanceLogContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otc.controller.rpc.client.dto.ModuleCredential;
import net.flex.dci.otc.controller.rpc.client.httpclient.OdlRpcClient;
import net.flex.dci.otc.controller.rpc.client.httpclient.OdlRpcClientConfig;
import net.flex.dci.otc.controller.rpc.client.rpcs.FtpRpc;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.controller.rpc.client.utils.ModuleUtils;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.serialization.JsonUtil;
import org.bson.Document;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeDatabaseOperateOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateInputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.NeSoftwareOperateOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ne.software.operate.input.SftpServer;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.DbOperationType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SwOperationType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

/**
 * neMgr微服务RPC调用封装
 *
 * 职责：仅负责构造RPC输入并调用neMgr，不处理任务状态和通知 参考：controller/schedule-manager DeviceImpl.java
 */
@Service
@Slf4j
public class NeMgrIntegrationService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired(required = false)
    private NeManagerRpc neManagerRpc;
    @Autowired(required = false)
    private PhyNodeDao phyNodeDao;
    @Autowired(required = false)
    private JsonUtil jsonUtil;
    @Autowired(required = false)
    private OdlRpcClient odlRpcClient;
    @Autowired(required = false)
    private MongoTemplate mongoTemplate;
    @Autowired
    private FtpRpc ftpRpc;


    /**
     * 通过反射动态添加字段到RPC对象 尝试绕过YANG模型限制，在运行时添加protocol和source-address字段
     */
    private boolean addFieldByReflection(Object target, String fieldName, Object value) {
        log.info("  🔧 尝试通过反射添加字段: {} = {}", fieldName, value);
        log.info("  🔍 目标对象类型: {}", target.getClass().getName());

        // 获取所有可用的字段和方法（用于调试）
        log.info("  📋 目标对象的所有字段:");
        java.lang.reflect.Field[] fields = target.getClass().getDeclaredFields();
        for (java.lang.reflect.Field f : fields) {
            log.info("    - 字段: {} (类型: {})", f.getName(), f.getType().getName());
        }

        log.info("  📋 目标对象的所有方法:");
        java.lang.reflect.Method[] methods = target.getClass().getDeclaredMethods();
        for (java.lang.reflect.Method m : methods) {
            if (m.getName().startsWith("set")) {
                log.info("    - setter方法: {} (参数: {})", m.getName(),
                        m.getParameterTypes().length > 0 ? m.getParameterTypes()[0].getName()
                                : "无参数");
            }
        }

        // 尝试多种字段名变体
        String[] fieldNameVariants = {
                fieldName,
                fieldName.replace("-", ""),  // source-address -> sourceaddress
                fieldName.replace("-", "_"), // source-address -> source_address
                toCamelCase(fieldName)       // source-address -> sourceAddress
        };

        for (String variant : fieldNameVariants) {
            try {
                // 尝试通过字段直接设置
                java.lang.reflect.Field field = target.getClass().getDeclaredField(variant);
                field.setAccessible(true);
                field.set(target, value);
                log.info("  ✅ 通过字段反射成功设置 {} = {} (使用字段名: {})", fieldName, value,
                        variant);
                return true;
            } catch (NoSuchFieldException e) {
                // 字段不存在，尝试setter方法
                try {
                    String setterName =
                            "set" + variant.substring(0, 1).toUpperCase() + variant.substring(1);
                    java.lang.reflect.Method setter = target.getClass()
                            .getDeclaredMethod(setterName, value.getClass());
                    setter.setAccessible(true);
                    setter.invoke(target, value);
                    log.info("  ✅ 通过setter反射成功设置 {} = {} (使用方法: {})", fieldName, value,
                            setterName);
                    return true;
                } catch (Exception setterException) {
                    log.debug("  ⚠️ setter方法 {} 不存在或调用失败: {}",
                            "set" + variant.substring(0, 1).toUpperCase() + variant.substring(1),
                            setterException.getMessage());
                }
            } catch (Exception e) {
                log.warn("  ❌ 字段反射设置 {} 失败: {}", variant, e.getMessage());
            }
        }

        log.warn("  ❌ 反射设置 {} 完全失败 - 所有尝试都无效", fieldName);
        return false;
    }

    /**
     * 转换为驼峰命名
     */
    private String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        StringBuilder result = new StringBuilder();
        boolean capitalizeNext = false;
        for (char c : str.toCharArray()) {
            if (c == '-' || c == '_') {
                capitalizeNext = true;
            } else {
                result.append(capitalizeNext ? Character.toUpperCase(c) : c);
                capitalizeNext = false;
            }
        }
        return result.toString();
    }

    /**
     * 提取字段值（优先从augmentation，然后从直接字段）
     */
    private String extractFieldValue(Object target, String fieldName) {
        // 1. 尝试从augmentation中获取
        try {
            java.lang.reflect.Field augmentationField = target.getClass()
                    .getDeclaredField("augmentation");
            augmentationField.setAccessible(true);

            @SuppressWarnings("unchecked")
            java.util.Map<Class<?>, Object> augmentationMap = (java.util.Map<Class<?>, Object>) augmentationField.get(
                    target);

            if (augmentationMap != null) {
                Object extensionData = augmentationMap.get(java.util.Map.class);
                if (extensionData instanceof java.util.Map) {
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> extensionMap = (java.util.Map<String, Object>) extensionData;
                    Object value = extensionMap.get(fieldName);
                    if (value != null) {
                        log.debug("    🔍 从augmentation获取 {} = {}", fieldName, value);
                        return value.toString();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("    ⚠️ 从augmentation获取{}失败: {}", fieldName, e.getMessage());
        }

        // 2. 尝试从直接字段/getter方法获取
        try {
            String getterName =
                    "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1)
                            .replace("-", "");
            java.lang.reflect.Method getter = target.getClass().getMethod(getterName);
            Object value = getter.invoke(target);
            if (value != null) {
                log.debug("    🔍 从getter方法获取 {} = {}", fieldName, value);
                return value.toString();
            }
        } catch (Exception e) {
            log.debug("    ⚠️ 从getter方法获取{}失败: {}", fieldName, e.getMessage());
        }

        return null;
    }

    /**
     * 通过YANG Augmentation机制添加字段 YANG模型通常提供augmentation Map来添加扩展字段
     */
    private boolean addFieldsViaAugmentation(Object target, String sourceAddress) {
        try {
            log.info("  🔧 尝试通过YANG Augmentation机制添加字段");

            // 尝试获取augmentation字段
            java.lang.reflect.Field augmentationField = target.getClass()
                    .getDeclaredField("augmentation");
            augmentationField.setAccessible(true);

            @SuppressWarnings("unchecked")
            java.util.Map<Class<?>, Object> augmentationMap = (java.util.Map<Class<?>, Object>) augmentationField.get(
                    target);

            if (augmentationMap == null) {
                // 创建新的augmentation Map
                augmentationMap = new java.util.HashMap<>();
                augmentationField.set(target, augmentationMap);
                log.info("    📝 创建新的augmentation Map");
            }

            // 创建扩展数据对象
            java.util.Map<String, Object> extensionData = new java.util.HashMap<>();
            extensionData.put("protocol", "SFTP");
            extensionData.put("source-address", sourceAddress);

            // 使用一个通用的扩展类作为key（如果存在的话）
            // 否则直接使用Map.class作为key
            augmentationMap.put(java.util.Map.class, extensionData);

            log.info("    ✅ 成功添加到augmentation: protocol=SFTP, source-address={}",
                    sourceAddress);
            return true;

        } catch (NoSuchFieldException e) {
            log.warn("    ❌ augmentation字段不存在: {}", e.getMessage());
            return false;
        } catch (IllegalAccessException e) {
            log.warn("    ❌ 无法访问augmentation字段: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("    ❌ YANG Augmentation操作失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 创建调试用的 request-params（脱敏处理）
     */
    private Map<String, Object> createDebugPayload(NeSoftwareOperateInput input,
            SftpServerDetails sftpInfo) {
        Map<String, Object> debugPayload = new LinkedHashMap<>();
        debugPayload.put("node-id", input.getNodeId().getValue());
        debugPayload.put("vendor-type", input.getVendorType());
        debugPayload.put("file-name", input.getFileName());
        debugPayload.put("software-operation", input.getSoftwareOperation().toString());

        if (input.getSftpServer() != null) {
            Map<String, Object> sftpServerMap = new LinkedHashMap<>();
            sftpServerMap.put("address", input.getSftpServer().getAddress());
            sftpServerMap.put("port", input.getSftpServer().getPort().getValue());
            sftpServerMap.put("user", input.getSftpServer().getUser());
            sftpServerMap.put("password", maskPassword(input.getSftpServer().getPassword()));
            sftpServerMap.put("download-path", input.getSftpServer().getDownloadPath());
            sftpServerMap.put("upload-path", input.getSftpServer().getUploadPath());

            // 检查是否成功设置了protocol和source-address（通过Builder方法）
            String protocol = input.getSftpServer().getProtocol() != null ?
                    input.getSftpServer().getProtocol().toString() : "⚠️未设置";
            String sourceAddress = input.getSftpServer().getSourceAddress() != null ?
                    input.getSftpServer().getSourceAddress() : "⚠️未设置";

            sftpServerMap.put("protocol", protocol);
            sftpServerMap.put("source-address", sourceAddress);

            log.debug("  🔍 调试payload: protocol = {}, source-address = {}", protocol,
                    sourceAddress);

            debugPayload.put("sftp-server", sftpServerMap);
        }

        return debugPayload;
    }

    /**
     * 构建最终的 debug payload（分离 request-params 和 rpc-response）
     */
    private Map<String, Object> buildFinalDebugPayload(Map<String, Object> requestParams,
            Object output, Exception exception) {
        Map<String, Object> finalDebugPayload = new LinkedHashMap<>();
        finalDebugPayload.put("request-params", requestParams);

        Map<String, Object> rpcResponse = new LinkedHashMap<>();
        if (exception != null) {
            // 异常情况
            rpcResponse.put("result", "exception");
            rpcResponse.put("error-message", exception.getMessage());
            rpcResponse.put("exception-type", exception.getClass().getName());
        } else if (output instanceof NeSoftwareOperateOutput) {
            // 软件操作响应
            NeSoftwareOperateOutput softwareOutput = (NeSoftwareOperateOutput) output;
            if (softwareOutput != null) {
                rpcResponse.put("result", softwareOutput.getResult());
            } else {
                rpcResponse.put("result", null);
                rpcResponse.put("error-message", "RPC returned null");
            }
        } else if (output instanceof NeDatabaseOperateOutput) {
            // 数据库操作响应
            NeDatabaseOperateOutput dbOutput = (NeDatabaseOperateOutput) output;
            if (dbOutput != null) {
                rpcResponse.put("result", dbOutput.getResult());
                // 数据库操作可能有额外信息
            } else {
                rpcResponse.put("result", null);
                rpcResponse.put("error-message", "RPC returned null");
            }
        } else if (output == null) {
            rpcResponse.put("result", null);
            rpcResponse.put("error-message", "RPC returned null");
        }

        finalDebugPayload.put("rpc-response", rpcResponse);
        finalDebugPayload.put("timestamp", System.currentTimeMillis());

        return finalDebugPayload;
    }

    /**
     * 软件下载（完整版本）
     *
     * @param nodeId 设备节点ID (格式: Site-xxx#Ne-xxx)
     * @param sftpServerId SFTP服务器ID
     * @param filePath 完整文件路径（包含目录和文件名）
     * @return RPC响应和调试信息
     */
    public RpcResult softwareDownload(String nodeId, String sftpServerId, String filePath)
            throws CommonException {
        long start = System.currentTimeMillis();
        DeviceMaintenanceLogContext.ensureTraceId();
        DeviceMaintenanceLogContext.setIdentifiers(null, null, null, nodeId, null, "DOWNLOAD");
        DeviceMaintenanceLogContext.setPhase("RPC_BUILD", "software-download");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚀 [RPC调用] 软件下载 - 开始");
        log.info("  设备节点: {}", nodeId);
        log.info("  SFTP服务器: {}", sftpServerId);
        log.info("  文件路径: {}", filePath);

        // 1. 获取设备信息（包含 vendor-type）
        log.info("📡 [步骤1] 获取设备信息 (获取vendor-type)");
        DevicePhysicalInfo deviceInfo = null;
        try {
            deviceInfo = getDeviceInfoByNodeId(nodeId);
            log.info("  ✓ 设备友好名: {}", deviceInfo.getFriendlyName());
            log.info("  ✓ Vendor Type: {}", deviceInfo.getVendorType());
        } catch (Exception e) {
            log.error("❌ 获取设备信息失败: {}", e.getMessage());
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "设备不存在或无法获取信息: " + nodeId, e);
        }

        // 2. 获取SFTP服务器信息
        log.info("📡 [步骤2] 获取SFTP配置 (sftp-server微服务)");
        SftpServerDetails sftpInfo = null;
        try {
            sftpInfo = getSftpServerById(sftpServerId);
            log.info("  ✓ SFTP地址: {}@{}:{}", sftpInfo.getUser(), sftpInfo.getAddress(),
                    sftpInfo.getPort());
        } catch (Exception e) {
            log.error("❌ 获取SFTP配置失败: {}", e.getMessage());
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "SFTP服务器不存在或无法获取配置: " + sftpServerId, e);
        }

        // 3. 解析文件路径
        String fileName = extractFileName(filePath);
        String downloadPath = extractDirectory(filePath);
        log.debug("  ✓ 文件名: {}, 下载路径: {}", fileName, downloadPath);

        String sourceAddress = resolveDeviceSourceAddress(deviceInfo);
        log.info("  source-address: {} (source={})", sourceAddress,
                describeSourceAddress(deviceInfo));
        SftpServer sftpServer = buildSftpServer(sftpInfo, downloadPath, sourceAddress);
        // 4. 构造完整的RPC输入（使用真实的vendor-type）
        NeSoftwareOperateInput input = new NeSoftwareOperateInputBuilder()
                .setNodeId(new NodeId(nodeId))
                .setVendorType(deviceInfo.getVendorType())  // ✅ 使用真实的vendor-type
                .setFileName(fileName)
                .setSoftwareOperation(SwOperationType.SWOPDOWNLOAD)
                .setSftpServer(sftpServer)
                .build();

        // ✅ 详细日志：打印实际构造的 RPC Input 对象
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🔍 [DEBUG] 实际构造的 RPC Input 对象详情:");
        log.info("  Java Class: {}", input.getClass().getName());
        log.info("  nodeId: {}", input.getNodeId());
        log.info("  vendorType: {}", input.getVendorType());
        log.info("  fileName: {}", input.getFileName());
        log.info("  softwareOperation: {}", input.getSoftwareOperation());
        if (input.getSftpServer() != null) {
            log.info("  sftpServer Java Class: {}", input.getSftpServer().getClass().getName());
            log.info("  sftpServer.address: {}", input.getSftpServer().getAddress());
            log.info("  sftpServer.port: {}", input.getSftpServer().getPort());
            log.info("  sftpServer.user: {}", input.getSftpServer().getUser());
            log.info("  sftpServer.downloadPath: {}", input.getSftpServer().getDownloadPath());
            log.info("  sftpServer.uploadPath: {}", input.getSftpServer().getUploadPath());

            // ✅ 检查是否有 protocol 和 source-address 字段
            try {
                java.lang.reflect.Method getProtocol = input.getSftpServer().getClass()
                        .getMethod("getProtocol");
                Object protocol = getProtocol.invoke(input.getSftpServer());
                log.info("  sftpServer.protocol: {} ✅", protocol);
            } catch (NoSuchMethodException e) {
                log.warn("  sftpServer.protocol: ❌ 方法不存在！");
            } catch (Exception e) {
                log.warn("  sftpServer.protocol: ❌ 调用失败: {}", e.getMessage());
            }

            try {
                java.lang.reflect.Method getSourceAddress = input.getSftpServer().getClass()
                        .getMethod("getSourceAddress");
                Object sourceAddressValue = getSourceAddress.invoke(input.getSftpServer());
                log.info("  sftpServer.sourceAddress: {} ✅", sourceAddressValue);
            } catch (NoSuchMethodException e) {
                log.warn("  sftpServer.sourceAddress: ❌ 方法不存在！");
            } catch (Exception e) {
                log.warn("  sftpServer.sourceAddress: ❌ 调用失败: {}", e.getMessage());
            }
        }
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        log.info("🔧 [步骤2] 验证RPC对象包含必需字段");
        sftpServer = input.getSftpServer();

        // 验证字段已正确设置
        if (sftpServer.getProtocol() != null && sftpServer.getSourceAddress() != null) {
            log.info("  ✅ 必需字段已通过Builder正确设置");
            log.info("    protocol = {}", sftpServer.getProtocol());
            log.info("    source-address = {}", sftpServer.getSourceAddress());
        } else {
            log.warn("  ⚠️ Builder设置字段可能失败");
            log.warn("    protocol = {}", sftpServer.getProtocol());
            log.warn("    source-address = {}", sftpServer.getSourceAddress());
        }

        DeviceMaintenanceLogContext.setPhase("RPC_SEND", "ne-software-operate");
        log.info("📤 [步骤4] 发起neMgr RPC调用 (ne-software-operate)");
        log.info("  操作类型: SWOP_DOWNLOAD");
        log.info("  RPC Input 详情:");
        log.info("    node-id: {}", nodeId);
        log.info("    vendor-type: {}", deviceInfo.getVendorType());
        log.info("    file-name: {}", fileName);
        log.info("    download-path: {}", downloadPath);
        log.info("    sftp-server: {}@{}:{}", sftpInfo.getUser(), sftpInfo.getAddress(),
                sftpInfo.getPort());

        // 构造调试用的 request-params（脱敏密码）
        java.util.Map<String, Object> requestParams = createDebugPayload(input, sftpInfo);

        // 发起RPC调用
        NeSoftwareOperateOutput output = null;
        java.util.Map<String, Object> finalDebugPayload = null;

        try {
            output = neManagerRpc.neSoftwareOperate(input);
            DeviceMaintenanceLogContext.setPhase("RPC_ACK", "ne-software-operate");
            log.info("rpc ack success elapsedMs={} result={}", System.currentTimeMillis() - start,
                    output != null ? output.getResult() : "null");

            // ✅ 使用统一方法构造完整的 debugPayload
            finalDebugPayload = buildFinalDebugPayload(requestParams, output, null);

        } catch (Exception e) {
            DeviceMaintenanceLogContext.setPhase("RPC_ACK", "ne-software-operate");
            log.error("rpc ack failed elapsedMs={} error={}", System.currentTimeMillis() - start, e.getMessage());
            log.error("💡 请检查以上RPC Input详情中的所有字段是否正确");

            // ✅ 使用统一方法构造包含异常信息的 debugPayload
            finalDebugPayload = buildFinalDebugPayload(requestParams, null, e);

            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "neMgr RPC调用失败", e);
        }

        DeviceMaintenanceLogContext.setPhase("ASYNC_WAIT", "system-change");
        log.info("⏳ 等待Kafka异步通知 (objectUpdateTopic) 获取下载进度和最终结果");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return new RpcResult(output, finalDebugPayload, null);
    }

    /**
     * 软件激活（完整版本，包含SFTP信息）
     */
    public RpcResult softwareActivate(String nodeId, String sftpServerId, String filePath)
            throws CommonException {
        long start = System.currentTimeMillis();
        DeviceMaintenanceLogContext.ensureTraceId();
        DeviceMaintenanceLogContext.setIdentifiers(null, null, null, nodeId, null, "UPGRADE");
        DeviceMaintenanceLogContext.setPhase("RPC_BUILD", "software-activate");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚀 [RPC调用] 软件激活 - 开始");
        log.info("  设备节点: {}", nodeId);
        log.info("  SFTP服务器: {}", sftpServerId);
        log.info("  软件文件: {}", filePath);

        // 📋 [步骤1] 获取设备信息（获取vendor-type）
        log.info("📡 [步骤1] 获取设备信息");
        DevicePhysicalInfo deviceInfo = getDeviceInfoByNodeId(nodeId);
        log.info("  ✓ Vendor Type: {}", deviceInfo.getVendorType());

        // 📋 [步骤2] 获取SFTP服务器信息
        log.info("📡 [步骤2] 获取SFTP服务器信息");
        SftpServerDetails sftpInfo = getSftpServerById(sftpServerId);
        log.info("  ✓ SFTP服务器: {}:{}", sftpInfo.getAddress(), sftpInfo.getPort());

        // 📋 [步骤3] 解析文件路径
        String fileName = extractFileName(filePath);
        String softwarePath = extractDirectory(filePath);
        log.info("📂 [步骤3] 解析文件路径");
        log.info("  文件名: {}", fileName);
        log.info("  软件路径: {}", softwarePath);

        String sourceAddress = resolveDeviceSourceAddress(deviceInfo);
        log.info("  source-address: {} (source={})", sourceAddress,
                describeSourceAddress(deviceInfo));
        SftpServer sftpServer = buildSftpServer(sftpInfo, softwarePath, sourceAddress);

        // 📋 [步骤4] 构造RPC输入
        NeSoftwareOperateInput input = new NeSoftwareOperateInputBuilder()
                .setNodeId(new NodeId(nodeId))
                .setVendorType(deviceInfo.getVendorType())
                .setFileName(fileName)
                .setSoftwareOperation(SwOperationType.SWOPACTIVATE)
                .setSftpServer(sftpServer)
                .build();

        DeviceMaintenanceLogContext.setPhase("RPC_SEND", "ne-software-operate");
        log.info("📤 [步骤5] 发起neMgr RPC调用 (ne-software-operate)");
        log.info("  操作类型: SWOP_ACTIVATE");
        log.info("  RPC Payload 详情:");
        log.info("    node-id: {}", nodeId);
        log.info("    vendor-type: {}", deviceInfo.getVendorType());
        log.info("    file-name: {}", fileName);
        log.info("    software-path: {}", softwarePath);
        log.info("    sftp-server: {}@{}:{}", sftpInfo.getUser(), sftpInfo.getAddress(),
                sftpInfo.getPort());

        // 构造调试用的 request-params（脱敏密码）
        Map<String, Object> requestParams = createDebugPayload(input, sftpInfo);

        NeSoftwareOperateOutput output = null;
        Map<String, Object> finalDebugPayload = null;

        try {
            output = neManagerRpc.neSoftwareOperate(input);
            DeviceMaintenanceLogContext.setPhase("RPC_ACK", "ne-software-operate");
            log.info("rpc ack success elapsedMs={} result={}", System.currentTimeMillis() - start,
                    output != null ? output.getResult() : "null");

            // ✅ 使用统一方法构造完整的 debugPayload
            finalDebugPayload = buildFinalDebugPayload(requestParams, output, null);

        } catch (Exception e) {
            DeviceMaintenanceLogContext.setPhase("RPC_ACK", "ne-software-operate");
            log.error("rpc ack failed elapsedMs={} error={}", System.currentTimeMillis() - start, e.getMessage());

            // ✅ 使用统一方法构造包含异常信息的 debugPayload
            finalDebugPayload = buildFinalDebugPayload(requestParams, null, e);

            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "neMgr服务调用失败", e);
        }

        DeviceMaintenanceLogContext.setPhase("ASYNC_WAIT", "system-change");
        log.info("⏳ 等待Kafka异步通知 (objectUpdateTopic) 获取激活进度和最终结果");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return new RpcResult(output, finalDebugPayload, deviceInfo.getFriendlyName());
    }

    private String resolveFriendlyName(String nodeId, String fallbackFriendlyName) {
        String configuredFriendlyName = getConfiguredFriendlyName(nodeId);
        return hasText(configuredFriendlyName) ? configuredFriendlyName : fallbackFriendlyName;
    }

    private String getConfiguredFriendlyName(String nodeId) {
        if (mongoTemplate == null) {
            return null;
        }

        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("data.node.node-id").is(nodeId));
            query.fields()
                    .include("data.node.node-id")
                    .include("data.node.otn-phy-topology:physical.friendly-name");

            Document configDoc = mongoTemplate.findOne(query, Document.class, "config-phy-node");
            if (configDoc == null) {
                return null;
            }

            return extractFriendlyNameFromMongoNode(configDoc);
        } catch (Exception e) {
            log.warn("查询 config-phy-node friendly-name 失败: nodeId={}, error={}",
                    nodeId, e.getMessage());
            return null;
        }
    }

    private String extractFriendlyNameFromMongoNode(Document mongoDoc) {
        if (mongoDoc == null) {
            return null;
        }

        try {
            Document data = mongoDoc.get("data", Document.class);
            if (data == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            java.util.List<Document> nodes = (java.util.List<Document>) data.get("node");
            if (nodes == null || nodes.isEmpty()) {
                return null;
            }

            Document physical = nodes.get(0).get("otn-phy-topology:physical", Document.class);
            if (physical == null) {
                return null;
            }

            String friendlyName = physical.getString("friendly-name");
            return hasText(friendlyName) ? friendlyName.trim() : null;
        } catch (Exception e) {
            log.warn("解析 config-phy-node friendly-name 失败: {}", e.getMessage());
            return null;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String resolveDeviceSourceAddress(DevicePhysicalInfo deviceInfo) throws CommonException {
        String sourceAddress = firstText(
                deviceInfo != null ? deviceInfo.getLoopbackAddress() : null,
                deviceInfo != null ? deviceInfo.getNmsAddress() : null,
                deviceInfo != null ? deviceInfo.getIp() : null
        );
        if (!hasText(sourceAddress)) {
            throw new CommonException(
                    CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Device source address not found"
            );
        }
        return sourceAddress;
    }

    private String describeSourceAddress(DevicePhysicalInfo deviceInfo) {
        if (deviceInfo == null) {
            return "unknown";
        }
        if (hasText(deviceInfo.getLoopbackAddress())) {
            return "loopback";
        }
        if (hasText(deviceInfo.getNmsAddress())) {
            return "nms";
        }
        if (hasText(deviceInfo.getIp())) {
            return "ip";
        }
        return "unknown";
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String extractDeviceAddress(String nodeId, Physical physical, boolean loopback) {
        String fromYang = extractAddressByReflection(physical, loopback);
        if (hasText(fromYang)) {
            return fromYang;
        }
        return extractDeviceAddressFromMongo(nodeId, loopback);
    }

    private String extractAddressByReflection(Object target, boolean loopback) {
        if (target == null) {
            return null;
        }
        String[] getterNames = loopback
                ? new String[]{"getLoopbackAddress", "getLoopbackIpAddress", "getLoopbackIp",
                "getLoopBackAddress", "getLoopBackIpAddress", "getLoopBackIp", "getLoopback",
                "getLoopBack"}
                : new String[]{"getNmsAddress", "getNmsIpAddress", "getNmsIp", "getIpAddress",
                "getIp"};

        for (String getterName : getterNames) {
            try {
                java.lang.reflect.Method getter = target.getClass().getMethod(getterName);
                Object value = getter.invoke(target);
                if (value != null && hasText(value.toString())) {
                    return value.toString().trim();
                }
            } catch (NoSuchMethodException e) {
                log.debug("Address getter not found: {}", getterName);
            } catch (Exception e) {
                log.debug("Address getter failed: {}, error={}", getterName, e.getMessage());
            }
        }
        return null;
    }

    private String extractDeviceAddressFromMongo(String nodeId, boolean loopback) {
        if (mongoTemplate == null) {
            return null;
        }
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("data.node.node-id").is(nodeId));
            Document mongoDoc = mongoTemplate.findOne(query, Document.class, "op-phy-node");
            Document physical = extractPhysicalDocument(mongoDoc);
            return extractAddressFromDocument(physical, loopback);
        } catch (Exception e) {
            log.debug("Query device address from Mongo failed: nodeId={}, loopback={}, error={}",
                    nodeId, loopback, e.getMessage());
            return null;
        }
    }

    private Document extractPhysicalDocument(Document mongoDoc) {
        if (mongoDoc == null) {
            return null;
        }
        Document direct = mongoDoc.get("otn-phy-topology:physical", Document.class);
        if (direct != null) {
            return direct;
        }
        Document data = mongoDoc.get("data", Document.class);
        if (data == null) {
            return null;
        }
        @SuppressWarnings("unchecked")
        java.util.List<Document> nodes = (java.util.List<Document>) data.get("node");
        if (nodes == null || nodes.isEmpty()) {
            return null;
        }
        return nodes.get(0).get("otn-phy-topology:physical", Document.class);
    }

    private String extractAddressFromDocument(Document document, boolean loopback) {
        java.util.Set<String> candidateKeys = new java.util.HashSet<>(
                java.util.Arrays.asList(loopback
                        ? new String[]{"loopback", "loopbackip", "loopbackaddress",
                        "loopbackipaddress"}
                        : new String[]{"nmsaddress", "nmsip", "nmsipaddress", "ipaddress", "ip"}));
        return extractAddressFromDocument(document, candidateKeys);
    }

    private String extractAddressFromDocument(Document document, java.util.Set<String> candidateKeys) {
        if (document == null) {
            return null;
        }
        String propertyValue = extractAddressFromPropertyDocument(document, candidateKeys);
        if (hasText(propertyValue)) {
            return propertyValue;
        }

        for (java.util.Map.Entry<String, Object> entry : document.entrySet()) {
            Object value = entry.getValue();
            if (candidateKeys.contains(normalizeAddressKey(entry.getKey())) && value != null
                    && hasText(value.toString())) {
                return value.toString().trim();
            }
            String nestedValue = extractAddressFromObject(value, candidateKeys);
            if (hasText(nestedValue)) {
                return nestedValue;
            }
        }
        return null;
    }

    private String extractAddressFromObject(Object value, java.util.Set<String> candidateKeys) {
        if (value instanceof Document) {
            return extractAddressFromDocument((Document) value, candidateKeys);
        }
        if (value instanceof java.util.List) {
            for (Object item : (java.util.List<?>) value) {
                String nestedValue = extractAddressFromObject(item, candidateKeys);
                if (hasText(nestedValue)) {
                    return nestedValue;
                }
            }
        }
        return null;
    }

    private String extractAddressFromPropertyDocument(Document document,
            java.util.Set<String> candidateKeys) {
        Object name = firstTextObject(document.get("name"), document.get("property-name"),
                document.get("key"));
        Object value = firstTextObject(document.get("value"), document.get("property-value"),
                document.get("actual-value"));
        if (name != null && value != null && candidateKeys.contains(normalizeAddressKey(name.toString()))
                && hasText(value.toString())) {
            return value.toString().trim();
        }
        return null;
    }

    private Object firstTextObject(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null && hasText(value.toString())) {
                return value;
            }
        }
        return null;
    }

    private String normalizeAddressKey(String key) {
        return key == null ? "" : key.replace("-", "")
                .replace("_", "")
                .replace(":", "")
                .toLowerCase();
    }

    /**
     * 软件提交（完整版本，包含SFTP信息）
     * @param filePath 完整文件路径（如 /software/Release_xxx.tar）
     */
    public RpcResult softwareCommit(String nodeId, String sftpServerId, String filePath)
            throws CommonException {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚀 [RPC调用] 软件提交 - 开始");
        log.info("  设备节点: {}", nodeId);
        log.info("  SFTP服务器: {}", sftpServerId);
        log.info("  文件路径: {}", filePath);

        // 📋 [步骤1] 获取设备信息
        log.info("📡 [步骤1] 获取设备信息");
        DevicePhysicalInfo deviceInfo = getDeviceInfoByNodeId(nodeId);
        log.info("  ✓ Vendor Type: {}", deviceInfo.getVendorType());

        // 📋 [步骤2] 获取SFTP服务器信息
        log.info("📡 [步骤2] 获取SFTP服务器信息");
        SftpServerDetails sftpInfo = getSftpServerById(sftpServerId);
        log.info("  ✓ SFTP服务器: {}:{}", sftpInfo.getAddress(), sftpInfo.getPort());
        
        // 📋 [步骤3] 解析文件路径
        String fileName = extractFileName(filePath);
        String softwarePath = extractDirectory(filePath);
        log.info("📂 [步骤3] 解析文件路径");
        log.info("  文件名: {}", fileName);
        log.info("  软件路径: {}", softwarePath);
        
        String sourceAddress = resolveDeviceSourceAddress(deviceInfo);
        log.info("  source-address: {} (source={})", sourceAddress,
                describeSourceAddress(deviceInfo));
        SftpServer sftpServer = buildSftpServer(sftpInfo, softwarePath, sourceAddress);
        // 📋 [步骤4] 构造RPC输入（commit需要file-name）
        NeSoftwareOperateInput input = new NeSoftwareOperateInputBuilder()
                .setNodeId(new NodeId(nodeId))
                .setVendorType(deviceInfo.getVendorType())
                .setFileName(fileName)
                .setSoftwareOperation(SwOperationType.SWOPCOMMIT)
                .setSftpServer(sftpServer)
                .build();

        log.info("📤 [步骤5] 发起neMgr RPC调用 (ne-software-operate)");
        log.info("  操作类型: SWOP_COMMIT");
        log.info("  RPC Payload 详情:");
        log.info("    node-id: {}", nodeId);
        log.info("    vendor-type: {}", deviceInfo.getVendorType());
        log.info("    file-name: {}", fileName);
        log.info("    software-path: {}", softwarePath);
        log.info("    sftp-server: {}@{}:{}", sftpInfo.getUser(), sftpInfo.getAddress(),
                sftpInfo.getPort());

        // 构造调试用的 request-params（脱敏密码）
        Map<String, Object> requestParams = createDebugPayload(input, sftpInfo);

        NeSoftwareOperateOutput output = null;
        Map<String, Object> finalDebugPayload = null;

        try {
            output = neManagerRpc.neSoftwareOperate(input);
            log.info("✅ [RPC调用] 提交命令下发成功");
            log.info("  返回结果: {}", output != null ? output.getResult() : "无返回值");

            // ✅ 使用统一方法构造完整的 debugPayload
            finalDebugPayload = buildFinalDebugPayload(requestParams, output, null);

        } catch (Exception e) {
            log.error("❌ neMgr RPC调用失败: {}", e.getMessage());

            // ✅ 使用统一方法构造包含异常信息的 debugPayload
            finalDebugPayload = buildFinalDebugPayload(requestParams, null, e);

            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "neMgr服务调用失败", e);
        }

        log.info("⏳ 等待Kafka异步通知 (objectUpdateTopic) 获取提交进度和最终结果");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return new RpcResult(output, finalDebugPayload, deviceInfo.getFriendlyName());
    }

    /**
     * 软件回滚（完整版本，包含SFTP信息）
     * @param filePath 完整文件路径（如 /software/Release_xxx.tar）
     */
    public RpcResult softwareRollback(String nodeId, String sftpServerId, String filePath)
            throws CommonException {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚀 [RPC调用] 软件回滚 - 开始");
        log.info("  设备节点: {}", nodeId);
        log.info("  SFTP服务器: {}", sftpServerId);
        log.info("  文件路径: {}", filePath);

        // 📋 [步骤1] 获取设备信息
        log.info("📡 [步骤1] 获取设备信息");
        DevicePhysicalInfo deviceInfo = getDeviceInfoByNodeId(nodeId);
        log.info("  ✓ Vendor Type: {}", deviceInfo.getVendorType());

        // 📋 [步骤2] 获取SFTP服务器信息
        log.info("📡 [步骤2] 获取SFTP服务器信息");
        SftpServerDetails sftpInfo = getSftpServerById(sftpServerId);
        log.info("  ✓ SFTP服务器: {}:{}", sftpInfo.getAddress(), sftpInfo.getPort());

        // 📋 [步骤3] 解析文件路径
        String fileName = extractFileName(filePath);
        String softwarePath = extractDirectory(filePath);
        log.info("📂 [步骤3] 解析文件路径");
        log.info("  文件名: {}", fileName);
        log.info("  软件路径: {}", softwarePath);
        
        String sourceAddress = resolveDeviceSourceAddress(deviceInfo);
        log.info("  source-address: {} (source={})", sourceAddress,
                describeSourceAddress(deviceInfo));
        SftpServer sftpServer = buildSftpServer(sftpInfo, softwarePath, sourceAddress);
        // 📋 [步骤4] 构造RPC输入（rollback需要file-name）
        NeSoftwareOperateInput input = new NeSoftwareOperateInputBuilder()
                .setNodeId(new NodeId(nodeId))
                .setVendorType(deviceInfo.getVendorType())
                .setFileName(fileName)
                .setSoftwareOperation(SwOperationType.SWOPROLLBACK)
                .setSftpServer(sftpServer)
                .build();

        log.info("📤 [步骤5] 发起neMgr RPC调用 (ne-software-operate)");
        log.info("  操作类型: SWOP_ROLLBACK");
        log.info("  RPC Payload 详情:");
        log.info("    node-id: {}", nodeId);
        log.info("    vendor-type: {}", deviceInfo.getVendorType());
        log.info("    file-name: {}", fileName);
        log.info("    software-path: {}", softwarePath);
        log.info("    sftp-server: {}@{}:{}", sftpInfo.getUser(), sftpInfo.getAddress(),
                sftpInfo.getPort());

        // 构造调试用的 request-params（脱敏密码）
        Map<String, Object> requestParams = createDebugPayload(input, sftpInfo);

        NeSoftwareOperateOutput output = null;
        Map<String, Object> finalDebugPayload = null;

        try {
            output = neManagerRpc.neSoftwareOperate(input);
            log.info("✅ [RPC调用] 回滚命令下发成功");
            log.info("  返回结果: {}", output != null ? output.getResult() : "无返回值");

            // ✅ 使用统一方法构造完整的 debugPayload
            finalDebugPayload = buildFinalDebugPayload(requestParams, output, null);

        } catch (Exception e) {
            log.error("❌ neMgr RPC调用失败: {}", e.getMessage());

            // ✅ 使用统一方法构造包含异常信息的 debugPayload
            finalDebugPayload = buildFinalDebugPayload(requestParams, null, e);

            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "neMgr服务调用失败", e);
        }

        log.info("⏳ 等待Kafka异步通知 (objectUpdateTopic) 获取回滚进度和最终结果");
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return new RpcResult(output, finalDebugPayload, deviceInfo.getFriendlyName());
    }

    /**
     * 数据库备份（完整版本，支持debug）
     */
    public RpcResult databaseBackup(String nodeId, String sftpServerId, String backupFilePath,
            boolean debug) throws CommonException {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚀 [RPC调用] 数据库备份 - 开始");
        log.info("  设备节点: {}", nodeId);
        log.info("  SFTP服务器: {}", sftpServerId);
        log.info("  备份文件: {}", backupFilePath);
        log.info("  调试模式: {}", debug ? "开启" : "关闭");

        // 📋 [步骤1] 获取设备信息
        log.info("📡 [步骤1] 获取设备信息");
        DevicePhysicalInfo deviceInfo = getDeviceInfoByNodeId(nodeId);
        log.info("  ✓ Vendor Type: {}", deviceInfo.getVendorType());

        // 📋 [步骤2] 获取SFTP服务器信息
        log.info("📡 [步骤2] 获取SFTP服务器信息");
        SftpServerDetails sftpInfo = getSftpServerById(sftpServerId);
        log.info("  ✓ SFTP服务器: {}:{}", sftpInfo.getAddress(), sftpInfo.getPort());

        // 📋 [步骤3] 解析路径
        String fileName = extractFileName(backupFilePath);
        String uploadPath = extractDirectory(backupFilePath);
        log.info("📂 [步骤3] 解析文件路径");
        log.info("  文件名: {}", fileName);
        log.info("  上传路径: {}", uploadPath);
        String sourceAddress = resolveDeviceSourceAddress(deviceInfo);
        log.info("  source-address: {} (source={})", sourceAddress,
                describeSourceAddress(deviceInfo));
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ne.database.operate.input.SftpServer sftpServer = buildDatabaseSftpServer(
                sftpInfo, fileName, uploadPath, sourceAddress);
        // 📋 [步骤4] 构造RPC输入
        NeDatabaseOperateInput input = new NeDatabaseOperateInputBuilder()
                .setNodeId(new NodeId(nodeId))
                .setVendorType(deviceInfo.getVendorType())
                .setFileName(fileName)
                .setDbOperation(DbOperationType.DBOPBACKUP)
                .setSftpServer(sftpServer)
                .build();

        // 📋 [调试] 构造debugPayload（如果启用）
        java.util.Map<String, Object> debugPayload = null;
        if (debug) {
            debugPayload = new java.util.LinkedHashMap<>();
            debugPayload.put("node-id", nodeId);
            debugPayload.put("vendor-type", deviceInfo.getVendorType());
            debugPayload.put("file-name", fileName);
            debugPayload.put("db-operation", "DBOP_BACKUP");

            java.util.Map<String, Object> sftpServerMap = new java.util.LinkedHashMap<>();
            sftpServerMap.put("address", sftpInfo.getAddress());
            sftpServerMap.put("port", sftpInfo.getPort());
            sftpServerMap.put("user", sftpInfo.getUser());
            sftpServerMap.put("password", maskPassword(sftpInfo.getPassword()));
            sftpServerMap.put("upload-path", uploadPath);
            sftpServerMap.put("download-path", uploadPath);
            sftpServerMap.put("protocol", "SFTP");  // ✅ V2: 手动添加
            sftpServerMap.put("source-address", sourceAddress);
            debugPayload.put("sftp-server", sftpServerMap);

            log.info("🐛 [调试] RPC Payload详情:");
            try {
                log.info("  {}", objectMapper.writeValueAsString(debugPayload));
            } catch (Exception e) {
                log.info("  {}", debugPayload);
            }
        }

        log.info("📤 [步骤3] 发起neMgr RPC调用 (ne-database-operate)");
        NeDatabaseOperateOutput output = null;
        try {
            output = neManagerRpc.neDatabaseOperate(input);
        } catch (Exception e) {
            log.error("❌ neMgr RPC调用失败: {}", e.getMessage());
            log.error("💡 请检查以上RPC Payload详情中的所有字段是否正确");
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "neMgr服务调用失败", e);
        }

        log.info("✅ [RPC调用] 备份命令下发成功");
        log.info("⏳ 等待Kafka异步通知 (objectUpdateTopic) 获取备份进度和最终结果");

        // ⭐ 如果启用了调试模式，添加 RPC 响应到 debugPayload
        if (debug && debugPayload != null && output != null) {
            java.util.Map<String, Object> rpcResponse = new java.util.LinkedHashMap<>();
            if (output.getResult() != null) {
                rpcResponse.put("result", output.getResult());  // "success" or "fail"
            }
            debugPayload.put("rpc-response", rpcResponse);
            debugPayload.put("timestamp", System.currentTimeMillis());

            log.info("🐛 [调试] RPC 响应已添加到 debugPayload");
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return new RpcResult(output, debugPayload, null);
    }


    /**
     * 数据库恢复（完整版本，支持debug）
     */
    public RpcResult databaseRestore(String nodeId, String sftpServerId, String backupFilePath,
            boolean debug) throws CommonException {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("🚀 [RPC调用] 数据库恢复 - 开始");
        log.info("  设备节点: {}", nodeId);
        log.info("  SFTP服务器: {}", sftpServerId);
        log.info("  恢复文件: {}", backupFilePath);
        log.info("  调试模式: {}", debug ? "开启" : "关闭");

        // 📋 [步骤1] 获取设备信息
        log.info("📡 [步骤1] 获取设备信息");
        DevicePhysicalInfo deviceInfo = getDeviceInfoByNodeId(nodeId);
        log.info("  ✓ Vendor Type: {}", deviceInfo.getVendorType());

        // 📋 [步骤2] 获取SFTP服务器信息
        log.info("📡 [步骤2] 获取SFTP服务器信息");
        SftpServerDetails sftpInfo = getSftpServerById(sftpServerId);
        log.info("  ✓ SFTP服务器: {}:{}", sftpInfo.getAddress(), sftpInfo.getPort());

        // 📋 [步骤3] 解析路径
        String fileName = extractFileName(backupFilePath);
        String downloadPath = extractDirectory(backupFilePath);
        log.info("📂 [步骤3] 解析文件路径");
        log.info("  文件名: {}", fileName);
        log.info("  下载路径: {}", downloadPath);
        String sourceAddress = resolveDeviceSourceAddress(deviceInfo);
        log.info("  source-address: {} (source={})", sourceAddress,
                describeSourceAddress(deviceInfo));
        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ne.database.operate.input.SftpServer sftpServer = buildDatabaseSftpServer(
                sftpInfo, fileName, downloadPath, sourceAddress);
        // 📋 [步骤4] 构造RPC输入
        NeDatabaseOperateInput input = new NeDatabaseOperateInputBuilder()
                .setNodeId(new NodeId(nodeId))
                .setVendorType(deviceInfo.getVendorType())
                .setFileName(fileName)
                .setDbOperation(DbOperationType.DBOPRESTORE)
                .setSftpServer(sftpServer)
                .build();

        // 📋 [调试] 构造debugPayload（如果启用）
        java.util.Map<String, Object> debugPayload = null;
        if (debug) {
            debugPayload = new java.util.LinkedHashMap<>();
            debugPayload.put("node-id", nodeId);
            debugPayload.put("vendor-type", deviceInfo.getVendorType());  // ✅ 使用真实的vendor-type
            debugPayload.put("file-name", fileName);
            debugPayload.put("db-operation", "DBOP_RESTORE");

            java.util.Map<String, Object> sftpServerMap = new java.util.LinkedHashMap<>();
            sftpServerMap.put("address", sftpInfo.getAddress());
            sftpServerMap.put("port", sftpInfo.getPort());
            sftpServerMap.put("user", sftpInfo.getUser());
            sftpServerMap.put("password", maskPassword(sftpInfo.getPassword()));
            sftpServerMap.put("upload-path", downloadPath);
            sftpServerMap.put("download-path", downloadPath);
            sftpServerMap.put("protocol", "SFTP");  // ✅ V2: 手动添加
            sftpServerMap.put("source-address", sourceAddress);
            debugPayload.put("sftp-server", sftpServerMap);

            log.info("🐛 [调试] RPC Payload详情:");
            try {
                log.info("  {}", objectMapper.writeValueAsString(debugPayload));
            } catch (Exception e) {
                log.info("  {}", debugPayload);
            }
        }

        log.info("📤 [步骤3] 发起neMgr RPC调用 (ne-database-operate)");
        NeDatabaseOperateOutput output = null;
        try {
            output = neManagerRpc.neDatabaseOperate(input);
        } catch (Exception e) {
            log.error("❌ neMgr RPC调用失败: {}", e.getMessage());
            log.error("💡 请检查以上RPC Payload详情中的所有字段是否正确");
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "neMgr服务调用失败", e);
        }

        log.info("✅ [RPC调用] 恢复命令下发成功");
        log.info("⏳ 等待Kafka异步通知 (objectUpdateTopic) 获取恢复进度和最终结果");

        // ⭐ 如果启用了调试模式，添加 RPC 响应到 debugPayload
        if (debug && debugPayload != null && output != null) {
            java.util.Map<String, Object> rpcResponse = new java.util.LinkedHashMap<>();
            if (output.getResult() != null) {
                rpcResponse.put("result", output.getResult());  // "success" or "fail"
            }
            debugPayload.put("rpc-response", rpcResponse);
            debugPayload.put("timestamp", System.currentTimeMillis());

            log.info("🐛 [调试] RPC 响应已添加到 debugPayload");
        }

        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        return new RpcResult(output, debugPayload, null);
    }

    /**
     * 获取设备信息（支持多种数据源）
     *
     * 优先级： 1. MongoDB (PhyNodeDao) - 快速本地查询 2. NMS微服务 (RESTCONF) - 远程备选方案
     *
     * @param nodeId 设备节点ID (格式: Site-xxx#Ne-xxx)
     * @return 包含node-id, vendor-type, telnet-info的设备信息
     */
    public DevicePhysicalInfo getDeviceInfoByNodeId(String nodeId) throws CommonException {
        log.info("🔍 [获取设备信息] 开始获取设备信息: {}", nodeId);
        DevicePhysicalInfo info = getDeviceInfoFromMongoDB(nodeId);
        info.setFriendlyName(resolveFriendlyName(nodeId, info.getFriendlyName()));
        log.info("  ✅ get node (currentVersion={})",
                info.getSoftwareVersion());
        return info;

    }

    // ========== 辅助方法：获取设备和SFTP信息 ==========

    /**
     * 从MongoDB获取设备信息 容错处理：捕获枚举值校验错误，忽略不兼容的字段
     */
    private DevicePhysicalInfo getDeviceInfoFromMongoDB(String nodeId) throws CommonException {

        Node deviceNode = phyNodeDao.getOpPhyNodeById(nodeId);

        if (deviceNode == null) {
            throw new CommonException(
                    CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Device not found in MongoDB: " + nodeId
            );
        }

        Node1 physicalData = deviceNode.getAugmentation(Node1.class);
        if (physicalData == null || physicalData.getPhysical() == null) {
            throw new CommonException(
                    CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Device physical data not found in MongoDB: " + nodeId
            );
        }

        Physical physical = physicalData.getPhysical();

        // 提取IP地址和端口（YANG类型转Java类型）
        String ipAddress = physical.getIp() != null ? physical.getIp() : null;
        Integer port = physical.getPort() != null ? physical.getPort().getValue() : null;
        String loopbackAddress = extractDeviceAddress(nodeId, physical, true);
        String nmsAddress = firstText(extractDeviceAddress(nodeId, physical, false), ipAddress);

        // ⭐ 提取软件版本（尝试多个可能的位置）
        String softwareVersion = extractSoftwareVersionFromDAO(physical);

        return DevicePhysicalInfo.builder()
                .nodeId(deviceNode.getNodeId().getValue())
                .friendlyName(physical.getFriendlyName())  // 提取友好名称
                .ip(ipAddress)
                .loopbackAddress(loopbackAddress)
                .nmsAddress(nmsAddress)
                .port(port)
                .loginName(physical.getLoginName())
                .loginPassword(physical.getLoginPasswd())
                .vendorType(physical.getVendorType())
                .vendorName(physical.getVendorName())
                .softwareVersion(softwareVersion)
                .build();


    }


    /**
     * 从 physical Document 构建 DevicePhysicalInfo（使用 physical 中的 friendly-name）
     */
    private DevicePhysicalInfo buildDevicePhysicalInfo(String nodeId, org.bson.Document physical) {
        String friendlyName = physical.getString("friendly-name");
        return buildDevicePhysicalInfo(nodeId, physical, friendlyName);
    }

    /**
     * 从 physical Document 构建 DevicePhysicalInfo（使用指定的 friendly-name）
     *
     * @param nodeId 节点ID
     * @param physical physical Document（从 op-phy-node）
     * @param friendlyName 友好名称（从 config-phy-node，如果为 null 则使用 physical 中的值）
     */
    private DevicePhysicalInfo buildDevicePhysicalInfo(String nodeId, org.bson.Document physical,
            String friendlyName) {
        // 提取基本信息
        String ip = physical.getString("ip");
        Integer port = physical.getInteger("port");
        String loginName = physical.getString("login-name");
        String loginPasswd = physical.getString("login-passwd");
        String vendorType = physical.getString("vendor-type");
        String vendorName = physical.getString("vendor-name");
        String loopbackAddress = extractAddressFromDocument(physical, true);
        String nmsAddress = firstText(extractAddressFromDocument(physical, false), ip);

        // 如果没有提供 friendlyName，则从 physical 中获取
        if (friendlyName == null) {
            friendlyName = physical.getString("friendly-name");
        }

        log.info("    📋 MongoDB中的设备属性:");

        // ⭐ 判断 friendly-name 来源（防止 NPE）
        String opFriendlyName = physical.getString("friendly-name");
        String source = java.util.Objects.equals(friendlyName, opFriendlyName) ? "op-phy-node"
                : "config-phy-node";
        log.info("      - friendly-name: {} (来源: {})", friendlyName, source);

        log.info("      - ip: {}", ip);
        log.info("      - vendor: {}/{}", vendorType, vendorName);

        // ⭐ 提取软件版本（尝试多个可能的位置）
        String softwareVersion = extractSoftwareVersion(physical);

        return DevicePhysicalInfo.builder()
                .nodeId(nodeId)
                .friendlyName(friendlyName)
                .ip(ip)
                .loopbackAddress(loopbackAddress)
                .nmsAddress(nmsAddress)
                .port(port)
                .loginName(loginName)
                .loginPassword(loginPasswd)
                .vendorType(vendorType)
                .vendorName(vendorName)
                .softwareVersion(softwareVersion)
                .build();
    }

    /**
     * 提取软件版本（统一入口，尝试多个可能的位置）
     *
     * @param physical Physical Document
     * @return software version 值
     */
    private String extractSoftwareVersion(org.bson.Document physical) {
        // 方案1: 从 physical.system.properties.property 中提取 current-software
        // （与 get-device-operations-status 一致）
        String softwareVersion = extractCurrentSoftwareFromSystemProperties(physical);
        if (softwareVersion != null) {
            log.info("  ✅ 从 system.properties 提取 current-software: {}", softwareVersion);
            return softwareVersion;
        }

        // 方案2: 从 physical.properties.property 中提取 software-version
        // （NMS 接口返回的结构）
        softwareVersion = extractSoftwareVersionFromDocument(physical);
        if (softwareVersion != null) {
            log.info("  ✅ 从 physical.properties 提取 software-version: {}", softwareVersion);
            return softwareVersion;
        }

        log.warn(
                "  ⚠️ 未找到软件版本信息（尝试了 system.properties.current-software 和 physical.properties.software-version）");
        return null;
    }

    /**
     * 从 physical.system.properties.property 中提取 current-software （与 DeviceMaintenanceStatusService
     * 保持一致）
     *
     * @param physical Physical Document
     * @return current-software 值
     */
    private String extractCurrentSoftwareFromSystemProperties(org.bson.Document physical) {
        try {
            log.info("      🔍 [方式1] 尝试从 system.properties.property 提取 current-software...");

            org.bson.Document system = physical.get("system", org.bson.Document.class);
            if (system == null) {
                log.info("      ⚠️ physical.system 为 null，跳过");
                return null;
            }

            org.bson.Document properties = system.get("properties", org.bson.Document.class);
            if (properties == null) {
                log.info("      ⚠️ system.properties 为 null，跳过");
                return null;
            }

            @SuppressWarnings("unchecked")
            java.util.List<org.bson.Document> propertyList =
                    (java.util.List<org.bson.Document>) properties.get("property");

            if (propertyList == null) {
                log.info("      ⚠️ system.properties.property 为 null，跳过");
                return null;
            }

            log.info("      ✓ 找到 {} 个 system.property 条目，开始遍历...", propertyList.size());

            for (org.bson.Document property : propertyList) {
                String name = property.getString("name");
                if ("current-software".equals(name)) {
                    String value = property.getString("value");
                    log.info("      ✅ [方式1成功] 找到 current-software = {}", value);
                    return value;
                }
            }

            log.info("      ⚠️ system.properties 中未找到 current-software 属性");
            return null;
        } catch (Exception e) {
            log.warn("      ❌ [方式1失败] 从 system.properties 提取异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从 MongoDB Document 的 properties.property 数组中提取 software-version
     *
     * @param physical Physical Document
     * @return software-version 值，如果不存在则返回 null
     */
    private String extractSoftwareVersionFromDocument(org.bson.Document physical) {
        try {
            log.info(
                    "      🔍 [方式2] 尝试从 physical.properties.property 提取 software-version...");

            org.bson.Document properties = physical.get("properties", org.bson.Document.class);
            if (properties == null) {
                log.info("      ⚠️ physical.properties 为 null，跳过");
                log.info("      📋 physical keys: {}", physical.keySet());
                return null;
            }

            @SuppressWarnings("unchecked")
            java.util.List<org.bson.Document> propertyList =
                    (java.util.List<org.bson.Document>) properties.get("property");

            if (propertyList == null) {
                log.info("      ⚠️ properties.property 为 null，跳过");
                log.info("      📋 properties keys: {}", properties.keySet());
                return null;
            }

            log.info("      ✓ 找到 {} 个 physical.property 条目，开始遍历...", propertyList.size());

            for (org.bson.Document property : propertyList) {
                String name = property.getString("name");
                if ("software-version".equals(name)) {
                    String value = property.getString("value");
                    log.info("      ✅ [方式2成功] 找到 software-version = {}", value);
                    return value;
                }
            }

            log.info("      ⚠️ physical.properties 中未找到 software-version 属性");
            return null;
        } catch (Exception e) {
            log.warn("      ❌ [方式2失败] 提取 software-version 异常: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从 Physical 对象提取软件版本（统一入口，尝试多个可能的位置） （针对 DAO 查询的 YANG 对象）
     *
     * @param physical Physical 对象
     * @return software version 值
     */
    private String extractSoftwareVersionFromDAO(Physical physical) {
        String softwareVersion = extractPropertyValueFromSystem(physical, "current-software");
        if (softwareVersion != null) {
            log.info("      ✅ [DAO方式1成功] 从 system.properties 提取 current-software: {}",
                    softwareVersion);
            return softwareVersion;
        }
        return null;
    }

    /**
     * 从 Physical 对象的 system.properties.property 数组中提取指定属性值 （反射方式，针对 YANG 对象）
     *
     * @param physical Physical 对象
     * @param propertyName 属性名（如 "current-software"）
     * @return 属性值，如果不存在则返回 null
     */
    private String extractPropertyValueFromSystem(Physical physical, String propertyName) {
        try {
            log.info("      🔍 [DAO方式1] 尝试从 system.properties.property 提取 {}...",
                    propertyName);

            // 检查是否有 System
            if (physical.getSystem() == null) {
                log.info("      ⚠️ Physical.system is null，跳过");
                return null;
            }

            // 获取 System.properties
            if (physical.getSystem().getProperties() == null) {
                log.info("      ⚠️ System.properties is null，跳过");
                return null;
            }

            // 获取 Property 列表（使用泛型通配符避免具体类型）
            java.util.List<?> properties = physical.getSystem().getProperties().getProperty();

            if (properties == null || properties.isEmpty()) {
                log.info("      ⚠️ System.properties.property is null or empty，跳过");
                return null;
            }

            log.info("      ✓ 找到 {} 个 system.property 条目，开始遍历...", properties.size());

            // 遍历查找指定属性（使用反射）
            for (Object propertyObj : properties) {
                try {
                    // 通过反射调用 getName() 和 getValue()
                    java.lang.reflect.Method getNameMethod = propertyObj.getClass()
                            .getMethod("getName");
                    java.lang.reflect.Method getValueMethod = propertyObj.getClass()
                            .getMethod("getValue");

                    String name = (String) getNameMethod.invoke(propertyObj);
                    if (propertyName.equals(name)) {
                        String value = (String) getValueMethod.invoke(propertyObj);
                        log.info("      ✓ 找到 {} = {}", propertyName, value);
                        return value;
                    }
                } catch (Exception reflectionEx) {
                    log.debug("      ⚠️ 反射调用失败: {}", reflectionEx.getMessage());
                    continue;
                }
            }

            log.info("      ⚠️ system.properties 中未找到 {} 属性", propertyName);
            return null;

        } catch (Exception e) {
            log.warn("      ❌ [DAO方式1失败] 提取 system.properties 属性异常: {}, 错误: {}",
                    propertyName, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 从 MongoDB Physical 对象的 properties.property 数组中提取指定属性值 （反射方式，作为备用）
     *
     * @param physical Physical 对象
     * @param propertyName 属性名（如 "software-version"）
     * @return 属性值，如果不存在则返回 null
     */
    private String extractPropertyValueFromMongoDB(Physical physical, String propertyName) {
        try {
            // 检查是否有 Properties
            if (physical.getProperties() == null) {
                log.debug("  ⚠️ Physical.properties is null");
                return null;
            }

            // 获取 Property 列表（使用泛型通配符避免具体类型）
            java.util.List<?> properties = physical.getProperties().getProperty();

            if (properties == null || properties.isEmpty()) {
                log.debug("  ⚠️ Physical.properties.property is null or empty");
                return null;
            }

            // 遍历查找指定属性（使用反射）
            for (Object propertyObj : properties) {
                try {
                    // 通过反射调用 getName() 和 getValue()
                    java.lang.reflect.Method getNameMethod = propertyObj.getClass()
                            .getMethod("getName");
                    java.lang.reflect.Method getValueMethod = propertyObj.getClass()
                            .getMethod("getValue");

                    String name = (String) getNameMethod.invoke(propertyObj);
                    if (propertyName.equals(name)) {
                        String value = (String) getValueMethod.invoke(propertyObj);
                        log.debug("  ✓ 提取MongoDB属性: {} = {}", propertyName, value);
                        return value;
                    }
                } catch (Exception reflectionEx) {
                    log.debug("  ⚠️ 反射调用失败: {}", reflectionEx.getMessage());
                    continue;
                }
            }

            log.debug("  ⚠️ MongoDB属性不存在: {}", propertyName);
            return null;

        } catch (Exception e) {
            log.warn("  ⚠️ 提取MongoDB属性失败: {}, 错误: {}", propertyName, e.getMessage());
            return null;
        }
    }


    /**
     * 从 physical.properties.property 数组中提取指定属性值
     *
     * @param physical physical 节点的 JSON 数据
     * @param propertyName 属性名（如 "software-version"）
     * @return 属性值，如果不存在则返回 null
     */
    private String extractPropertyValue(com.fasterxml.jackson.databind.JsonNode physical,
            String propertyName) {
        com.fasterxml.jackson.databind.JsonNode properties = physical.path("properties")
                .path("property");

        if (properties.isMissingNode() || !properties.isArray()) {
            return null;
        }

        for (com.fasterxml.jackson.databind.JsonNode property : properties) {
            String name = property.path("name").asText();
            if (propertyName.equals(name)) {
                String value = property.path("value").asText(null);
                log.debug("  ✓ 提取属性: {} = {}", propertyName, value);
                return value;
            }
        }

        log.debug("  ⚠️ 属性不存在: {}", propertyName);
        return null;
    }


    /**
     * 从sftpserver微服务获取SFTP服务器信息 使用RESTCONF CONFIG端点（不是RPC）
     * 注意：虽然方法名叫getId，但实际上是按name匹配（如"192.168.3.206:22"），忽略id字段
     *
     * @param serverId SFTP服务器name（如"192.168.3.206:22"）
     * @return SFTP服务器信息
     */
    public SftpServerDetails getSftpServerById(String serverId) throws CommonException {
        try {
            if (odlRpcClient == null || jsonUtil == null) {
                throw new CommonException(
                        CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "RPC client not available"
                );
            }

            // 通过Zookeeper获取sftpserver服务地址
            // 注意：服务在Zookeeper中注册名为 "ftpServer" (不是 "sftpserver")
            ModuleCredential credential = ModuleUtils.getCredential("ftpServer");

            if (credential == null) {
                throw new CommonException(
                        CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "ftpServer service is not available in Zookeeper"
                );
            }

            // 构造RESTCONF CONFIG URL (正确处理 IPv6 地址)
            String url = String.format("http://%s:%d/restconf/config/ftp-server:ftp-servers",
                    CommonUtil.formateIpAddress(credential.getIp()), credential.getPort());

            log.debug("Querying SFTP server list from: {}", url);

            // 发送GET请求
            String responseBody = odlRpcClient.setConfig(
                    new OdlRpcClientConfig.Builder()
                            .user("admin")
                            .password("admin")
                            .build()
            ).get(url);

            if (responseBody == null || responseBody.isEmpty()) {
                throw new CommonException(
                        CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "Failed to get SFTP server list - empty response"
                );
            }

            log.debug("SFTP server list response: {}", responseBody);

            // 从响应中解析SFTP服务器信息（按name匹配）
            if (responseBody.contains(serverId)) {
                return parseSftpServerFromJson(responseBody, serverId);
            } else {
                throw new CommonException(
                        CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "SFTP服务器不存在: " + serverId
                );
            }

        } catch (CommonException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CommonException(
                    CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Failed to get SFTP server info: " + ex.getMessage(),
                    ex
            );
        }
    }

    /**
     * 从JSON响应中解析SFTP服务器信息 简化版本，实际应该使用YANG反序列化
     */
    private SftpServerDetails parseSftpServerFromJson(String jsonResponse, String serverName) {
        // TODO: 使用proper YANG反序列化
        // 这里简化处理，假设JSON格式符合Postman测试用例中的格式
        try {
            com.alibaba.fastjson.JSONObject root = com.alibaba.fastjson.JSON.parseObject(
                    jsonResponse);
            com.alibaba.fastjson.JSONObject ftpServers = root.getJSONObject("ftp-servers");
            com.alibaba.fastjson.JSONArray serverArray = ftpServers.getJSONArray("ftp-server");

            for (int i = 0; i < serverArray.size(); i++) {
                com.alibaba.fastjson.JSONObject server = serverArray.getJSONObject(i);
                // 只使用name字段匹配，忽略id字段
                if (serverName.equals(server.getString("name"))) {
                    return SftpServerDetails.builder()
                            .id(server.getString("name"))  // 使用name作为标识
                            .address(server.getString("address"))
                            .port(server.getInteger("port"))
                            .user(server.getString("user"))
                            .password(server.getString("password"))
                            .build();
                }
            }
        } catch (Exception ex) {
            log.error("Failed to parse SFTP server JSON", ex);
        }

        throw new CommonException(
                CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "Failed to parse SFTP server from response"
        );
    }

    /**
     * 从完整文件路径中提取文件名
     */
    private String extractFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return filePath;
        }
        int lastSlash = filePath.lastIndexOf('/');
        return lastSlash >= 0 ? filePath.substring(lastSlash + 1) : filePath;
    }

    /**
     * 从完整文件路径中提取目录路径
     * 如果路径不包含目录分隔符，使用默认路径 /software（向后兼容）
     */
    private String extractDirectory(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "/software";
        }
        int lastSlash = filePath.lastIndexOf('/');
        if (lastSlash <= 0) {
            // 只有文件名，没有路径 → 使用默认路径（向后兼容）
            log.debug("  ⚠️ 文件路径不包含目录: {}，使用默认路径 /software", filePath);
            return "/software";
        }
        return filePath.substring(0, lastSlash);
    }

    /**
     * 密码脱敏（显示首尾字符，中间用*代替）
     */
    private String maskPassword(String password) {
        if (password == null || password.length() <= 2) {
            return "***";
        }
        return password.charAt(0) + "****" + password.charAt(password.length() - 1);
    }

    /**
     * 在 SFTP 服务器上创建目录 调用 ftpServer 服务的 mkdir RPC接口
     *
     * @param sftpServerName SFTP服务器名称
     * @param remoteFolder 要创建的远程目录路径（可以是多层目录，如 /dbbackup/device1/20251020）
     * @throws CommonException 如果创建目录失败
     */
    public void createRemoteDirectory(String sftpServerName, String remoteFolder)
            throws CommonException {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📁 [SFTP mkdir] 创建远程目录");
        log.info("  SFTP服务器: {}", sftpServerName);
        log.info("  远程目录: {}", remoteFolder);

        try {
            if (odlRpcClient == null || jsonUtil == null) {
                throw new CommonException(
                        CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "RPC client not available"
                );
            }

            // 通过Zookeeper获取ftpServer服务地址
            ModuleCredential credential = ModuleUtils.getCredential("ftpServer");

            if (credential == null) {
                throw new CommonException(
                        CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "ftpServer service is not available in Zookeeper"
                );
            }

            // 构造 mkdir RPC URL
            String url = String.format("http://%s:%d/restconf/operations/ftp-server:mkdir",
                    CommonUtil.formateIpAddress(credential.getIp()), credential.getPort());

            log.debug("  RPC URL: {}", url);

            // ⭐ 递归创建父目录（类似 mkdir -p）
            createDirectoryRecursively(url, sftpServerName, remoteFolder);

            log.info("✅ SFTP目录创建成功（或已存在）: {}", remoteFolder);
            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");

        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ 调用mkdir RPC失败", e);
            throw new CommonException(
                    CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "调用SFTP mkdir服务失败: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * 递归创建目录（类似 mkdir -p）
     *
     * @param url SFTP mkdir RPC URL
     * @param sftpServerName SFTP服务器名称
     * @param remoteFolder 要创建的远程目录路径
     * @throws CommonException 如果创建目录失败
     */
    private void createDirectoryRecursively(String url, String sftpServerName, String remoteFolder)
            throws CommonException {
        // 1. 分割路径
        String[] parts = remoteFolder.split("/");
        StringBuilder currentPath = new StringBuilder();

        // 2. 逐级创建目录
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;  // 跳过空字符串（如开头的 /）
            }

            currentPath.append("/").append(part);
            String pathToCreate = currentPath.toString();

            try {
                // 构造 mkdir 输入 JSON
                String requestJson = String.format(
                        "{\"input\":{\"server-name\":\"%s\",\"remote-folder\":\"%s\"}}",
                        sftpServerName, pathToCreate
                );

                log.debug("  尝试创建目录: {}", pathToCreate);

                // 发送 POST 请求
                odlRpcClient.setConfig(
                        new OdlRpcClientConfig.Builder()
                                .user("admin")
                                .password("admin")
                                .build()
                );
                String responseBody = odlRpcClient.post(url, requestJson);

                // 解析响应（如果目录已存在，也算成功）
                parseSftpRpcResponse(responseBody, "mkdir");
                log.debug("  ✓ 目录创建成功: {}", pathToCreate);

            } catch (Exception e) {
                // 如果是"目录已存在"错误，忽略（继续创建子目录）
                String errorMsg = e.getMessage();
                if (errorMsg != null && (errorMsg.contains("already exists") ||
                        errorMsg.contains("File exists") ||
                        errorMsg.contains("已存在"))) {
                    log.debug("  ℹ️  目录已存在，跳过: {}", pathToCreate);
                    continue;
                }

                // 其他错误，抛出异常
                log.error("  ✗ 创建目录失败: {}", pathToCreate, e);
                throw new CommonException(
                        CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "创建目录失败: " + pathToCreate + " - " + e.getMessage(),
                        e
                );
            }
        }
    }

    /**
     * 统一解析 SFTP RPC 响应结果 兼容两种响应格式: 1. {"output":{"result":"success"}}  (ftp-server 服务) 2.
     * {"output":{"return-code":"Success"}}  (eml-manager 服务)
     *
     * @param responseBody RPC 响应 JSON 字符串
     * @param operationName 操作名称（用于日志）
     * @return true=成功, false=失败
     * @throws CommonException 如果响应格式异常或操作失败
     */
    private boolean parseSftpRpcResponse(String responseBody, String operationName)
            throws CommonException {
        if (responseBody == null || responseBody.isEmpty()) {
            throw new CommonException(
                    CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    operationName + " RPC返回为空"
            );
        }

        try {
            com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(
                    responseBody);
            com.alibaba.fastjson.JSONObject output = json.getJSONObject("output");

            if (output == null) {
                log.warn("⚠️ {} 响应中没有 output 对象，假设成功", operationName);
                return true;
            }

            // 尝试两种格式:
            // 格式1: {"result": "success"}
            String result = output.getString("result");
            if (result != null) {
                log.info("  ✅ {} 结果: {}", operationName, result);
                if ("success".equalsIgnoreCase(result)) {
                    return true;
                }

                // 获取错误信息
                String errorMessage = output.getString("error-message");
                if (errorMessage == null) {
                    errorMessage = output.getString("message");
                }

                log.error("❌ {} 失败: result={}, message={}", operationName, result, errorMessage);

                // 如果是目录已存在的错误，不抛异常（这是预期情况）
                if (errorMessage != null && errorMessage.toLowerCase().contains("exist")) {
                    log.info("  ℹ️ 目录已存在，继续执行");
                    return true;
                }

                throw new CommonException(
                        CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        operationName + " 失败: " + (errorMessage != null ? errorMessage : result)
                );
            }

            // 格式2: {"return-code": "Success"}
            String returnCode = output.getString("return-code");
            if (returnCode != null) {
                log.info("  ✅ {} 返回码: {}", operationName, returnCode);
                if ("success".equalsIgnoreCase(returnCode)) {
                    return true;
                }

                String errorMessage = output.getString("error-message");
                if (errorMessage == null) {
                    errorMessage = output.getString("message");
                }

                log.error("❌ {} 失败: return-code={}, message={}", operationName, returnCode,
                        errorMessage);

                throw new CommonException(
                        CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        operationName + " 失败: " + (errorMessage != null ? errorMessage
                                : returnCode)
                );
            }

            // 都没有，假设成功
            log.warn("⚠️ {} 响应中没有 result 或 return-code 字段，假设成功", operationName);
            return true;

        } catch (com.alibaba.fastjson.JSONException e) {
            log.warn("⚠️ 无法解析 {} 响应JSON，假设成功: {}", operationName, e.getMessage());
            return true;
        }
    }

    /**
     * 列出 SFTP 服务器上指定目录的内容 调用 ftpServer 服务的 list RPC接口
     *
     * @param sftpServerName SFTP服务器名称
     * @param remotePath 远程目录路径
     * @return 文件/目录列表（包含名称、类型、修改时间等）
     */
    public java.util.List<SftpFileInfo> listRemoteDirectory(String sftpServerName,
            String remotePath) {
        log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        log.info("📂 [SFTP list] 列出目录内容");
        log.info("  SFTP服务器: {}", sftpServerName);
        log.info("  目录路径: {}", remotePath);

        try {
            // 1. 检查 RPC client 是否可用
            if (odlRpcClient == null) {
                throw new CommonException(
                        CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "RPC client not available"
                );
            }

            // 2. 通过Zookeeper获取ftpServer服务地址（与mkdir相同）
            ModuleCredential credential = ModuleUtils.getCredential("ftpServer");

            if (credential == null) {
                throw new CommonException(
                        CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "ftpServer service is not available in Zookeeper"
                );
            }

            // 3. 构造 list RPC URL
            String url = String.format("http://%s:%d/restconf/operations/ftp-server:list",
                    CommonUtil.formateIpAddress(credential.getIp()), credential.getPort());

            log.debug("  RPC URL: {}", url);

            // 4. 构造 list 输入 JSON
            // 格式: {"input": {"server-name": "xxx", "folder-name": "/path/to/dir"}}
            String requestJson = String.format(
                    "{\"input\":{\"server-name\":\"%s\",\"folder-name\":\"%s\"}}",
                    sftpServerName, remotePath
            );

            log.debug("  请求 JSON: {}", requestJson);

            // 5. 发送 POST 请求
            odlRpcClient.setConfig(
                    new OdlRpcClientConfig.Builder()
                            .user("admin")
                            .password("admin")
                            .build()
            );
            String responseBody = odlRpcClient.post(url, requestJson);

            if (responseBody == null) {
                throw new CommonException(
                        CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "list RPC返回为空"
                );
            }

            log.debug("  响应内容: {}", responseBody);

            // 使用统一的响应解析方法检查操作是否成功
            parseSftpRpcResponse(responseBody, "list");

            // 解析文件列表
            java.util.List<SftpFileInfo> fileList = new java.util.ArrayList<>();

            try {
                com.alibaba.fastjson.JSONObject json = com.alibaba.fastjson.JSON.parseObject(
                        responseBody);
                com.alibaba.fastjson.JSONObject output = json.getJSONObject("output");

                if (output != null) {
                    // ✅ 尝试解析新格式: {"output": {"file": [{"name": "xxx", "type": "file/directory", ...}]}}
                    com.alibaba.fastjson.JSONArray fileArray = output.getJSONArray("file");
                    if (fileArray != null && !fileArray.isEmpty()) {
                        for (int i = 0; i < fileArray.size(); i++) {
                            com.alibaba.fastjson.JSONObject fileObj = fileArray.getJSONObject(i);
                            String name = fileObj.getString("name");
                            String type = fileObj.getString("type");

                            if (name != null && !name.isEmpty() && !name.equals(".")
                                    && !name.equals("..")) {
                                boolean isDirectory = "directory".equalsIgnoreCase(type);

                                fileList.add(SftpFileInfo.builder()
                                        .name(name)
                                        .isDirectory(isDirectory)
                                        .build());
                            }
                        }
                        log.info("  ✅ 找到 {} 个文件/目录（数组格式）", fileList.size());
                    } else {
                        // ⚠️ 兼容旧格式: {"output": {"file-list": "file1\nfile2\n..."}}
                        String fileListStr = output.getString("file-list");
                        if (fileListStr != null && !fileListStr.trim().isEmpty()) {
                            String[] lines = fileListStr.split("\n");
                            for (String line : lines) {
                                line = line.trim();
                                if (!line.isEmpty() && !line.equals(".") && !line.equals("..")) {
                                    boolean isDirectory = line.endsWith("/");
                                    String name = isDirectory ? line.substring(0, line.length() - 1)
                                            : line;

                                    fileList.add(SftpFileInfo.builder()
                                            .name(name)
                                            .isDirectory(isDirectory)
                                            .build());
                                }
                            }
                            log.info("  ✅ 找到 {} 个文件/目录（字符串格式）", fileList.size());
                        }
                    }
                }
            } catch (com.alibaba.fastjson.JSONException e) {
                log.error("❌ 解析文件列表失败: {}", e.getMessage());
                log.error("  响应内容: {}", responseBody);
                // 不抛异常，返回空列表
            }

            log.info("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            return fileList;

        } catch (CommonException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ 调用ls RPC失败", e);
            throw new CommonException(
                    CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "调用SFTP ls服务失败: " + e.getMessage(),
                    e
            );
        }
    }

    /**
     * RPC调用结果（泛型支持不同类型的output）
     */
    @Data
    @AllArgsConstructor
    public static class RpcResult {

        private Object output;  // 可以是 NeSoftwareOperateOutput 或 NeDatabaseOperateOutput
        private java.util.Map<String, Object> debugPayload;
        private String deviceFriendlyName;  // 设备友好名称（用于TaskInfo通知）
    }

    // ========== 内部数据类 ==========

    @lombok.Builder
    @lombok.Data
    public static class DevicePhysicalInfo {

        private String nodeId;
        private String friendlyName;  // 设备友好名称，如 "FRA-COHERENT-336"
        private String ip;
        private String loopbackAddress;
        private String nmsAddress;
        private Integer port;
        private String loginName;
        private String loginPassword;
        private String vendorType;      // 设备厂商类型（如 "CHASSIS", "DC908"）
        private String vendorName;      // 设备厂商名称（如 "COHERENT", "HUAWEI"）
        private String softwareVersion; // 软件版本（从 properties.property 数组中提取）
    }

    @lombok.Builder
    @lombok.Data
    public static class SftpServerDetails {

        private String id;
        private String address;
        private Integer port;
        private String user;
        private String password;
    }

    /**
     * SFTP文件信息
     */
    @lombok.Builder
    @lombok.Data
    public static class SftpFileInfo {

        private String name;           // 文件/目录名
        private boolean isDirectory;   // 是否为目录
        private Long modifiedTime;     // 修改时间（可选）
        private Long size;             // 文件大小（可选）
    }
}
