package net.flex.dci.otc.controller.ne.manager.service;

/**
 * @version 1.0
 * @date 7/30/2023 4:25 PM
 */
public interface NeSupervisionManager {


    /**
     * supervise ne data
     *
     * @param input
     * @return
     */
    String superviseNe(String input);

    /**
     * stop supervise ne
     *
     * @param input
     * @return
     */
    String stopSuperviseNe(String input);
}
