package net.flex.dci.otn.controller.cli.component;

import java.io.IOException;
import net.flex.dci.otn.controller.cli.ssh.TerminalSession;

/**
 *
 * @version 1.0
 * @date 9/17/2025 1:48 PM
 */
public interface DeviceSessionFactory {

    String addSession(String host, int port, String username, String password) throws IOException;

    TerminalSession getSession(String sessionId);

    void removeSession(String sessionId) throws IOException;

    void cleanupAllSessions();
}
