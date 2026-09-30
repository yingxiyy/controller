/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.subscribe.utils;

import static net.flex.dci.otc.common.constants.Constants.POUND;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.system.config.subscribe.model.AlarmGroupTopicDetail;
import net.flex.dci.otn.controller.system.config.subscribe.model.AlarmGroupTopicDetails;
import net.flex.dci.otn.controller.system.config.subscribe.model.AlarmTopic;
import net.flex.dci.otn.controller.system.config.subscribe.model.AlarmTopicGroupInfo;
import net.flex.dci.otn.db.jpa.entity.BoardAlarmGroup;
import net.flex.dci.otn.db.jpa.entity.BoardAlarmInfo;

/**
 * @version 1.0
 * @date 2021/12/9 16:53
 */
@Slf4j
public class AlarmGroupGenerator {

    public static AlarmGroupTopicDetails extractAlarmGroupTopicDetails(
            List<BoardAlarmGroup> alarmGroups, List<BoardAlarmInfo> alarmTopics) {
        Map<Long, AlarmTopicGroupInfo> alarmTopicGroupMap = alarmGroups.stream()
                .map(ConvertorUtils::convert2AlarmTopicGroupInfo)
                .collect(Collectors.toMap(AlarmTopicGroupInfo::getId, a -> a, (k1, k2) -> k1));
        Map<Long, List<AlarmTopic>> alarmTopicGroupBy = alarmTopics.stream()
                .map(ConvertorUtils::convertAlarmInfo2Topic)
                .collect(Collectors.groupingBy(AlarmTopic::getBoardAlarmType));
        AlarmGroupTopicDetails alarmGroupTopicDetails = new AlarmGroupTopicDetails();
        List<AlarmGroupTopicDetail> alarmGroupTopicDetailList = new ArrayList<>();
        for (Long id : alarmTopicGroupMap.keySet()) {
            AlarmGroupTopicDetail alarmGroupTopicDetail = new AlarmGroupTopicDetail();
            AlarmTopicGroupInfo alarmTopicGroup = alarmTopicGroupMap.get(id);
            List<AlarmTopic> alarmTopicList = alarmTopicGroupBy.get(id);
            if (alarmTopicList != null && !alarmTopicList.isEmpty()) {
                alarmGroupTopicDetail.setAlarmTopics(alarmTopicList);
                alarmGroupTopicDetail.setGroupName(alarmTopicGroup.getName());
                alarmGroupTopicDetailList.add(alarmGroupTopicDetail);
            }
        }
        alarmGroupTopicDetails.setAlarmGroupTopicDetails(alarmGroupTopicDetailList);

        return alarmGroupTopicDetails;
    }

    public static String generatorAlarmGroupKey(BoardAlarmInfo alarmTopic) {
        StringBuilder sb = new StringBuilder();
        sb.append(alarmTopic.getAlarm_group())
                .append(POUND).append(alarmTopic.getTypeId()).append(POUND)
                .append(alarmTopic.getSeverity());
        return sb.toString().toUpperCase();
    }

    public static String generatorAlarmGroupKey(String alarmGroup, String typeId, String severity) {
        StringBuilder sb = new StringBuilder();
        sb.append(alarmGroup.toUpperCase())
                .append(POUND).append(typeId.toUpperCase()).append(POUND)
                .append(severity.toUpperCase());
        return sb.toString();
    }
}
