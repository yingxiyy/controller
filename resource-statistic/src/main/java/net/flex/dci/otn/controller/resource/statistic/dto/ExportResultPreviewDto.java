package net.flex.dci.otn.controller.resource.statistic.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/6/23
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ExportResultPreviewDto implements Serializable {

    private List<List<String>> pageRows;

    private List<String> headers;

    private Long total;

    private Long page;

    private Long limit;

    private boolean expired = false;

}
