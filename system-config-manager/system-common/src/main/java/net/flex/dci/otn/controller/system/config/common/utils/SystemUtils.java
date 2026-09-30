package net.flex.dci.otn.controller.system.config.common.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * 2026/1/4
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public class SystemUtils {

    public static InputStream loadFileInputStream(String fileName) throws IOException {
        log.info("load file  inputStream:{}", fileName);
        InputStream inputStream = null;
        String rootDir = java.lang.System.getProperty("user.dir");
        File file = new File(rootDir, "config/" + fileName);
        if (file.exists()) {
            inputStream = Files.newInputStream(file.toPath());
        } else {
            Resource resource = new ClassPathResource(fileName);
            inputStream = resource.getInputStream();
        }
        return inputStream;
    }

}
