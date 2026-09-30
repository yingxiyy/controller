/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otc.controller.status.alarm.core;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.configuration.DciStatusConfiguration;
import net.flex.dci.otn.db.jpa.service.dao.AlarmDaoService;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AlarmCalculator {

    @Autowired
    private DciStatusConfiguration alarmConfig;

    @Autowired
    private AlarmDaoService alarmDaoService;

    public AlarmSeverity calculateSeverity(String linkId, List<String> nmlKeyList) {
        AlarmSeverity ret = AlarmSeverity.Cleared;
        List<String> likeStrList = this.getAlarmLossKeyword();
        Long count = alarmDaoService.getCountByAlarmText(likeStrList, nmlKeyList, true);
        if (count > 0) {
            log.debug("Link {} has critical alarm number {}", linkId, count);
            ret = AlarmSeverity.Critical;
        } else {
            count = alarmDaoService.getCount(nmlKeyList);
            if (count > 0) {
                log.debug("Link {} has minor alarm number {}", linkId, count);
                ret = AlarmSeverity.Minor;
            }
        }
        likeStrList = new ArrayList<>();
        likeStrList.add("Ne is isolated");
        Long cnt = alarmDaoService.getCountByAlarmText(likeStrList, nmlKeyList, false);
        if (cnt > 0) {
            ret = AlarmSeverity.Unknown;
        }
        return ret;
    }

    public AlarmSeverity calculateCriticalSeverity(String linkId, List<String> nmlKeyList) {
        List<String> likeStrList = this.getAlarmLossKeyword();
        AlarmSeverity ret = null;
        Long count = alarmDaoService.getCountByAlarmText(likeStrList, nmlKeyList, true);
        if (count > 0) {
            log.debug("Link {} has critical alarm number {}", linkId, count);
            ret = AlarmSeverity.Critical;
        }
        return ret;
    }

    public AlarmSeverity calculateSeverityBelowCritical(String linkId, List<String> nmlKeyList) {
        AlarmSeverity ret = AlarmSeverity.Cleared;
        List<String> likeStrList = this.getAlarmLossKeyword();
        Long count = alarmDaoService.getCountByAlarmText(likeStrList, nmlKeyList, true);
        if (count > 0) {
            log.debug("Link {} has critical alarm number {}, but think as majar", linkId,
                    count);
            ret = AlarmSeverity.Major;
        } else {
            count = alarmDaoService.getCount(nmlKeyList);
            if (count > 0) {
                log.debug("Link {} has minor alarm number {}", linkId, count);
                ret = AlarmSeverity.Minor;
            }
        }
        return ret;
    }

    private List<String> getAlarmLossKeyword() {
        List<String> ret = new ArrayList<>();
        String str = alarmConfig.getAlarmKeyword();
        if (str != null && !str.equals("")) {
            str = str.trim();
            String arr[] = str.split(":");
            if (arr != null) {
                for (int i = 0; i < arr.length; i++) {
                    if (arr[i] != null) {
                        String temp = arr[i].trim();
                        if (!temp.equals("")) {
                            ret.add(temp);
                        }
                    }
                }
            }

        }
        return ret;
    }
}
