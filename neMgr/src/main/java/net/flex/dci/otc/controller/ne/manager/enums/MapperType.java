package net.flex.dci.otc.controller.ne.manager.enums;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;

/**
 * @version 1.0
 * @date 6/9/2025 4:16 PM
 */
@Getter
public enum MapperType {
    ADAPTER(Arrays.asList("adapter", "deviceMapper")),
    TELEMETRY_COLLECTOR(Arrays.asList("collector",
            "ts")),
    UNKNOWN(Arrays.asList("unknown"));

    private final List<String> moduleNames;

    MapperType(List<String> moduleNames) {
        this.moduleNames = moduleNames;
    }
}
