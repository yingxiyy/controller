package net.flex.dci.otn.controller.resource.statistic.dto.inventory;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;

/**
 * @version 1.0
 * @date 10/30/2025 4:14 PM
 */
@Data
@Builder
public class SiteDetails implements Serializable {

    //    private String region;
//
    private String campus;

    private String siteId;

    private String siteName;

    private String city;

//    private String country;
//
//    private String location;

}
