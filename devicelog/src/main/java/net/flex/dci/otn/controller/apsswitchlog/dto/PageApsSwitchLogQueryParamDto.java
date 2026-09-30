package net.flex.dci.otn.controller.apsswitchlog.dto;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import net.flex.dci.otn.controller.apsswitchlog.enums.ResourceType;
import org.springframework.data.domain.Sort;

/**
 * @version 1.0
 * @date 2022/5/5 10:48
 */
@Data
@Builder
public class PageApsSwitchLogQueryParamDto implements Serializable {

    private Integer limit;

    private Integer page;

    private String neName;

    private String neId;

    private Sort sort;

    private String keywords;

    private String activePath;

    private String triggerType;

    private String apsMode;

    private String apsModuleName;

    private Long startTime;

    private Long endTime;

    private ResourceType resourceType;

    private String resourceId;

    private String subnetId;
}
