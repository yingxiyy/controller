package devicemaintenance.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Kafka监听器配置
 * 启用Kafka消息监听功能
 */
@Configuration
@EnableKafka
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaListenerConfig {

    public KafkaListenerConfig() {
        log.info("Kafka监听器配置已启用");
    }
}
