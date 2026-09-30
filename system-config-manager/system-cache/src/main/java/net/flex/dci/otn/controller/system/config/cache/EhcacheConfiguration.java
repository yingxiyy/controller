package net.flex.dci.otn.controller.system.config.cache;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.system.config.cache.util.EhcacheUtil;
import net.flex.dci.otn.topology.cache.EnableDciCache;
import net.sf.ehcache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @version 1.0
 * @date 2022/3/13 22:53
 */
@Configuration
@Slf4j
@EnableDciCache
public class EhcacheConfiguration {

    private final static String CACHE_FILE = "ehcache.xml";

    @Bean
    public CacheManager cacheManager() {
        log.info("----init cache manager---------");
        CacheManager cacheManager = CacheManager.create(
                Thread.currentThread().getContextClassLoader().getResource(CACHE_FILE));
        EhcacheUtil.setCacheManager(cacheManager);
        return cacheManager;
    }

}
