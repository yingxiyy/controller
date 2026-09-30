/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.alarm.service;

import net.flex.dci.otc.common.exception.CommonException;

/**
 * @version 1.0
 * @date 2021/10/21 11:20
 */


public interface AlarmService {

    public String statisticsAlarm() throws CommonException;

    String clearAlarm(String input) throws CommonException;

    String generateAlarm(String input) throws CommonException;

    String getAlarmsDetail(String input) throws CommonException;

    String refreshAlarm(String input) throws CommonException;

    String getAlarms(String input) throws Exception;

    String getHistoryAlarms(String input) throws Exception;

    String getCurrentAlarms(String input) throws Exception;

    String refreshDeviceAlarm() throws Exception;
}
