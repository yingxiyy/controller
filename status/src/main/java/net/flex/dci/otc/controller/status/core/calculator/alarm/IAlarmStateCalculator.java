package net.flex.dci.otc.controller.status.core.calculator.alarm;

import java.util.List;

/**
 * @version 1.0
 * @date 2022/4/2 16:23
 */
public interface IAlarmStateCalculator<T, V> {

    default T calculate(V node) {
        return null;
    }

    default T calculateSiteCurrentState(String node) {
        return null;
    }

    default T calculate(String id) {
        return null;
    }

    default List<T> calculateRefs(String id) {
        return null;
    }

    default List<T> calculateViewNodeAlarms(List<String> viewNodeIds) {
        return null;
    }
    
}
