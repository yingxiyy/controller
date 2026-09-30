package net.flex.dci.otc.controller.status.core.calculator.alarm;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otn.db.jpa.entity.AlarmRecord;
import net.flex.dci.otn.db.jpa.service.dao.AlarmDaoService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 2025/12/6
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public abstract class AbstractAlarmStateCalculator<T, V> implements IAlarmStateCalculator<T, V> {

    @Autowired
    protected AlarmDaoService alarmDaoService;

    protected AlarmSeverity calculateCurrentAlarmSeverity(List<String> neIds) {
        log.debug("calculate current alarm severity,neIds :{}", neIds);
        List<AlarmRecord> alarmRecords = alarmDaoService.getCurrentAlarmsByNeIds(neIds);
        List<AlarmSeverity> alarmSeverities = alarmRecords.stream()
                .map(AlarmRecord::getSeverity).collect(
                        Collectors.toList());
        log.debug("[ALARM-CALC] neCount={}, dbAlarmCount={}, severities={}",
                neIds.size(), alarmRecords.size(), alarmSeverities);
//        List<Node> inRackNodes = phyNodeDao.listOperPhyNodeByIds(inRackNeIds);
//        NmlKeyHelper.getDetailInfoFromNmlKey();

        AlarmSeverity currentAlarmState = StatusUtil.calculateAlarmStateByList(
                alarmSeverities);
        return currentAlarmState;
    }

}
