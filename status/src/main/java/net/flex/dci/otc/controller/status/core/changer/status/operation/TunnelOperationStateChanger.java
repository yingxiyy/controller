package net.flex.dci.otc.controller.status.core.changer.status.operation;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.changer.IStateChanger;
import net.flex.dci.otc.controller.status.dto.operation.TunnelsOperState;
import net.flex.dci.otc.controller.status.dto.operation.TunnelsOperState.TunnelOperState;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.dto.batch.TunnelOperStatus;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/24/2023 11:23 AM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TunnelOperationStateChanger implements IStateChanger<TunnelsOperState> {

    private final TunnelDao tunnelDao;

    @Override
    public void changeState(TunnelsOperState state) {
        log.debug("chang the tunnel operation state");
        List<TunnelOperState> tunnelOperStates = state.getTunnelOperStates();
        if (tunnelOperStates == null || tunnelOperStates.isEmpty()) {
            log.warn("nothing to change the tunnel operation state,do noting");
            return;
        }
//        tunnelOperStates.forEach(this::updateTunnelOperState);
        List<TunnelOperStatus> tunnelOperStatuses = tunnelOperStates.stream()
                .map(tunnelOperState -> TunnelOperStatus.builder()
                        .tunnelId(tunnelOperState.getTunnelId())
                        .operStatus(tunnelOperState.getOperStatus())
                        .build())
                .collect(Collectors.toList());
        tunnelDao.bulkUpdateTunnelOperStatus(tunnelOperStatuses);
    }

//    private void updateTunnelOperState(TunnelOperState tunnelOperState) {
//        String tunnelId = tunnelOperState.getTunnelId();
//        OperStatus operStatus = tunnelOperState.getOperStatus();
//        log.debug("update tunnel oper state,the tunnel id is:{},oper status is :{}", tunnelId,
//                operStatus);
//        tunnelDao.updateTunnelOperStatus(tunnelId, operStatus);
//    }
}
