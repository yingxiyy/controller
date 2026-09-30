/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.alarm.controller;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.controller.status.alarm.service.AlarmService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class AlarmController {

    @Autowired
    private AlarmService alarmService;


    @PostMapping(value = "/restconf/operations/alarm:generate-alarm", produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String generateAlarm(@RequestBody String input) throws CommonException {
        log.info("start to generate alarm input:{}", input);
        return alarmService.generateAlarm(input);
    }

    @PostMapping(value = "/restconf/operations/alarm:clear-alarm", produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String clearAlarm(@RequestBody String input) throws CommonException {
        log.info("clear the alarm");
        return alarmService.clearAlarm(input);
    }

    @PostMapping(value = {
            "/restconf/operations/alarm:get-alarm-statistics"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String getAlarmStatistics() throws CommonException {
        log.info("start to clear alarm statistics");
        return alarmService.statisticsAlarm();
    }

    @PostMapping(value = {
            "/restconf/operations/alarm:get-alarm-detail"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String getAlarmDetail(@RequestBody String input) throws CommonException {
        log.info("start to get alarm detail {}", input);
        return alarmService.getAlarmsDetail(input);
    }

    @PostMapping(value = {
            "/restconf/operations/alarm:get-current-alarms"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String getCurrentAlarms(@RequestBody String input) throws Exception {
        return alarmService.getCurrentAlarms(input);
    }

    @PostMapping(value = {
            "/restconf/operations/alarm:get-history-alarms"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String getHistoryAlarms(@RequestBody String input) throws Exception {

        return alarmService.getHistoryAlarms(input);
    }

    @PostMapping(value = {
            "/restconf/operations/alarm:get-alarms"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String getAlarms(@RequestBody String input) throws Exception {
        log.info("start to get alarms, input {}", input);
        return alarmService.getAlarms(input);
    }

    @PostMapping(value = {
            "/restconf/operations/alarm:refresh-alarm"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String refreshAlarm(@RequestBody String input) throws Exception {
        log.info("start refresh alarm :{}", input);
        return alarmService.refreshAlarm(input);
    }


    @PostMapping(value = {
            "/restconf/operations/alarm:refresh-device-alarm"}, produces = "application/json;charset=UTF-8")
    public @ResponseBody
    String refreshDeviceAlarm() throws Exception {
        log.info("start refresh all device alarm ");
        return alarmService.refreshDeviceAlarm();
    }


}
