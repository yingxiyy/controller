package net.flex.dci.otn.controller.idc.manager.component.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.idc.manager.component.SiteInfoDecorator;
import net.flex.dci.otn.db.jpa.entity.Campus;
import net.flex.dci.otn.db.jpa.entity.City;
import net.flex.dci.otn.db.jpa.entity.Region;
import net.flex.dci.otn.db.jpa.entity.SiteInfo;
import net.flex.dci.otn.db.jpa.service.dao.CampusDaoService;
import net.flex.dci.otn.db.jpa.service.dao.CityDaoService;
import net.flex.dci.otn.db.jpa.service.dao.RegionDaoService;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/3/28 10:49
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SiteInfoDecoratorImpl implements SiteInfoDecorator {

    private final RegionDaoService regionDaoService;

    private final CityDaoService cityDaoService;

    private final CampusDaoService campusDaoService;


    @Override
    public SiteInfo enrichSiteInfo(SiteInfo siteInfo) {
        log.info("enrich the site info");
        String region = siteInfo.getRegion();
        String city = siteInfo.getCity();
        String campus = siteInfo.getCampus();
        Long regionId = getOrNewRegion(region);
        Long cityId = getOrNewCity(regionId, city);
        Long campusId = getOrNewCampusId(cityId, campus);

        siteInfo.setRegionId(regionId);
        siteInfo.setCityId(cityId);
        siteInfo.setCampusId(campusId);
        return siteInfo;
    }

    private Long getOrNewCampusId(Long cityId, String campusName) {
        log.debug("start to get or new a campus :{},cityId is :{}", campusName, cityId);
        Campus campus = campusDaoService.getCampusByNameAndCityId(campusName, cityId);
        if (null == campus) {
            campus = campusDaoService.save(
                    Campus.builder().cityId(cityId).name(campusName).build());
        }
        return campus.getId();
    }

    private Long getOrNewCity(Long regionId, String cityName) {
        log.debug("start to get or new a city for cityName:{}", cityName);
        City city = cityDaoService.getCityByRegionIdAndName(regionId, cityName);
        if (city == null) {
            city = cityDaoService.save(City.builder().regionId(regionId).name(cityName).build());
        }
        return city.getId();
    }

    private Long getOrNewRegion(String regionName) {
        log.debug("get or new region for regionName:{}", regionName);
        Region region = regionDaoService.getRegionInfoByName(regionName);
        if (region == null) {
            region = regionDaoService.save(Region.builder().name(regionName).build());
        }
        return region.getId();
    }
}
