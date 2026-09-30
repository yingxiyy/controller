package net.flex.dci.otc.controller.status.core.changer.status.align;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.changer.IStateChanger;
import net.flex.dci.otc.controller.status.dto.align.TunnelsAlignState;
import net.flex.dci.otc.controller.status.dto.align.TunnelsAlignState.TunnelAlignState;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/22/2023 1:51 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TunnelAlignStateChanger implements IStateChanger<TunnelsAlignState> {

    private final TunnelDao tunnelDao;

    @Override
    public void changeState(TunnelsAlignState state) {
        log.debug("start to change the tunnel align state");
        List<TunnelAlignState> tunnelAlignStates = state.getTunnelAlignStates();
        if (tunnelAlignStates == null || tunnelAlignStates.isEmpty()) {
            log.warn("change the tunnel align state is meaningless ,do nothing");
            return;
        }
        List<net.flex.dci.otc.mongo.dto.batch.TunnelAlignState> updateTunnelAlignState = tunnelAlignStates.stream()
                .map(tunnelAlignState -> net.flex.dci.otc.mongo.dto.batch.TunnelAlignState.builder()
                        .tunnelId(tunnelAlignState.getTunnelId())
                        .alignmentStatusType(tunnelAlignState.getAlignmentStatusType())
                        .build())
                .collect(Collectors.toList());
        tunnelDao.bulkUpdateTunnelAlignState(updateTunnelAlignState);
    }

//    private void changeAlignState(TunnelAlignState tunnelAlignState) {
//        String tunnelId = tunnelAlignState.getTunnelId();
//        AlignmentStatusType alignmentStatusType = tunnelAlignState.getAlignmentStatusType();
//        log.debug("change the tunnel align state");
//        tunnelDao.updateTunnelAlignState(tunnelId, alignmentStatusType);
//    }
}
