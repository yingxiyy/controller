package net.flex.dci.otn.controller.nms.nms.dto.omslink;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;

/**
 * 2025/8/9
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class RouteContractInfoDto implements Serializable {

    private String provider;


    private DateAndTime openDate;

    private DirectionMetrics aToz;

    private DirectionMetrics zToa;
}
