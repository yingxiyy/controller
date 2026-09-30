package net.flex.dci.otn.controller.idc.manager.service.impl;

import static net.flex.dci.otn.controller.idc.manager.utils.ConvertorUtils.convert2SiteInfo;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.idc.manager.component.SiteInfoDecorator;
import net.flex.dci.otn.controller.idc.manager.dto.CityInfo;
import net.flex.dci.otn.controller.idc.manager.dto.RegionInfo;
import net.flex.dci.otn.controller.idc.manager.model.BatchIdcDto;
import net.flex.dci.otn.controller.idc.manager.model.IdcData;
import net.flex.dci.otn.controller.idc.manager.model.IdcDisplayData;
import net.flex.dci.otn.controller.idc.manager.model.IdcDto;
import net.flex.dci.otn.controller.idc.manager.model.IdcRpc;
import net.flex.dci.otn.controller.idc.manager.model.PageIdcData;
import net.flex.dci.otn.controller.idc.manager.model.RegionData;
import net.flex.dci.otn.controller.idc.manager.model.RpcCityInput;
import net.flex.dci.otn.controller.idc.manager.model.RpcInput;
import net.flex.dci.otn.controller.idc.manager.model.RpcOutput;
import net.flex.dci.otn.controller.idc.manager.model.RpcRegionInput;
import net.flex.dci.otn.controller.idc.manager.service.IdcDataService;
import net.flex.dci.otn.controller.idc.manager.utils.ConvertorUtils;
import net.flex.dci.otn.controller.idc.manager.utils.ValidateUtils;
import net.flex.dci.otn.controller.idc.manager.validate.IdcValidateGroup;
import net.flex.dci.otn.db.jpa.entity.City;
import net.flex.dci.otn.db.jpa.entity.Region;
import net.flex.dci.otn.db.jpa.entity.SiteInfo;
import net.flex.dci.otn.db.jpa.service.dao.CityDaoService;
import net.flex.dci.otn.db.jpa.service.dao.RegionDaoService;
import net.flex.dci.otn.db.jpa.service.dao.SiteInfoDaoService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/1/20 10:51
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class IdcDataServiceImpl implements IdcDataService {


    private final SiteInfoDaoService siteInfoDaoService;

    private final RegionDaoService regionDaoService;

    private final CityDaoService cityDaoService;

    private final SiteInfoDecorator siteInfoDecorator;

    /**
     * batch save to the db
     *
     * @param idcDatas
     * @throws CommonException
     */
    @Override
    public void BatchSave(Collection<IdcData> idcDatas) throws CommonException {
        log.debug("idc data is {}", idcDatas);
        Collection<SiteInfo> siteInfos = convert2SiteInfo(idcDatas);
        int[] result = siteInfoDaoService.batchSave(siteInfos);
        if (result != null) {
            log.info("finish to save the idc to db ,{}", idcDatas);
        }
    }

    @Override
    public PageIdcData listAllIdcByCondition(int offset, int limit) {
        log.debug("list all idc data offset is {},limit is {}", offset, limit);
        Page<SiteInfo> idcDataPage = siteInfoDaoService.findAll(offset - 1, limit);
        PageIdcData pageIdcData = ConvertorUtils.convertIdc2Pageed(idcDataPage);
        return pageIdcData;
    }

    @Override
    public List<IdcData> listAllIdc() throws Exception {
        log.debug("retrieve all idc data");
        List<SiteInfo> siteInfos = siteInfoDaoService.findAll();
        List<IdcData> idcDatas = ConvertorUtils.convert2IdcDataList(siteInfos);
        return idcDatas;
    }

    @Override
    public void addNewIdc(IdcData idcData) throws ValidationException {
        log.info("add a new idc data is :{}", idcData);
        ValidateUtils.validateData(idcData);
        SiteInfo newSiteInfo = ConvertorUtils.convert2SiteInfo(idcData);
        SiteInfo addSiteInfo = siteInfoDecorator.enrichSiteInfo(newSiteInfo);
        int result = siteInfoDaoService.ignoreSave(addSiteInfo);
        log.debug("finish to save the idc data is {},result is {}", idcData, result);
        log.trace("finish to save the idc data is {}", idcData);
    }


    @Override
    public void removeIdcById(Long id) {
        log.info("start to delete idc data ,idc id is :{}", id);
        if (!siteInfoDaoService.existedId(id)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the idc data is not existed!");
        }
        //todo:judge if the idc is already been used
        if (siteInfoDaoService.isActive(id)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the idc should be inactive first");
        }
        siteInfoDaoService.delete(id);
        log.info("finish to delete idc data,idc id is :{}", id);
        log.debug("finish to delete idc data,idc id is :{}", id);
        log.trace("finish to delete idc data,idc id is :{}", id);
    }

    @Override
    public void editIdc(IdcDto idcDto) {
        Long id = idcDto.getId();
        log.info("edit idc data ,idc id is :{}", id);
        if (!siteInfoDaoService.existedId(id)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the idc data is not existed!");
        }
        IdcData rewriteIdcData = ConvertorUtils.convert2IDCData(idcDto);
        SiteInfo rewriteSiteInfo = ConvertorUtils.convert2SiteInfo(rewriteIdcData);
        siteInfoDaoService.saveAndFlush(rewriteSiteInfo);
        log.info("finish to rewrite idc data,idc id is :{}", id);
        log.debug("finish to rewrite idc data,idc id is :{}", id);
        log.trace("finish to rewrite idc data,idc id is :{}", id);
    }

    @Override
    public IdcRpc getAllCityByRegion(RpcInput input) throws CommonException {

        ValidateUtils.validateData(input, IdcValidateGroup.RegionName.class);
        log.info("get all city by region, region is :{}", input.getRegionName());
        String regionName = input.getRegionName();
//        List<String> cities = siteInfoDaoService.getCitiesByRegionName(regionName);
        List<City> cities = cityDaoService.getCityByRegion(regionName);
        List<String> cityNames = cities.stream().map(City::getName).collect(Collectors.toList());
        return IdcRpc.builder().output(RpcOutput.builder().cityName(cityNames).build()).build();


    }

    @Override
    public IdcRpc getAllCityByRegion(RpcRegionInput input) throws IOException {
        ValidateUtils.validateData(input, IdcValidateGroup.RegionName.class);
        log.info("get all city by region, region is :{}", input.getRegionName());
        String regionName = input.getRegionName();
        Long regionId = input.getRegionId();
//        List<String> cities = siteInfoDaoService.getCitiesByRegionName(regionName);
        List<City> cities = cityDaoService.getCityByRegionId(regionId);
        List<CityInfo> cityNames = cities.stream()
                .map(city -> CityInfo.builder().cityId(city.getId()).cityName(city.getName())
                        .build()).collect(Collectors.toList());
        return IdcRpc.builder().output(RpcOutput.builder().cityInfo(cityNames).build()).build();
    }

    @Override
    public IdcRpc getAllIdcListByCity(RpcInput input) {

        ValidateUtils.validateData(input, IdcValidateGroup.CityData.class);
        log.info("get all idc list by city,city is {}", input.getCityName());
        String cityName = input.getCityName();
        City city = cityDaoService.getCityByName(cityName);
        if (city == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    String.format("can not find the city :{0}", city.getName()));
        }
        List<Map<String, Object>> idcList = siteInfoDaoService.getIdcsByCityId(city.getId());
        List<IdcDisplayData> idcDisplayDataList = ConvertorUtils.convert2IdcDisplayDatas(idcList);
        return IdcRpc.builder().output(RpcOutput.builder().idcs(idcDisplayDataList).build())
                .build();
    }

    @Override
    public IdcRpc getAllIdcListByCity(RpcCityInput input) {
        ValidateUtils.validateData(input, IdcValidateGroup.CityData.class);
        log.info("get all idc list by city,city is {}", input.getName());
        String cityName = input.getName();
        City city = cityDaoService.getCityByCityId(input.getId());
        if (city == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    String.format("can not find the city :{0}", city.getName()));
        }
        List<Map<String, Object>> idcList = siteInfoDaoService.getIdcsByCityId(city.getId());
        List<IdcDisplayData> idcDisplayDataList = ConvertorUtils.convert2IdcDisplayDatas(idcList);
        return IdcRpc.builder().output(RpcOutput.builder().idcs(idcDisplayDataList).build())
                .build();
    }

    @Override
    public List<String> getAllRegion() {
        log.info("retrieve all the region list");
        List<Region> regions = regionDaoService.getAllRegion();
        List<RegionData> regionData = regions.stream().map(region ->
                RegionData.builder().id(region.getId()).name(region.getName()).build()
        ).collect(Collectors.toList());
        return regionData.stream().map(RegionData::getName).collect(Collectors.toList());
    }

    @Override
    public List<RegionInfo> getAllRegions() {
        log.info("retrieve all the region list");
        List<Region> regions = regionDaoService.getAllRegion();
        List<RegionData> regionData = regions.stream().map(region ->
                RegionData.builder().id(region.getId()).name(region.getName()).build()
        ).collect(Collectors.toList());
        return regionData.stream()
                .map(region -> RegionInfo.builder().regionId(region.getId()).name(region.getName())
                        .build()).collect(Collectors.toList());
    }

    @Override
    public void batchEditeIdc(BatchIdcDto batchIdcDto) {
        List<Long> ids = batchIdcDto.getId();
        Boolean enable = batchIdcDto.getEnable();
        if (CollectionUtils.isEmpty(ids)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "No idc selected.");
        }
        if (enable == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Target status is required.");
        }
        List<Long> distinctIds = ids.stream().distinct().collect(Collectors.toList());
        int updated = siteInfoDaoService.batchUpdateActive(distinctIds, enable);
        log.info(
                "batch update idc active finished, requested={}, distinct={}, updated={}, active={}",
                ids.size(), distinctIds.size(), updated, enable);
    }


}
