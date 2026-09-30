package net.flex.dci.otn.controller.resource.statistic.core.manager;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 2026/4/12
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
public abstract class AbstractElementInventoryManager<T> {

    @Autowired
    @Qualifier("inventoryExecutor")
    protected ThreadPoolTaskExecutor asyncExecutor;

    public abstract T getInventoryDetail(String id);


    public abstract List<T> getInventoryDetails(List<String> ids);

    public abstract List<T> getInventoryDetails(List<String> ids, List<String> subnetIds);
}
