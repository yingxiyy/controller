package net.flex.dci.otn.controller.configuration;


import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Slf4j
@Configuration
public class ConfigLoader {

    @Value("${kafka.historyPMTopic:DCI}")
    private String kafkaTopic;

    public static final String PROCESSING_ROOT = "processing";

    @PostConstruct
    public void createProcessingFolder() {
        try {
            Path path = Paths.get(PROCESSING_ROOT);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
                log.info("Created local folder: {}", path.toAbsolutePath());
            } else {
                log.info("Folder already exists: {}", path.toAbsolutePath());
            }
        } catch (Exception e) {
            log.error("Failed to create processing folder", e);
        }
    }
}
