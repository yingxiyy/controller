package net.flex.dci.otn.controller.idc.manager.service.impl;

import static net.flex.dci.otn.controller.idc.manager.utils.ConvertorUtils.convert2SiteInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.idc.manager.model.IdcData;
import net.flex.dci.otn.controller.idc.manager.service.IDCDataUploadService;
import net.flex.dci.otn.db.jpa.entity.Campus;
import net.flex.dci.otn.db.jpa.entity.City;
import net.flex.dci.otn.db.jpa.entity.Region;
import net.flex.dci.otn.db.jpa.entity.SiteInfo;
import net.flex.dci.otn.db.jpa.service.dao.CampusDaoService;
import net.flex.dci.otn.db.jpa.service.dao.CityDaoService;
import net.flex.dci.otn.db.jpa.service.dao.RegionDaoService;
import net.flex.dci.otn.db.jpa.service.dao.SiteInfoDaoService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @version 1.0
 * @date 2022/3/27 12:20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IDCDataUploadServiceImpl implements IDCDataUploadService {

    private final RegionDaoService regionDaoService;

    private final CityDaoService cityDaoService;

    private final CampusDaoService campusDaoService;

    private final SiteInfoDaoService siteInfoDaoService;

    //region->city->campus->idc

    @Override
    public void batchUploadIdcData(Collection<IdcData> idcDatas) {
        int dataCount = idcDatas.size();
        log.info("Starting batch upload IDC data, total records: {}", dataCount);
        if (0 == dataCount) {
            log.warn("Batch upload IDC data: no data to process, return directly.");
            return;
        }
        try {
            Collection<SiteInfo> siteInfos = convert2SiteInfo(idcDatas);
            log.debug("Convert IdcData to SiteInfo finished,converted records:{}",
                    siteInfos.size());
            Map<String, Long> regionMaps = extractDistinctRegionMap(
                    siteInfos);//map region name -> region id map
            log.debug("extract distinct region map finished,unique regions:{}", regionMaps.size());
            Map<String, List<CityDto>> cityNameRefCityIdMap = extractDistinctCityMap(siteInfos,
                    regionMaps);
            log.debug("Extract distinct city name map finished,unique city:{}",
                    cityNameRefCityIdMap.size());
            Map<String, List<CampusDto>> campusNameRefIdMap = extractDistinctCampusMap(siteInfos);
            log.debug("Extract campus map finished,unique campus :{}", campusNameRefIdMap.size());
            Collection<SiteInfo> siteInfoCollection = addAdditionalAttribute(siteInfos,
                    cityNameRefCityIdMap, campusNameRefIdMap);
            log.debug("Supplement SiteInfo attributes finished, total to save: {}",
                    siteInfoCollection.size());
            int[] result = siteInfoDaoService.batchSave(siteInfoCollection);
//            if (result != null) {
//                log.info("finish to save the idc to db ,{}", idcDatas);
//            }
            int successCount = result != null ? Arrays.stream(result).sum() : 0;

            log.info("Batch save IDC data finished, total to save: {}, successfully saved: {}",
                    siteInfoCollection.size(), successCount);

            if (successCount < siteInfoCollection.size()) {
                log.warn("Partial failure in batch save: total={}, success={}",
                        siteInfoCollection.size(), successCount);
            }
        } catch (Exception e) {
            log.error("Batch upload IDC data failed, records={}, error: {}", dataCount,
                    e.getMessage(), e);

            if (e instanceof CommonException) {
                throw e;
            }

            if (e.getCause() instanceof DuplicateKeyException
                    || e.getMessage().contains("Duplicate entry")
                    || e.getMessage().contains("unique constraint")) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "Duplicate data: duplicate IDC or site info exists, cannot save", e);
            }

            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "Batch upload IDC data failed: " + e.getMessage(), e);
        }
    }

    /**
     * add
     *
     * @param siteInfos
     * @param cityNameRefCityIdMap
     * @param campusNameRefIdMap
     * @return
     */
    private Collection<SiteInfo> addAdditionalAttribute(Collection<SiteInfo> siteInfos,
            Map<String, List<CityDto>> cityNameRefCityIdMap,
            Map<String, List<CampusDto>> campusNameRefIdMap) {
        return siteInfos.stream().map(siteInfo -> {
            String cityName = siteInfo.getCity();
            String regionName = siteInfo.getRegion();
            String campusName = siteInfo.getCampus();
            log.debug("Processing SiteInfo: region={}, city={}, campus={}",
                    regionName, cityName, campusName);
            CityDto cityDto = getRefCityDto(regionName, cityName, cityNameRefCityIdMap);
            if (cityDto == null) {
                log.error("No matching city found for region={}, city={}", regionName, cityName);
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "No matching city found: region=" + regionName + ", city=" + cityName);
            }
            CampusDto campusDto = getRefCampusDto(cityName, campusName, campusNameRefIdMap);
            if (campusDto == null) {
                log.error("No matching campus found for city={}, campus={}", cityName, campusName);
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "No matching campus found: city=" + cityName + ", campus=" + campusName);
            }
            Long regionId = cityDto.getRegionId();
            Long cityId = cityDto.getCityId();
            Long campusId = campusDto.campusId;
            siteInfo.setCampusId(campusId);
            siteInfo.setCityId(cityId);
            siteInfo.setRegionId(regionId);
            log.debug("Attributes supplemented: regionId={}, cityId={}, campusId={}",
                    cityDto.getRegionId(), cityDto.getCityId(), campusDto.getCampusId());
            return siteInfo;
        }).collect(Collectors.toList());
    }

    private CampusDto getRefCampusDto(String cityName, String campusName,
            Map<String, List<CampusDto>> campusNameRefIdMap) {
        List<CampusDto> campusDtos = campusNameRefIdMap.get(cityName);
        if (CollectionUtils.isEmpty(campusDtos)) {
            log.warn("No campus list found for city: {}", cityName);
            return null;
        }
        Optional<CampusDto> findDto = campusDtos.stream()
                .filter(campusDto -> campusDto.getCampusName().equals(campusName)).findAny();
        return findDto.get();
    }

    private CityDto getRefCityDto(String regionName, String cityName,
            Map<String, List<CityDto>> cityNameRefCityIdMap) {
        List<CityDto> citiesDtos = cityNameRefCityIdMap.get(regionName);
        if (CollectionUtils.isEmpty(citiesDtos)) {
            log.warn("No city list found for region:{}", regionName);
            return null;
        }
        Optional<CityDto> findDto = citiesDtos.stream()
                .filter(cityDto -> cityDto.getCityName().equals(cityName)).findAny();
        return findDto.get();
    }

    private Map<String, List<CampusDto>> extractDistinctCampusMap(Collection<SiteInfo> siteInfos) {
        Map<String, Set<String>> campusMap = new HashMap<>();
        for (SiteInfo siteInfo : siteInfos) {
            String city = siteInfo.getCity();
            String campus = siteInfo.getCampus();
            if (campusMap.containsKey(city)) {
                campusMap.get(city).add(campus);
            } else {
                Set<String> campuses = new HashSet<>();
                campuses.add(campus);
                campusMap.put(city, campuses);
            }
            log.debug("Extract campus: city={}, campus={}", city, campus);
        }
        log.info("Extract unique campus finished, cities={}, total campuses={}",
                campusMap.size(),
                campusMap.values().stream().mapToInt(Set::size).sum());
        Map<String, List<CampusDto>> campusCityMap = new HashMap<>();
        for (Entry<String, Set<String>> entry : campusMap.entrySet()) {
            String city = entry.getKey();
            if (city == null) {
                log.error("City not found: {}", city);
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "City not found: " + city);
            }
            Set<String> campuses = entry.getValue();
            List<CampusDto> campusDtos = new ArrayList<>();
            City cityEntity = cityDaoService.getCityByName(city);
            for (String campus : campuses) {
                Campus campusEntity = campusDaoService.getCampusByNameAndCityId(campus,
                        cityEntity.getId());
                if (null == campusEntity) {
                    campusEntity = campusDaoService.save(Campus.builder().name(campus).cityId(
                            cityEntity.getId()).build());
                    log.info("Create new campus: city={}, name={}, id={}",
                            city, campus, cityEntity.getId());
                } else {
                    log.debug("Found existing campus: city={}, name={}, id={}",
                            city, campus, cityEntity.getId());
                }
                CampusDto campusDto = CampusDto.builder().campusId(campusEntity.getId())
                        .campusName(campus).cityId(cityEntity.getId())
                        .cityName(cityEntity.getName()).build();
                campusDtos.add(campusDto);
            }
            campusCityMap.put(city, campusDtos);
        }
        return campusCityMap;
    }

    private Map<String, List<CityDto>> extractDistinctCityMap(Collection<SiteInfo> siteInfos,
            Map<String, Long> regionMaps) {
        try {
            Map<String, Set<String>> regionCityMap = new HashMap<>();
            for (SiteInfo siteInfo : siteInfos) {
                String region = siteInfo.getRegion();
                String city = siteInfo.getCity();
//                if (regionCityMap.containsKey(region)) {
//                    regionCityMap.get(region).add(city);
//                } else {
//                    Set<String> cities = new HashSet<>();
//                    cities.add(city);
//                    regionCityMap.put(region, cities);
//                }
                regionCityMap.computeIfAbsent(region, k -> new HashSet<>()).add(city);
                log.debug("Extract city: region={}, city={}", region, city);
            }
            Map<String, List<CityDto>> map = new HashMap<>();
            for (Entry<String, Set<String>> entry : regionCityMap.entrySet()) {
                Long regionId = regionMaps.get(entry.getKey());
                Set<String> citySet = entry.getValue();
                List<CityDto> cityDtos = new ArrayList<>();
                for (String city : citySet) {
                    City cityEntity = cityDaoService.getCityByName(city);
                    if (cityEntity == null) {
                        cityEntity = cityDaoService.save(
                                City.builder().name(city).regionId(regionId).build());
                        log.info("Create new city: region={}, name={}, id={}",
                                entry.getKey(), city, cityEntity.getId());
                    } else {
                        log.debug("Found existing city: region={}, name={}, id={}",
                                entry.getKey(), city, cityEntity.getId());
                    }
                    cityDtos.add(
                            CityDto.builder().cityId(cityEntity.getId())
                                    .cityName(cityEntity.getName())
                                    .regionName(entry.getKey()).regionId(regionId).build());
                }
                map.put(entry.getKey(), cityDtos);
            }
            return map;
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage(),
                    ex);
        }
    }


    private Map<String, Long> extractDistinctRegionMap(Collection<SiteInfo> siteInfos) {
        log.debug("extract the distinct region for the siteInfos :{}", siteInfos);
        Set<String> regionNames = siteInfos.stream().map(SiteInfo::getRegion).collect(
                Collectors.toSet());
        List<Region> regions = new ArrayList<>();
        for (String regionName : regionNames) {
            Region region = regionDaoService.getRegionInfoByName(regionName);
            if (null == region) {
                region = regionDaoService.save(Region.builder().name(regionName).build());
                log.info("Create new region: name={}, id={}", regionName, region.getId());
            } else {
                log.debug("Found existing region: name={}, id={}", regionName, region.getId());
            }
            regions.add(region);
        }
        return regions.stream().collect(Collectors.toMap(Region::getName, Region::getId));
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class CityDto implements Serializable {

        private String cityName;

        private String regionName;

        private Long cityId;

        private Long regionId;

    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class CampusDto implements Serializable {

        private Long campusId;

        private String campusName;

        private Long cityId;

        private Long regionId;

        private String regionName;

        private String cityName;
    }

}
