package net.flex.dci.otn.controller.nms.nms.dto.tunnel;

import java.io.Serializable;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.mongo.base.page.PageResult;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;

/**
 * 2026/4/20
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class TunnelBetweenSitePageResult implements Serializable {

    private PageResult<Tunnel> tunnelPageResult;

    private BigDecimal totalBandwidth;

    private Long occupiedChannelCount;
}
