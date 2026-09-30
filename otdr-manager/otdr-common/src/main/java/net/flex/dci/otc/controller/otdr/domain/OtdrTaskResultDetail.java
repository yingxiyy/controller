package net.flex.dci.otc.controller.otdr.domain;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.OtdrScanResultType;

/**
 * @version 1.0
 * @date 2022/8/30 14:48
 */
@Data
@Builder
public class OtdrTaskResultDetail implements Serializable {

    @JSONField(name = "node-id")
    private String nodeId;

    @JSONField(name = "scan-state")
    private OtdrScanResultType scanResultType;

    @JSONField(name = "monitor-port")
    private String monitorTpName;


}
