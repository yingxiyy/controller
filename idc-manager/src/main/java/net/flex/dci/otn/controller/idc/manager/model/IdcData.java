package net.flex.dci.otn.controller.idc.manager.model;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.fastjson.annotation.JSONField;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;
import net.flex.dci.otn.controller.idc.manager.excel.Excel;

/**
 * @version 1.0
 * @date 2022/1/19 10:24
 */
@Data
@Builder
public class IdcData implements Excel {

    @Tolerate
    public IdcData() {

    }


    @ExcelIgnore
    private Long id;

    @ExcelIgnore
    private Integer physicalRow;

    @ExcelProperty(value = "国家", index = 0)
    @NotNull(message = "idc.data.countryNullError")
    @Pattern(regexp = "^[^()]*$", message = "idc.data.country.noParentheses")
    private String country;

    @ExcelProperty(value = "区域", index = 1)
    @NotNull(message = "idc.data.regionNullError")
    @Pattern(regexp = "^[^()]*$", message = "idc.data.region.noParentheses")
    private String region;

    @ExcelProperty(value = "省/自治区/直辖市", index = 2)
    @NotNull(message = "idc.data.provinceNullError")
    @Pattern(regexp = "^[^()]*$", message = "idc.data.province.noParentheses")
    private String province;

    @ExcelProperty(value = "市", index = 3)
    @NotNull(message = "idc.data.cityNullError")
    @Pattern(regexp = "^[^()]*$", message = "idc.data.city.noParentheses")
    private String city;

    @ExcelProperty(value = "区/县", index = 4)
    @NotNull(message = "idc.data.districtNullError")
    @Pattern(regexp = "^[^()]*$", message = "idc.data.district.noParentheses")
    private String district;

    @ExcelProperty(value = "园区", index = 5)
    @NotNull(message = "idc.data.siteNullError")
    @Pattern(regexp = "^[^()]*$", message = "idc.data.site.noParentheses")
    private String site;


    @ExcelProperty(value = "园区经度坐标", index = 6)
    @NotNull(message = "idc.data.site.longitudeNullError")
    @Pattern(regexp = "^(\\-|\\+)?(((\\d|[1-9]\\d|1[0-7]\\d|0{1,3})\\.\\d{0,6})|(\\d|[1-9]\\d|1[0-7]\\d|0{1,3})|180\\.0{0,6}|180)$", message = "idc.data.site.longitudeWrongFormatter")
    private String site_longitude;


    @ExcelProperty(value = "园区维度坐标", index = 7)
    @NotNull(message = "idc.data.site.latitudeNullError")
    @Pattern(regexp = "^(\\-|\\+)?([0-8]?\\d{1}\\.\\d{0,6}|90\\.0{0,6}|[0-8]?\\d{1}|90)$", message = "idc.data.site.latitudeWrongFormatter")
    private String site_latitude;

    @ExcelProperty(value = "机房", index = 8)
    @NotNull(message = "idc.data.site.roomNullError")
    @Pattern(regexp = "^[^()]*$", message = "idc.data.room.noParentheses")
    private String room;

    @ExcelProperty(value = "机房编码", index = 9)
    @NotNull(message = "idc.data.site.roomCodeNullError")
    @Pattern(regexp = "^[^()]*$", message = "idc.data.room_code.noParentheses")
    private String room_code;

    @ExcelProperty(value = "机房简称", index = 10)
    @NotNull(message = "idc.data.site.roomAbbreviationNullError")
    @Pattern(regexp = "^[^()]*$", message = "idc.data.room_abbreviation.noParentheses")
    private String room_abbreviation;

    @ExcelIgnore
    @JSONField(defaultValue = "true", name = "active")
    private Boolean isActive;
}
