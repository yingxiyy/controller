/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.site.nbi.impl;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.roadm.attribute.SiteLinkRelation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;


/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
public class WssLink {

    private RouteInfo rInfo;
    private Link siteLink;
    private static SiteNodeDao siteNodeDao = SpringBeanFinder.getBean(SiteNodeDao.class);

    public WssLink(RouteInfo rInfo, Link siteLink) {
        this.rInfo = rInfo;
        this.siteLink = siteLink;
    }

    public void getRoute() {
        log.debug("update wssLink related resource in routeInfo");

        String siteLinkId = siteLink.getLinkId().getValue();
        String srcSiteId = SiteLinkIdNamingRule.getSiteA(siteLinkId);
        String dstSiteId = SiteLinkIdNamingRule.getSiteZ(siteLinkId);

        addWssLinkResourceOnSite(srcSiteId, siteLinkId);
        addWssLinkResourceOnSite(dstSiteId, siteLinkId);
    }

    private void addWssLinkResourceOnSite(String siteId, String siteLinkId) {
        Node siteNode = siteNodeDao.getSiteNodeById(siteId);
        if (siteNode == null) {
            log.error("Can not find site node for siteId: {}, skip to fetch wss link route from site", siteId);
            return;
        }
        Site nodeAttr = siteNode.getAugmentation(Node1.class).getSite();
        if (nodeAttr.getSiteLinkRelation() != null && !nodeAttr.getSiteLinkRelation().isEmpty()) {
            SiteLinkRelation relation = nodeAttr.getSiteLinkRelation().stream()
                    .filter(x -> x.getLinkaId().equals(siteLinkId))
                    .findAny().orElse(null);

            if (relation == null) {
                //try another
                relation = nodeAttr.getSiteLinkRelation().stream()
                        .filter(x -> x.getLinkzId().equals(siteLinkId))
                        .findAny().orElse(null);

            }
            if (relation == null) {
                log.error("can not find wssLink which related to this siteLink, skip to insert wss link resource, siteLinkId: {}",
                        siteLinkId);
            } else {
                String wssLinkId = relation.getWssLinkIdBetweenAZ();
                log.debug("start insert wssLink related route to route, wssLinkId: {}, siteLinkId: {}",
                        wssLinkId, siteLinkId);

                //this is a list remove at first and insert again to avoid duplicate id
                rInfo.getPhyLinkIdList().remove(wssLinkId);
                rInfo.getPhyLinkIdList().add(wssLinkId);

                String sTpId = PhysicalLinkIdNamingRule.getTpAId(wssLinkId);
                String dTpId = PhysicalLinkIdNamingRule.getTpZId(wssLinkId);

                rInfo.getTpIdList().remove(sTpId);
                rInfo.getTpIdList().add(sTpId);

                rInfo.getTpIdList().remove(dTpId);
                rInfo.getTpIdList().add(dTpId);
            }
        } else {
            log.debug("site link relation is empty, no wss link, siteId: {}, siteLinkId: {}",
                    siteId, siteLinkId);
        }
    }

}
