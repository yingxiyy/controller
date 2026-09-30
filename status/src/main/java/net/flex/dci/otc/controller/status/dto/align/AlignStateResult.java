package net.flex.dci.otc.controller.status.dto.align;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 8/22/2023 1:35 PM
 */
@Builder
@Data
public class AlignStateResult implements Serializable {

    private PhyLinksAlignState phyLinksAlignState;

    private SiteLinksAlignState siteLinksAlignState;

    private OchLinksAlignState ochLinksAlignStates;

    private TunnelsAlignState tunnelsAlignState;
}
