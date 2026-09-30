package net.flex.dci.otc.controller.status.alarm.service.extractor;

import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.alarm.service.extractor.dto.QueryParamDto;
import net.flex.dci.otn.db.jpa.service.dao.dto.AlarmConditionDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/1/11 16:34
 */
@Slf4j
@Component
public class PhyLinkExtractor extends AbstractExtractor {


    @Override
    public QueryParamDto extractDetailInfo(String id, AlarmConditionDto alarmConditionDto) {
        log.debug("get phy link alarm query param id is :{} ", id);
        QueryParamDto queryParamDto = new QueryParamDto();
        Set<String> nmlKeySet = getPhyLinkNmlKeys(id);
        queryParamDto.setNmlKeySet(nmlKeySet);
//        queryParamDto.setSa(true);
        return queryParamDto;
    }

    @Override
    public AlarmObjectType getAlarmObjectType() {
        return AlarmObjectType.PhyLink;
    }


}
