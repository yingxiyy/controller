package net.flex.dci.otn.controller.resource.statistic.dto.inventory;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * 2026/1/30
 *
 * @author musa
 * @version 1.0
 **/
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
@AllArgsConstructor
public class TransceiverQuery extends BaseQuery implements Serializable {

}
