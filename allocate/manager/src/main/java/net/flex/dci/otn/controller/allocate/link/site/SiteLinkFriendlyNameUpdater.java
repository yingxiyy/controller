/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.link.site;

/**
 * @author YYX
 * @date 11/24/2021
 */

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.BroadcastMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.mongo.dao.RackDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.allocate.common.util.Constant;
import net.flex.dci.otn.controller.tools.kafka.service.BroadcastMessager;
import net.flex.dci.otn.controller.tools.kafka.service.TaskInfoMessager;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRack;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.site.SupportingRackBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp;

@Slf4j
@Data
public class SiteLinkFriendlyNameUpdater {

    protected TaskInfoMessage taskInfoMessage;
//  private TaskInfoKafkaService kafka;

    public SiteLinkFriendlyNameUpdater() {
    }

    /**
     * rack friendly name equals link friendly name, thus when changing link friendly name, we
     * change rack's too.
     *
     * @param siteLinkId
     * @param friendlyName
     * @return
     * @throws CommonException
     */
    public UpdateLinkOutput updateFriendlyName(String siteLinkId, String friendlyName)
            throws CommonException {
        log.debug("update site link friendly name");
        Link link = check(siteLinkId, friendlyName);

        SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);
        ZkResourceLock locker = new ZkResourceLock();

        try {
            Set<String> phyNodeIdList = lockResource(locker, link);
            renameRackFriendlyNameByLink(phyNodeIdList, friendlyName);

            Site mergeSite = new SiteBuilder().setFriendlyName(friendlyName).build();
            Link1 mergeLink1 = new Link1Builder().setSite(mergeSite).build();
            Link mergeLink = new LinkBuilder()
                    .setLinkId(new LinkId(siteLinkId))
                    .addAugmentation(Link1.class, mergeLink1)
                    .setKey(new LinkKey(new LinkId(siteLinkId))).build();

            siteLinkDao.saveSiteLink(mergeLink);

            UpdateLinkOutput output = new UpdateLinkOutputBuilder()
                    .setReturnCode(RpcResultType.Success)
                    .build();

            logMessage(link, null);

            return output;
        } catch (CommonException ce) {
            log.error("update friendlyName fail", ce);
            logMessage(link, null);
            throw ce;
        } catch (Exception e) {
            log.error("update friendlyName fail", e);
            logMessage(link, null);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "update siteLink friendly name fail" + e.toString(), e);
        } finally {
            locker.unlock();
        }
    }

    private void logMessage(Link siteLink, String errorMessage) {
        Link1 linkAttr = siteLink.getAugmentation(Link1.class);
        String msg = String.format("updated siteLink %s ", linkAttr.getSite().getFriendlyName());

        String extMsg = "fail";
        boolean isOk = false;
        if (errorMessage == null) {
            //成功
            isOk = true;
            extMsg = "successfully.";
        }

        if (taskInfoMessage != null) {
            if (isOk) {
                taskInfoMessage.setResourceId(siteLink.getLinkId().getValue());
                taskInfoMessage.setResourceName(linkAttr.getSite().getFriendlyName());
                taskInfoMessage.setSuccessfully(isOk);
            } else {
                taskInfoMessage.setSuccessfully(isOk);
                taskInfoMessage.setErrorReason(errorMessage);
            }
            TaskInfoMessager.sendMessage(taskInfoMessage);
        }

        BroadcastMessager.publishKafkaMessage(
                BroadcastMessage.builder()
                        .title("update siteLink friendlyName")
                        .message(msg + extMsg)
                        .error(!isOk)
                        .build());
    }

    /**
     * based on route of siteLink, find out all siteNode and lock them.
     *
     * @param locker
     * @param link
     * @return ID of phyNodeID
     */
    private Set<String> lockResource(ZkResourceLock locker, Link link) {
        log.debug("start lock resource");
        Set<String> phyNodeIdList = new HashSet();
        locker.addResource(link.getLinkId().getValue());

        Route route = link.getAugmentation(Link1.class).getSite().getExplictRoute().getRoute()
                .get(0);
        List<PathRouteObject> proList = route.getPrimary().getExplicitRouteObjects().get(0)
                .getPathRouteObject();
        Set<String> phyNodeIdSet = getRoutePhyNode(proList);

        if (route.getSecondary() != null) {
            proList = route.getSecondary().getExplicitRouteObjects().get(0).getPathRouteObject();
            phyNodeIdSet.addAll(getRoutePhyNode(proList));
        }
        for (String phyNodeId : phyNodeIdSet) {
            String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(phyNodeId);
            phyNodeIdList.add(phyNodeId);
            locker.addResource(siteNodeId);
        }

        locker.getLock();
        return phyNodeIdList;
    }

    /**
     * based on siteLink, find out all siteNode with route and change these siteNode's rack friendly
     * name
     *
     * @param phyNodeIdList
     * @param friendlyName
     * @throws CommonException
     */
    private void renameRackFriendlyNameByLink(Set<String> phyNodeIdList, String friendlyName)
            throws CommonException {
        log.debug("start update rack friendly name");
        SiteNodeDao siteNodeDao = SpringBeanFinder.getBean(SiteNodeDao.class);
        RackDao rackDao = SpringBeanFinder.getBean(RackDao.class);

        for (String phyNodeId : phyNodeIdList) {
            String siteNodeId = PhysicalNodeIdNamingRule.getSiteId(phyNodeId);
            Node node = siteNodeDao.getSiteNodeById(siteNodeId);
            List<SupportingRack> rackList = node.getAugmentation(Node1.class).getSite()
                    .getSupportingRack();
            for (SupportingRack rack : rackList) {
                if (rack.getSupportingNe().get(0).getNodeRef().getValue().equals(phyNodeId)) {
                    SupportingRack newRack = new SupportingRackBuilder()
                            .setFriendlyName(friendlyName)
                            .setRackId(rack.getRackId())
                            .setKey(rack.getKey())
                            .build();

                    rackDao.saveRack2Site(siteNodeId, newRack);
                    break;
                }
            }
        }
    }

    private Set<String> getRoutePhyNode(List<PathRouteObject> proList) {
        Set<String> phyNodeIdSet = new HashSet<>();

        for (PathRouteObject pathRouteObject : proList) {
            if (pathRouteObject.getResourceType() instanceof org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Tp) {
                Tp tp = (Tp) pathRouteObject.getResourceType();
                phyNodeIdSet.add(tp.getTpHop().getNodeRef().getValue());
            }
        }
        return phyNodeIdSet;
    }

    private Link check(String siteLinkId, String friendlyName) throws CommonException {
        if (friendlyName.length() > Constant.friendlyNameLength) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "sitelink's friendly-name should be smaller than 64 characters");
        }

        SiteLinkDao siteLinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);
        Link link = siteLinkDao.getSiteLinkById(siteLinkId);
        Link1 link1 = link.getAugmentation(Link1.class);
        if (link1.getSite() != null
                && link1.getSite().getFriendlyName() != null
                && friendlyName.equals(link1.getSite().getFriendlyName())
                && !link.getLinkId().getValue().equals(siteLinkId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Duplicate friendlyName");
        }

        return link;
    }

    public SiteLinkFriendlyNameUpdater setTaskInfo(TaskInfoMessage taskInfoMessage) {
        this.taskInfoMessage = taskInfoMessage;
//    this.kafka = SpringBeanFinder.getBean(TaskInfoKafkaService.class);

        return this;
    }
}
