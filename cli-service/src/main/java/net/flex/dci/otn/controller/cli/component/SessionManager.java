package net.flex.dci.otn.controller.cli.component;

import net.flex.dci.otn.controller.cli.component.SessionManagerImpl.SessionInfo;
import net.flex.dci.otn.controller.cli.component.SessionManagerImpl.SessionType;
import org.springframework.web.socket.WebSocketSession;

/**
 *
 * 2025/9/20
 *
 * @author musa
 * @version 1.0
 **/
public interface SessionManager {

    void registerSession(SessionType type, Object session, String sessionId);

    void updateSessionActivity(String sessionId);

    SessionInfo getSessionInfo(String sessionId);

    void removeSession(String sessionId);

    void cleanupExpiredSessions();

    int getActiveSession();

    void markSessionAsDisconnected(String sessionId, long gracePeriodMiles);

    boolean tryReconnect(String sessionId, WebSocketSession newWebsocketSession);

    void linkSessions(String sessionId, String sshSessionId);
}
