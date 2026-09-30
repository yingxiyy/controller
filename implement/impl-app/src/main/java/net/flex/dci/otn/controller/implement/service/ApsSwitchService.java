package net.flex.dci.otn.controller.implement.service;

import javax.servlet.http.HttpServletRequest;

/**
 * 2025/8/12
 *
 * @author musa
 * @version 1.0
 **/
public interface ApsSwitchService {

    String batchApsSwitch(String input, HttpServletRequest request);

    String apsSwitchControl(String input, HttpServletRequest request);

    String restoreApsPath(String input, HttpServletRequest request);
}
