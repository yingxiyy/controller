package net.flex.dci.otn.controller.idc.manager.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2026/9/16
 *
 * @author musa
 * @version 1.0
 **/
@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping(value = "/idc")
public class CascadeController {

    @RequestMapping(value = "/country")
    public ResponseEntity<?> listAllCountry() {
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

    @RequestMapping(value = "/country/{countryId}/region")
    public ResponseEntity<?> listAllRegion(@PathVariable("countryId") Long id) {
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }


    @RequestMapping(value = "/region/{regionId}/province")
    public ResponseEntity<?> listAllProvinceByRegionId(@PathVariable("regionId") Long id) {
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

    @RequestMapping(value = "/province/{provinceId}/city")
    public ResponseEntity<?> listAllCityByProvinceId(@PathVariable("provinceId") Long id) {
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

}
