package net.flex.dci.otn.controller.implement.common.dto;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;

/**
 * 2025/9/4
 *
 * @author musa
 * @version 1.0
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApsSwitchControlSubTaskDetail implements Serializable {

    private String tunnelId;

    private String tunnelName;

    private String neId;

    private String neName;

    private String crossConnectionId;

    private String apsName;

    private ApsPath apsPath;

    private String detailResult;
}
