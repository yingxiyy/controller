package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otc.common.util.NeYangModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;

/**
 * @version 1.0
 * @date 11/10/2023 2:49 PM
 */
@Data
@Builder
public class MuxGeneralInfoDto implements Serializable {

    private NeYangModel neYangModel;

    private GridType gridType;

}
