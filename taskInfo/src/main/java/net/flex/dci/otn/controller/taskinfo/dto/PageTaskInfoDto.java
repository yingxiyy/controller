package net.flex.dci.otn.controller.taskinfo.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.db.jpa.entity.TaskInfo;

/**
 * @version 1.0
 * @date 2022/1/26 15:03
 */
@Data
@Builder
public class PageTaskInfoDto implements Serializable {

    private List<TaskInfo> taskInfoList;

    private Long currentPage;

    private Long totalElements;

    private Integer totalPages;
}
