package net.flex.dci.otn.controller.cli.component;

import java.io.IOException;
import java.net.SocketException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.cli.ssh.DeviceSession;
import net.flex.dci.otn.controller.cli.ssh.LocalTerminalSession;
import net.flex.dci.otn.controller.cli.ssh.TerminalSession;
import net.schmizz.sshj.userauth.UserAuthException;
import org.springframework.stereotype.Component;

/**
 *
 * @version 1.0
 * @date 9/17/2025 1:49 PM
 */
@Component
@Slf4j
@AllArgsConstructor
public class DeviceSessionFactoryImpl implements DeviceSessionFactory {

    private static final String LOCAL_TERMINAL_USERNAME = "@@@TTY1";
    private static final String LOCAL_TERMINAL_PASSWORD = "TTY1@@@";

    private final CliSessionCacheHandler cliSessionCacheHandler;

    private final Map<String, TerminalSession> sessionMap = new ConcurrentHashMap<>();


    @Override
    public String addSession(String host, int port, String username, String password) {
        boolean localTerminal = isLocalTerminalCredential(username, password);
        try {
            if (localTerminal) {
                // 特殊凭据由会话工厂识别，本机 Bash 继承 cli-service 的操作系统账户权限。
                log.info("start local server terminal session");
                return recordSession(new LocalTerminalSession());
            }
            log.info("start to connect to device cli ip:{} port:{}", host, port);
            log.debug("start to connect to device cli ip:{} port:{} user:{}", host, port,
                    username);
            DeviceSession deviceSession = new DeviceSession(host, port, username, password);
            return recordSession(deviceSession);
        } catch (Exception ex) {
            String reason = "Unknown error";
            if (localTerminal) {
                reason = "Failed to start local server terminal: " + ex.getMessage();
            } else if (ex instanceof UserAuthException) {
                reason = "Authentication failed, please check username and password";
            } else if (ex instanceof SocketException) {
                reason = "Network connection failed, please check network settings";
            } else if (ex instanceof IOException) {
                reason = "IO exception occurred, operation failed";
            } else {
                reason = ex.getMessage(); // Use original message as fallback
            }
            log.error("failed to create terminal session host:{} port:{} localTerminal:{} reason:{}",
                    host, port, localTerminal, reason, ex);
            throw new CommonException(CommonExceptionType.DEVICE_ERROR, reason);
        }
    }

    static boolean isLocalTerminalCredential(String username, String password) {
        return false;
          //规避合规风险，关闭local terminal 入口
//        return LOCAL_TERMINAL_USERNAME.equals(username)
//                && LOCAL_TERMINAL_PASSWORD.equals(password);
    }

    private String recordSession(TerminalSession terminalSession) {
        String sessionId = UUID.randomUUID().toString();
        sessionMap.put(sessionId, terminalSession);
        cliSessionCacheHandler.recordCache(sessionId);
        return sessionId;
    }


    @Override
    public TerminalSession getSession(String sessionId) {
        log.debug("get session by sessionId:{}", sessionId);
        return sessionMap.get(sessionId);
    }

    @Override
    public void removeSession(String sessionId) throws IOException {
        log.debug("remove session,session id:{}", sessionId);
        TerminalSession terminalSession = sessionMap.get(sessionId);
        if (terminalSession != null) {
            terminalSession.close();
            sessionMap.remove(sessionId);
        }
    }

    @Override
    public void cleanupAllSessions() {
        log.debug("cleanup all current sessions");
        for (Map.Entry<String, TerminalSession> entry : sessionMap.entrySet()) {
            String sessionId = entry.getKey();
            TerminalSession terminalSession = entry.getValue();
            terminalSession.close();
            cliSessionCacheHandler.removeCache(sessionId);
        }
        sessionMap.clear();
        log.info("cleanup all current sessions");
    }


}
