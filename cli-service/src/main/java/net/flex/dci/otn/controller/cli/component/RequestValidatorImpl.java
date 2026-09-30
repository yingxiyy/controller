package net.flex.dci.otn.controller.cli.component;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.cli.dto.ConnectRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 *
 * @version 1.0
 * @date 9/17/2025 1:57 PM
 */
@Component
@Slf4j
public class RequestValidatorImpl implements RequestValidator {


    @Override
    public void validateConnectRequest(ConnectRequest connectRequest) {
        log.debug("validate the connect request :{}", connectRequest);
        String host = connectRequest.getHost();
        if (!StringUtils.hasText(host)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "connect device cli host ip should not be null");
        }

        String username = connectRequest.getUsername();
        if (!StringUtils.hasText(username)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the connect device cli username should not be null");
        }
        String password = connectRequest.getPassword();
        if (!StringUtils.hasText(password)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the connect device cli password should not be null");
        }

    }
}
