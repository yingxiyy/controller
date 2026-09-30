package net.flex.dci.otn.controller.cli.service;

import java.io.IOException;
import net.flex.dci.otn.controller.cli.dto.ConnectRequest;
import net.flex.dci.otn.controller.cli.dto.ConnectSessionInfo;

/**
 *
 * 2025/9/16
 *
 * @author musa
 * @version 1.0
 **/

public interface CliService {

    ConnectSessionInfo connect(ConnectRequest connectRequest) throws IOException;
}
