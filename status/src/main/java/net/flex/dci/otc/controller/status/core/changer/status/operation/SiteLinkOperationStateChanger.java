package net.flex.dci.otc.controller.status.core.changer.status.operation;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.changer.IStateChanger;
import net.flex.dci.otc.controller.status.dto.operation.SiteLinksOperState;
import net.flex.dci.otc.controller.status.dto.operation.SiteLinksOperState.SiteLinkOperState;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dto.batch.SiteLinkOperStatus;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/24/2023 11:22 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SiteLinkOperationStateChanger implements IStateChanger<SiteLinksOperState> {

    private final SiteLinkDao siteLinkDao;

    @Override
    public void changeState(SiteLinksOperState state) {
        log.debug("change the operational state for the site link");
        List<SiteLinkOperState> siteLinkOperStates = state.getSiteLinkOperStates();
        if (siteLinkOperStates == null || siteLinkOperStates.isEmpty()) {
            log.warn("nothing to change the site link operational state,do nothing ");
            return;
        }
//        siteLinkOperStates.forEach(this::changeSiteLinkOperState);
        List<SiteLinkOperStatus> siteLinkOperStatuses = siteLinkOperStates.stream()
                .map(siteLinkOperState -> SiteLinkOperStatus.builder()
                        .linkId(siteLinkOperState.getSiteLinkId())
                        .operStatus(siteLinkOperState.getOperStatus())
                        .build())
                .collect(Collectors.toList());
        siteLinkDao.bulkUpdateSiteLinkOperStatus(siteLinkOperStatuses);
    }

//    private void changeSiteLinkOperState(SiteLinkOperState siteLinkOperState) {
//        String siteLinkId = siteLinkOperState.getSiteLinkId();
//        OperStatus operStatus = siteLinkOperState.getOperStatus();
//        log.debug("change the site link :{} operstatus:{}", siteLinkId, operStatus);
//        siteLinkDao.updateSiteLinkOperStatus(siteLinkId, operStatus);
//    }
}
