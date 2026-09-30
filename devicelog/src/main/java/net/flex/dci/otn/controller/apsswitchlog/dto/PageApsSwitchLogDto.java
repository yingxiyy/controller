package net.flex.dci.otn.controller.apsswitchlog.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PageApsSwitchLogDto implements Serializable {

    private List<ApsSwitchLogDto> apsSwitchLogs;

    private Long currentPage;

    private Long pageSize;

    private Long totalElements;

    private Integer totalPages;
}
