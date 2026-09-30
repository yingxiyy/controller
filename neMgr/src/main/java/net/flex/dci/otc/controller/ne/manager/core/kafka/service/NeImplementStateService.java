package net.flex.dci.otc.controller.ne.manager.core.kafka.service;

import java.util.Map;

/**
 * 2025/6/21
 *
 * @author musa
 * @version 1.0
 **/
public interface NeImplementStateService {

    void handleStateChange(String neId, String implementState);

    void handleStateChanges(Map<String, String> latestStates);
}
