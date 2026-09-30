package net.flex.dci.otc.controller.status.alarm.service.extractor;

import net.flex.dci.otc.controller.status.alarm.service.extractor.dto.QueryParamDto;
import net.flex.dci.otn.db.jpa.service.dao.dto.AlarmConditionDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmObjectType;

/**
 * @version 1.0
 * @date 2022/1/11 16:38
 */

public interface IExtractor {

    default QueryParamDto extractDetailInfo(String id, AlarmConditionDto alarmConditionDto) {
        return null;
    }

    AlarmObjectType getAlarmObjectType();
}
