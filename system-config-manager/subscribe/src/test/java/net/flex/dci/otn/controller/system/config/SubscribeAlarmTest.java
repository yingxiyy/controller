/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config;

import com.alibaba.fastjson.JSON;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.system.config.common.model.ToopAlarm;

/**
 * @version 1.0
 * @date 2021/12/13 11:11
 */

@Slf4j
public class SubscribeAlarmTest {


    public static void main(String[] args) {
        String json = "[\n"
                + "  {\n"
                + "    \"severity\": \"WARNING\",\n"
                + "    \"toop-ip\": \"10.242.111.38\",\n"
                + "    \"resource\": \"APS-1-3-1\",\n"
                + "    \"ip\": \"172.24.168.6\",\n"
                + "    \"toop-key\": \"Site-1637822513176#Ne-1637822657149#LINECARD-1-1#Port-1-3-SIG\",\n"
                + "    \"type-id\": \"AIS\",\n"
                + "    \"is-cleared\": false,\n"
                + "    \"id\": \"325213\",\n"
                + "    \"text\": \"OP_Switch;Received signal is switched from port A to B. Start time:2021-12-09 14:55:37.0 End time:2021-12-09 14:55:37.1 Duration:1 ms\",\n"
                + "    \"time-created\": 1639029978730,\n"
                + "    \"service-affect\": true,\n"
                + "    \"group\": \"OP_Switch\"\n"
                + "  }\n"
                + "]";
        List<ToopAlarm> toopAlarmList = JSON.parseArray(json, ToopAlarm.class);
        log.info("toop alarm list:{}", toopAlarmList);
    }
}
