package net.flex.dci.otc.controller.otdr.domain;

import java.io.Serializable;
import java.util.Date;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.controller.otdr.enums.OtdrMonitorDirection;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.OtdrScanResultType;

/**
 * @version 1.0
 * @date 2022/9/2 15:32
 */
@Data
@Builder
public class OtdrScanDetail implements Serializable {

    private String nodeId;

    private String nodeName;

    private String monitorPortId;

    private String monitorName;

    private Date startTime;

    private OtdrMonitorDirection direction;

    private OtdrScanResultType state;

    private String errorMessage;

}
