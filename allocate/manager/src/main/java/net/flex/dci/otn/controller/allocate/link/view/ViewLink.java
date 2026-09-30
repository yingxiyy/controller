/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.view;

import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.ViewLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.ViewNodeNamingRule;
import net.flex.dci.otc.mongo.dao.SubNetTreeNodeDao;
import net.flex.dci.otc.mongo.dao.ViewLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.mdoel.subnet.SubNetTreeNode;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.*;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.View;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.link.attributes.ViewBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AlarmSeverity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.DestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SourceBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.*;

/**
 * @author YYX
 * @date 11/24/2021
 */
@Slf4j
public class ViewLink {

    private ChangedObject changedObject;
    private String planeId;

    private SubNetTreeNode subnet;

    SubNetTreeNodeDao subNetTreeNodeDao = SpringBeanFinder.getBean(SubNetTreeNodeDao.class);
    ViewLinkDao viewLinkDao = SpringBeanFinder.getBean(ViewLinkDao.class);

    public ViewLink(ChangedObject changedObject, String planeId) {
        this.changedObject = changedObject;
        this.planeId = planeId;
    }

    public static String[] getViewLinkId(String srcSite, String dstSite,
            ViewLinkType viewLinkType, String planeId) {

        String[] viewLinkId = new String[2];
        viewLinkId[0] = ViewLinkIdNamingRule.generatedIdWithPlaneId(srcSite, dstSite, viewLinkType, planeId);
        viewLinkId[1] = ViewLinkIdNamingRule.generatedIdWithPlaneId(dstSite, srcSite, viewLinkType, planeId);

        return viewLinkId;
    }

    /**
     * based on a/z, check does same viewLink is existed. if yes, number++. otherwise create new one.
     * 只生成本层的viewLink
     *
     * @param mappingLink, mapping this link into viewLink
     * @return
     */
    public synchronized Link create(Link mappingLink) {
        log.debug("create view link based on {}", mappingLink.getLinkId().getValue());

        String srcSite, dstSite;
        ViewLinkType linkType = getViewLinkType(mappingLink);
        if (linkType.equals(ViewLinkType.SiteLink)) {
            srcSite = mappingLink.getSource().getSourceNode().getValue();
            dstSite = mappingLink.getDestination().getDestNode().getValue();
        } else {
            srcSite = PhysicalNodeIdNamingRule.getSiteId(
                    mappingLink.getSource().getSourceNode().getValue());
            dstSite = PhysicalNodeIdNamingRule.getSiteId(
                    mappingLink.getDestination().getDestNode().getValue());
        }
        if (srcSite.equals(dstSite)) {
            return null;
        }

        subnet = subNetTreeNodeDao.findBySubNetId(planeId).orElse(null);
        if (subnet == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "I should find a subnet with planeId: " + planeId);
        }

        String[] viewLinkIds = getViewLinkId(srcSite, dstSite, linkType, planeId);

        Link viewLink = getViewLink(viewLinkIds);

        if (viewLink == null) {
            log.debug("will create a new view link");
            //create a new one.

            String viewLinkId = viewLinkIds[0];
            viewLink = newLink(srcSite, dstSite, linkType, new LinkId(viewLinkId),
                    mappingLink.getLinkId());

            updateSiteNodeWithPlaneId(srcSite);
            updateSiteNodeWithPlaneId(dstSite);
        } else {
            //update bundle number;
            log.debug("the view link has existed {}", viewLink.getLinkId().getValue());

            viewLink = increaseBundleNumber(viewLink, mappingLink.getLinkId());
        }

        changedObject.addChangedViewLink(viewLink);
        return viewLink;
    }

    private void updateSiteNodeWithPlaneId(String siteNodeId) {
        String planeViewId = ViewNodeNamingRule.generatedIdWithPlaneId(siteNodeId, planeId);
        Node planeViewNode = changedObject.getChangedViewNode(planeViewId);
        Node siteViewNode = changedObject.getChangedViewNode(siteNodeId);
        if (siteViewNode == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "I should find a site view node, this is impossible, siteNodeId: " + siteNodeId);
        }

        org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.View siteViewNodeAttr =
                siteViewNode.getAugmentation(Node1.class).getView();

        if (planeViewNode == null) {
            log.debug("without planeViewNode, create it");
            planeViewNode = new NodeBuilder()
                    .setNodeId(new NodeId(planeViewId))
                    .setKey(new NodeKey(new NodeId(planeViewId)))
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setView(new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.ViewBuilder()
                                    .setFriendlyName(siteViewNodeAttr.getFriendlyName())
                                    .setPosX(siteViewNodeAttr.getPosX())
                                    .setPosY(siteViewNodeAttr.getPosY())
                                    .setSubnetId(subnet.getSubNetId())
                                    .setSubnetName(subnet.getName())
                                    .setAlarmState(AlarmSeverity.Unknown)
                                    .setSubnetLevel(subnet.getLevel())
                                    .build())
                            .build())
                    .build();

            changedObject.addChangedViewNode(planeViewNode);

            if (subnet.getLevel() == 0) {
                log.debug("current is root, not up level");
            } else {
                String parentPlaneId = subnet.getParentId();

                planeViewId = ViewNodeNamingRule.generatedIdWithPlaneId(siteNodeId, parentPlaneId);
                planeViewNode = changedObject.getChangedViewNode(planeViewId);

                if (planeViewNode == null) {
                    log.debug("missing up level planeViewNode, create it");

                    SubNetTreeNode parrentSubnet = subNetTreeNodeDao.findBySubNetId(parentPlaneId).orElse(null);
                    if (subnet == null) {
                        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                                "I should find a parent subnet with planeId: " + planeId);
                    }
                    //create related viewNode
                    planeViewNode = new NodeBuilder()
                            .setNodeId(new NodeId(planeViewId))
                            .setKey(new NodeKey(new NodeId(planeViewId)))
                            .addAugmentation(Node1.class, new Node1Builder()
                                    .setView(new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.ViewBuilder()
                                            .setFriendlyName(siteViewNodeAttr.getFriendlyName())
                                            .setPosX(siteViewNodeAttr.getPosX())
                                            .setPosY(siteViewNodeAttr.getPosY())
                                            .setSubnetId(parrentSubnet.getSubNetId())
                                            .setSubnetName(parrentSubnet.getName())
                                            .setAlarmState(AlarmSeverity.Unknown)
                                            .setSubnetLevel(parrentSubnet.getLevel())
                                            .build())
                                    .build())
                            .build();

                    changedObject.addChangedViewNode(planeViewNode);
                }
            }
        }
    }

    private ViewLinkType getViewLinkType(Link mappingLink) {
        log.debug("get view link type,view link id:{}", mappingLink.getLinkId().getValue());
        if (mappingLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                != null) {
            return ViewLinkType.SiteLink;
        } else if (mappingLink.getAugmentation(
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                != null) {
            return ViewLinkType.OchLink;
        }
        //here must be phy link, I still need distinct OTS (used in siteLink) or OS (used in 电中继或者无光层）
        if (PhysicalLinkIdNamingRule.isOtsLink(mappingLink.getLinkId().getValue())) {
            return ViewLinkType.OtsLink;
        } else {
            return ViewLinkType.OsLink;
        }
    }

    private Link getViewLink(String[] viewLinkIds) {
        String viewLinkIdAZ = viewLinkIds[0];
        Link viewLink = changedObject.getChangedViewLink(viewLinkIdAZ);
        if (viewLink == null) {
            String viewLinkIdZA = viewLinkIds[1];
            viewLink = changedObject.getChangedViewLink(viewLinkIdZA);
        }
        return viewLink;
    }

    private Link increaseBundleNumber(Link viewLink, LinkId siteLinkId) {
        View viewAttr = viewLink.getAugmentation(Link1.class).getView();

        Link newLink = new LinkBuilder(viewLink)
                .addAugmentation(Link1.class, new Link1Builder()
                        .setView(new ViewBuilder(viewAttr)
                                .setBundleNumber(viewLink.getSupportingLink().size() + 1)
                                .build())
                        .build())
                .build();

        viewLink.getSupportingLink().add(new SupportingLinkBuilder().setLinkRef(siteLinkId)
                .setKey(new SupportingLinkKey(siteLinkId)).build());
        return newLink;
    }

    private Link newLink(String srcSite, String dstSite, ViewLinkType linkType, LinkId viewLinkId,
            LinkId siteLinkId) {
        Link viewLink = new LinkBuilder()
                .setLinkId(new LinkId(viewLinkId))
                .setKey(new LinkKey(viewLinkId))
                .setSource(new SourceBuilder().setSourceNode(new NodeId(srcSite)).build())
                .setDestination(new DestinationBuilder().setDestNode(new NodeId(dstSite)).build())
                .setSupportingLink(new LinkedList<>())
                .addAugmentation(Link1.class, new Link1Builder()
                        .setView(new ViewBuilder()
                                .setBundleNumber(1)
                                .setAlarmState(AlarmSeverity.Unknown)
                                .setLevel(linkType)
                                .setSubnetId(subnet.getSubNetId())
                                .setSubnetName(subnet.getName())
                                .setSubnetLevel(subnet.getLevel())
                                .build())
                        .build())
                .build();
        viewLink.getSupportingLink().add(
                new SupportingLinkBuilder().setLinkRef(siteLinkId)
                        .setKey(new SupportingLinkKey(siteLinkId)).build());

        return viewLink;
    }

    /**
     * if this isn't last one mappingLink in viewLink, just bundleNumber-- otherwise, remove the
     * mappingLink together
     *
     * @param mappingLink, which link want to mapping to viewLink
     * @return
     */
    public synchronized void remove(Link mappingLink) {
        if (mappingLink == null) {
            return;
        }
        ViewLinkType linkType = getViewLinkType(mappingLink);

        String srcSite, dstSite;
        if (linkType.equals(ViewLinkType.SiteLink)) {
            srcSite = mappingLink.getSource().getSourceNode().getValue();
            dstSite = mappingLink.getDestination().getDestNode().getValue();
        } else {
            srcSite = PhysicalNodeIdNamingRule.getSiteId(
                    mappingLink.getSource().getSourceNode().getValue());
            dstSite = PhysicalNodeIdNamingRule.getSiteId(
                    mappingLink.getDestination().getDestNode().getValue());
        }
        if (srcSite.equals(dstSite)) {
            log.debug("skip same-site viewLink remove for {}", mappingLink.getLinkId().getValue());
            return;
        }
        String[] viewLinkIds = getViewLinkId(srcSite, dstSite, linkType, planeId);

        log.debug("remove viewLink {} or {}", viewLinkIds[0], viewLinkIds[1]);

        Link viewLink = getViewLink(viewLinkIds);
        if (viewLink == null) {
            log.error("I should find a view link");
        } else {
            String viewLinkId = viewLink.getLinkId().getValue();

            //must be here.
            if (viewLink.getSupportingLink().size() > 1) {
                //remove supporting link.
                String siteLinkId = mappingLink.getLinkId().getValue();
                Link newLink = removeSupportingSiteLink(viewLink, siteLinkId);
                changedObject.addChangedViewLink(newLink);
            } else {
                //the viewLink should be remove.
                log.debug("I want to remove viewLink {}", viewLinkId);
                changedObject.addRemovedViewLink(viewLink);

                //if this related viewNode is based on plane, remove it together if no other viewLink based on it.
                //需要递归检查上层， 如果需要，上层的viewNode 也需要删除
                removeViewNode(srcSite, planeId);
                removeViewNode(dstSite, planeId);
            }
        }

    }

    private void removeViewNode(String siteNodeId, String planeId) {
        try {
            List<Link> viewLinks = viewLinkDao.getLinkOnNode(siteNodeId, planeId);
            List<Link> restViewLinks = viewLinks.stream().filter(x -> {
                String viewLinkId = x.getLinkId().getValue();
                if (changedObject.getRemovedViewLinkIdList().contains(viewLinkId)) {
                    return false;
                }
                return true;
            }).collect(Collectors.toList());

            log.debug("the siteNodeId at this plane {} ({}) has other viewLinks.",
                    siteNodeId, planeId, restViewLinks.stream().map(x -> x.getLinkId().getValue()).collect(Collectors.joining(",")));

            if (restViewLinks.isEmpty()) {
                log.debug("start remove viewNode at this plane");
                String viewNodeId = ViewNodeNamingRule.generatedIdWithPlaneId(siteNodeId, planeId);
                changedObject.addRemovedViewNode(viewNodeId);

                subNetTreeNodeDao.findBySubNetId(planeId).ifPresent(subnet -> {
                    if (subnet.getLevel() >= 0) {
                        removeViewNode(siteNodeId, subnet.getParentId());
                    }
                });
            }
        } catch (Exception e) {
            log.error("remove view node.", e);
            throw e;
        }
    }

    private Link removeSupportingSiteLink(Link viewLink, String siteLinkId) {
        View viewLinkAttr = viewLink.getAugmentation(Link1.class).getView();
        List<SupportingLink> newSupportingLinkList = new ArrayList<>();
        newSupportingLinkList.addAll(viewLink.getSupportingLink());

        Iterator<SupportingLink> iter = newSupportingLinkList.iterator();
        while (iter.hasNext()) {
            SupportingLink sl = iter.next();
            if (sl.getLinkRef().getValue().equals(siteLinkId)) {
                iter.remove();
                break;
            }
        }
        Link newLink = new LinkBuilder(viewLink)
                .setSupportingLink(newSupportingLinkList)
                .addAugmentation(Link1.class, new Link1Builder()
                        .setView(new ViewBuilder(viewLinkAttr)
                                .setBundleNumber(newSupportingLinkList.size())
                                .build())
                        .build())
                .build();

        return newLink;
    }

}
