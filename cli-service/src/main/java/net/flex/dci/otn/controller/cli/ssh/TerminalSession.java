package net.flex.dci.otn.controller.cli.ssh;

import java.io.IOException;
import java.io.InputStream;

/**
 * Web 终端统一会话接口，设备 SSH 和服务器本机 Bash 共用同一条 WebSocket 通路。
 */
public interface TerminalSession {

    boolean isConnected();

    InputStream getOutputStream();

    InputStream getErrorStream();

    void writeTerminalData(String terminalData) throws IOException;

    void resizeTerminal(int cols, int rows) throws IOException;

    void close();
}
