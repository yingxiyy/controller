package net.flex.dci.otn.controller.resource.statistic.dto.query;

import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 2026/7/20
 *
 * @author musa
 * @version 1.0
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class CircuitQueryDTO extends BaseQueryDTO {

    private List<String> siteLink;

    private boolean includeLLdp = true;

}
