package net.flex.dci.otc.controller.ne.manager.components.consistent;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 2025/6/11
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
public class MapperConsistentImpl implements MapperConsistent {

    public final List<ConsistentCleaner> consistentCleaners;

    public MapperConsistentImpl(List<ConsistentCleaner> consistentCleaners) {
        this.consistentCleaners = consistentCleaners;
    }


    @Override
    public void cleanInConsistentMapper() {
        log.info("clean inconsistent mapper from db and zookeeper");
        consistentCleaners.forEach(ConsistentCleaner::checkAndCleanInconsistent);
        log.info("finish to clean inconsistent mapper");
    }


}
