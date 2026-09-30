package net.flex.dci.otn.controller.cli.ssh;

import static net.flex.dci.otn.controller.cli.utils.CliServiceUtils.SESSION_ID;
import static net.flex.dci.otn.controller.cli.utils.CliServiceUtils.getSshSessionId;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.cli.component.DeviceSessionFactory;
import net.flex.dci.otn.controller.cli.component.SessionManager;
import net.flex.dci.otn.controller.cli.component.SessionManagerImpl.SessionInfo;
import net.flex.dci.otn.controller.cli.component.SessionManagerImpl.SessionType;
import net.flex.dci.otn.controller.cli.dto.WsFrontMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * @version 1.0
 * @date 9/17/2025 3:09 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CliWebSocketHandler extends TextWebSocketHandler {

    private static final long GRACE_PERIOD_MILES = 60 * 1000;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ConcurrentHashMap<String, BlockingQueue<String>> messageQueues = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Boolean> isProcessing = new ConcurrentHashMap<>();

    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(5, new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName("ws-scheduler-" + counter.getAndIncrement());
                    t.setDaemon(true);
                    return t;
                }
            });
    private final ExecutorService ioExecutor =
            Executors.newCachedThreadPool(new ThreadFactory() {
                private final AtomicInteger counter = new AtomicInteger(0);

                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable);
                    thread.setName("cli-io-" + counter.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                }
            });
    //    private final ConcurrentHashMap<String, ReentrantLock> sessionLocks = new ConcurrentHashMap<>();
    private final DeviceSessionFactory deviceSessionFactory;
    private final SessionManager sessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("a connection established");
        String sessionId = getParam(session, SESSION_ID);
        if (null == sessionId) {
            log.error("web socket connect session id is null");
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid sessionId"));
            return;
        }
        boolean reconnected = sessionManager.tryReconnect(sessionId, session);
        if (reconnected) {
            log.info("Session resumed via reconnection:{}", sessionId);
//            sessionLocks.putIfAbsent(sessionId, new ReentrantLock());
            sendMessageWithQueue(session, sessionId,
                    "\r\n\u001B[33m↻ Session reconnected. Resuming...\u001B[0m\r\n");
            return;
        }
//        sessionLocks.put(sessionId, new ReentrantLock());
        sessionManager.registerSession(SessionType.WEB_SOCKET, session, sessionId);
        try {
            TerminalSession terminalSession = deviceSessionFactory.getSession(sessionId);
            if (null == terminalSession) {
                sendMessageWithQueue(session, sessionId,
                        "\r\n\u001B[31mERROR: Terminal session not found\u001B[0m\r\n");
                log.error("terminal session is not found session Id:{}", sessionId);
                scheduler.schedule(() -> {
                    try {
                        if (session.isOpen()) {
                            session.close(
                                    CloseStatus.NOT_ACCEPTABLE.withReason("Invalid sessionId"));
                        }
                    } catch (IOException e) {
                        log.error("Failed to close session", e);
                    }
                }, 100, TimeUnit.MILLISECONDS);

                return;
            }
            String sshSessionId = getSshSessionId(sessionId);
            sessionManager.registerSession(SessionType.SSH_SESSION, terminalSession,
                    sshSessionId);
            sessionManager.linkSessions(sessionId, sshSessionId);
            scheduler.schedule(() -> {
                listenStreamForXterm(terminalSession.getOutputStream(), session, sessionId);
                listenStreamForXterm(terminalSession.getErrorStream(), session, sessionId);
            }, 200, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            log.error("Failed to attach terminal session for {}:{}", sessionId, e.getMessage(), e);
            sendMessageWithQueue(session, sessionId,
                    "\r\n\u001B[31mERROR: Failed to attach terminal session: " + e.getMessage()
                            + "\u001B[0m\r\n");
            session.close(CloseStatus.SERVER_ERROR.withReason("failed to attach terminal"));
            sessionManager.removeSession(sessionId);
            return;
        }
        sessionManager.updateSessionActivity(getSshSessionId(sessionId));
        sessionManager.updateSessionActivity(sessionId);
        log.info("Websocket connection  established for session {}", sessionId);
        scheduler.schedule(() -> {
            String welcomeMsg = "\r\n\u001B[32m✓ Connected to terminal session: " + sessionId +
                    "\u001B[0m\r\n\r\n";
            sendMessageWithQueue(session, sessionId, welcomeMsg);
        }, 200, TimeUnit.MILLISECONDS);

    }


    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message)
            throws Exception {
        String sessionId = getParam(session, SESSION_ID);
        if (sessionId == null) {
            log.error("Received message without valid sessionId");
            return;
        }
        sessionManager.updateSessionActivity(sessionId);

        TerminalSession terminalSession = deviceSessionFactory.getSession(sessionId);
        if (terminalSession != null && terminalSession.isConnected()) {
            sessionManager.updateSessionActivity(getSshSessionId(sessionId));
            try {
                handleTerminalMessage(terminalSession, message.getPayload());
            } catch (IOException ex) {
                log.error("Failed to forward terminal data for session:{} {}",
                        sessionId, ex.getMessage(), ex);
                sendMessageWithQueue(session, sessionId,
                        "\r\n\u001B[31mERROR: Failed to write terminal: "
                                + ex.getMessage() + "\u001B[0m\r\n");
            }
        } else {
//            session.sendMessage(new TextMessage(
//                    "{\"type\":\"error\", \"message\":\"SSH connection not available\"}"));
            sendMessageWithQueue(session, sessionId,
                    "\r\n\u001B[31mERROR: Terminal connection not available\u001B[0m\r\n");
        }
    }


    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception)
            throws Exception {
        String sessionId = getParam(session, SESSION_ID);
        log.error("WebSocket transport error for session {}: {}", sessionId,
                exception.getMessage());

        if (sessionId != null) {
//            sessionManager.removeSession(sessionId);
//            scheduleSshSessionCleanup(sessionId);
            sessionManager.markSessionAsDisconnected(sessionId, GRACE_PERIOD_MILES);
        }
    }


    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = getParam(session, SESSION_ID);
        log.debug("connection closed for the session id:{}", sessionId);
        if (sessionId != null) {
            log.info("Websocket connection closed for session:{},reason:{}", sessionId, status);
//            sessionManager.removeSession(sessionId);
//            scheduleSshSessionCleanup(sessionId);
            sessionManager.markSessionAsDisconnected(sessionId, GRACE_PERIOD_MILES);
//            sessionLocks.remove(sessionId);
            messageQueues.remove(sessionId);
            isProcessing.remove(sessionId);
        }

    }

    /**
     * handle terminal message
     */
    private void handleTerminalMessage(TerminalSession terminalSession, String payload)
            throws IOException {
        if (payload.startsWith("{") && payload.endsWith("}")) {
            try {
                WsFrontMessage frontMessage = OBJECT_MAPPER.readValue(payload,
                        WsFrontMessage.class);
                if ("resize".equals(frontMessage.getType())
                        && frontMessage.getCols() != null && frontMessage.getRows() != null) {
                    terminalSession.resizeTerminal(frontMessage.getCols(), frontMessage.getRows());
                    return;
                }
            } catch (IOException ex) {
                log.trace("Terminal payload is not a resize control message");
            }
        }
        terminalSession.writeTerminalData(payload);
    }

    private void scheduleSshSessionCleanup(String sessionId) {
        log.debug("schedule ssh session clean up,the session id:{}", sessionId);
        scheduler.schedule(() -> {
            try {
                log.debug("start to remove session task schedule ");
                SessionInfo sessionInfo = sessionManager.getSessionInfo(sessionId);
                if (null == sessionInfo) {
                    sessionManager.removeSession(getSshSessionId(sessionId));
                    log.info("Cleaned up SSH session for:{}", sessionId);
                }
            } catch (Exception ex) {
                log.error("Error during SSH session cleanup for:{} {}", sessionId, ex.getMessage(),
                        ex);
            }
        }, 30, TimeUnit.SECONDS);

    }


    private String getParam(WebSocketSession session, String key) {
        String sessionId = Arrays.stream(
                        Objects.requireNonNull(session.getUri()).getQuery().split("&"))
                .filter(p -> p.startsWith(key + "="))
                .map(p -> p.split("=")[1])
                .findFirst().orElse(null);

        //get sessionId from the header
        if (sessionId == null) {
            Map<String, Object> attributes = session.getAttributes();
            if (attributes.containsKey(key)) {
                sessionId = attributes.get(key).toString();
            }
        }
        return sessionId;
    }

    private void listenStreamForXterm(InputStream in, WebSocketSession session, String sessionId) {
        // 复用受生命周期管理的共享线程池，避免每条输出流遗留独立 Executor。
        ioExecutor.submit(() -> {
            try {
                byte[] buffer = new byte[5096];
                int len;
                while ((len = in.read(buffer)) != -1 && session.isOpen()) {
                    String msg = new String(buffer, 0, len, StandardCharsets.UTF_8);
                    sendMessageWithQueue(session, sessionId, msg);

                    if (!session.isOpen()) {
                        log.warn("WebSocket closed while sending response");
                        break;
                    }
                }
            } catch (IOException e) {
                if (e.getMessage() != null && e.getMessage().contains("Stream closed")) {
                    log.debug(" stream closed for session {}", sessionId);
                } else {
                    log.error("failed to read device  stream for session {}: {}",
                            sessionId, e.getMessage(), e);
                }
            } finally {
                messageQueues.remove(sessionId);
                isProcessing.remove(sessionId);
            }
        });
    }

    private void sendMessageWithQueue(WebSocketSession session, String sessionId, String msg) {
        BlockingQueue<String> queue = messageQueues.computeIfAbsent(sessionId,
                k -> new LinkedBlockingQueue<>());
        queue.offer(msg);
        if (isProcessing.putIfAbsent(sessionId, true) == null) {
            startQueueProcessor(session, sessionId, queue);
        }
    }

    private void startQueueProcessor(WebSocketSession session, String sessionId,
            BlockingQueue<String> queue) {
        ioExecutor.submit(() -> {
            try {
                while (session.isOpen() && !Thread.currentThread().isInterrupted()) {
                    String message = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (message != null) {
                        sendMessageDirectly(session, sessionId, message);
                    } else {
                        if (queue.isEmpty()) {
                            isProcessing.remove(sessionId);
                            break;
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Queue processor interrupted for session {}", sessionId);
            } finally {
                isProcessing.remove(sessionId);
            }
        });
    }

    private void sendMessageDirectly(WebSocketSession session, String sessionId, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new TextMessage(message));
            } else {
                log.warn("Session {} is closed,skip message:{}", sessionId, message);
            }
        } catch (IllegalStateException e) {
            if (e.getMessage() != null && e.getMessage().contains("TEXT_PARTIAL_WRITING")) {
                log.error("WebSocket in bad state for session {}, retrying after delay", sessionId);

                scheduler.schedule(() -> {
                    sendMessageWithQueue(session, sessionId, message);
                }, 50, TimeUnit.MILLISECONDS);

            } else {
                log.error("Failed to send message to session {}: {}", sessionId, e.getMessage(), e);
            }
        } catch (Exception ex) {
            log.error("Failed to send message to session {}: {}", sessionId, ex.getMessage(), ex);
        }
    }


    @PreDestroy
    void shutdownIoExecutor() {
        ioExecutor.shutdownNow();
    }


}
