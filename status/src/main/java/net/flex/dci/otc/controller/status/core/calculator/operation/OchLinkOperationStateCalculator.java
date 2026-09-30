package net.flex.dci.otc.controller.status.core.calculator.operation;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.core.cache.ConnectionCacheManager;
import net.flex.dci.otc.controller.status.core.cache.NodeCacheManager;
import net.flex.dci.otc.controller.status.dto.ProtectedLinkDto;
import net.flex.dci.otc.controller.status.dto.operation.OchLinksOperState;
import net.flex.dci.otc.controller.status.dto.operation.OchLinksOperState.OchLinkOperState;
import net.flex.dci.otc.controller.status.util.LinkHelper;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/8 16:41
 */
@Component
@Slf4j
public class OchLinkOperationStateCalculator extends
        AbstractLinkOperationStateCalculator<OchLinksOperState, LinkStateDto> {


    public OchLinkOperationStateCalculator(LinkHelper linkHelper,
            ConnectionCacheManager connectionCacheManager,
            NodeCacheManager nodeCacheManager) {
        super(linkHelper, connectionCacheManager, nodeCacheManager);
    }

    @Override
    public OchLinksOperState calculate(List<LinkStateDto> links) {
        log.debug("start to calculate the och link oper state");
        List<OchLinkOperState> ochLinkOperStates = links.stream().map(this::calculateOchOperState)
                .collect(
                        Collectors.toList());
        return OchLinksOperState.builder().ochLinkOperStates(ochLinkOperStates).build();
    }

    @Override
    public OchLinksOperState calculate(List<LinkStateDto> links, OperStatus operStatus) {
        return null;
    }

    /**
     * support ochp operation state calculator
     *
     * @param ochLink
     * @return
     */
    private OchLinkOperState calculateOchOperState(LinkStateDto ochLink) {
        String linkId = ochLink.getId();
        log.debug("calculate the phy link oper state ,link id :{}", linkId);
//        Boolean isProtected = linkHelper.isOchLinkInProtectedMode(
//                ochLink);
        OperStatus operStatus = OperStatus.Unknown;
//        if (isProtected) {
//            operStatus = calculateProtectedOchOperState(ochLink);
//        } else {
        operStatus = calculateUnprotectedOchOperState(ochLink);
//        }
        return OchLinkOperState.builder().linkId(linkId).operStatus(operStatus).build();
    }

    /**
     * calculate protected och operStatus like OCHP
     *
     * @param ochLink
     * @return
     */
    private OperStatus calculateProtectedOchOperState(Link ochLink) {
        log.debug("calculate protected och operState ,och link id is:{}", ochLink.getLinkId());
        ProtectedLinkDto ochProtectedLinkDto = linkHelper.getOCHPOchWorkModeDetailInfo(ochLink);
        OperStatus operStatus = calculateProtectedLinkOperState(ochProtectedLinkDto);
        return operStatus;
    }


    /**
     * calculate unprotected och operStatus
     *
     * @param ochLink
     * @return
     */
    private OperStatus calculateUnprotectedOchOperState(LinkStateDto ochLink) {
        log.debug("calculate unprotected ochOperState och link is:{}", ochLink.getId());
        List<String> refLinkIds = ochLink.getSupportingLink();
        return calculateRefLinkOperStatus(refLinkIds);
    }


}
