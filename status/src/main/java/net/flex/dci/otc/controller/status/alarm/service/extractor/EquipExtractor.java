package net.flex.dci.otc.controller.status.alarm.service.extractor;

import static net.flex.dci.otc.common.constants.Constants.POUND;

import java.util.Collections;
import java.util.HashSet;
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
public class EquipExtractor implements IExtractor {

    private final static String TRANSCEIVER_HASH_FIX = "#TRANSCEIVER-";

    private final static String CHASSIS_HASH_FIX = "#CHASSIS-";

    private final static String LINECARD = "LINECARD";

    private final static String TRANSCEIVER = "TRANSCEIVER";

    @Override
    public QueryParamDto extractDetailInfo(String id, AlarmConditionDto alarmConditionDto) {
        log.debug("extract detail info for equipment id is {}", id);
        QueryParamDto queryParamDto = new QueryParamDto();
        String neId = getNeId(id);
        queryParamDto.setNeId(neId);
        if (id.contains(TRANSCEIVER_HASH_FIX)) {
            queryParamDto.setNmlKeySet(Collections.singleton(id));
        } else if (id.contains(CHASSIS_HASH_FIX)) {
            //current show only chassis alarm not sub equipment and transceiver port alarm
            queryParamDto.setNmlKeyLike(neId);
        } else if (id.contains(LINECARD)) {
            String transceiverPrefix = id.replace(LINECARD, TRANSCEIVER);
            Set<String> queryLikeSet = new HashSet<>();
            queryLikeSet.add(transceiverPrefix);
            queryLikeSet.add(id);
            queryParamDto.setNmlKeyLikeSet(queryLikeSet);

        } else {
            queryParamDto.setNmlKeyLike(id);
        }
        return queryParamDto;
    }

    @Override
    public AlarmObjectType getAlarmObjectType() {
        return AlarmObjectType.Equip;
    }

    private String getNeId(String id) {
        String[] arras = id.split(POUND);
        return arras[0] + POUND + arras[1];
    }
}
