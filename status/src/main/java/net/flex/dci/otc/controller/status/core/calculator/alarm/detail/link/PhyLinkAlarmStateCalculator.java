package net.flex.dci.otc.controller.status.core.calculator.alarm.detail.link;

import static net.flex.dci.otc.controller.status.util.NmlKeyHelper.getPhyLinkRefNmlKeys;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.dto.alarm.PhyLinksAlarmState;
import net.flex.dci.otc.controller.status.dto.alarm.PhyLinksAlarmState.PhyLinkAlarmState;
import net.flex.dci.otc.controller.status.properties.AlarmConfigurationProperties;
import net.flex.dci.otc.controller.status.util.StatusUtil;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otn.db.jpa.service.dao.AlarmDaoService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/2 17:14
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class PhyLinkAlarmStateCalculator extends
        AbstractLinkAlarmStateCalculator<PhyLinksAlarmState, List<LinkStateDto>> {

    private final AlarmConfigurationProperties alarmConfigurationProperties;

    private final AlarmDaoService alarmDaoService;

    @Override
    public PhyLinksAlarmState calculate(List<LinkStateDto> phyLinks) {
        log.debug("start to update phy node ref phy link state :{}",
                phyLinks.stream().map(LinkStateDto::getId).collect(
                        Collectors.toList()));
        List<PhyLinkAlarmState> phyLinkAlarmStates = calculateRefPhyLinkAlarmState(phyLinks);
        log.debug("phy link alarm state:{}", phyLinkAlarmStates);
        return PhyLinksAlarmState.builder().phyLinksAlarmStates(phyLinkAlarmStates).build();
    }


    @Override
    public PhyLinksAlarmState calculate(String id) {
        return null;
    }


    private List<PhyLinkAlarmState> calculateRefPhyLinkAlarmState(List<LinkStateDto> phyLinks) {
        log.debug("calculate the ref phy link alarm state:{}", phyLinks);

        Map<String, List<String>> linkIdToNmlKeys = phyLinks.stream()
                .collect(Collectors.toMap(
                        LinkStateDto::getId,
                        link -> getPhyLinkRefNmlKeys(link.getId())
                ));

        List<String> allNmlKeys = linkIdToNmlKeys.values().stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());
        Map<String, List<AlarmSeverity>> nmlKeyToAlarmMap = alarmDaoService.getAlarmSeverityByNmlKeysWithMap(
                allNmlKeys);

        List<PhyLinkAlarmState> phyLinkAlarmStates = phyLinks.stream()
                .map(link -> calculateLinkAlarmStateFromCache(link, linkIdToNmlKeys,
                        nmlKeyToAlarmMap))
                .collect(Collectors.toList());
        return phyLinkAlarmStates;
    }

    private PhyLinkAlarmState calculateLinkAlarmStateFromCache(LinkStateDto link,
            Map<String, List<String>> linkIdToNmlKeys,
            Map<String, List<AlarmSeverity>> nmlKeyToAlarmsMap) {
        log.debug("get phy link state from phyLink:{}", link.getId());
        List<String> nmlKeys = linkIdToNmlKeys.get(link.getId());
        List<AlarmSeverity> alarmSeverities = nmlKeys.stream()
                .flatMap(key -> nmlKeyToAlarmsMap.getOrDefault(key, Collections.emptyList())
                        .stream())
                .collect(Collectors.toList());
        AlarmSeverity alarmSeverity = StatusUtil.calculateAlarmStateByList(alarmSeverities);

        return PhyLinkAlarmState.builder().phyLinkId(link.getId())
                .alarmSeverity(alarmSeverity).build();
    }


}
