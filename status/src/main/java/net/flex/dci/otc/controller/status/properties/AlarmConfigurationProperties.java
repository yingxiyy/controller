package net.flex.dci.otc.controller.status.properties;

import com.alibaba.fastjson.JSON;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import net.flex.dci.otc.controller.status.model.AlarmConfigRoot;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @version 1.0
 * @date 2022/4/6 15:08
 */

@Configuration
public class AlarmConfigurationProperties {

    private static final String ALARM_CONFIGURATION_FILE = "alarmConfiguration.json";
    private final AlarmConfigRoot alarmConfigRoot;

    public AlarmConfigurationProperties() throws IOException {
        InputStream inputStream = null;
        String rootDir = System.getProperty("user.dir");
        File alarmConfigFile = new File(rootDir, "config/" + ALARM_CONFIGURATION_FILE);
        if (alarmConfigFile.exists()) {
            inputStream = Files.newInputStream(alarmConfigFile.toPath());
        } else {
            Resource resource = new ClassPathResource(ALARM_CONFIGURATION_FILE);
            inputStream = resource.getInputStream();
        }
        alarmConfigRoot = JSON.parseObject(inputStream,
                AlarmConfigRoot.class);
    }


    public List<String> affectLinkKeywordList() {
        return alarmConfigRoot.getAlarm().getLink().getKeywords();
    }

    public boolean isContainsKeyWords(String alarmText) {
        AtomicBoolean result = new AtomicBoolean(false);
        affectLinkKeywordList().forEach(linkRefAlarmKeyWord -> {
            if (alarmText.contains(linkRefAlarmKeyWord)) {
                result.set(true);
            }
        });
        return result.get();
    }

    public boolean isFilter() {
        return alarmConfigRoot.getFilter() != null && alarmConfigRoot.getFilter();
    }

}
