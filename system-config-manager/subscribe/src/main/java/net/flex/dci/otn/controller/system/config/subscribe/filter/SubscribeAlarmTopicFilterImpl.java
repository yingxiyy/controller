/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.subscribe.filter;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.system.config.common.model.ToopAlarm;
import net.flex.dci.otn.controller.system.config.subscribe.mapper.AlarmTextPatternMapper;
import net.flex.dci.otn.controller.system.config.subscribe.utils.PatternUtils;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2021/12/10 15:37
 */
@Component
@Slf4j
public class SubscribeAlarmTopicFilterImpl implements SubscribeAlarmTopicFilter {


    @Override
    public List<ToopAlarm> filterBySubscribeAlarmTopic(List<ToopAlarm> toopAlarms) {
        log.debug("start to filter the subscribe alarm topic ");
        List<ToopAlarm> filteredAlarm = toopAlarms.stream()
                .filter(this::isInSubscribeAlarmTopic)
                .collect(Collectors.toList());
        return filteredAlarm;
    }

    private Boolean isInSubscribeAlarmTopic(ToopAlarm toopAlarm) {
        log.debug("start to find the alarm topic is or not subscribe topic");
        String alarmGroup = toopAlarm.getGroup();
        String typeId = toopAlarm.getTypeId();
        String severity = toopAlarm.getSeverity();
        String alarmMessage = getAlarmText(toopAlarm.getText());
        Map<String, Boolean> patternMap = AlarmTextPatternMapper.getInstance()
                .getPatternMap(alarmGroup, typeId, severity);
        if (patternMap == null) {
            return false;
        }
        boolean result = false;
        for (String regexPath : patternMap.keySet()) {
            Pattern pattern = PatternUtils.buildAlarmMessageRegex(regexPath);
            Matcher matcher = pattern.matcher(alarmMessage);
            if (matcher.matches()) {
                result = patternMap.get(regexPath);
                break;
            }
        }
        return result;
    }

    private String getAlarmText(String text) {
        return text.split(";")[0];
    }
}
