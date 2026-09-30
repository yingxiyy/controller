package net.flex.dci.otn.controller.cli.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.flex.dci.otn.controller.cli.component.DeviceSessionFactory;
import net.flex.dci.otn.controller.cli.component.RequestValidator;
import net.flex.dci.otn.controller.cli.dto.ConnectRequest;
import net.flex.dci.otn.controller.cli.dto.ConnectSessionInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CliServiceImplTest {

    private RequestValidator requestValidator;
    private DeviceSessionFactory deviceSessionFactory;
    private CliServiceImpl cliService;

    @BeforeEach
    void setUp() {
        requestValidator = mock(RequestValidator.class);
        deviceSessionFactory = mock(DeviceSessionFactory.class);
        cliService = new CliServiceImpl(requestValidator, deviceSessionFactory);
    }

    @Test
    void shouldPassExactServerCredentialPairToSessionFactory() throws Exception {
        ConnectRequest request = connectRequest("@@@TTY1", "TTY1@@@");
        when(deviceSessionFactory.addSession("192.0.2.10", 22,
                "@@@TTY1", "TTY1@@@")).thenReturn("local-session");

        ConnectSessionInfo result = cliService.connect(request);

        assertEquals("local-session", result.getSessionInfo());
        verify(requestValidator).validateConnectRequest(request);
        verify(deviceSessionFactory).addSession("192.0.2.10", 22,
                "@@@TTY1", "TTY1@@@");
    }

    @Test
    void shouldKeepDeviceSshWhenOnlyOneServerCredentialValueMatches() throws Exception {
        ConnectRequest request = connectRequest("@@@TTY1", "device-password");
        when(deviceSessionFactory.addSession("192.0.2.10", 22,
                "@@@TTY1", "device-password")).thenReturn("device-session");

        ConnectSessionInfo result = cliService.connect(request);

        assertEquals("device-session", result.getSessionInfo());
        verify(deviceSessionFactory).addSession("192.0.2.10", 22,
                "@@@TTY1", "device-password");
    }

    private ConnectRequest connectRequest(String username, String password) {
        ConnectRequest request = new ConnectRequest();
        request.setHost("192.0.2.10");
        request.setPort(22);
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }
}
