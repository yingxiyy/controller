package net.flex.dci.otn.controller.sftp.manager.configuration;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.sftp.manager.properties.FtpAntPathMatcherMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;

/**
 * @version 1.0
 * @date 6/7/2023 3:07 PM
 */
@Configuration
@Slf4j
public class FtpServerConfiguration {

    @Bean
    public FtpAntPathMatcherMap loadFtpAntPathMatcherMap() throws IOException {
        Resource resource = new ClassPathResource("FtpPathMatcher.json");
        InputStream inputStream = resource.getInputStream();
        FtpAntPathMatcherMap ftpAntPathMatcherMap = JSON.parseObject(inputStream,
                FtpAntPathMatcherMap.class);
        return ftpAntPathMatcherMap;
    }
}
