package net.flex.dci.otn.controller.resource.statistic.dto;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * 2026/6/25 to add to the resource Search parameter
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
public class ResourceSearchParameter implements Serializable {

    private List<String> subnet;

    private PreviewList<String> ne;

    private PreviewList<String> card;

    private PreviewList<String> site;

    private PreviewList<FilterCondition> filters;

    private PreviewList<String> siteLink;

    private PreviewList<String> phyLink;

    private PreviewList<String> tunnel;

}
