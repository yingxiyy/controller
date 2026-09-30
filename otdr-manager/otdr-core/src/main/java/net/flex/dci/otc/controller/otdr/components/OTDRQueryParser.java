package net.flex.dci.otc.controller.otdr.components;

import net.flex.dci.otc.controller.otdr.dto.OTDRLinkQueryDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.GetOtdrResultsInput;

/**
 * @version 1.0
 * @date 2023/1/9 11:31
 */
public interface OTDRQueryParser {

    OTDRLinkQueryDto parsePagedDto(GetOtdrResultsInput getOtdrResultsInput);

}
