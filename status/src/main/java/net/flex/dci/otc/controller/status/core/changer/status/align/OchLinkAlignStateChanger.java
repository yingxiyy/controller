package net.flex.dci.otc.controller.status.core.changer.status.align;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.changer.IStateChanger;
import net.flex.dci.otc.controller.status.dto.align.OchLinksAlignState;
import net.flex.dci.otc.controller.status.dto.align.OchLinksAlignState.OchLinkAlignState;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/22/2023 1:51 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OchLinkAlignStateChanger implements IStateChanger<OchLinksAlignState> {

    private final OchLinkDao ochLinkDao;

    @Override
    public void changeState(OchLinksAlignState state) {
        log.debug("change the och link align state");
        List<OchLinkAlignState> ochLinkAlignStates = state.getAlignStates();
        if (ochLinkAlignStates == null || ochLinkAlignStates.isEmpty()) {
            log.warn("meaningless align state change for och link,do nothing");
            return;
        }
        List<net.flex.dci.otc.mongo.dto.batch.OchLinkAlignState> alignStates = ochLinkAlignStates.stream()
                .map(ochLinkAlignState -> net.flex.dci.otc.mongo.dto.batch.OchLinkAlignState.builder()
                        .ochLinkId(ochLinkAlignState.getOchLinkId())
                        .alignmentStatusType(ochLinkAlignState.getAlignmentStatusType())
                        .build()).collect(
                        Collectors.toList());
        ochLinkDao.bulkUpdateOchLinkAlignState(alignStates);

    }

//    private void changeOchLinkAlignState(OchLinkAlignState ochLinkAlignState) {
//        String ochLinkId = ochLinkAlignState.getOchLinkId();
//        AlignmentStatusType alignmentStatusType = ochLinkAlignState.getAlignmentStatusType();
//        log.debug("change the och link id:{}  alignment status type is:{}", ochLinkId,
//                alignmentStatusType);
//        ochLinkDao.updateOchLinkAlignState(ochLinkId, alignmentStatusType);
//    }
}
