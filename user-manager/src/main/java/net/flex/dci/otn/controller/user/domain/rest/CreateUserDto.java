package net.flex.dci.otn.controller.user.domain.rest;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/4/19 14:11
 */
@Data
@Builder
@AllArgsConstructor
public class CreateUserDto implements Serializable {

    private String username;

    private String password;

    private String email;

    @JSONField(name = "role")
    private RoleDto roleDto;
}
