package net.flex.dci.otc.controller.status.alarm.service.extractor;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.alarm.service.extractor.dto.QueryParamDto;
import net.flex.dci.otn.db.jpa.service.dao.dto.AlarmConditionDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/1/11 16:32
 */
@Slf4j
@Component
public class PhyNodeExtractor implements IExtractor {

    @Override
    public QueryParamDto extractDetailInfo(String id, AlarmConditionDto alarmConditionDto) {
        QueryParamDto queryParamDto = new QueryParamDto();
        log.debug("start to extract node id :{}", id);
        queryParamDto.setNmlKeyLike(id);
        return queryParamDto;
    }

    @Override
    public AlarmObjectType getAlarmObjectType() {
        return AlarmObjectType.PhyNode;
    }
}
