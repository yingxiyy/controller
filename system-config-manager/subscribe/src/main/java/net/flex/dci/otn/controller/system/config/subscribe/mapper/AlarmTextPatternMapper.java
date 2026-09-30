/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.subscribe.mapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otn.controller.system.config.subscribe.utils.AlarmGroupGenerator;
import net.flex.dci.otn.db.jpa.entity.BoardAlarmInfo;
import net.flex.dci.otn.db.jpa.service.dao.BoardAlarmInfoDaoService;

/**
 * @version 1.0
 * @date 2021/12/10 16:14
 */
@Slf4j
public class AlarmTextPatternMapper {

    private static AlarmTextPatternMapper instance = null;

    private Map<String, Map<String, Boolean>> alarmGroupPatternSubMap = new ConcurrentHashMap<>();

    private AlarmTextPatternMapper() {
        BoardAlarmInfoDaoService alarmTopicDao = SpringBeanFinder.getBean(
                BoardAlarmInfoDaoService.class);
        List<BoardAlarmInfo> allTopics = alarmTopicDao.findAll();
        initMap(allTopics);
    }

    public static AlarmTextPatternMapper getInstance() {
        if (instance == null) {
            synchronized (AlarmTextPatternMapper.class) {
                if (instance == null) {
                    instance = new AlarmTextPatternMapper();
                }
            }
        }
        return instance;
    }

    public void initMap(List<BoardAlarmInfo> alarmTopics) {
        Map<String, Map<String, Boolean>> patternSubMap = new HashMap<>();
        for (BoardAlarmInfo alarmTopic : alarmTopics) {
            String alarmGroupKey = AlarmGroupGenerator.generatorAlarmGroupKey(alarmTopic);
            String regexStr = alarmTopic.getMessage();
            Boolean subscribe = alarmTopic.getSubscribe();

            if (!patternSubMap.containsKey(alarmGroupKey)) {
                Map<String, Boolean> patternMap = new HashMap<>();
                patternMap.put(regexStr, subscribe);
                patternSubMap.put(alarmGroupKey, patternMap);
//                Map<AlarmPatternRefDto> tempSet = new HashSet<>();
//                tempSet.add(alarmPatternRefDto);
//                patternSubMap.put(alarmGroupKey, tempSet);
            } else {
//                patternSubMap.get(alarmGroupKey)
//                        .add(alarmPatternRefDto);
                patternSubMap.get(alarmGroupKey).put(regexStr, subscribe);
            }
        }
        this.alarmGroupPatternSubMap = patternSubMap;
    }


    /**
     * get PatternList
     *
     * @param alarmGroup
     * @param typeId
     * @return
     */
    public Map<String, Boolean> getPatternMap(String alarmGroup, String typeId,
            String severity) {
        log.debug("get pattern list for the alarm group {},type id is {}", alarmGroup, typeId);
        String alarmGroupKey = AlarmGroupGenerator.generatorAlarmGroupKey(alarmGroup, typeId,
                severity);
        return this.alarmGroupPatternSubMap.get(alarmGroupKey);
    }

    /**
     * update current subscribe state for the cache mapper
     *
     * @param changeStateAlarmInfos
     */
    public void updateMapper(
            List<BoardAlarmInfo> changeStateAlarmInfos) {
        log.debug("update the cache mapper state");
        for (BoardAlarmInfo boardAlarmInfo : changeStateAlarmInfos) {
            String alarmGroupKey = AlarmGroupGenerator.generatorAlarmGroupKey(boardAlarmInfo);
            String pattern = boardAlarmInfo.getMessage();
            Boolean subscribe = boardAlarmInfo.getSubscribe();
            this.alarmGroupPatternSubMap.get(alarmGroupKey).put(pattern, subscribe);
        }
    }


}
