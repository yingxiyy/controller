package net.flex.dci.otn.controller.resource.statistic.dto.task.display;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 2026/9/16
 *
 * @author musa
 * @version 1.0
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class CircuitQueryDisplayDto extends QueryTaskDisplayDto {

    private List<String> siteLinkNames;


    private Boolean includeLLdp;

}
