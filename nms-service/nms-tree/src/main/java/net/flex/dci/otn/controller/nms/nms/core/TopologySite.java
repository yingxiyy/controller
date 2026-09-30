/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.core;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otn.controller.nms.nms.handler.SiteHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMiddleSitesBetweenTwoSitesInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMiddleSitesBetweenTwoSitesOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetSiteTopoOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.site.network.topology.Topology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * getMiddleSitesBetweenTwoSites
 *
 * getSiteTopo
 *
 * @date: 2021/4/7
 */
@Slf4j
@Component
public class TopologySite extends BaseNms {

    private final String GET_SITE_TOPO = "nms:get-site-topo";

    private final String GET_MIDDLE_SITES = "nms:get-middle-sites-between-two-sites";

    @Autowired
    private SiteHandler siteHandler;

    public TopologySite(NetconfTopology netconfTopology) {
        super(netconfTopology);

    }

    @Override
    public String executeRequest(String cmd, String requestBody)
            throws CommonException, UnsupportedOperationException {
        String returnValue = null;
        switch (cmd) {
            case GET_SITE_TOPO:
                returnValue = getSiteTopo(cmd);
                break;
            case GET_MIDDLE_SITES:
                returnValue = getMiddleSites(cmd, requestBody);
                break;
            default:
                throw new UnsupportedOperationException(
                        "failed to execute the nms operations for site");
        }
        return returnValue;
    }

    //TODO:GET MIDDLE SITE BETWEEN SITES
    private String getMiddleSites(String cmd, String requestBody) throws CommonException {
        try {
            log.info("start to get middle site between two sites ");
            GetMiddleSitesBetweenTwoSitesInput input = parseInput(
                    cmd, requestBody, GetMiddleSitesBetweenTwoSitesInput.class);
            GetMiddleSitesBetweenTwoSitesOutput output = siteHandler
                    .getMiddleSitesBetweenTwoSites(input);
            return serializeDataObject(cmd, output);
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get middle site the reason is:" + ex.getMessage());
        }
    }


    private String getSiteTopo(String cmd) throws CommonException {
        log.info("start to get site topo ");
        try {
            GetSiteTopoOutputBuilder getSiteTopoOutputBuilder = new GetSiteTopoOutputBuilder();
            List<Topology> topologies = siteHandler.getSiteTopo();
            getSiteTopoOutputBuilder.setTopology(topologies);
            return serializeDataObject(cmd, getSiteTopoOutputBuilder.build());
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get site topo the reason is:" + ex.getMessage());
        }
    }
}
