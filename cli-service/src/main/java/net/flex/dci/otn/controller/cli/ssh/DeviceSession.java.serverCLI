package net.flex.dci.otn.controller.cli.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.PTYMode;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.util.StringUtils;

/**
 * @version 1.0
 * @date 9/17/2025 10:40 AM
 */

@Slf4j
public class DeviceSession implements Serializable, TerminalSession {

    private final String host;
    private final int port;
    private final String user;
    private final String password;
    @Getter
    private boolean connected = false;
    private SSHClient sshClient;
    private Session.Shell shell;
    private OutputStream inputStream;
    @Getter
    private InputStream outputStream;
    @Getter
    private InputStream errorStream;
    private ScheduledExecutorService keepAliveScheduler;
    private final Object writeLock = new Object();

    public DeviceSession(String host, int port, String user, String password) throws IOException {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        connectSSH();
        startKeepAlive();
    }


    private void connectSSH() throws IOException {
        sshClient = new SSHClient();
        sshClient.addHostKeyVerifier(new PromiscuousVerifier());
        sshClient.connect(host, port);
        sshClient.authPassword(user, password);
        sshClient.getConnection().getKeepAlive().setKeepAliveInterval(30);//heartbeat
        Session session = sshClient.startSession();
        allocateEnhancedPTY(session);
        shell = session.startShell();
        synchronized (writeLock) {
            inputStream = shell.getOutputStream();
        }
        inputStream = shell.getOutputStream();//stdin
        outputStream = shell.getInputStream();//stdout
        errorStream = shell.getErrorStream();//stderr
        connected = true;
        log.info("SSH connection established to {}:{}", host, port);
    }

    private void allocateEnhancedPTY(Session session) {
        try {
            String[] preferredTermTypes = {
                    "xterm-256color",  // 最好的现代支持
                    "xterm",           // 标准 xterm
                    "vt220",           // 增强 VT
                    "vt100",           // 基本 VT（你的选择）
                    "ansi",            // 基本 ANSI
                    "linux"            // 控制台终端
            };
            for (String termType : preferredTermTypes) {
                try {
                    Map<PTYMode, Integer> ptyModes = new HashMap<>();
                    ptyModes.put(PTYMode.ECHO, 1);        // 本地回显
                    ptyModes.put(PTYMode.ICRNL, 1);       // 回车转换为换行
                    ptyModes.put(PTYMode.ONLCR, 1);       // 输出时换行转换为回车
                    session.allocatePTY(termType, 80, 24, 0, 0, ptyModes);
                    log.debug("Allocated {} PTY", termType);
                    return;
                } catch (Exception e) {
                    log.warn(
                            "{} allocation failed, proceeding without PTY", termType);
                }
            }

            session.allocateDefaultPTY();
            log.debug("Allocated default PTY as fallback");
        } catch (Exception ex) {
            log.error("PTY allocation failed: {}", ex.getMessage(), ex);
        }
    }

    private void startKeepAlive() {
        keepAliveScheduler = Executors.newSingleThreadScheduledExecutor();
        keepAliveScheduler.scheduleAtFixedRate(() -> {
            if (!connected) {
                return;
            }
            try {
                sendKeepAlive();
                log.debug("Keep-alive sent to :{}", host);
            } catch (IOException e) {
                log.warn("Keep-alive failed for {}:{} ", host, e.getMessage());
                connected = false;
                reconnect();
            }
        }, 25, 25, TimeUnit.SECONDS);
    }

    /**
     * reconnect
     */
    private synchronized void reconnect() {
        if (connected) {
            return;
        }
        log.info("Attempting to reconnect to {}:{}", host, port);
        try {
            close();
            connectSSH();
            log.info("Successfully reconnect to {}:{}", host, port);
        } catch (Exception e) {
            log.error("Reconnection failed for {}:{}, will retry: {}",
                    host, port, e.getMessage());
            // 30秒后重试
            keepAliveScheduler.schedule(this::reconnect, 30, TimeUnit.SECONDS);
        }

    }


    public void sendCommand(String command) throws IOException {
        log.debug("send command:{}", command);
        synchronized (writeLock) {
            if (inputStream == null) {
                throw new IOException("OutputStream is null");
            }
//                boolean shouldAddNewline = shouldAddNewline(command);
            String toSend = command;
//                if (shouldAddNewline && !command.endsWith("\n")) {
//                    toSend += "\n";
//                }
            log.info("Sending : {}",
                    escapeForLog(toSend));
            inputStream.write(toSend.getBytes(StandardCharsets.UTF_8));
            inputStream.flush();
        }
    }

    public void sendInteractiveChar(String interactiveChar) throws IOException {
        log.debug("send interactive char:{}", interactiveChar);
        synchronized (writeLock) {
            if (inputStream == null) {
                throw new IOException("OutputStream is null");
            }
            byte[] bytes = interactiveChar.getBytes(StandardCharsets.UTF_8);
            log.debug("send interactive byte(Hex):{}", bytesToHex(bytes));
            inputStream.write(bytes);
            inputStream.flush();
        }
    }

    /**
     * handle the terminal payload
     */
    @Override
    public void writeTerminalData(String terminalData) throws IOException {
        synchronized (writeLock) {
            if (inputStream == null) {
                throw new IOException("OutputStream is null");
            }
            inputStream.write(terminalData.getBytes(StandardCharsets.UTF_8));
            inputStream.flush();
        }
    }

    /**
     * resize the cli terminal
     */
    @Override
    public void resizeTerminal(int cols, int rows) throws IOException {
        if (cols <= 0 || rows <= 0) {
            throw new IOException("Terminal dimensions must be positive");
        }
        if (shell == null) {
            throw new IOException("SSH shell is null");
        }
        shell.changeWindowDimensions(cols, rows, 0, 0);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x ", b));
        }
        return sb.toString().trim();
    }

    private String escapeForLog(String str) {
        if (str == null) {
            return "null";
        }

        return str
                .replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\u0003", "[Ctrl+C]")
                .replace("\u0004", "[Ctrl+D]")
                .replace("\u001B", "[ESC]");
    }


    /**
     * @param command
     * @return
     */
    private boolean shouldAddNewline(String command) {
        if (!StringUtils.hasText(command)) {
            return true;
        }
        if (command.endsWith("\n")) {
            return false;
        }
        if (command.endsWith("\r")) {
            return true;
        }
        if (command.endsWith("\\t")) {
            return false;
        }

        if (command.length() == 1) {
            char c = command.charAt(0);
            // Tab、Enter、Esc、控制字符等
            return !(c == '\t' || c == '\u0003' || c == '\u0004' ||
                    c == '\u001B' || c < 32 || c == 127);
        }
        if (command.matches("^\\t+$")) {
            return false;
        }
        if (command.startsWith("\u001B[")) {
            return false;
        }

        return true;

    }

    @Override
    public void close() {
        connected = false;
        if (keepAliveScheduler != null) {
            keepAliveScheduler.shutdown();
        }
        try {
            shell.close();
        } catch (Exception e) {
            log.warn("Error closing shell:{}", e.getMessage(), e);
        }
        try {
            sshClient.disconnect();
        } catch (Exception e) {
            log.warn("Error closing ssh client:{}", e.getMessage(), e);
        }
    }

    public void sendKeepAlive() throws IOException {
        log.trace("Sending keep-alive to {}:{}", host, port);
        synchronized (writeLock) {
            if (inputStream != null) {
                inputStream.write(new byte[]{0});
                inputStream.flush();
            } else {
                throw new IOException("OutputStream is null");
            }
        }
    }


}
