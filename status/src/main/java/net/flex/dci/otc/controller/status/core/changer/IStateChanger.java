package net.flex.dci.otc.controller.status.core.changer;

import java.util.List;

/**
 * @version 1.0
 * @date 2022/4/4 14:28
 */
public interface IStateChanger<T> {

    void changeState(T state);

    default void changeBatchState(List<T> states) {

    }
}
