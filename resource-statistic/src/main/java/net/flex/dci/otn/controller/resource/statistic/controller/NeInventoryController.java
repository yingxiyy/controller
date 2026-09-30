package net.flex.dci.otn.controller.resource.statistic.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.dto.inventory.CardQuery;
import net.flex.dci.otn.controller.resource.statistic.dto.inventory.NeQuery;
import net.flex.dci.otn.controller.resource.statistic.dto.inventory.TransceiverQuery;
import net.flex.dci.otn.controller.resource.statistic.rest.NeDevice;
import net.flex.dci.otn.controller.resource.statistic.rest.Page;
import net.flex.dci.otn.controller.resource.statistic.rest.equipment.CardInfo;
import net.flex.dci.otn.controller.resource.statistic.rest.equipment.TransceiverInfo;
import net.flex.dci.otn.controller.resource.statistic.service.InventoryService;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 10/24/2025 4:14 PM
 */
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(value = "/resource/inventory/network-elements")
public class NeInventoryController {

    private final InventoryService inventoryService;

    @RequestMapping(value = "/ne", method = RequestMethod.POST)
    public ResponseEntity<?> getNeInventoryStatisticPaged(
            @RequestBody NeQuery neQuery) {
        log.info("get ne inventory statistic paged:{}", neQuery);
        
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }


    @RequestMapping(value = "/{neId}", method = RequestMethod.GET)
    public ResponseEntity<?> getNeInventoryDetails(@PathVariable("neId") String neId) {
        log.info("get ne inventory details:{}", neId);
        NeDevice neDevice = inventoryService.getNeInventoryDetail(neId);
        return new ResponseEntity<>(Result.ok(neDevice), HttpStatus.OK);
    }

    @RequestMapping(value = "/card", method = RequestMethod.POST)
    public ResponseEntity<?> getEquipmentInventoryPaged(
            @RequestBody CardQuery cardQuery) {
        log.info("get equipment inventory information paged :{}", cardQuery);
        Page<CardInfo> equipmentInfoPage = inventoryService.fetchEquipmentInfoPaged(
                cardQuery);
        return new ResponseEntity<>(Result.ok(equipmentInfoPage), HttpStatus.OK);
    }

    @RequestMapping(value = "/transceiver", method = RequestMethod.POST)
    public ResponseEntity<?> getTransceiverInventoryPaged(
            @RequestBody TransceiverQuery query) {
        log.info("get transceiver inventory information paged :{}", query);
        Page<TransceiverInfo> transeiverInfoPage = inventoryService.fetchTransceiverInfoPaged(
                query);
        return new ResponseEntity<>(Result.ok(transeiverInfoPage), HttpStatus.OK);
    }

}
