package net.flex.dci.otc.controller.ne.manager.core.filter;

import java.util.List;

/**
 * @version 1.0
 * @date 2022/5/7 15:51
 */

public interface Filter<T, R> {

    List<T> filter(List<T> elements);

    default R filterNoneBusinessEquip(List<T> source, List<T> target) {
        return null;
    }
}
