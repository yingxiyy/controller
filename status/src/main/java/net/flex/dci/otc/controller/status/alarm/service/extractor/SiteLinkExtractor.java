package net.flex.dci.otc.controller.status.alarm.service.extractor;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.alarm.service.extractor.dto.QueryParamDto;
import net.flex.dci.otn.db.jpa.service.dao.dto.AlarmConditionDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/1/11 16:33
 */
@Slf4j
@Component
public class SiteLinkExtractor extends AbstractExtractor {

    @Override
    public QueryParamDto extractDetailInfo(String id, AlarmConditionDto alarmConditionDto) {
        log.debug("extract site link alarm query param,id is {}", id);
        QueryParamDto queryParamDto = new QueryParamDto();
        Set<String> nmlKeys = getSiteLinkNmlKeys(id);
        queryParamDto.setNmlKeySet(nmlKeys);
//        queryParamDto.setSa(true);
        return queryParamDto;
    }

    @Override
    public AlarmObjectType getAlarmObjectType() {
        return AlarmObjectType.SiteLink;
    }
}
