/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.utils;

import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.enums.AlarmSeverityCode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;

/**
 * common filter
 *
 * @author: xinyzhao
 * @date: 2021/3/25
 */
@Slf4j
public class CommonUtils {

    /**
     * generate base authorization token
     *
     * @param username
     * @param password
     * @return
     */
    public static String basicAuthorization(String username, String password) {
        String auth = username + ":" + password;
        String authHeader = "Basic " + Base64.getUrlEncoder().encodeToString(auth.getBytes());
        return authHeader;
    }


    public static int convert2Int(String floatString) {
        return new BigDecimal(Double.valueOf(floatString) * 1000000).intValue();
    }


    public static String convert2String(Integer value) {
        return new BigDecimal(Double.valueOf(value) / 1000000d).toString();
    }

    public static <T> Set<T> getIntersectionSetByGuava(Set<T> before, Set<T> after) {
        Set<T> diff = Sets.intersection(before, after);
        return diff;
    }

    public static <T> Set<T> getDifferenceSetByGuava(Set<T> before, Set<T> after) {
        Set<T> diff = Sets.difference(before, after);
        return diff;
    }

    public static AlarmSeverity calculateAlarmSeverity(List<AlarmSeverity> alarmSeverities) {
        Integer code = 0b0000000;
        for (AlarmSeverity alarmSeverity : alarmSeverities) {
            code |= AlarmSeverityCode.getSeverityCode(alarmSeverity);
        }
        return AlarmSeverityCode.getSeverity(code);
    }

}
