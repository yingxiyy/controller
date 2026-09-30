package devicemaintenance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate配置类
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 配置RestTemplate Bean
     */
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        
        // 配置HTTP客户端超时设置
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000); // 10秒连接超时
        factory.setReadTimeout(30000);    // 30秒读取超时
        
        restTemplate.setRequestFactory(factory);
        
        return restTemplate;
    }
}
