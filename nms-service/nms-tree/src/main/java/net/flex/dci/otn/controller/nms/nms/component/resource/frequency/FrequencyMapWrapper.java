package net.flex.dci.otn.controller.nms.nms.component.resource.frequency;

import java.util.List;

import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otn.controller.nms.nms.dto.FrequencyMapDto;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;

/**
 * @version 1.0
 * @date 2022/6/23 10:04
 */
public interface FrequencyMapWrapper {

    FrequencyMapDto getTpsFrequencyMap(String neId, String tpId);

    FrequencyMapDto getSiteLinksFrequencyMapBySiteLinkAndGrid(List<String> siteLinkIds, WDM_Band band,
            GridType grid);
}
