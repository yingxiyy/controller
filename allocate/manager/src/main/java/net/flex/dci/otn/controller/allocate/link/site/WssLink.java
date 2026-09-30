/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.site;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.allocate.link.phy.PhyLinkUtil;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.roadm.attribute.SiteLinkRelation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.SiteBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * @author YYX
 * @version 1.0
 */
@Slf4j
public class WssLink {

    private final ChangedObject changedObject;

    public WssLink(ChangedObject changedObject) {
        this.changedObject = changedObject;
    }

    public void remove(Link removedSiteLink) {
        log.debug("Try to remove wss link which attached with site link: {}, {}",
                removedSiteLink.getLinkId().getValue(),
                removedSiteLink.getAugmentation(Link1.class).getSite().getFriendlyName());

        String siteLinkId = removedSiteLink.getLinkId().getValue();
        String srcSiteId = SiteLinkIdNamingRule.getSiteA(siteLinkId);
        String dstSiteId = SiteLinkIdNamingRule.getSiteZ(siteLinkId);

        Set<String> removedWssLinkIds = new LinkedHashSet<>();
        removeWssLinkOnSite(srcSiteId, siteLinkId, removedWssLinkIds);
        removeWssLinkOnSite(dstSiteId, siteLinkId, removedWssLinkIds);

        // WSS Link属于相邻两条SiteLink，删除OCH时不能回收；任一关联SiteLink删除时，
        // 统一通过PhyLinkUtil回收phyLink、internalLink并把两端EXP端口恢复为空闲。
        PhyLinkUtil phyLinkUtil = new PhyLinkUtil(changedObject);
        removedWssLinkIds.forEach(wssLinkId -> {
            log.debug("Remove wss link {} related to removed site link {}",
                    wssLinkId, siteLinkId);
            phyLinkUtil.removePhyLink(wssLinkId, null);
        });
    }

    private void removeWssLinkOnSite(String siteId, String siteLinkId,
            Set<String> removedWssLinkIds) {
        Node siteNode = changedObject.getChangedSiteNode(siteId);
        if (siteNode == null) {
            log.warn("Can not find site node for siteId: {}, skip to remove wss link", siteId);
            return;
        }
        Site nodeAttr = siteNode.getAugmentation(Node1.class).getSite();
        if (nodeAttr.getSiteLinkRelation() != null && !nodeAttr.getSiteLinkRelation().isEmpty()) {
            List<SiteLinkRelation> relations =
                    new ArrayList<>(nodeAttr.getSiteLinkRelation());
            if (removeRelations(relations, siteLinkId, removedWssLinkIds)) {
                Node newNode = new NodeBuilder(siteNode)
                        .addAugmentation(Node1.class, new Node1Builder()
                                        .setSite(new SiteBuilder(nodeAttr)
                                                .setSiteLinkRelation(relations)
                                                .build())
                                        .build())
                        .build();
                changedObject.addChangedSiteNode(newNode);
            }
        } else {
            log.debug("site link relation is empty, skip to remove wss link, siteId: {}, siteLinkId: {}",
                    siteId, siteLinkId);
        }
    }

    /**
     * 一个SiteLink在同一站点可能关联多条WSS Link，必须全部回收，不能只处理第一条。
     */
    static boolean removeRelations(List<SiteLinkRelation> relations, String siteLinkId,
            Set<String> removedWssLinkIds) {
        boolean changed = false;
        Iterator<SiteLinkRelation> iterator = relations.iterator();
        while (iterator.hasNext()) {
            SiteLinkRelation relation = iterator.next();
            if (siteLinkId.equals(relation.getLinkaId())
                    || siteLinkId.equals(relation.getLinkzId())) {
                iterator.remove();
                if (relation.getWssLinkIdBetweenAZ() != null) {
                    removedWssLinkIds.add(relation.getWssLinkIdBetweenAZ());
                }
                changed = true;
            }
        }
        return changed;
    }

}
