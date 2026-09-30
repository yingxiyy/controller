package net.flex.dci.otn.controller.cli.ssh;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import net.flex.dci.otn.controller.cli.component.DeviceSessionFactory;
import net.flex.dci.otn.controller.cli.component.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.test.util.ReflectionTestUtils;

class CliWebSocketHandlerTest {

    private TerminalSession terminalSession;
    private WebSocketSession webSocketSession;
    private CliWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        DeviceSessionFactory deviceSessionFactory = mock(DeviceSessionFactory.class);
        SessionManager sessionManager = mock(SessionManager.class);
        terminalSession = mock(TerminalSession.class);
        webSocketSession = mock(WebSocketSession.class);
        handler = new CliWebSocketHandler(deviceSessionFactory, sessionManager);

        when(webSocketSession.getUri()).thenReturn(
                URI.create("ws://localhost/cli/ws?sessionId=session-1"));
        when(deviceSessionFactory.getSession("session-1")).thenReturn(terminalSession);
        when(terminalSession.isConnected()).thenReturn(true);
    }

    @Test
    void shouldForwardTerminalInputWithoutTrimmingOrAddingNewline() throws Exception {
        // Tab、方向键和首尾空格必须保持 xterm 产生的原始终端语义。
        String terminalData = " show\t\u001B[A ";

        handler.handleTextMessage(webSocketSession, new TextMessage(terminalData));

        verify(terminalSession, timeout(1000)).writeTerminalData(terminalData);
    }

    @Test
    void shouldResizeTerminalWithoutForwardingControlJson() throws Exception {
        handler.handleTextMessage(webSocketSession,
                new TextMessage("{\"type\":\"resize\",\"cols\":120,\"rows\":40}"));

        verify(terminalSession, timeout(1000)).resizeTerminal(120, 40);
        verify(terminalSession, never()).writeTerminalData(anyString());
    }

    @Test
    void shouldShutdownSharedIoExecutorWhenHandlerIsDestroyed() {
        ExecutorService ioExecutor = (ExecutorService) ReflectionTestUtils.getField(handler,
                "ioExecutor");
        assertFalse(ioExecutor.isShutdown());

        handler.shutdownIoExecutor();

        assertTrue(ioExecutor.isShutdown());
    }
}
