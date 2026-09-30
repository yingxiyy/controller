package net.flex.dci.otn.controller.resource.statistic.converter.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otn.controller.resource.statistic.core.enums.ResourceQueryType;
import net.flex.dci.otn.controller.resource.statistic.dto.query.CircuitQueryDTO;
import net.flex.dci.otn.controller.resource.statistic.dto.query.UnifiedQueryParam;
import net.flex.dci.otn.controller.resource.statistic.dto.task.display.CircuitQueryDisplayDto;
import net.flex.dci.otn.controller.resource.statistic.dto.task.display.QueryTaskDisplayDto;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * 2026/9/16
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class CircuitQueryConvertor extends AbstractResourceQueryConvertor {


    @Override
    public QueryTaskDisplayDto resolveDisplay(UnifiedQueryParam queryParam) {
        log.debug("resolve the circuit query display :{}",
                queryParam.getQueryCondition().getTunnelQuery());
        CircuitQueryDTO circuitQueryDTO = queryParam.getQueryCondition().getTunnelQuery();
        boolean isIncludeLLdp = circuitQueryDTO.isIncludeLLdp();
        List<String> subnetNames = getSubnetNames(queryParam.getSubnet());
        List<String> siteLinkNames = getSiteLinkNames(circuitQueryDTO.getSiteLink());
        return CircuitQueryDisplayDto.builder().subnetNames(subnetNames).includeLLdp(isIncludeLLdp)
                .siteLinkNames(siteLinkNames).build();
    }

    private List<String> getSiteLinkNames(List<String> siteLink) {
        if (CollectionUtils.isEmpty(siteLink)) {
            return new ArrayList<>();
        }
        List<LinkStateDto> siteLinkState = siteLinkDao.getSiteLinkStateByIds(siteLink);
        List<String> siteLinkNames = siteLinkState.stream()
                .map(LinkStateDto::getFriendlyName).collect(
                        Collectors.toList());
        return siteLinkNames;
    }


    @Override
    public ResourceQueryType resourceQueryType() {
        return ResourceQueryType.Tunnel;
    }
}
