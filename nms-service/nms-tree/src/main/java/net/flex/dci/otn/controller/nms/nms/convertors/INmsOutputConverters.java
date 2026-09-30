package net.flex.dci.otn.controller.nms.nms.convertors;

import java.util.List;

/**
 * @version 1.0
 * @date 2022/3/23 10:30
 */
public interface INmsOutputConverters<T, R> {

    List<T> convert2NmsOutput(List<R> nodeList);

    default List<T> convert2FullNmsOutput(List<R> nodeList) {
        return null;
    }
}
