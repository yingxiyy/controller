package net.flex.dci.otc.controller.status.core.calculator.align;

import java.util.List;

/**
 * @version 1.0
 * @date 2022/4/7 15:24
 */
public interface IAlignStateCalculator<T, V> {

    T calculateAlignState(List<V> objects);

}
