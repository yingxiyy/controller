package net.flex.dci.otn.controller.idc.manager.utils;


import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.idc.manager.model.IdcData;
import net.flex.dci.otn.controller.idc.manager.model.IdcDisplayData;
import net.flex.dci.otn.controller.idc.manager.model.IdcDto;
import net.flex.dci.otn.controller.idc.manager.model.PageIdcData;
import net.flex.dci.otn.db.jpa.entity.SiteInfo;
import org.springframework.data.domain.Page;

/**
 * @version 1.0
 * @date 2022/1/26 16:06
 */
@Slf4j
public class ConvertorUtils {

    /**
     * convert2SiteInfos
     *
     * @param idcDatas
     * @return
     */
    public static Collection<SiteInfo> convert2SiteInfo(Collection<IdcData> idcDatas) {
        return idcDatas.stream().map(ConvertorUtils::convert2SiteInfo)
                .collect(Collectors.toList());
    }


    public static SiteInfo convert2SiteInfo(IdcData idcData) {
        SiteInfo siteInfo = new SiteInfo();
        siteInfo.setId(idcData.getId());
        siteInfo.setCampus(idcData.getSite());
        siteInfo.setCity(idcData.getCity());
        siteInfo.setDistrict(idcData.getDistrict());
        siteInfo.setProvince(idcData.getProvince());
        siteInfo.setCountry(idcData.getCountry());
        siteInfo.setRegion(idcData.getRegion());
        siteInfo.setRoom(idcData.getRoom());
        siteInfo.setRoomCode(idcData.getRoom_code());
        siteInfo.setRoomAbbreviation(idcData.getRoom_abbreviation());
        siteInfo.setSiteLatitude(idcData.getSite_latitude() == null ? null
                : Double.valueOf(idcData.getSite_latitude()));
        siteInfo.setSiteLongitude(idcData.getSite_longitude() == null ? null
                : Double.valueOf(idcData.getSite_longitude()));
        siteInfo.setActive(idcData.getIsActive());
        return siteInfo;
    }

    public static IdcData convert2IDCData(SiteInfo siteInfo) {
        return IdcData.builder()
                .site(siteInfo.getCampus())
                .city(siteInfo.getCity())
                .district(siteInfo.getDistrict())
                .country(siteInfo.getCountry())
                .region(siteInfo.getRegion())
                .province(siteInfo.getProvince())
                .room(siteInfo.getRoom())
                .room_abbreviation(siteInfo.getRoomAbbreviation())
                .room_code(siteInfo.getRoomCode())
                .site_latitude(String.valueOf(siteInfo.getSiteLatitude()))
                .site_longitude(String.valueOf(siteInfo.getSiteLongitude()))
                .isActive(siteInfo.getActive())
                .id(siteInfo.getId())
                .build();
    }

    public static PageIdcData convertIdc2Pageed(Page<SiteInfo> idcDataPage) {
        PageIdcData pageIdcData = PageIdcData.builder()
                .idcData(idcDataPage.getContent().stream().map(ConvertorUtils::convert2IDCData)
                        .collect(
                                Collectors.toList()))
                .currentPage(idcDataPage.getPageable().getOffset() + 1)
                .totalPages(idcDataPage.getTotalPages()).totalElements(
                        idcDataPage.getTotalElements()).build();
        return pageIdcData;
    }

    public static List<IdcData> convert2IdcDataList(List<SiteInfo> siteInfos) {
        return siteInfos.stream().map(ConvertorUtils::convert2IDCData).collect(Collectors.toList());
    }

    /**
     * convert to idc excel rows to the template
     */
    public static List<List<Object>> convert2IdcExcelRows(List<SiteInfo> siteInfos) {
        return siteInfos.stream().map(siteInfo -> Arrays.<Object>asList(
                siteInfo.getCountry(),
                siteInfo.getRegion(),
                siteInfo.getProvince(),
                siteInfo.getCity(),
                siteInfo.getDistrict(),
                siteInfo.getCampus(),
                siteInfo.getSiteLongitude(),
                siteInfo.getSiteLatitude(),
                siteInfo.getRoom(),
                siteInfo.getRoomCode(),
                siteInfo.getRoomAbbreviation()
        )).collect(Collectors.toList());
    }

    /**
     * convert idcDto 2 idcData
     *
     * @param idcDto
     * @return
     */
    public static IdcData convert2IDCData(IdcDto idcDto) {
        return IdcData.builder().id(idcDto.getId()).isActive(idcDto.getEnable()).build();
    }

    /**
     * convert idcData 2 display data
     *
     * @param idcList
     * @return
     */
    public static List<IdcDisplayData> convert2IdcDisplayDatas(List<Map<String, Object>> idcList) {
        return idcList.stream().map(ConvertorUtils::convert2IdcDisplayData)
                .collect(Collectors.toList());
    }

    public static IdcDisplayData convert2IdcDisplayData(Map<String, Object> idc) {
        BigInteger id = (BigInteger) idc.get("id");
        String roomAbbreviation = (String) idc.get("roomAbbreviation");
        String city = (String) idc.get("city");
        String room = (String) idc.get("room");
        String roomCode = (String) idc.get("roomCode");
        StringBuilder idcDisplayNameBuilder = new StringBuilder();
        idcDisplayNameBuilder.append(city).append("-").append(roomAbbreviation).append("(")
                .append(roomCode).append(")");
        return IdcDisplayData.builder().idcDisplayName(idcDisplayNameBuilder.toString())
                .id(id.longValue())
                .idcName(room)
                .idcCode(roomCode)
                .build();

    }
}
