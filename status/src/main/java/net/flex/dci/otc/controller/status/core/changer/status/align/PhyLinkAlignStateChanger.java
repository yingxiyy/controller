package net.flex.dci.otc.controller.status.core.changer.status.align;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.changer.IStateChanger;
import net.flex.dci.otc.controller.status.dto.align.PhyLinksAlignState;
import net.flex.dci.otc.controller.status.dto.align.PhyLinksAlignState.PhyLinkAlignState;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/22/2023 1:44 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PhyLinkAlignStateChanger implements IStateChanger<PhyLinksAlignState> {

    private final PhyLinkDao phyLinkDao;

    @Override
    public void changeState(PhyLinksAlignState state) {
        log.debug("change the phy link align state");
        List<PhyLinkAlignState> phyLinkAlignStates = state.getPhyLinkAlignStates();
        if (phyLinkAlignStates == null || phyLinkAlignStates.isEmpty()) {
            log.warn("meaningless phy link align state change,do nothing");
            return;
        }
//        phyLinkAlignStates.forEach(this::changeAlignState);
        List<net.flex.dci.otc.mongo.dto.batch.PhyLinkAlignState> alignStates = phyLinkAlignStates.stream()
                .map(phyLinkAlignState -> net.flex.dci.otc.mongo.dto.batch.PhyLinkAlignState.builder()
                        .linkId(phyLinkAlignState.getLinkId())
                        .alignmentStatus(phyLinkAlignState.getAlignmentStatusType())
                        .build())
                .collect(
                        Collectors.toList());
        phyLinkDao.bulkUpdateLinkAlignState(alignStates);
    }

//    private void changeAlignState(PhyLinkAlignState phyLinkAlignState) {
//        String phyLinkId = phyLinkAlignState.getLinkId();
//        AlignmentStatusType alignmentStatusType = phyLinkAlignState.getAlignmentStatusType();
//        log.debug("change the phy link id:{},alignment status type is:{}", phyLinkId,
//                alignmentStatusType);
//        phyLinkDao.updateLinkAlignState(phyLinkId, alignmentStatusType);
//    }
}
