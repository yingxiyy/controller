package net.flex.dci.otc.controller.status.alarm.service.extractor.dto;

import java.util.Set;
import lombok.Data;

/**
 * @version 1.0
 * @date 2022/1/11 16:35
 */
@Data
public class QueryParamDto {

    private String nmlKeyLike;

    private Set<String> nmlKeyLikeSet;

    private Set<String> nmlKeySet;

    private Set<String> neIds;

    private String neId;

    private Boolean sa;
}
