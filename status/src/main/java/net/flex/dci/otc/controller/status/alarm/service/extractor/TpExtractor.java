package net.flex.dci.otc.controller.status.alarm.service.extractor;

import static net.flex.dci.otc.common.constants.Constants.HYPHEN;
import static net.flex.dci.otc.common.constants.Constants.POUND;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.alarm.service.extractor.dto.QueryParamDto;
import net.flex.dci.otc.controller.status.util.Constants;
import net.flex.dci.otn.db.jpa.service.dao.dto.AlarmConditionDto;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.alarm.rev180927.AlarmObjectType;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/1/11 16:34
 */
@Slf4j
@Component
public class TpExtractor extends AbstractExtractor {

    @Override
    public QueryParamDto extractDetailInfo(String id, AlarmConditionDto alarmConditionDto) {
        QueryParamDto queryParamDto = new QueryParamDto();
        Set<String> nmlKeyLikeSet = new HashSet<>();
        String[] strArr = id.split(POUND);
        String neId = strArr[0] + POUND + strArr[1];
        String tpSegment = strArr[3];
        String transceiverSegment =
                Constants.TRANSCEIVER + tpSegment.substring(tpSegment.indexOf(HYPHEN));
        nmlKeyLikeSet.add(
                neId + POUND + transceiverSegment);
        nmlKeyLikeSet.add(id);
        queryParamDto.setNmlKeyLikeSet(nmlKeyLikeSet);
        return queryParamDto;
    }

    @Override
    public AlarmObjectType getAlarmObjectType() {
        return AlarmObjectType.Tp;
    }
}
