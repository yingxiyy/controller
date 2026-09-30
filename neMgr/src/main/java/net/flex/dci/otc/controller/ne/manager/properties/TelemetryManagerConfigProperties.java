package net.flex.dci.otc.controller.ne.manager.properties;

import com.alibaba.fastjson.JSON;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.model.TelemetryMgrConfig;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.TelemetryConfig;
import net.flex.dci.otc.controller.ne.manager.utils.NeManagerUtils;
import org.springframework.context.annotation.Configuration;

/**
 * @version 1.0
 * @date 6/11/2025 10:52 AM
 */
@Configuration
@Slf4j
public class TelemetryManagerConfigProperties {

    private final TelemetryMgrConfig telemetryMgrConfig;

    public TelemetryManagerConfigProperties() throws IOException {
        log.info("load telemetry manager config properties");
        InputStream inputStream = NeManagerUtils.loadFileInputStream(
                TelemetryConfig.TELEMETRY_MANAGER_CONFIG_FILE);
        this.telemetryMgrConfig = JSON.parseObject(inputStream, TelemetryMgrConfig.class);
    }

    public boolean isMixed() {
        return telemetryMgrConfig.getTelemetryMgrConfigRoot().getMixed();
    }

    public String privateIpArea() {
        return telemetryMgrConfig.getTelemetryMgrConfigRoot().getIpPrefix().getPrivateIpArea();
    }

    public String privateNtpAddress() {
        return telemetryMgrConfig.getTelemetryMgrConfigRoot().getTelemetryMgrNtpConfig()
                .getPrivateNtp();
    }

    public String publicNtpAddress() {
        return telemetryMgrConfig.getTelemetryMgrConfigRoot().getTelemetryMgrNtpConfig()
                .getPublicNtp();
    }

    public String timezone() {
        return telemetryMgrConfig.getTelemetryMgrConfigRoot().getTimezone();
    }
}
