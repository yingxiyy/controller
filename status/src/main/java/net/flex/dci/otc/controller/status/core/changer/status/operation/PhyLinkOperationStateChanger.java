package net.flex.dci.otc.controller.status.core.changer.status.operation;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.changer.IStateChanger;
import net.flex.dci.otc.controller.status.dto.operation.PhyLinksOperState;
import net.flex.dci.otc.controller.status.dto.operation.PhyLinksOperState.PhyLinkOperState;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otc.mongo.dto.batch.PhyLinkOperStatus;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/24/2023 11:22 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PhyLinkOperationStateChanger implements IStateChanger<PhyLinksOperState> {

    private final PhyLinkDao phyLinkDao;

    @Override
    public void changeState(PhyLinksOperState state) {
        log.info("change the phy link operational state");
        List<PhyLinkOperState> phyLinkOperStates = state.getPhyLinkOperStates();
        if (phyLinkOperStates == null || phyLinkOperStates.isEmpty()) {
            log.warn("nothing to change the phy link operational state,do nothing");
            return;
        }
//        phyLinkOperStates.forEach(this::changePhyLinkOperState);
        List<PhyLinkOperStatus> updateOperStatus = phyLinkOperStates.stream()
                .map(operstate -> PhyLinkOperStatus.builder().linkId(operstate.getPhyLinkId())
                        .operStatus(operstate.getOperStatus()).build()).collect(
                        Collectors.toList());
        phyLinkDao.bulkUpdateLinkOperState(updateOperStatus);
    }

//    private void changePhyLinkOperState(PhyLinkOperState phyLinkOperState) {
//        String refPhyLinkId = phyLinkOperState.getPhyLinkId();
//        OperStatus operStatus = phyLinkOperState.getOperStatus();
//        log.debug("change the phy link id:{} operational state :{}", refPhyLinkId, operStatus);
//        phyLinkDao.updateLinkOperationState(refPhyLinkId, operStatus);
//    }
}
