package net.flex.dci.otn.controller.resource.statistic.dto.query;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/7/22
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class QueryResultInfo implements Serializable {

    private String sheetName;
    private Class<?> clazz;
}
