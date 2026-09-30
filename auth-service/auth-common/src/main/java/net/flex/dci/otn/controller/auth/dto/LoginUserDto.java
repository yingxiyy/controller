package net.flex.dci.otn.controller.auth.dto;

import java.io.Serializable;
import java.util.List;
import javax.management.relation.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/4/17 21:37
 */
@Data
@Builder
@AllArgsConstructor
public class LoginUserDto implements Serializable {

    private String username;

    private String email;

    private String loginClientId;
    
    private List<Role> roles;
}
