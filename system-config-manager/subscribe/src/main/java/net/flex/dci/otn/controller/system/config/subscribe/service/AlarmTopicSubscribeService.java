/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.subscribe.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.system.config.subscribe.mapper.AlarmTextPatternMapper;
import net.flex.dci.otn.controller.system.config.subscribe.model.AlarmGroupTopicDetails;
import net.flex.dci.otn.controller.system.config.subscribe.model.AlarmTopic;
import net.flex.dci.otn.controller.system.config.subscribe.model.AlarmTopicGroupInfo;
import net.flex.dci.otn.controller.system.config.subscribe.model.UpdateAlarmTopics;
import net.flex.dci.otn.controller.system.config.subscribe.utils.AlarmGroupGenerator;
import net.flex.dci.otn.controller.system.config.subscribe.utils.ConvertorUtils;
import net.flex.dci.otn.db.jpa.entity.BoardAlarmGroup;
import net.flex.dci.otn.db.jpa.entity.BoardAlarmInfo;
import net.flex.dci.otn.db.jpa.service.dao.BoardAlarmGroupDaoService;
import net.flex.dci.otn.db.jpa.service.dao.BoardAlarmInfoDaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @version 1.0
 * @date 2021/12/8 15:52
 */
@Slf4j
@Service
public class AlarmTopicSubscribeService {

    @Autowired
    private BoardAlarmGroupDaoService alarmGroupDao;

    @Autowired
    private BoardAlarmInfoDaoService alarmInfoDao;

    /**
     * get alarm topic group by id
     *
     * @param id
     * @return
     */
    public AlarmTopicGroupInfo getAlarmTopicGroupById(Long id) {
        log.debug("start to get alarm topic group by id:{}", id);
        BoardAlarmGroup boardAlarmGroup = alarmGroupDao.getBoardAlarmGroupById(id);
        if (boardAlarmGroup == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "invalidate group id,please connect administrator");
        }
        AlarmTopicGroupInfo alarmTopicGroupInfo = ConvertorUtils.convert2AlarmTopicGroupInfo(
                boardAlarmGroup);
        return alarmTopicGroupInfo;
    }

    /**
     * get alarm topics by group name
     *
     * @param name
     * @return
     */
    public List<AlarmTopic> getAlarmTopicsByGroupName(String name) {
        List<BoardAlarmInfo> alarmTopics = alarmInfoDao.getBoardAlarmInfoByName(name);
        if (alarmTopics == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "invalid group name,can not find the alarm topics by the name :" + name);
        }
        return ConvertorUtils.convertAlarmInfoToTopics(alarmTopics);
    }

    public List<AlarmTopic> getAlarmTopicsByGroupId(Long id) {
        List<BoardAlarmInfo> alarmTopics = alarmInfoDao.getBoardAlarmInfoById(id);
        if (null == alarmTopics) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "invalid group name,can not find the alarm topics by the id :" + id);
        }
        return ConvertorUtils.convertAlarmInfoToTopics(alarmTopics);
    }

    /**
     * get alarm subscribed alarm topics
     *
     * @return
     */
    public List<AlarmTopic> getSubscribedAlarmTopics() {
        log.info("list all alarm topics ");
        List<BoardAlarmInfo> alarmTopics = alarmInfoDao.listAllSubscribed();
        return ConvertorUtils.convertAlarmInfoToTopics(alarmTopics);
    }

    /**
     * get alarm topics group by group name
     *
     * @return
     */
    public AlarmGroupTopicDetails getAlarmTopicsByGroup() {
        log.info("list all alarm topics group by name");
        List<BoardAlarmInfo> alarmTopics = alarmInfoDao.findAll();
        List<BoardAlarmGroup> alarmGroups = alarmGroupDao.findAll();
        AlarmGroupTopicDetails alarmGroupTopicDetails = null;
        alarmGroupTopicDetails = AlarmGroupGenerator.extractAlarmGroupTopicDetails(alarmGroups,
                alarmTopics);
        return alarmGroupTopicDetails;
    }

    public AlarmGroupTopicDetails updateAlarmTopics(UpdateAlarmTopics updateAlarmTopics) {
        log.info("change the update alarm topics subscribe state,{}", updateAlarmTopics);
        List<AlarmTopic> alarmTopics = updateAlarmTopics.getAlarmTopics();
        List<BoardAlarmInfo> changeStateAlarmInfo = alarmTopics.stream()
                .map(alarmTopic -> alarmInfoDao.updateAlarmInfoSubscribeState(
                        ConvertorUtils.convertAlarmTopic2BoardAlarmInfo(alarmTopic))).collect(
                        Collectors.toList());
        List<BoardAlarmGroup> alarmGroups = alarmGroupDao.findAll();
        AlarmGroupTopicDetails alarmGroupTopicDetails = null;
        alarmGroupTopicDetails = AlarmGroupGenerator.extractAlarmGroupTopicDetails(alarmGroups,
                changeStateAlarmInfo);
        AlarmTextPatternMapper.getInstance().updateMapper(changeStateAlarmInfo);
        return alarmGroupTopicDetails;
    }

    public List<AlarmTopic> listAllTopics() {
        log.info("list all alarm topics ");
        List<BoardAlarmInfo> alarmInfos = alarmInfoDao.findAll();
        return ConvertorUtils.convertAlarmInfoToTopics(alarmInfos);
    }
}
