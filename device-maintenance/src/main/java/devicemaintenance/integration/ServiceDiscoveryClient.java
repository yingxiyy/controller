package devicemaintenance.integration;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otc.zk.common.constants.DciClientConstants;
import net.flex.dci.otc.zk.common.entity.InstanceDetails;
import net.flex.dci.otc.zkclient4boot.refactor.utils.DciInstancesUtils;
import net.flex.dci.otc.zkclient4boot.refactor.utils.ZkUtils;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.KeeperException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Component
@Slf4j
public class ServiceDiscoveryClient {

    public ServiceDiscoveryClient() {
        log.info("ServiceDiscoveryClient initialized using DciInstancesUtils");
    }

    public Optional<ServiceInstance> getRegisteredInstance(String serviceName) {
        return discoverService(serviceName);
    }

    public Optional<ServiceInstance> discoverService(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            return Optional.empty();
        }
        
        log.info("Discovering service: {} using DciInstancesUtils", serviceName);
        
        // 使用与其他组件相同的方法：DciInstancesUtils.getStateInstancesByModuleName
        return discoverServiceUsingDciUtils(serviceName);
    }
    
    private Optional<ServiceInstance> discoverServiceUsingDciUtils(String serviceName) {
        try {
            log.info("Discovering service: {} using unified native ZK approach", serviceName);
            
            // 统一策略: 所有服务都使用原生ZK客户端（包括device-maintenance自身）
            // 因为发现DciInstancesUtils对device-maintenance也不能正常工作
            log.info("Using native ZK client for service: {}", serviceName);
            return discoverServiceWithNativeZk(serviceName);
            
        } catch (Exception ex) {
            log.error("Failed to discover service {} using namespace-aware approach: {}", serviceName, ex.getMessage(), ex);
            return Optional.empty();
        }
    }
    
    private Optional<ServiceInstance> discoverServiceDirectPath(String serviceName) {
        String namespace = resolveNamespace();
        
        // 尝试多种路径策略，因为ZkUtils可能有namespace上下文
        String[] possiblePaths = {
            "STATE/" + serviceName,                    // 相对路径 - 如果ZkUtils已配置namespace
            buildAbsoluteStatePath(namespace, serviceName), // 绝对路径（使用配置的namespace）
            "/STATE/" + serviceName                    // 简单绝对路径
        };
        
        for (String directPath : possiblePaths) {
            log.info("Trying path for service {}: {}", serviceName, directPath);
            
            try {
                if (ZkUtils.checkExists(directPath) == null) {
                    log.debug("Path does not exist: {}", directPath);
                    continue; // 尝试下一个路径
                }
                
                List<String> nodes = ZkUtils.getChildren(directPath);
                log.info("Found {} nodes under {}: {}", nodes == null ? 0 : nodes.size(), directPath, nodes);
                
                if (nodes == null || nodes.isEmpty()) {
                    continue; // 尝试下一个路径
                }
                
                for (String node : nodes) {
                    String nodePath = directPath + "/" + node;
                    log.info("Reading node data from: {}", nodePath);
                    String data = ZkUtils.getNodeData(nodePath);
                    
                    InstanceDetails detail = DciInstancesUtils.parseDetailFromValue(data);
                    if (detail != null) {
                        String host = firstNonBlank(detail.getHostIp(), detail.getMyIp());
                        int port = detail.getPort();
                        String instanceId = detail.getId();
                        
                        ServiceInstance serviceInstance = new ServiceInstance(serviceName, instanceId, host, port);
                        log.info("Service {} found via direct path {}: {}", serviceName, serviceInstance);
                        return Optional.of(serviceInstance);
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to access path {} for service {}: {}", directPath, serviceName, ex.getMessage());
                continue; // 尝试下一个路径
            }
        }
        
        log.warn("Service {} not found in any of the attempted paths", serviceName);
        return Optional.empty();
    }
    
    /**
     * 使用原生ZooKeeper客户端绕过namespace限制直接访问
     */
    private Optional<ServiceInstance> discoverServiceWithNativeZk(String serviceName) {
        // 从 zkclient_conf.properties 读取配置（与其他微服务保持一致）
        String zkConnectString = net.flex.dci.otc.zkclient4boot.util.ConfLoader.getValue("ZOOKEEPER_SERVERS");
        String namespace = resolveNamespace();
        
        // 如果没有配置，使用默认值
        if (zkConnectString == null || zkConnectString.isEmpty()) {
            zkConnectString = "127.0.0.1:2190,127.0.0.1:2191,127.0.0.1:2192";
        }

        String targetPath = buildAbsoluteStatePath(namespace, serviceName);
        
        try {
            log.info("Using native ZK client to discover service: {} at path: {}", serviceName, targetPath);
            
            ZooKeeper zk = new ZooKeeper(zkConnectString, 10000, event -> {
                log.debug("ZK event: {}", event);
            });
            
            // 等待连接
            int retries = 0;
            while (zk.getState() != ZooKeeper.States.CONNECTED && retries < 50) {
                Thread.sleep(100);
                retries++;
            }
            
            if (zk.getState() != ZooKeeper.States.CONNECTED) {
                log.error("Failed to connect to ZooKeeper");
                zk.close();
                return Optional.empty();
            }

            // 检查路径是否存在
            if (zk.exists(targetPath, false) == null) {
                log.warn("Native ZK: Path does not exist: {}", targetPath);
                zk.close();
                return Optional.empty();
            }

            // 获取子节点
            List<String> children = zk.getChildren(targetPath, false);
            log.info("Native ZK: Found {} children under {}: {}", children.size(), targetPath, children);
            
            if (children.isEmpty()) {
                zk.close();
                return Optional.empty();
            }

            // 读取第一个子节点的数据
            String firstChild = children.get(0);
            String nodePath = targetPath + "/" + firstChild;
            byte[] data = zk.getData(nodePath, false, null);
            String nodeData = new String(data);
            
            log.info("Native ZK: Read data from {}: {}", nodePath, nodeData);
            
            zk.close();
            
            // 解析数据
            InstanceDetails detail = DciInstancesUtils.parseDetailFromValue(nodeData);
            if (detail != null) {
                String host = firstNonBlank(detail.getHostIp(), detail.getMyIp());
                int port = detail.getPort();
                ServiceInstance instance = new ServiceInstance(serviceName, firstChild, host, port);
                log.info("Native ZK: Successfully discovered service {}: {}", serviceName, instance);
                        return Optional.of(instance);
                    }
            
        } catch (Exception ex) {
            log.error("Native ZK discovery failed for service {}: {}", serviceName, ex.getMessage(), ex);
        }
        
        return Optional.empty();
    }
    
    private Optional<ServiceInstance> discoverServiceDirectly(String serviceName) {
        String namespace = resolveNamespace();
        
        // 根据ZK根目录结构，服务注册在 /{namespace}/STATE/{serviceName}
        String[] possibleBasePaths = {
            "/STATE",                           // 主要路径 - 如果已配置namespace，直接用/STATE
            buildAbsoluteStateBasePath(namespace), // 配置的namespace路径
            "/dciworld/default/STATE",          // 备用路径 - 默认namespace
            "/default/STATE"                    // 其他可能路径
        };
        
        for (String basePath : possibleBasePaths) {
            String directPath = basePath + "/" + serviceName;
            log.info("Trying ZK path: {}", directPath);
            Optional<ServiceInstance> result = resolveFromZkPath(serviceName, directPath);
            if (result.isPresent()) {
                log.info("Service {} found at path: {}", serviceName, directPath);
                return result;
            }
            
            // 尝试别名
            List<String> aliases = buildAliases(serviceName);
            for (String alias : aliases) {
                String aliasPath = basePath + "/" + alias;
                log.debug("Trying alias path: {}", aliasPath);
                Optional<ServiceInstance> instance = resolveFromZkPath(serviceName, aliasPath);
                if (instance.isPresent()) {
                    log.info("Service {} found via alias {} at path: {}", serviceName, alias, aliasPath);
                    return instance;
                }
            }
        }
        
        log.warn("Service {} not found in any ZK paths", serviceName);
            return Optional.empty();
    }
    
    private Optional<ServiceInstance> resolveFromZkPath(String serviceName, String zkPath) {
        try {
            log.debug("Checking ZK path: {}", zkPath);
            if (ZkUtils.checkExists(zkPath) == null) {
                log.debug("Path does not exist: {}", zkPath);
                return Optional.empty();
            }
            List<String> nodes = ZkUtils.getChildren(zkPath);
            log.debug("Found {} nodes under {}: {}", nodes == null ? 0 : nodes.size(), zkPath, nodes);
            if (nodes == null || nodes.isEmpty()) {
            return Optional.empty();
            }
            for (String node : nodes) {
                String nodePath = zkPath + "/" + node;
                log.debug("Reading node data from: {}", nodePath);
                String data = ZkUtils.getNodeData(nodePath);
                log.debug("Node {} data: {}", nodePath, data);
                InstanceDetails detail = DciInstancesUtils.parseDetailFromValue(data);
                ServiceInstance instance = toServiceInstance(serviceName, node, detail);
                log.debug("Parsed service instance: {}", instance);
                if (instance.getHost() != null && instance.getPort() > 0) {
                    return Optional.of(instance);
                }
            }
        } catch (Exception ex) {
            log.warn("resolveFromZkPath failed for {}: {}", zkPath, ex.getMessage(), ex);
        }
        return Optional.empty();
    }

    public Optional<String> getServiceUrl(String serviceName) {
        return discoverService(serviceName)
                .map(instance -> String.format("http://%s:%d", 
                        CommonUtil.formateIpAddress(instance.getHost()), 
                        instance.getPort()));
    }


    private ServiceInstance toServiceInstance(String serviceName, String nodeId, InstanceDetails detail) {
        if (detail == null) {
            return new ServiceInstance(serviceName, nodeId, null, -1);
        }
        String host = firstNonBlank(detail.getHostIp(), detail.getMyIp(), parseHost(detail.getRestful()));
        int port = detail.getPort();
        if (port <= 0) {
            port = parsePort(detail.getRestful());
        }
        return new ServiceInstance(serviceName, nodeId, host, port);
    }


    private List<String> buildAliases(String serviceName) {
        Set<String> aliases = new LinkedHashSet<>();
        String trimmed = serviceName.trim();
        aliases.add(trimmed);
        aliases.add(trimmed.toLowerCase(Locale.ROOT));
        aliases.add(trimmed.toUpperCase(Locale.ROOT));
        aliases.add(trimmed.replace("_", "").replace("-", ""));
        aliases.add(camelize(trimmed, "-"));
        aliases.add(camelize(trimmed, "_"));
        return new ArrayList<>(aliases);
    }

    private String camelize(String value, String delimiter) {
        if (!value.contains(delimiter)) {
            return capitalize(value);
        }
        StringBuilder builder = new StringBuilder();
        String[] parts = value.split(delimiter);
        for (String part : parts) {
            if (!part.isEmpty()) {
                builder.append(capitalize(part));
            }
        }
        return builder.toString();
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (value.length() == 1) {
            return value.toUpperCase(Locale.ROOT);
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private String normalizeBase(String basePath) {
        String path = basePath.trim();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private String joinPath(String parent, String child) {
        if (parent.endsWith("/")) {
            return parent + child;
        }
        return parent + "/" + child;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private String resolveNamespace() {
        String namespace = net.flex.dci.otc.zkclient4boot.util.ConfLoader.getValue("NAMESPACE");
        if (namespace == null || namespace.trim().isEmpty()) {
            return "default";
        }
        namespace = namespace.trim();
        String rootPrefix = DciClientConstants.ROOT + "/";
        if (namespace.startsWith("/")) {
            namespace = namespace.substring(1);
        }
        if (namespace.startsWith(rootPrefix)) {
            return namespace.substring(rootPrefix.length());
        }
        return namespace;
    }

    private String buildAbsoluteStateBasePath(String namespace) {
        return "/" + DciClientConstants.ROOT + "/" + namespace + "/STATE";
    }

    private String buildAbsoluteStatePath(String namespace, String serviceName) {
        return buildAbsoluteStateBasePath(namespace) + "/" + serviceName;
    }

    private String parseHost(String restful) {
        if (restful == null) {
            return null;
        }
        try {
            java.net.URI uri = new java.net.URI(restful);
            return uri.getHost();
        } catch (Exception ignored) {
            return null;
        }
    }

    private int parsePort(String restful) {
        if (restful == null) {
            return -1;
        }
        try {
            java.net.URI uri = new java.net.URI(restful);
            return uri.getPort();
        } catch (Exception ignored) {
            return -1;
        }
    }

    public static class ServiceInstance {
        private final String serviceName;
        private final String instanceId;
        private final String host;
        private final int port;

        public ServiceInstance(String serviceName, String instanceId, String host, int port) {
            this.serviceName = serviceName;
            this.instanceId = instanceId;
            this.host = host;
            this.port = port;
        }

        public String getServiceName() {
            return serviceName;
        }

        public String getInstanceId() {
            return instanceId;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        @Override
        public String toString() {
            return String.format("ServiceInstance{name='%s', id='%s', address='%s:%d'}", 
                    serviceName, instanceId, host == null ? "" : host, port);
        }
    }
}
