package devicemaintenance.controller;

import devicemaintenance.integration.ServiceDiscoveryClient;
import devicemaintenance.integration.ServiceDiscoveryClient.ServiceInstance;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otc.zkclient4boot.util.ConfLoader;
import net.flex.dci.otc.zk.common.util.ZkConst;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 对外暴露健康检查与ZooKeeper注册状态查询接口
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class HealthController {

    private final ServiceDiscoveryClient serviceDiscoveryClient;
    private final RestTemplate restTemplate;
    private final String moduleName;
    private final String zkNamespace;
    private final List<String> defaultExternalServices;
    private final String defaultHealthPath;
    private final Map<String, String> healthPathOverrides;

    @Autowired
    public HealthController(ServiceDiscoveryClient serviceDiscoveryClient,
                            RestTemplateBuilder restTemplateBuilder,
                            @Value("${spring.application.name:device-maintenance}") String moduleName,
                            @Value("${device-maintenance.external-services:nemgr,taskinfo,sftpserver,notifier,scheduler}") String externalServices,
                            @Value("${device-maintenance.external-service-health-path:/actuator/health}") String defaultHealthPath,
                            @Value("#{${device-maintenance.service-health-path-overrides:{}}}") Map<String, String> healthPathOverrides) {
        this.serviceDiscoveryClient = serviceDiscoveryClient;
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3))
                .build();
        this.moduleName = moduleName;
        // 直接从 zkclient4boot 的 ConfLoader 读取 namespace（与其他微服务一致）
        // 不使用 @Value，因为 install.sh 的 render_template 无法正确处理 YAML 中的占位符
        String namespace = net.flex.dci.otc.zkclient4boot.util.ConfLoader.getValue("NAMESPACE");
        this.zkNamespace = (namespace == null || namespace.isEmpty()) ? "dciworld/default" : namespace;
        this.defaultExternalServices = parseExternalServices(externalServices);
        this.defaultHealthPath = normalizeHealthPath(defaultHealthPath);
        this.healthPathOverrides = normalizeOverrides(healthPathOverrides);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("status", "UP");
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("moduleName", moduleName);
        payload.put("zk", buildZkStatus(null));
        return payload;
    }

    @GetMapping("/health/zookeeper")
    public ResponseEntity<ZookeeperHealthResponse> zookeeperStatus(
            @RequestParam(value = "services", required = false) String services) {
        ZookeeperHealthResponse response = buildZkStatus(services);
        if (response == null) {
            return ResponseEntity.ok(new ZookeeperHealthResponse("UNAVAILABLE", zkNamespace, LocalDateTime.now().toString(),
                    new ArrayList<ServiceStatus>(), new ArrayList<ServiceStatus>()));
        }
        return ResponseEntity.ok(response);
    }

    private ZookeeperHealthResponse buildZkStatus(String services) {
        List<ServiceStatus> self = new ArrayList<>();
        Optional<ServiceInstance> selfInstance = serviceDiscoveryClient.getRegisteredInstance(moduleName);
        if (selfInstance.isPresent()) {
            ServiceStatus status = buildServiceStatus(moduleName, selfInstance.get());
            self.add(status);
        } else {
            self.add(new ServiceStatus(moduleName, null, -1, "NOT_FOUND", "UNKNOWN", null));
        }

        Set<String> targetServices = new LinkedHashSet<>(defaultExternalServices);
        if (services != null && !services.trim().isEmpty()) {
            String[] extra = services.split(",");
            for (String service : extra) {
                String trimmed = service.trim();
                if (!trimmed.isEmpty()) {
                    targetServices.add(trimmed);
                }
            }
        }
        
        // 防止自身服务在otherServices中重复出现
        targetServices.remove(moduleName);
        log.info("Final target services for otherServices: {}", targetServices);

        List<ServiceStatus> others = new ArrayList<>();
        for (String target : targetServices) {
            others.add(checkService(target));
        }

        boolean allZkUp = self.stream().allMatch(s -> "REGISTERED".equalsIgnoreCase(s.getZkStatus()))
                && others.stream().allMatch(s -> "REGISTERED".equalsIgnoreCase(s.getZkStatus()));
        boolean allHealthUp = self.stream().allMatch(s -> "UP".equalsIgnoreCase(s.getHealthStatus())
                        || "UNKNOWN".equalsIgnoreCase(s.getHealthStatus()))
                && others.stream().allMatch(s -> "UP".equalsIgnoreCase(s.getHealthStatus())
                        || "UNKNOWN".equalsIgnoreCase(s.getHealthStatus()));

        String overallStatus;
        if (allZkUp && allHealthUp) {
            overallStatus = "UP";
        } else if (allZkUp || allHealthUp) {
            overallStatus = "PARTIAL";
        } else {
            overallStatus = "DOWN";
        }

        return new ZookeeperHealthResponse(overallStatus, zkNamespace, LocalDateTime.now().toString(), self, others);
    }

    private ServiceStatus checkService(String service) {
        Optional<ServiceInstance> instance = serviceDiscoveryClient.discoverService(service);
        if (instance.isPresent()) {
            return buildServiceStatus(service, instance.get());
        }
        return new ServiceStatus(service, null, -1, "NOT_FOUND", "UNKNOWN", null);
    }

    private ServiceStatus buildServiceStatus(String serviceName, ServiceInstance instance) {
        String host = instance.getHost();
        int port = instance.getPort();
        String healthPath = resolveHealthPath(serviceName);
        String healthUrl = buildHealthUrl(host, port, healthPath, serviceName);
        String healthStatus = probeHealth(healthUrl);
        return new ServiceStatus(serviceName, host, port, "REGISTERED", healthStatus, healthUrl);
    }

    private String probeHealth(String url) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("status")) {
                return String.valueOf(response.get("status")).toUpperCase();
            }
            return "UNKNOWN";
        } catch (RestClientException ex) {
            log.warn("Health probe failed for {}: {}", url, ex.getMessage());
            return "UNREACHABLE";
        }
    }

    private List<String> parseExternalServices(String services) {
        List<String> list = new ArrayList<>();
        if (services != null && !services.trim().isEmpty()) {
            String[] parts = services.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    list.add(trimmed);
                }
            }
        }
        return list;
    }

    private String normalizeHealthPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            return "/actuator/health";
        }
        String trimmed = path.trim();
        if (!trimmed.startsWith("/")) {
            trimmed = "/" + trimmed;
        }
        return trimmed;
    }

    private String resolveHealthPath(String serviceName) {
        if (serviceName == null) {
            return defaultHealthPath;
        }
        String key = serviceName.trim();
        if (key.isEmpty()) {
            return defaultHealthPath;
        }
        String direct = healthPathOverrides.get(key);
        if (direct != null) {
            return normalizeHealthPath(direct);
        }
        String lower = healthPathOverrides.get(key.toLowerCase(Locale.ROOT));
        if (lower != null) {
            return normalizeHealthPath(lower);
        }
        String upper = healthPathOverrides.get(key.toUpperCase(Locale.ROOT));
        if (upper != null) {
            return normalizeHealthPath(upper);
        }
        return defaultHealthPath;
    }

    private Map<String, String> normalizeOverrides(Map<String, String> overrides) {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (overrides == null) {
            return normalized;
        }
        for (Map.Entry<String, String> entry : overrides.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.trim().isEmpty() || value == null || value.trim().isEmpty()) {
                continue;
            }
            String normalizedKey = key.trim();
            String normalizedPath = normalizeHealthPath(value);
            normalized.put(normalizedKey, normalizedPath);
            normalized.put(normalizedKey.toLowerCase(Locale.ROOT), normalizedPath);
            normalized.put(normalizedKey.toUpperCase(Locale.ROOT), normalizedPath);
        }
        return normalized;
    }

    private String buildHealthUrl(String host, int port, String healthPath, String serviceName) {
        // 正确处理 IPv6 地址
        String formattedHost = CommonUtil.formateIpAddress(host);
        if (moduleName.equalsIgnoreCase(serviceName)) {
            return String.format("http://%s:%d/device-maintenance%s", formattedHost, port, healthPath);
        }
        return String.format("http://%s:%d%s", formattedHost, port, healthPath);
    }

    private List<String> buildServiceAliases(String serviceName) {
        Set<String> aliases = new LinkedHashSet<>();
        if (serviceName == null) {
            return new ArrayList<>(aliases);
        }
        String trimmed = serviceName.trim();
        if (trimmed.isEmpty()) {
            return new ArrayList<>(aliases);
        }
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

    @Getter
    @AllArgsConstructor
    public static class ZookeeperHealthResponse {
        private final String status;
        private final String namespace;
        private final String timestamp;
        private final List<ServiceStatus> self;
        private final List<ServiceStatus> otherServices;
    }

    @Getter
    @AllArgsConstructor
    public static class ServiceStatus {
        private final String serviceName;
        private final String host;
        private final int port;
        private final String zkStatus;
        private final String healthStatus;
        private final String healthUrl;
    }
}

