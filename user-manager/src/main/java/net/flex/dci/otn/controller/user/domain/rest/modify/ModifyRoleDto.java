package net.flex.dci.otn.controller.user.domain.rest.modify;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/4/19 14:28
 */
@Data
@Builder
@AllArgsConstructor
public class ModifyRoleDto implements Serializable {

    @JSONField(name = "user-id")
    private Long userId;

    @JSONField(name = "role-id")
    private Long roleId;

    @JSONField(name = "role-code")
    private String roleCode;
}
