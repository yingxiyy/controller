package net.flex.dci.otn.controller.cli.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * 运行在 cli-service 当前操作系统账户下的本机交互式 Bash 会话。
 */
@Slf4j
public class LocalTerminalSession implements TerminalSession {

    private static final long PROCESS_STOP_TIMEOUT_SECONDS = 1L;

    private final Process process;
    private final OutputStream inputStream;
    private final InputStream outputStream;
    private final InputStream errorStream;
    private final Object writeLock = new Object();

    public LocalTerminalSession() throws IOException {
        this(createProcessBuilder().start());
    }

    LocalTerminalSession(Process process) {
        this.process = process;
        this.inputStream = process.getOutputStream();
        this.outputStream = process.getInputStream();
        this.errorStream = process.getErrorStream();
    }

    /**
     * util-linux script 为 Bash 分配真正的 Linux PTY，不引入额外服务或 Java 原生依赖。
     */
    static ProcessBuilder createProcessBuilder() {
        ProcessBuilder processBuilder = new ProcessBuilder("/usr/bin/script", "-qfc",
                "/bin/bash -il", "/dev/null");
        processBuilder.environment().put("TERM", "xterm-256color");
        return processBuilder;
    }

    @Override
    public boolean isConnected() {
        return process.isAlive();
    }

    @Override
    public InputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public InputStream getErrorStream() {
        return errorStream;
    }

    @Override
    public void writeTerminalData(String terminalData) throws IOException {
        synchronized (writeLock) {
            if (!process.isAlive()) {
                throw new IOException("Local terminal process is not running");
            }
            inputStream.write(terminalData.getBytes(StandardCharsets.UTF_8));
            inputStream.flush();
        }
    }

    @Override
    public void resizeTerminal(int cols, int rows) throws IOException {
        if (cols <= 0 || rows <= 0) {
            throw new IOException("Terminal dimensions must be positive");
        }
        // script 已提供 PTY；Java 8 标准库没有安全的 ioctl 接口，客户端仍按窗口尺寸渲染。
    }

    @Override
    public void close() {
        synchronized (writeLock) {
            try {
                inputStream.close();
            } catch (IOException ex) {
                log.debug("Failed to close local terminal input stream: {}", ex.getMessage());
            }
        }
        process.destroy();
        try {
            if (!process.waitFor(PROCESS_STOP_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }
}
