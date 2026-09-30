/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.system.config.email.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.DataTimeConvert;
import net.flex.dci.otn.controller.system.config.common.model.ToopAlarm;
import net.flex.dci.otn.controller.system.config.email.service.AlarmConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.DateAndTime;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;
import org.springframework.ui.freemarker.SpringTemplateLoader;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
public class TextMailGenerator {

    public static final String ALARM_TEMPLATE_FILE = "alarm.ftl";

    public static final String TEST_MAIL_TEMPLATE_FILE = "mailTest.ftl";
    public static final String SEVERITY = "severity";
    public static final String TIME_CREATED = "timeCreated";
    private static final Configuration cfg;

    static {
        cfg = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        cfg.setTemplateLoader(new SpringTemplateLoader(new DefaultResourceLoader(), "templates"));
    }

    public TextMailGenerator() {

    }

    public static String getAlarmMail(ToopAlarm alarm) {
        log.debug("get alarm text mail for the alarm ,the alarm is:{}", alarm);
        try {
            Template template = null;
            template = cfg.getTemplate(ALARM_TEMPLATE_FILE);
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> dataMap = objectMapper.convertValue(alarm, Map.class);
            String severity = AlarmConstants.getSeverity(alarm.getSeverity());
            DateAndTime timeCreated = DataTimeConvert.convertToDateAndTime(
                    DataTimeConvert.long2date(alarm.getTimeCreated()));
            dataMap.put(SEVERITY, severity);
            dataMap.put(TIME_CREATED, timeCreated.getValue());
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, dataMap);

        } catch (IOException | TemplateException e) {
            log.error("Failed to convert alarm to mail text", e);
            return alarm.toString();
        }

    }

    public static String getTestMail() {
        log.debug("get test mail template");
        try {
            Template template = null;
            template = cfg.getTemplate(TEST_MAIL_TEMPLATE_FILE);
//            ObjectMapper objectMapper = new ObjectMapper();
            return FreeMarkerTemplateUtils.processTemplateIntoString(template, null);

        } catch (IOException | TemplateException e) {
            log.error("Failed to convert alarm to mail text", e);
            return "test";
        }
    }
}
