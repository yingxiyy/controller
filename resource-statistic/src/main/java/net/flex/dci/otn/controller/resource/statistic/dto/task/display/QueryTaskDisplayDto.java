package net.flex.dci.otn.controller.resource.statistic.dto.task.display;

import java.io.Serializable;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 2026/9/16
 *
 * @author musa
 * @version 1.0
 **/
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class QueryTaskDisplayDto implements Serializable {

    private List<String> subnetNames;


}
