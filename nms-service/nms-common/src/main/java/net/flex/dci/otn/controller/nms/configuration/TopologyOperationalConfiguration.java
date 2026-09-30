package net.flex.dci.otn.controller.nms.configuration;

import com.alibaba.fastjson.JSON;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.nms.properties.topo.TopologyAntPathMatcherMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * @version 1.0
 * @date 2022/4/27 13:45
 */
@Configuration
@Slf4j
public class TopologyOperationalConfiguration {

    @Bean
    public TopologyAntPathMatcherMap loadTopologyAntPathMatcherMap() throws IOException {
        Resource resource = new ClassPathResource("TopologyPathMatcher.json");
        InputStream inputStream = resource.getInputStream();
        TopologyAntPathMatcherMap topologyAntPathMatcherMap = JSON.parseObject(inputStream,
                TopologyAntPathMatcherMap.class);
        return topologyAntPathMatcherMap;
    }

}
