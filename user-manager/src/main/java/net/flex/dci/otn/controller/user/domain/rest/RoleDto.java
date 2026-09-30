package net.flex.dci.otn.controller.user.domain.rest;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/4/19 13:51
 */
@Data
@Builder
@AllArgsConstructor
public class RoleDto implements Serializable {

    @JSONField(name = "role-id")
    private Long roleId;

    @JSONField(name = "role-name")
    private String roleName;

    @JSONField(name = "role-code")
    private String roleCode;

    @JSONField(name = "description")
    private String roleDescription;
}
