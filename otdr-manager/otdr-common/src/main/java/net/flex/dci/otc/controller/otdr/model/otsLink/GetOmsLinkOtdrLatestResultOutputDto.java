package net.flex.dci.otc.controller.otdr.model.otsLink;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 *
 * 2025/8/10
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class GetOmsLinkOtdrLatestResultOutputDto implements Serializable {

    private OmsOtsLinkOtdrLatestRecord output;
}
