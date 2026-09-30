package net.flex.dci.otn.controller.nms.nms.dto.route;

import java.io.Serializable;
import java.util.List;
import lombok.Data;

/**
 * 2026/1/19
 *
 * @author musa
 * @version 1.0
 **/
@Data
public class ConnectionPathInfoDto implements Serializable {

    private String startEquipId;

    private String endEquipId;

    private List<ExternalLinkInfoDto> edges;

}
