package net.flex.dci.otn.controller.system.config.common.properties;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.system.config.common.model.TimezoneInfo;
import net.flex.dci.otn.controller.system.config.common.utils.SystemUtils;
import org.springframework.context.annotation.Configuration;

/**
 * 2026/1/4
 *
 * @author musa
 * @version 1.0
 **/
@Getter
@Configuration
@Slf4j
public class TimezoneProperties {

    private static final String TIME_ZONE_FILE="zone.tab";

    private final List<TimezoneInfo> timezones = new ArrayList<>();

    public TimezoneProperties() throws IOException {
        log.info("start to load timezone properties");
        loadTimeZones(TIME_ZONE_FILE);
    }

    private void loadTimeZones(String fileName) throws IOException {
        InputStream inputStream = SystemUtils.loadFileInputStream(fileName);
        try(BufferedReader reader=new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while((line= reader.readLine())!=null) {
              if(!line.startsWith("#")) {
                  TimezoneInfo timezoneInfo = pareseTimezoneInfo(line);
                  timezones.add(timezoneInfo);
              }
            }
        }
    }

    private TimezoneInfo pareseTimezoneInfo(String line) {
        String[] timezoneArray = line.split("\t");
        return TimezoneInfo.builder().code(timezoneArray[0]).coordinates(timezoneArray[1]).tz(timezoneArray[2]).build();
    }

}
