package net.flex.dci.otn.controller.cli.service;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.cli.component.DeviceSessionFactory;
import net.flex.dci.otn.controller.cli.component.RequestValidator;
import net.flex.dci.otn.controller.cli.dto.ConnectRequest;
import net.flex.dci.otn.controller.cli.dto.ConnectSessionInfo;
import org.springframework.stereotype.Component;

/**
 *
 * 2025/9/16
 *
 * @author musa
 * @version 1.0
 **/
@Slf4j
@Component
@RequiredArgsConstructor
public class CliServiceImpl implements CliService {

    private final RequestValidator requestValidator;

    private final DeviceSessionFactory deviceSessionFactory;

    @Override
    public ConnectSessionInfo connect(ConnectRequest connectRequest) throws IOException {
        requestValidator.validateConnectRequest(connectRequest);
        log.debug("connect cli host:{} port:{} user:{}", connectRequest.getHost(),
                connectRequest.getPort(), connectRequest.getUsername());
        // 工厂统一选择设备 SSH 或服务器本机 Bash，Service 保持原有单一调用入口。
        String sessionId = deviceSessionFactory.addSession(connectRequest.getHost(),
                connectRequest.getPort(), connectRequest.getUsername(),
                connectRequest.getPassword());
        return ConnectSessionInfo.builder().sessionInfo(sessionId).build();
    }
}
