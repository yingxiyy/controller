package net.flex.dci.otc.controller.status.core.changer.status;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.changer.IStateChanger;
import net.flex.dci.otc.controller.status.core.changer.status.operation.OchLinkOperationStateChanger;
import net.flex.dci.otc.controller.status.core.changer.status.operation.PhyLinkOperationStateChanger;
import net.flex.dci.otc.controller.status.core.changer.status.operation.SiteLinkOperationStateChanger;
import net.flex.dci.otc.controller.status.core.changer.status.operation.TunnelOperationStateChanger;
import net.flex.dci.otc.controller.status.dto.operation.OchLinksOperState;
import net.flex.dci.otc.controller.status.dto.operation.OperationStateResult;
import net.flex.dci.otc.controller.status.dto.operation.PhyLinksOperState;
import net.flex.dci.otc.controller.status.dto.operation.SiteLinksOperState;
import net.flex.dci.otc.controller.status.dto.operation.TunnelsOperState;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/22/2023 1:34 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OperStateChanger implements IStateChanger<OperationStateResult> {

    private final OchLinkOperationStateChanger ochLinkOperationStateChanger;

    private final PhyLinkOperationStateChanger phyLinkOperationStateChanger;

    private final SiteLinkOperationStateChanger siteLinkOperationStateChanger;

    private final TunnelOperationStateChanger tunnelOperationStateChanger;

    @Override
    public void changeState(OperationStateResult state) {
        log.debug("change the operation state for the link");
        PhyLinksOperState phyLinksOperState = state.getPhyLinksOperState();
        if (phyLinksOperState != null) {
            phyLinkOperationStateChanger.changeState(phyLinksOperState);
        }

        SiteLinksOperState siteLinksOperState = state.getSiteLinksOperState();
        if (siteLinksOperState != null) {
            siteLinkOperationStateChanger.changeState(siteLinksOperState);
        }

        OchLinksOperState ochLinksOperState = state.getOchLinksOperState();
        if (ochLinksOperState != null) {
            ochLinkOperationStateChanger.changeState(ochLinksOperState);
        }

        TunnelsOperState tunnelsOperState = state.getTunnelsOperState();
        if (tunnelsOperState != null) {
            tunnelOperationStateChanger.changeState(tunnelsOperState);
        }
    }
}
