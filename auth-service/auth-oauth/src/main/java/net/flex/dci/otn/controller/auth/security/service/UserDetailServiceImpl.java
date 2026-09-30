package net.flex.dci.otn.controller.auth.security.service;

import static net.flex.dci.otc.common.constants.AuthConstant.DEFAULT_USER_NAME;
import static net.flex.dci.otn.controller.auth.utils.Constants.DEFAULT_USER_PASSWORD;
import static net.flex.dci.otn.controller.auth.utils.Constants.DEFAULT_USER_ROLE;

import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.auth.dto.UserDto;
import net.flex.dci.otn.controller.auth.enums.ErrorCode;
import net.flex.dci.otn.db.jpa.entity.security.Role;
import net.flex.dci.otn.db.jpa.entity.security.User;
import net.flex.dci.otn.db.jpa.service.dao.UserDaoService;
import net.flex.dci.otn.db.jpa.service.dao.dto.UserDetailDto;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/4/15 16:32
 */
@Component("userDetailsService")
@Slf4j
@RequiredArgsConstructor
public class UserDetailServiceImpl implements UserDetailsService {

    private final UserDaoService userDaoService;


    @Override
    public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
        log.debug("Authenticating user '{}'", username);

        if (username.equals(DEFAULT_USER_NAME)) {
            return defaultUser();
        }
        UserDetailDto userDetail = userDaoService.loadByUsername(username);
        if (userDetail == null) {
            throw new UsernameNotFoundException(ErrorCode.ERROR_USER_PASSWORD_INCORRECT.getMsg());
        }
        User user = userDetail.getUser();
        Role role = userDetail.getRole();
        return UserDto.builder().username(user.getUsername()).password(user.getPassword())
                .id(user.getId())
                .roles(Collections.singletonList(role.getRoleCode()))
                .build();
//        if ("admin".equals(username)) {

//            return UserDto.builder().username(username)
//                    .password(new BCryptPasswordEncoder().encode("admin"))
//                    .roles(Collections.singletonList(role))
//                    .build();
//
//        } else {
//
//            return null;
//
//
//        }

    }

    private UserDetails defaultUser() {
        return UserDto.builder().username(DEFAULT_USER_NAME)
                .password(new BCryptPasswordEncoder().encode(DEFAULT_USER_PASSWORD))
                .roles(Collections.singletonList(DEFAULT_USER_ROLE))
                .id(0l)
                .build();
    }
}
