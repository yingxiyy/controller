package net.flex.dci.otn.controller.apsswitchlog.service;

import java.util.List;
import net.flex.dci.otn.controller.apsswitchlog.dto.ApsRelatedTunnelOrSiteLink;
import net.flex.dci.otn.controller.apsswitchlog.dto.PageApsSwitchLogDto;
import net.flex.dci.otn.controller.apsswitchlog.dto.PageApsSwitchLogQueryParamDto;

/**
 * @version 1.0
 * @date 2022/5/5 10:52
 */
public interface ApsSwitchLogService {

    PageApsSwitchLogDto listAllApsSwitchLogByCondition(
            PageApsSwitchLogQueryParamDto pageQueryParamDto);

    void deleteApsSwitchLog(Long taskInfoId);

    List<ApsRelatedTunnelOrSiteLink> findApsRelativeSiteLinkOrTunnel(String neId, String apsName);

    PageApsSwitchLogDto retrieveResourceAllApsSwitchLogByCondition(
            PageApsSwitchLogQueryParamDto pageQueryParamDto);
}
