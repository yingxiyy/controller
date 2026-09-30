package devicemaintenance;

import net.flex.dci.otc.zkclient4boot.EnableDciClient;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Device Maintenance 微服务启动类
 */
@SpringBootApplication
@EnableKafka
@EnableAsync
@EnableScheduling
@EnableDciClient
public class DeviceMaintenanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeviceMaintenanceApplication.class, args);
    }
}
