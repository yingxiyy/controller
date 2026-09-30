package net.flex.dci.otn.controller.user.domain.rest.paged;

import com.alibaba.fastjson.annotation.JSONField;
import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.user.domain.rest.UserDto;

/**
 * @version 1.0
 * @date 2022/4/19 14:52
 */
@Data
@Builder
@AllArgsConstructor
public class UserPagedDto implements Serializable {

    @JSONField(name = "users")
    private List<UserDto> userDtos;

    @JSONField(name = "current-page")
    private int currentPage;

    @JSONField(name = "total")
    private Long totalUsers;

    @JSONField(name = "total-page")
    private int totalPage;
}
