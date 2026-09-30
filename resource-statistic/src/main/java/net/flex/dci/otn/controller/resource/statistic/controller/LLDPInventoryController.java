package net.flex.dci.otn.controller.resource.statistic.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.dto.inventory.LLDPQuery;
import net.flex.dci.otn.controller.resource.statistic.rest.Page;
import net.flex.dci.otn.controller.resource.statistic.rest.lldp.LLDPInfo;
import net.flex.dci.otn.controller.resource.statistic.service.LLDPService;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @version 1.0
 * @date 10/31/2025 4:22 PM
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(value = "/resource/inventory/lldp")
public class LLDPInventoryController {

    private final LLDPService lldpService;

    @RequestMapping(method = RequestMethod.POST)
    public ResponseEntity<?> retrieveLLdpInventoryPaged(
            @RequestBody LLDPQuery lldpQuery
    ) {
        Page<LLDPInfo> lldpInfoPageResult = lldpService.fetchLLDPInfoPaged(lldpQuery);
        return new ResponseEntity<>(Result.ok(lldpInfoPageResult), HttpStatus.OK);
    }


}
