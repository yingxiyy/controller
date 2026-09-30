package net.flex.dci.otn.controller.resource.statistic.dto;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.CollectionUtils;

/**
 * 2026/6/29
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class PreviewList<T> implements Serializable {

    private Integer total;

    private List<T> items;

    public static <T> PreviewList<T> of(List<T> fullList) {
        return of(fullList, 5);
    }

    public static <T> PreviewList<T> of(List<T> fullList, int previewSize) {
        if (CollectionUtils.isEmpty(fullList)) {
            return PreviewList.<T>builder().build();
        }
        return PreviewList.<T>builder().total(fullList.size())
                .items(fullList.stream().limit(previewSize).collect(
                        Collectors.toList())).build();
    }
}
