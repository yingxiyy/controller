package net.flex.dci.otn.controller.schedule.task;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.mdoel.schedule.MoSchedule;
import net.flex.dci.otc.serialization.JsonUtil;

@Slf4j
public abstract class AbstractTaskImpl implements TaskProcessor {

    static private JsonUtil jsonUtil = null;
    static private TaskFactory taskFactory = null;

    public static TaskFactory getTaskFactory() {
        if (taskFactory == null) {
            taskFactory = SpringBeanFinder.getBean(TaskFactory.class);
        }
        return taskFactory;
    }

    public static JsonUtil getJsonUtil() {
        if (jsonUtil == null) {
            jsonUtil = SpringBeanFinder.getBean(JsonUtil.class);
        }
        return jsonUtil;
    }

//    protected abstract void addProcessor();

    /**
     * do action based on taskData
     *
     * @return
     */
    @Override
    public void process(MoSchedule mo) {
        log.debug(String.format("magic %s (%s), taskInfo: %s", Thread.currentThread().getName(),
                mo.getId(), mo.getName()));
    }

}
