package net.flex.dci.otn.controller.user.domain.notification;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/5/5 15:24
 */
@Data
@Builder
@AllArgsConstructor
public class DeleteDto implements Serializable {


    private Long userId;

    private Long roleId;

    private Long permissionId;
}
