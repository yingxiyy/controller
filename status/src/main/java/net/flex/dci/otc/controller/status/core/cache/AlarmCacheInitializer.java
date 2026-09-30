package net.flex.dci.otc.controller.status.core.cache;

import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.controller.status.counter.CounterManager;
import org.springframework.stereotype.Component;

/**
 * 2026/7/6
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class AlarmCacheInitializer {

    private final CounterManager counterManager;

    @PostConstruct
    public void clearCurrentAlarmHotCache() {
        log.debug("alarm cache initializer");
        counterManager.clearAlarmCache();
    }
}
