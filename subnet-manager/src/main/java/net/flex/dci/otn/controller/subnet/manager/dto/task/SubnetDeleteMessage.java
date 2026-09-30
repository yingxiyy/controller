package net.flex.dci.otn.controller.subnet.manager.dto.task;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 2026/2/24
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SubnetDeleteMessage implements Serializable {

    private String author;

    private String subnetId;

    private String subnetName;

    private Long executeTimestamp;

}
