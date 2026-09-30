package net.flex.dci.otn.controller.resource.statistic.controller;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.resource.statistic.dto.NodeInfo;
import net.flex.dci.otn.controller.resource.statistic.dto.SiteInfo;
import net.flex.dci.otn.controller.resource.statistic.dto.SiteLinkInfo;
import net.flex.dci.otn.controller.resource.statistic.service.CascadeService;
import net.flex.dci.otn.controller.webapp.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * 2025/11/2
 *
 * @author musa
 * @version 1.0
 **/
@RestController
@Slf4j
@RequestMapping(value = "/resource/cascade")
@RequiredArgsConstructor
public class CascadeController {

    private final CascadeService cascadeService;


    @RequestMapping(value = "/subnet/{network}/site", method = RequestMethod.GET)
    public ResponseEntity<?> getSiteByPlane(@PathVariable("network") String network) {
        log.info("get subnetwork ref site,network:{}", network);
        List<SiteInfo> refSiteInfos = cascadeService.retrieveSiteByNetwork(network);
        return new ResponseEntity<>(Result.ok(refSiteInfos), HttpStatus.OK);
    }


    @RequestMapping(value = "/subnet/{subnetId}/site/{siteId}/ne", method = RequestMethod.GET)
    public ResponseEntity<?> getNeBySite(@PathVariable("subnetId") String subnetId,
            @PathVariable("siteId") String siteId) {
        log.info("get site ref phy node,site:{}", siteId);
        List<NodeInfo> nodeInfos = cascadeService.getSiteSubPhyNodeBySite(siteId, subnetId);
        return new ResponseEntity<>(Result.ok(nodeInfos), HttpStatus.OK);
    }

    @RequestMapping(value = "/subnet/{subnetId}/siteLink", method = RequestMethod.GET)
    public ResponseEntity<?> getSiteLinkBySubNet(@PathVariable("subnetId") String subnetId) {
        log.info("get site link by subnetId:{}", subnetId);
        List<SiteLinkInfo> siteLinkInfos = cascadeService.getRefSiteLinkInfoBySubnet(subnetId);
        return new ResponseEntity<>(Result.ok(siteLinkInfos), HttpStatus.OK);
    }
}
