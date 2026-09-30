package net.flex.dci.otn.controller.db.monitor.core.dto.telemetry;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.List;
import lombok.Data;
import net.flex.dci.otn.controller.db.monitor.core.dto.RegisteredNe;

/**
 * @version 1.0
 * @date 2022/11/16 15:00
 */
@Data
public class TelemetryServerData implements Serializable {

    @JSONField(name = "telemetry-server")
    private List<RegisteredNe> ne;
}
