package net.flex.dci.otn.controller.resource.statistic.rest;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 *
 * @version 1.0
 * @date 10/31/2025 4:57 PM
 */
@Data
@Builder
@AllArgsConstructor
public class Page<T> implements Serializable {

    private int pageNo;
    private int pageSize;
    private long total;

    private long totalPages;

    private List<T> records;

    public Page(int pageNo, int pageSize, long total, List<T> records) {
        this.pageNo = pageNo;
        this.pageSize = pageSize;
        this.total = total;
        this.records = records;
        this.totalPages = (long) Math.ceil((double) total / pageSize);
    }

}
