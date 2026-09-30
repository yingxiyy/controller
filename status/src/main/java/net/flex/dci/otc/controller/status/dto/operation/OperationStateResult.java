package net.flex.dci.otc.controller.status.dto.operation;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 8/22/2023 1:39 PM
 */
@Data
@Builder
public class OperationStateResult implements Serializable {

    private PhyLinksOperState phyLinksOperState;

    private SiteLinksOperState siteLinksOperState;

    private OchLinksOperState ochLinksOperState;

    private TunnelsOperState tunnelsOperState;
}
