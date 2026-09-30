package net.flex.dci.otn.controller.nms.nms.dto;

import java.io.Serializable;
import java.util.List;

import lombok.Builder;
import lombok.Data;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.grouping.Map;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;

/**
 * @version 1.0
 * @date 2022/6/23 10:33
 */
@Data
@Builder
public class FrequencyMapDto implements Serializable {

    private GridType grid;
    
    private List<Map> mapList;
}
