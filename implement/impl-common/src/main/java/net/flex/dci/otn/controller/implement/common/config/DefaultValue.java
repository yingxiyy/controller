package net.flex.dci.otn.controller.implement.common.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

//@Component
public class DefaultValue {

    private final ResourceLoader resourceLoader;
    private Map<String, Map<String, String>> allConfigs;

    public DefaultValue(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @PostConstruct
    public void loadConfigs() {
        try {
            // Tries to load from classpath (external config/ folder included)
//            Resource resource = resourceLoader.getResource("classpath:default.json");
            Resource resource = resourceLoader.getResource("file:./config/default.json");

            if (!resource.exists()) {
                throw new FileNotFoundException(
                        "default.json not found in classpath (including config/ folder)");
            }

            try (InputStream inputStream = resource.getInputStream()) {
                ObjectMapper mapper = new ObjectMapper();
                allConfigs = mapper.readValue(inputStream,
                        new TypeReference<Map<String, Map<String, String>>>() {
                        });
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load default.json", e);
        }
    }

    public Map<String, String> getConfig(String name) {
        return allConfigs.getOrDefault(name.toLowerCase(), allConfigs.get("bytedance"));
    }
}
