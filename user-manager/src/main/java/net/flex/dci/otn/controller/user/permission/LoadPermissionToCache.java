package net.flex.dci.otn.controller.user.permission;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.user.permission.authorization.PermissionRoleLoadHandler;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/25 14:02
 */
@Slf4j
@Component
@Order(value = 1)
@RequiredArgsConstructor
public class LoadPermissionToCache implements ApplicationRunner {

    private final PermissionRoleLoadHandler permissionRoleLoadHandler;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.debug("back ground to load the role permission map for the redis");
        permissionRoleLoadHandler.loadThePermissionRole();
    }
}
