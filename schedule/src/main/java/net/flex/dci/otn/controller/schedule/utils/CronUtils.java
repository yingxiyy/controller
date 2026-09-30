package net.flex.dci.otn.controller.schedule.utils;

import java.text.ParseException;
import java.util.Date;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronExpression;

/**
 * 2026/6/2
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class CronUtils {

    public static boolean isValid(String cronExpression) {
        return CronExpression.isValidExpression(cronExpression);
    }

    public static Long getNextExecuteTime(String cronExpression) {
        if (!isValid(cronExpression)) {
            log.error("invalid cron expression:{}", cronExpression);
            return null;
        }
        try {
            CronExpression cron = new CronExpression(cronExpression);
            Date now = new Date();
            Date nextTime = cron.getNextValidTimeAfter(now);
            return nextTime.toInstant().getEpochSecond();
        } catch (ParseException e) {
            log.error("parse cron expression failed: {}", cronExpression, e);
            return null;
        }
    }
}
