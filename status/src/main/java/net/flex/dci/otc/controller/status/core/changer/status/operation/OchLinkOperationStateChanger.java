package net.flex.dci.otc.controller.status.core.changer.status.operation;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.changer.IStateChanger;
import net.flex.dci.otc.controller.status.dto.operation.OchLinksOperState;
import net.flex.dci.otc.controller.status.dto.operation.OchLinksOperState.OchLinkOperState;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dto.batch.OchLinkOperStatus;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/24/2023 10:26 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OchLinkOperationStateChanger implements IStateChanger<OchLinksOperState> {

    private final OchLinkDao ochLinkDao;

    @Override
    public void changeState(OchLinksOperState state) {
        log.debug("change och link operation state ");
        List<OchLinkOperState> ochLinkOperStates = state.getOchLinkOperStates();
        if (ochLinkOperStates == null || ochLinkOperStates.isEmpty()) {
            log.warn("nothing to change the och link operational state ,do nothing");
            return;
        }
//        ochLinkOperStates.forEach(this::changeOchLinkOperState);
        List<OchLinkOperStatus> operStatuses = ochLinkOperStates.stream()
                .map(ochLinkOperState -> OchLinkOperStatus.builder()
                        .operStatus(ochLinkOperState.getOperStatus())
                        .linkId(ochLinkOperState.getLinkId()).build())
                .collect(Collectors.toList());
        ochLinkDao.bulkUpdateOchLinkOperStatus(operStatuses);
    }

//    private void changeOchLinkOperState(OchLinkOperState ochLinkOperState) {
//        String ochLinkId = ochLinkOperState.getLinkId();
//        OperStatus operStatus = ochLinkOperState.getOperStatus();
//        log.debug("change the och link id:{} operational status:{}", ochLinkId, operStatus);
//        ochLinkDao.updateOchLinkOperStatus(ochLinkId, operStatus);
//    }
}
