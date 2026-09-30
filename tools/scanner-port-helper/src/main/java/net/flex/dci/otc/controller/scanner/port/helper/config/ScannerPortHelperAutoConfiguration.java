package net.flex.dci.otc.controller.scanner.port.helper.config;

import com.alibaba.fastjson.JSON;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import net.flex.dci.otc.controller.scanner.port.helper.ScannerPortHelper;
import net.flex.dci.otc.controller.scanner.port.helper.core.ScannerPortHelperImpl;
import net.flex.dci.otc.controller.scanner.port.helper.core.yang.ChinaTelecomScannerPort;
import net.flex.dci.otc.controller.scanner.port.helper.core.yang.TencentModelScannerPort;
import net.flex.dci.otc.controller.scanner.port.helper.core.yang.YangModelScannerPort;
import net.flex.dci.otc.controller.scanner.port.helper.model.TelecomScanPortConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @version 1.0
 * @date 12/5/2023 3:46 PM
 */
@Configuration
public class ScannerPortHelperAutoConfiguration {

    @Bean
    public TelecomScanPortConfiguration telecomScanPortConfiguration() throws IOException {
        Resource resource = new ClassPathResource("config/TelecomModelScanPortConfiguration.json");
        if (!resource.exists()) {
            resource = new ClassPathResource("TelecomModelScanPortConfiguration.json");
        }
        InputStream inputStream = resource.getInputStream();
        TelecomScanPortConfiguration telecomScanPortConfiguration = JSON.parseObject(inputStream,
                TelecomScanPortConfiguration.class);
        return telecomScanPortConfiguration;
    }

    @Bean(name = "tencentModelScannerPort")
    public YangModelScannerPort tencentModelScannerPort() {
        return new TencentModelScannerPort();
    }

    @Bean(name = "chinaTelecomScannerPort")
    public YangModelScannerPort chinaTelecomScannerPort() {
        return new ChinaTelecomScannerPort();
    }

    @Bean
    @ConditionalOnBean(TelecomScanPortConfiguration.class)
    public ScannerPortHelper scannerPortHelper(List<YangModelScannerPort> yangModelScannerPorts) {
        return new ScannerPortHelperImpl(yangModelScannerPorts);
    }
}
