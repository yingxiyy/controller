package net.flex.dci.otn.controller.cli.ssh;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LocalTerminalSessionTest {

    @Test
    void shouldForwardXtermInputToLocalShellWithoutModification() throws Exception {
        StubProcess process = new StubProcess();
        LocalTerminalSession session = new LocalTerminalSession(process);
        String terminalData = " cd /tmp\t\r";

        session.writeTerminalData(terminalData);

        assertArrayEquals(terminalData.getBytes(StandardCharsets.UTF_8),
                process.stdin.toByteArray());
    }

    @Test
    void shouldDestroyLocalShellWhenSessionCloses() {
        StubProcess process = new StubProcess();
        LocalTerminalSession session = new LocalTerminalSession(process);
        assertTrue(session.isConnected());

        session.close();

        assertTrue(process.destroyCalled);
        assertFalse(session.isConnected());
    }

    @Test
    void shouldStartLoginBashThroughLinuxPtyUtility() {
        ProcessBuilder processBuilder = LocalTerminalSession.createProcessBuilder();

        assertArrayEquals(new String[]{"/usr/bin/script", "-qfc", "/bin/bash -il",
                        "/dev/null"}, processBuilder.command().toArray(new String[0]));
        assertTrue("xterm-256color".equals(processBuilder.environment().get("TERM")));
    }

    private static class StubProcess extends Process {

        private final ByteArrayOutputStream stdin = new ByteArrayOutputStream();
        private final InputStream stdout = new ByteArrayInputStream(new byte[0]);
        private final InputStream stderr = new ByteArrayInputStream(new byte[0]);
        private boolean alive = true;
        private boolean destroyCalled;

        @Override
        public OutputStream getOutputStream() {
            return stdin;
        }

        @Override
        public InputStream getInputStream() {
            return stdout;
        }

        @Override
        public InputStream getErrorStream() {
            return stderr;
        }

        @Override
        public int waitFor() {
            alive = false;
            return 0;
        }

        @Override
        public int exitValue() {
            if (alive) {
                throw new IllegalThreadStateException("process is still running");
            }
            return 0;
        }

        @Override
        public void destroy() {
            destroyCalled = true;
            alive = false;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }
    }
}
