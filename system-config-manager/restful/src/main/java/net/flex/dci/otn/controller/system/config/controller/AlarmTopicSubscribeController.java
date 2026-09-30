/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.controller;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otn.controller.system.config.common.model.Result;
import net.flex.dci.otn.controller.system.config.subscribe.model.AlarmGroupTopicDetails;
import net.flex.dci.otn.controller.system.config.subscribe.model.AlarmTopic;
import net.flex.dci.otn.controller.system.config.subscribe.model.AlarmTopicGroupInfo;
import net.flex.dci.otn.controller.system.config.subscribe.model.UpdateAlarmTopics;
import net.flex.dci.otn.controller.system.config.subscribe.service.AlarmTopicSubscribeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 2021/12/8 13:17
 */
@Slf4j
@RestController
@RequestMapping(value = "/alarm/topic")
public class AlarmTopicSubscribeController {

    @Autowired
    private AlarmTopicSubscribeService topicSubscribeService;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<Result> getAllTopics() {
        log.info("list all alarm topic ");
        List<AlarmTopic> alarmTopics = topicSubscribeService.listAllTopics();
        return new ResponseEntity<>(Result.ok(alarmTopics), HttpStatus.OK);
    }

    @RequestMapping(value = "/group/{id}", method = RequestMethod.GET)
    public ResponseEntity<Result> getAlarmGroupById(@PathVariable("id") Long id)
            throws CommonException {
        log.info("start to get alarm group by id :{}", id);
        AlarmTopicGroupInfo groupInfo = topicSubscribeService.getAlarmTopicGroupById(id);
        return new ResponseEntity<>(Result.ok(groupInfo), HttpStatus.OK);
    }


    @RequestMapping(value = "/group/name/{name}/topics", method = RequestMethod.GET)
    public ResponseEntity<?> getAlarmTopicsByGroupName(@PathVariable("name") String name)
            throws CommonException {
        log.info("start to get alarm topics by alarm group name :{}", name);
        List<AlarmTopic> alarmTopics = topicSubscribeService.getAlarmTopicsByGroupName(name);
        return new ResponseEntity<>(Result.ok(alarmTopics), HttpStatus.OK);
    }

    @RequestMapping(value = "/group/id/{id}/topics", method = RequestMethod.GET)
    public ResponseEntity<?> getAlarmTopicsByGroupId(@PathVariable("id") Long id)
            throws CommonException {
        log.info("start to get alarm topics by alarm group id :{}", id);
        List<AlarmTopic> alarmTopics = topicSubscribeService.getAlarmTopicsByGroupId(id);
        return new ResponseEntity<>(Result.ok(alarmTopics), HttpStatus.OK);
    }


    @RequestMapping(value = "/subscribed-topics", method = RequestMethod.GET)
    public ResponseEntity<?> getSubscribedAlarmTopics()
            throws CommonException {
        log.info("start to get all subscribed alarm");
        List<AlarmTopic> alarmTopics = topicSubscribeService.getSubscribedAlarmTopics();
        return new ResponseEntity<>(Result.ok(alarmTopics), HttpStatus.OK);
    }

    @RequestMapping(value = "/byGroup", method = RequestMethod.GET)
    public ResponseEntity<?> getAlarmTopicsByGroup()
            throws CommonException {
        log.info("start to get all subscribed alarm");
        AlarmGroupTopicDetails alarmTopicsByGroup = topicSubscribeService.getAlarmTopicsByGroup();
        return new ResponseEntity<>(Result.ok(alarmTopicsByGroup), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT)
    public ResponseEntity<?> updateSubscribeTopics(
            @RequestBody UpdateAlarmTopics updateAlarmTopics)
            throws CommonException {
        log.info("update subscribe alarm topics state");
        AlarmGroupTopicDetails alarmTopicsByGroup = topicSubscribeService.updateAlarmTopics(
                updateAlarmTopics);
        return new ResponseEntity<>(Result.ok(alarmTopicsByGroup), HttpStatus.OK);
    }

}
