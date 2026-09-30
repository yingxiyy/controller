package net.flex.dci.otn.controller.auth.security.service;

import javax.servlet.http.HttpServletRequest;
import net.flex.dci.otn.controller.auth.dto.LoginUserDto;

/**
 * @version 1.0
 * @date 2022/4/17 21:28
 */

public interface AuthService {

    String login(LoginUserDto loginDto);

    void logout(HttpServletRequest httpServletRequest);
}
