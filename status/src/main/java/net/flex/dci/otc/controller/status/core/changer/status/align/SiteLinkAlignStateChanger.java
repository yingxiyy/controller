package net.flex.dci.otc.controller.status.core.changer.status.align;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.changer.IStateChanger;
import net.flex.dci.otc.controller.status.dto.align.SiteLinksAlignState;
import net.flex.dci.otc.controller.status.dto.align.SiteLinksAlignState.SiteLinkAlignState;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/22/2023 1:49 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SiteLinkAlignStateChanger implements IStateChanger<SiteLinksAlignState> {

    private final SiteLinkDao siteLinkDao;

    @Override
    public void changeState(SiteLinksAlignState state) {
        log.debug("change the site link align state");
        List<SiteLinkAlignState> siteLinkAlignStates = state.getSiteLinkAlignStates();
        if (null == siteLinkAlignStates || siteLinkAlignStates.isEmpty()) {
            log.warn("the change for the site link align is meaningless ,do nothing");
            return;
        }
        List<net.flex.dci.otc.mongo.dto.batch.SiteLinkAlignState> updateSiteLinkAlignState = siteLinkAlignStates.stream()
                .map(alignState -> net.flex.dci.otc.mongo.dto.batch.SiteLinkAlignState.builder()
                        .linkId(alignState.getSiteLinkId())
                        .alignmentStatusType(alignState.getAlignState())
                        .build()).collect(
                        Collectors.toList());
        siteLinkDao.bulkUpdateSiteLinkAlignStatus(updateSiteLinkAlignState);
    }

//    private void changeAlignState(SiteLinkAlignState siteLinkAlignState) {
//        String siteLinkId = siteLinkAlignState.getSiteLinkId();
//        AlignmentStatusType alignmentStatusType = siteLinkAlignState.getAlignState();
//        log.debug("change the site link align state,the siteLinkId is :{}", siteLinkId);
//        siteLinkDao.updateSiteLinkAlignState(siteLinkId, alignmentStatusType);
//    }
}
