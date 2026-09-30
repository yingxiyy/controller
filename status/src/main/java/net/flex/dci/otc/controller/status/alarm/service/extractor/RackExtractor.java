package net.flex.dci.otc.controller.status.alarm.service.extractor;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.alarm.service.extractor.dto.QueryParamDto;
import net.flex.dci.otn.db.jpa.service.dao.dto.AlarmConditionDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmObjectType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.rack.attributes.SupportingNe;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 2022/1/11 16:41
 */
@Slf4j
@Component
public class RackExtractor extends AbstractExtractor {

    @Override
    public QueryParamDto extractDetailInfo(String id, AlarmConditionDto alarmConditionDto) {
        log.debug("extract rack alarm query parameter ,id is {}", id);
        QueryParamDto queryParamDto = new QueryParamDto();
        String siteId = alarmConditionDto.getSiteId();
        if (!StringUtils.hasText(siteId)) {
            return queryParamDto;
        }
        Set<String> neIds = getRackNode(id, siteId);
        queryParamDto.setNeIds(neIds);
        return queryParamDto;
    }

    @Override
    public AlarmObjectType getAlarmObjectType() {
        return AlarmObjectType.Rack;
    }

    private Set<String> getRackNode(String rackId, String siteId) {
        Set<String> neIds = new HashSet<>();
        SupportingRack rack = rackDao.getRackBySiteIdRackRef(siteId, rackId);
        if (rack != null && rack.getSupportingNe() != null
                && rack.getSupportingNe().size() > 0) {
            for (SupportingNe sn : rack.getSupportingNe()) {
                neIds.add(sn.getNodeRef().getValue());
            }

        }
        return neIds;
    }
}
