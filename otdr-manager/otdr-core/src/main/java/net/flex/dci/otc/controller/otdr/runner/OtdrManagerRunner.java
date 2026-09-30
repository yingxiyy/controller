package net.flex.dci.otc.controller.otdr.runner;

import static net.flex.dci.otc.controller.otdr.utils.OtdrConstants.DAILY_CLEAR_SERIAL;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.otdr.clear.OtdrResultClear;
import net.flex.dci.otc.controller.otdr.configuration.OtdrDailyJobProperties;
import net.flex.dci.otc.controller.otdr.cron.CronTaskRegister;
import net.flex.dci.otc.controller.otdr.utils.OtdrUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/9/1 19:31
 */
@Order(value = 2)
@Component
@Slf4j
@RequiredArgsConstructor
public class OtdrManagerRunner implements ApplicationRunner {


    private final OtdrResultClear otdrResultClear;

    private final CronTaskRegister cronTaskRegister;

    private final OtdrDailyJobProperties otdrDailyJobProperties;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.debug("otdr result clear job start");
        otdrResultClear.clearResult();
        addScheduleTask(otdrResultClear);
    }

    private String buildDailyCronExpression(String dailyClearTime) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        Date date = sdf.parse(dailyClearTime);
        SimpleDateFormat dailyFormat = new SimpleDateFormat("ss mm HH * * ?");
        return dailyFormat.format(date);
    }

    private void addScheduleTask(OtdrResultClear otdrResultClear) throws ParseException {
        log.info("register daily issue clear job");
        String dailyClearTime = OtdrUtils.getTime(otdrDailyJobProperties.getDailyClearTime());
        String cronExpression = buildDailyCronExpression(dailyClearTime);

        cronTaskRegister.addCronTask(DAILY_CLEAR_SERIAL, otdrResultClear::clearResult,
                cronExpression);
    }
}
