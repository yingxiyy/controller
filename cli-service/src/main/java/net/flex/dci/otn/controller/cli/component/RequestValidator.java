package net.flex.dci.otn.controller.cli.component;

import net.flex.dci.otn.controller.cli.dto.ConnectRequest;

/**
 *
 * @version 1.0
 * @date 9/17/2025 1:57 PM
 */
public interface RequestValidator {

    void validateConnectRequest(ConnectRequest connectRequest);
}
