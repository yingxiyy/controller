/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.system.config.SystemConfigManagerApplication;
import net.flex.dci.otn.controller.system.config.common.model.ToopAlarm;
import net.flex.dci.otn.controller.system.config.email.service.MailImplService;
import net.flex.dci.otn.controller.system.config.email.utils.MailUtils;
import net.flex.dci.otn.controller.system.config.subscribe.filter.SubscribeAlarmTopicFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

/**
 * @version 1.0
 * @date 2021/12/13 11:11
 */

@SpringBootTest(classes = SystemConfigManagerApplication.class)
@Slf4j
public class SubscribeAlarmTest {

    @Autowired
    private MailImplService mailImplService;
    @Autowired
    private SubscribeAlarmTopicFilter subscribeAlarmTopicFilter;
    @Autowired
    private MailUtils mailUtils;

    @Test
    public void testAlarmFilter() {
        ToopAlarm toopAlarm = new ToopAlarm();
        toopAlarm.setGroup("OA_Port_Failure");
        toopAlarm.setTypeId("OTS");
        toopAlarm.setText(
                "APR_Active");
        toopAlarm.setSeverity("Major");
        ToopAlarm toopAlarm1 = new ToopAlarm();
        toopAlarm1.setGroup("OA_Port_Failure");
        toopAlarm1.setTypeId("LOS");
        toopAlarm1.setText(
                "PA_Input_LOS;PA input power is lower than threshold -35.00 dBm");
        toopAlarm1.setSeverity("Critical");
        toopAlarm.setToopKey("Site-1691561426178#Ne-1691561630904");
        toopAlarm.setIsClear(false);
        toopAlarm.setServiceAffect(true);
        toopAlarm.setTimeCreated(System.currentTimeMillis());
        List<ToopAlarm> alarms = new ArrayList<>();
        alarms.add(toopAlarm);
        alarms.add(toopAlarm1);
//        List<ToopAlarm> toopAlarms = subscribeAlarmTopicFilter.filterBySubscribeAlarmTopic(
//                alarms);
//        log.info("toop alarms is {}", toopAlarms);
//        mailImplService.send(toopAlarm);
//        String mailContent = mailUtils.getAlarmContent(toopAlarm);
        String mailSubject = mailUtils.getSubject(toopAlarm);

    }
}
