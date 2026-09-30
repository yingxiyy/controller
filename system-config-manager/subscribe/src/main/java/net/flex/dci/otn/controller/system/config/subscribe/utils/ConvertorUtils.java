/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.subscribe.utils;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.system.config.subscribe.model.AlarmTopic;
import net.flex.dci.otn.controller.system.config.subscribe.model.AlarmTopicGroupInfo;
import net.flex.dci.otn.db.jpa.entity.BoardAlarmGroup;
import net.flex.dci.otn.db.jpa.entity.BoardAlarmInfo;

/**
 * @version 1.0
 * @date 2021/12/8 16:05
 */
@Slf4j
public class ConvertorUtils {

    public static AlarmTopicGroupInfo convert2AlarmTopicGroupInfo(BoardAlarmGroup boardAlarmGroup) {
        log.debug("convert 2 alarm topic group info");
        return AlarmTopicGroupInfo.builder()
                .name(boardAlarmGroup.getName())
                .description(boardAlarmGroup.getDescription())
                .id(boardAlarmGroup.getId())
                .build();
    }

    public static List<AlarmTopic> convertAlarmInfoToTopics(List<BoardAlarmInfo> alarmTopics) {
        return alarmTopics.stream().map(boardAlarmInfo -> convertAlarmInfo2Topic(boardAlarmInfo))
                .collect(
                        Collectors.toList());
    }

    public static AlarmTopic convertAlarmInfo2Topic(BoardAlarmInfo alarmInfo) {
        return AlarmTopic.builder()
                .id(alarmInfo.getId())
                .subscribe(alarmInfo.getSubscribe())
                .alarmText(alarmInfo.getMessage())
                .severity(alarmInfo.getSeverity())
                .alarmTypeId(alarmInfo.getTypeId())
                .alarmGroup(alarmInfo.getAlarm_group())
                .boardAlarmType(alarmInfo.getBoardAlarmType())
                .build();
    }

    public static BoardAlarmInfo convertAlarmTopic2BoardAlarmInfo(AlarmTopic alarmTopic) {
        BoardAlarmInfo boardAlarmInfo = new BoardAlarmInfo();

        boardAlarmInfo.setId(alarmTopic.getId());
        boardAlarmInfo.setSubscribe(alarmTopic.getSubscribe());
        boardAlarmInfo.setAlarm_group(alarmTopic.getAlarmGroup());
        boardAlarmInfo.setBoardAlarmType(alarmTopic.getBoardAlarmType());
        boardAlarmInfo.setMessage(alarmTopic.getAlarmText());
        boardAlarmInfo.setSeverity(alarmTopic.getSeverity());
        boardAlarmInfo.setTypeId(alarmTopic.getAlarmTypeId());

        return boardAlarmInfo;
    }
}
