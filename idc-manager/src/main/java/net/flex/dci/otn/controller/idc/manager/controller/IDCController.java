package net.flex.dci.otn.controller.idc.manager.controller;

import java.io.IOException;
import java.util.List;
import javax.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.idc.manager.dto.RegionInfo;
import net.flex.dci.otn.controller.idc.manager.model.BatchIdcDto;
import net.flex.dci.otn.controller.idc.manager.model.GetCityListByRegionRpc;
import net.flex.dci.otn.controller.idc.manager.model.GetIdcListByCityRpc;
import net.flex.dci.otn.controller.idc.manager.model.IdcData;
import net.flex.dci.otn.controller.idc.manager.model.IdcDto;
import net.flex.dci.otn.controller.idc.manager.model.IdcRpc;
import net.flex.dci.otn.controller.idc.manager.model.PageIdcData;
import net.flex.dci.otn.controller.idc.manager.service.IdcDataService;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @version 1.0
 * @date 2022/1/26 14:14
 */
@RequestMapping(value = "/idc")
@RestController
@Slf4j
public class IDCController {

    @Autowired
    private IdcDataService idcDataService;

    @RequestMapping(method = RequestMethod.GET)
    public ResponseEntity<?> listAllIdcsByCondition(
            @RequestParam(value = "page", defaultValue = "1", required = false) int offset,
            @RequestParam(value = "limit", defaultValue = "20", required = false) int limit)
            throws CommonException {
        log.info("list all idcs by condition");
        offset = offset < 1 ? 1 : offset;
        limit = limit < 0 ? 20 : limit;
        PageIdcData pageIdcData = idcDataService.listAllIdcByCondition(offset, limit);
        return new ResponseEntity<>(Result.ok(pageIdcData), HttpStatus.OK);
    }

    @RequestMapping(value = "/all", method = RequestMethod.GET)
    public ResponseEntity<?> listAllIdcs() {
        try {
            List<IdcData> idcData = idcDataService.listAllIdc();
            return new ResponseEntity<>(Result.ok(idcData), HttpStatus.OK);
        } catch (Exception exception) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    exception.getMessage());
        }
    }

    @RequestMapping(method = RequestMethod.PUT)
    public ResponseEntity<?> addNewIdc(@RequestBody IdcData idcData) {
        try {
            idcDataService.addNewIdc(idcData);
            return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
        } catch (ValidationException exception) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    exception.getMessage());
        }
    }


    @RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
    public ResponseEntity<?> deleteIdc(@PathVariable("id") Long id) {
        idcDataService.removeIdcById(id);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.PUT, value = "/edit")
    public ResponseEntity<?> editIdc(@RequestBody IdcDto idcDto) {
        idcDataService.editIdc(idcDto);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }


    @RequestMapping(method = RequestMethod.PUT, value = "/active")
    public ResponseEntity<?> batchActive(@RequestBody BatchIdcDto batchIdcDto) {
        idcDataService.batchEditeIdc(batchIdcDto);
        return new ResponseEntity<>(Result.ok(), HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/get-city-list")
    public ResponseEntity<?> getRegionCities(@RequestBody GetCityListByRegionRpc input)
            throws IOException {
        log.debug("start to get city list for the region,input body is :{}", input);
        IdcRpc output = idcDataService.getAllCityByRegion(input.getInput());
        return new ResponseEntity<>(output, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.POST, value = "/get-idc-list")
    public ResponseEntity<?> getIdcListByCity(@RequestBody GetIdcListByCityRpc input) {
        log.debug("start to get the city's idc list ,rpc input is :{}", input);
        IdcRpc output = idcDataService.getAllIdcListByCity(input.getInput());
        return new ResponseEntity<>(output, HttpStatus.OK);
    }

    @RequestMapping(method = RequestMethod.GET, value = "/get-region-list")
    public ResponseEntity<?> getRegionList() {
        log.debug("start to get config region ");
        List<RegionInfo> regions = idcDataService.getAllRegions();
        return new ResponseEntity<>(Result.ok(regions), HttpStatus.OK);
    }
}
