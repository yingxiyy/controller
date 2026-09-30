package net.flex.dci.otn.controller.cli.component;

import static net.flex.dci.otn.controller.cli.utils.CliServiceUtils.getSessionIdFromSSHId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.cli.component.SessionManagerImpl.SessionInfo.SessionStatus;
import net.flex.dci.otn.controller.cli.ssh.TerminalSession;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

/**
 *
 * 2025/9/20
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class SessionManagerImpl implements SessionManager {

    private static final long SESSION_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30);
    private final DeviceSessionFactory deviceSessionFactory;
    private final Map<String, SessionInfo> activeSessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    @Override
    public void registerSession(SessionType type, Object session, String sessionId) {
        SessionInfo sessionInfo = new SessionInfo(type, session, sessionId);
        activeSessions.put(sessionId, sessionInfo);
        log.info("Registered new {} session:{}", type, sessionId);
    }

    @Override
    public void updateSessionActivity(String sessionId) {
        SessionInfo sessionInfo = activeSessions.get(sessionId);
        if (sessionInfo != null) {
            sessionInfo.updateLastActivity();
            log.debug("Update activity for session:{}", sessionId);
        }

    }

    @Override
    public SessionInfo getSessionInfo(String sessionId) {
        return activeSessions.get(sessionId);
    }

    @Override
    public void removeSession(String sessionId) {
        SessionInfo primaryInfo = activeSessions.remove(sessionId);
        if (primaryInfo != null) {
            performFinalCleanup(primaryInfo);
            String linkedSessionId = primaryInfo.linkedSessionId;
            if(linkedSessionId!=null) {
                SessionInfo linkedSessionInfo = activeSessions.remove(linkedSessionId);
                if(linkedSessionInfo!=null) {
                    performFinalCleanup(linkedSessionInfo);
                    log.info("Also cleaned up linked session: {}", linkedSessionId);
                }
                primaryInfo.setLinkedSessionId(null);
            }
            log.info("Session removed and cleaned up:{}", sessionId);
        }
    }


    private void performFinalCleanup(SessionInfo sessionInfo) {
        try {
            Object session = sessionInfo.getSession();
            sessionInfo.cancelPendingCleanupTask();
            if (session instanceof TerminalSession) {
                String realSessionId = getSessionIdFromSSHId(sessionInfo.sessionId);
                deviceSessionFactory.removeSession(realSessionId);
                log.debug("Cleaned up terminal session for:{}", sessionInfo.getSessionId());
            } else if (session instanceof WebSocketSession) {
                WebSocketSession webSocketSession = (WebSocketSession) session;
                if (webSocketSession.isOpen()) {
                    webSocketSession.close();
                }
                log.debug("Cleaned up WebSocketSession for:{}", sessionInfo.getSessionId());
            }
        } catch (Exception ex) {
            log.error("Error cleaning up session resources for {}:{}", sessionInfo.getSessionId(),
                    ex.getMessage(), ex);
        }
    }


    @Scheduled(fixedRate = 5 * 60 * 1000)
    @Override
    public void cleanupExpiredSessions() {
        log.debug("Starting  cleanup of  expired sessions");
        long currentTimestamp = System.currentTimeMillis();
        int cleanedCount = 0;
        for (Map.Entry<String, SessionInfo> entry : activeSessions.entrySet()) {
            SessionInfo sessionInfo = entry.getValue();
            if (sessionInfo.getStatus()== SessionStatus.ACTIVE&&currentTimestamp - sessionInfo.getLastActivityTime() > SESSION_TIMEOUT_MS) {
                log.info("Session {} expired due to inactivity,cleaning up",
                        sessionInfo.getSessionId());
                removeSession(sessionInfo.getSessionId());
                cleanedCount++;
            }
        }
        if (cleanedCount > 0) {
            log.info("Clean up {} expired session", cleanedCount);
        }
    }

    @Override
    public int getActiveSession() {
        return activeSessions.size();
    }

    @Override
    public void markSessionAsDisconnected(String sessionId, long gracePeriodMs) {
        log.debug("Marking session as disconnected:{} ",sessionId);
        SessionInfo sessionInfo = activeSessions.get(sessionId);
        if(sessionInfo!=null&&sessionInfo.getStatus() == SessionStatus.ACTIVE) {
            sessionInfo.setStatus(SessionStatus.DISCONNECTED);
            sessionInfo.cancelPendingCleanupTask();
            ScheduledFuture<?> cleanupTask = scheduler.schedule(()->{
                log.info("Grace period ended,executing final cleanup for session:{}",sessionId);
                this.removeSession(sessionId);
            },gracePeriodMs,TimeUnit.MILLISECONDS);
            sessionInfo.setCleanupTask(cleanupTask);
            log.info("Session [{}] is now in DISCONNECTED state, will be cleaned up in {} ms.", sessionId, gracePeriodMs);
        }else {
            log.warn("Cannot mark session as disconnected. Session not found or not active: {}", sessionId);
        }
    }

    @Override
    public boolean tryReconnect(String sessionId, WebSocketSession newWebsocketSession) {
        SessionInfo sessionInfo = activeSessions.get(sessionId);
        if(sessionInfo!=null&&sessionInfo.getStatus()==SessionStatus.DISCONNECTED) {
            sessionInfo.cancelPendingCleanupTask();
            sessionInfo.setStatus(SessionStatus.ACTIVE);
            sessionInfo.setSession(newWebsocketSession);
            sessionInfo.updateLastActivity();
            log.info("Session reconnected successfully:{}",sessionId);
            return true;
        }
        return false;
    }

    @Override
    public void linkSessions(String sessionId, String sshSessionId) {
        SessionInfo sessionInfo = activeSessions.get(sessionId);
        SessionInfo sshSessionInfo = activeSessions.get(sshSessionId);
        if (sessionInfo != null && sshSessionInfo != null) {
            sessionInfo.setLinkedSessionId(sshSessionId);
            sshSessionInfo.setLinkedSessionId(sessionId);
            log.debug("Linked sessions: {} <-> {}", sessionId, sshSessionId);
        }
    }


    /**
     * 会话类型枚举
     */
    public enum SessionType {
        WEB_SOCKET,
        SSH_SESSION
    }

    @Getter
    public static class SessionInfo {

        public enum SessionStatus {
            NEW,
            ACTIVE,
            DISCONNECTED,
            EXPIRED
        }

        private final SessionType type;
        private final String sessionId;

        @Setter
        private String linkedSessionId;
        @Setter
        private  Object session;
        private long lastActivityTime;

        @Setter
        private SessionStatus status;
        @Setter
        private ScheduledFuture<?> cleanupTask;

        public SessionInfo(SessionType type, Object session, String sessionId) {
            this.session = session;
            this.type = type;
            this.sessionId = sessionId;
            this.lastActivityTime = System.currentTimeMillis();
            this.status = SessionStatus.ACTIVE;
            this.cleanupTask = null;
            this.linkedSessionId = null;
        }

        public void updateLastActivity() {
            this.lastActivityTime = System.currentTimeMillis();
        }

        public void cancelPendingCleanupTask(){
            if(this.cleanupTask!=null&&!this.cleanupTask.isDone()) {
              this.cleanupTask.cancel(true);
              log.info("Cancelled pending cleanup task for session: {}",sessionId);
            }
            this.cleanupTask = null;
        }
    }
}
