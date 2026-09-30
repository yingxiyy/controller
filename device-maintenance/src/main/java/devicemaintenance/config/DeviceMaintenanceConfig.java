package devicemaintenance.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 设备维护配置类
 */
@Configuration
@ConfigurationProperties(prefix = "device-maintenance")
public class DeviceMaintenanceConfig {

    private String version = "1.0.0";

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
