package net.flex.dci.otc.controller.status.core.changer.status;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.changer.IStateChanger;
import net.flex.dci.otc.controller.status.core.changer.status.align.OchLinkAlignStateChanger;
import net.flex.dci.otc.controller.status.core.changer.status.align.PhyLinkAlignStateChanger;
import net.flex.dci.otc.controller.status.core.changer.status.align.SiteLinkAlignStateChanger;
import net.flex.dci.otc.controller.status.core.changer.status.align.TunnelAlignStateChanger;
import net.flex.dci.otc.controller.status.dto.align.AlignStateResult;
import net.flex.dci.otc.controller.status.dto.align.OchLinksAlignState;
import net.flex.dci.otc.controller.status.dto.align.PhyLinksAlignState;
import net.flex.dci.otc.controller.status.dto.align.SiteLinksAlignState;
import net.flex.dci.otc.controller.status.dto.align.TunnelsAlignState;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/22/2023 1:34 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AlignStateChanger implements IStateChanger<AlignStateResult> {

    private final PhyLinkAlignStateChanger phyLinkAlignStateChanger;

    private final OchLinkAlignStateChanger ochLinkAlignStateChanger;

    private final SiteLinkAlignStateChanger siteLinkAlignStateChanger;

    private final TunnelAlignStateChanger tunnelAlignStateChanger;


    @Override
    public void changeState(AlignStateResult state) {
        log.debug("start to change the align state ");
        PhyLinksAlignState phyLinksAlignState = state.getPhyLinksAlignState();
        if (null != phyLinksAlignState) {
            phyLinkAlignStateChanger.changeState(phyLinksAlignState);
        }
        SiteLinksAlignState siteLinksAlignState = state.getSiteLinksAlignState();
        if (null != siteLinksAlignState) {
            siteLinkAlignStateChanger.changeState(siteLinksAlignState);
        }
        OchLinksAlignState ochLinksAlignState = state.getOchLinksAlignStates();
        if (null != ochLinksAlignState) {
            ochLinkAlignStateChanger.changeState(ochLinksAlignState);
        }

        TunnelsAlignState tunnelsAlignState = state.getTunnelsAlignState();

        if (null != tunnelsAlignState) {
            tunnelAlignStateChanger.changeState(tunnelsAlignState);
        }

    }
}
