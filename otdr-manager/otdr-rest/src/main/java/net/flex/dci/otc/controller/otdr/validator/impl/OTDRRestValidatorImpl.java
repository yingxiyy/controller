package net.flex.dci.otc.controller.otdr.validator.impl;

import static java.util.stream.Collectors.joining;
import static net.flex.dci.otc.common.constants.Constants.DOT;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.controller.otdr.validator.OTDRRestValidator;
import net.flex.dci.otc.mongo.dao.PhyLinkDao;
import net.flex.dci.otn.db.jpa.entity.OtdrResultRecord;
import net.flex.dci.otn.db.jpa.service.dao.OtdrDaoService;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.SetOtdrBaseInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.otdr.rev190430.ShowOtdrGraphicsInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 8/18/2023 4:42 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class OTDRRestValidatorImpl implements OTDRRestValidator {

    private final OtdrDaoService otdrDaoService;

    private final PhyLinkDao phyLinkDao;


    @Override
    public void validateShowOtdrResultGraphicsInput(ShowOtdrGraphicsInput input) {
        log.debug("validate the graphics otdr result ");
        List<String> taskIds = input.getTaskIds();
        if (taskIds == null || taskIds.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the task id should be null");
        }
        List<Long> notExistTaskId = taskIds.stream().map(Long::parseLong)
                .filter(taskId -> !otdrDaoService.existsById(taskId))
                .collect(Collectors.toList());
        if (!notExistTaskId.isEmpty()) {
            String invalidTaskId = notExistTaskId.stream().map(Object::toString)
                    .collect(joining(DOT));
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("the task id is invalid,the invalid is %s", invalidTaskId));
        }
    }

    @Override
    public void validateSetOtdrBaseBenchmarkInput(SetOtdrBaseInput setOtdrBaseInput) {
        log.debug("validate the set otdr base benchmark input");
        String linkId = setOtdrBaseInput.getLinkId();
        Long taskId = setOtdrBaseInput.getTaskId().longValue();
        Link refPhyLink = phyLinkDao.getPhyLinkById(
                linkId);
        if (null == refPhyLink) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("can not find the phy link ,link id is: %s", linkId));
        }
        boolean existed = otdrDaoService.existsById(taskId);
        if (!existed) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("invalid task id for the phy link,link id id : %s", linkId));
        }
        OtdrResultRecord otdrTaskResult = otdrDaoService.findById(
                taskId);
        List<String> phyLinkRefTpIds = getPhysicalLinkRefTpIds(refPhyLink);
        String monitorTpId = otdrTaskResult.getTpId();
        if (!phyLinkRefTpIds.contains(monitorTpId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("invalid task id for the phy link,link id id : %s", linkId));
        }
    }

    private List<String> getPhysicalLinkRefTpIds(Link refPhyLink) {
        log.debug("get physical link ref tp Ids,phy link id is:{}",
                refPhyLink.getLinkId().getValue());
        List<String> physicalLinkRefTp = new ArrayList<>();
        String sourceTpId = refPhyLink.getSource().getSourceTp().getValue();
        String destinationTpId = refPhyLink.getDestination().getDestTp().getValue();
        physicalLinkRefTp.add(sourceTpId);
        physicalLinkRefTp.add(destinationTpId);
        return physicalLinkRefTp;
    }
}
