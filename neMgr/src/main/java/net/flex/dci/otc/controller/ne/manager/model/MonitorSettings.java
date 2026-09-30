package net.flex.dci.otc.controller.ne.manager.model;

import java.io.Serializable;
import java.time.Duration;
import lombok.Builder;
import lombok.Data;

/**
 * 2025/6/12
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class MonitorSettings implements Serializable {

    private Duration monitorInterval;

    public long staggerSeconds;

    public int sliceCount;

    public int maxSliceCount;
}
