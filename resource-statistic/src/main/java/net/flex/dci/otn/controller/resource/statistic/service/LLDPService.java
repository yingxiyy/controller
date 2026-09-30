package net.flex.dci.otn.controller.resource.statistic.service;

import net.flex.dci.otn.controller.resource.statistic.dto.inventory.LLDPQuery;
import net.flex.dci.otn.controller.resource.statistic.rest.Page;
import net.flex.dci.otn.controller.resource.statistic.rest.lldp.LLDPInfo;

/**
 *
 * @version 1.0
 * @date 12/24/2025 11:03 AM
 */
public interface LLDPService {

    Page<LLDPInfo> fetchLLDPInfoPaged(LLDPQuery lldpQuery);
}
