package net.flex.dci.otn.controller.apsswitchlog;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.configuration.ConfigLoader;
import net.flex.dci.otn.controller.pm.TelemetryDataFile;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Paths;

@Slf4j
@SpringBootTest
@ActiveProfiles("test")
public class HistoryPmFileParserTest {

    @Autowired
    private ConfigLoader configLoader;

    @Test
    void sendTelemetryToRealKafka() throws Exception {
        TelemetryDataFile df = new TelemetryDataFile(
                Paths.get("C:/working/dci/controller/devicelog/historypm.json"));

        try {
            df.parse();

            for (int i = 0; i<4; i++) {
                BroadcastMessager.publishMessage(
                        "DCI",
                        df.getTelemetryData().toByteArray());
            }
        } catch (Exception e) {
            log.error("error: ", e);
            throw new RuntimeException(e);
        }
        for (int i = 0; i<4; i++) {
            BroadcastMessager.publishMessage(
                    configLoader.getKafkaTopic(),
                    df.getTelemetryData().toByteArray());
        }
    }
}
