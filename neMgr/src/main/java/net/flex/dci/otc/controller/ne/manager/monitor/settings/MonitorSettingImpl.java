package net.flex.dci.otc.controller.ne.manager.monitor.settings;

import static net.flex.dci.otc.controller.ne.manager.utils.NeManagerConstants.DEFAULT_MINUTES_INTERVAL;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.ne.manager.model.MonitorSettings;
import net.flex.dci.otc.controller.ne.manager.properties.NeMonitorProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 6/13/2025 11:04 AM
 */
@Component
@Slf4j
public class MonitorSettingImpl implements MonitorSetting {

    private final String unit;

    private final Long interval;

    private final int sliceCount;

    private final int maxSliceCount;

    private final int staggerSeconds;

    public MonitorSettingImpl(NeMonitorProperties neMonitorProperties) {
        this.unit = neMonitorProperties.getUnit();
        this.interval = neMonitorProperties.getInterval();
        this.sliceCount = neMonitorProperties.getSliceCount();
        this.maxSliceCount = neMonitorProperties.getMaxSliceCount();
        this.staggerSeconds = neMonitorProperties.getStaggerSeconds();
    }

    @Override
    public MonitorSettings getMonitorSettings() {
        Duration accessTokenDuration = setIntervalTime(unit, interval, DEFAULT_MINUTES_INTERVAL);
        return MonitorSettings.builder().monitorInterval(accessTokenDuration).sliceCount(sliceCount)
                .maxSliceCount(maxSliceCount)
                .staggerSeconds(staggerSeconds).build();
    }

    private Duration setIntervalTime(String unit, Long interval, long defaultIntervalMinutes) {
        Duration duration = Duration.ofMinutes(defaultIntervalMinutes);
        if (StringUtils.hasText(unit)) {
            switch (unit.toUpperCase()) {
                case "M":
                case "MINUTE":
                case "MINUTES":
                    duration = Duration.ofMinutes(interval);
                    break;
                case "H":
                case "HOUR":
                case "HOURS":
                    duration = Duration.ofHours(interval);
                    break;
                case "D":
                case "DAY":
                case "DAYS":
                    duration = Duration.ofDays(interval);
                    break;
                case "W":
                case "WEEK":
                case "WEEKS":
                    duration = Duration.of(interval, ChronoUnit.WEEKS);
                    break;
                case "S":
                case "SECOND":
                case "SECONDS":
                    duration = Duration.ofSeconds(interval);
                    break;
            }
        }
        return duration;
    }
}
