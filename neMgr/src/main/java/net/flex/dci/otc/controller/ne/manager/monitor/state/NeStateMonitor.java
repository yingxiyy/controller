package net.flex.dci.otc.controller.ne.manager.monitor.state;

import net.flex.dci.otc.controller.ne.manager.dto.NeRegisteredInfo;

/**
 * @version 1.0
 * @date 6/12/2025 1:57 PM
 */
public interface NeStateMonitor {

    void checkAndSynchronizeState();

    void checkAndSynchronizeStateBySlice(int sliceIndex, int sliceCount);

    void checkAndSynchronizeStateBySlice(int sliceIndex, int sliceCount,
            NeRegisteredInfo neRegisteredInfo);
}
