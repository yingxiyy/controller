package net.flex.dci.otc.controller.otdr.model.terminationPoint;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 12/4/2023 4:46 PM
 */
@Data
@Builder
public class RealOTDRScanInfo implements Serializable {


    private String node;

    private String OTDRMonitorPort;

}
