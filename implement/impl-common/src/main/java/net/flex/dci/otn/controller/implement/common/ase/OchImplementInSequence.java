package net.flex.dci.otn.controller.implement.common.ase;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.impl.ImplActionType;
import net.flex.dci.otn.controller.implement.common.impl.LinkImplementState;
import net.flex.dci.otn.controller.implement.common.impl.OcmUpdator;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.utils.RouteExtractor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Slf4j
public class OchImplementInSequence {
    private List<Link> ochLinkList;
    private long taskGroupId;
    private Link impactedSiteLink;
    private ChangedObject changedObject;

    public OchImplementInSequence(ChangedObject changedObject, Link impactedSiteLink) {
        this.changedObject = changedObject;
        this.impactedSiteLink = impactedSiteLink;
    }

    public void setOchList(List<Link> ochList) {
        this.ochLinkList = ochList;
    }

    public void setTaskGroupId(Long taskGroupId) {
        this.taskGroupId = taskGroupId;
    }

    public void startRemove() {
        try {
            setRemovedOchPowerControlManual();
            executeOchImplsInSequenceAsync(true);
            log.debug("remove dummy OCH link has done.");
        } catch (Exception e) {
            log.error("remove OCH error", e);
            throw new RuntimeException("failure when OCH remove", e);
        }
    }

    public void startInsert() {
        try {
            executeOchImplsInSequenceAsync(false);
            log.debug("insert dummy OCH link has done.");
        } catch (Exception e) {
            log.error("insert OCH error", e);
            throw new RuntimeException("failure on OCH injection", ExceptionUtils.getRootCause(e));
        }
    }

    private void executeOchImplsInSequenceAsync(boolean toAllocate) {
        for (Link ochLink : ochLinkList) {
            processSingleOchLink(ochLink, toAllocate);
        }
    }

    private void processSingleOchLink(Link ochLink, boolean toAllocate) {
        log.info("start action dummy och: {} {}", toAllocate ? "Allocate" : "Implement", ochLink.getLinkId());

        if (toAllocate) {
            // startRemove() has already switched every dummy OCH that may be
            // included in this changedObject batch to MANUAL. Repeating the
            // operation here would be harmless but slow, and the important
            // guarantee is that all later XC deletes are protected up front.
            log.debug("dummy OCH power-control-mode has been prepared before remove: {}",
                    ochLink.getLinkId().getValue());
        }

        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();

        OcmUpdator ocmUpdator = new OcmUpdator(changedObject, ochLink);
        if (toAllocate) {
            ocmUpdator.remove(ochLink);
        } else {
            ocmUpdator.insert(ochLink);
        }
        ocmUpdator.getUpdatedNode().forEach(x -> changedObject.addChangedPhyNode(x));

        String friendlyName = ochLinkAttr.getFriendlyName();

        LifeCycleSevice lifeService = new LifeCycleSevice();
        lifeService.logStartLinkImpl(ochLink.getLinkId().getValue(),
                TaskInfoMessage.ResourceType.ochlink,
                friendlyName,
                toAllocate ? TaskInfoMessage.ActionType.deimplement : TaskInfoMessage.ActionType.implement,
                "", taskGroupId);

        RouteInfo rInfo = new RouteInfo();
        rInfo.parse(ochLinkAttr.getExplictRoute().getRoute());

        //关键点：OCH 路由中没有包含ILA， 因为ILA没有XC，所以这个地方需要把ocmUpdator提前出来的ILA网元的OCM也需要加入
        Set<String> nodeIdSet = new HashSet<>(rInfo.getNodeIdList());
        nodeIdSet.addAll(ocmUpdator.getUpdatedNode()
                .stream().map(x->x.getNodeId().getValue())
                .collect(Collectors.toList()));
        rInfo.setNodeIdList(new ArrayList<>(nodeIdSet));

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        LinkImplementState implementor = new LinkImplementState(LinkImplementState.LinkType.OchLink,
                ochLink.getLinkId().getValue(),
                friendlyName,
                rInfo,
                (success, throwable) -> {
                    if (throwable != null) {
                        String msg = String.format("%s Action on dummy OCH failure: (%s). %s",
                                toAllocate ? "deImplement" : "Implement",
                                friendlyName, throwable);
                        log.error(msg, throwable);
                        future.completeExceptionally(throwable);
                    } else {
                        future.complete(success);
                    }
                });

        String msg = null;
        try {
            if (toAllocate) {
                implementor.setActionType(ImplActionType.Deimplement)
                        .changeAs(changedObject, lifeService);
            } else {
                implementor.setActionType(ImplActionType.Implement)
                        .changeAs(changedObject, lifeService);
            }

            future.get();
            if (toAllocate) {
                DummyOchLinkConstructor ochLinkConstructor = new DummyOchLinkConstructor(changedObject);
                ochLinkConstructor.remove(ochLink);
            } else {
                ochLinkImplemented(ochLink);
            }
        }catch (InterruptedException e) {
            log.error("Thread was interrupted", e);
            Thread.currentThread().interrupt(); // Restore interrupt status
            msg = (e.getMessage() != null) ? e.getMessage() : e.toString();
            throw new RuntimeException("Thread was interrupted during action on NE");
        } catch (ExecutionException e) {
            log.error("Exception while waiting for NE action to complete", e.getCause());
            msg = (e.getMessage() != null) ? e.getMessage() : e.toString();
            throw new RuntimeException(e.getCause().getMessage(), ExceptionUtils.getRootCause(e));
        } finally {
            lifeService.logEndLinkImpl(msg);
        }
    }

    private void setRemovedOchPowerControlManual() {
        Map<String, List<String>> groupedByNodeId = new HashMap<>();
        if (ochLinkList == null || ochLinkList.isEmpty()) {
            return;
        }

        for (Link ochLink : ochLinkList) {
            if (ochLink == null || ochLink.getAugmentation(Link1.class) == null) {
                continue;
            }
            Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
            if (ochLinkAttr == null || ochLinkAttr.getExplictRoute() == null) {
                continue;
            }

            RouteExtractor.extractorXc(ochLinkAttr.getExplictRoute().getRoute()).stream()
                    .filter(xc -> xc.getWssChannel() != null)
                    .map(CrossConnectionAttributes::getCrossConnectionId)
                    .map(uri -> uri.getValue())
                    .forEach(xcId -> groupedByNodeId
                            .computeIfAbsent(PhysicalXcIdNamingRule.getNodeId(xcId), k -> new ArrayList<>())
                            .add(xcId));
        }

        if (groupedByNodeId.isEmpty()) {
            return;
        }

        // LinkImplementState may build one NE delete payload from the shared
        // changedObject, so a single dummy OCH action can carry WSS XCs from
        // other dummy OCHs. Set MANUAL for the whole remove batch before the
        // first delete to avoid auto-control IN_PROGRESS failures.
        AseInjectModeUpdator modeUpdator = new AseInjectModeUpdator(changedObject);
        String siteLinkId = impactedSiteLink == null ? "" : impactedSiteLink.getLinkId().getValue();
        modeUpdator.updateMCSrc2DstPowerControlModel(groupedByNodeId,
                siteLinkId, taskGroupId);
    }

    private void ochLinkImplemented(Link ochLink) {
        Link newLink = new LinkBuilder(ochLink).addAugmentation(Link1.class,
                new Link1Builder().setOch(
                        new OchBuilder(ochLink.getAugmentation(Link1.class).getOch())
                                .setImplementState(ImplementState.Implement)
                                .setAdminState(AdminStatus.Up)
                                .build())
                        .build())
                .build();
        changedObject.addChangedOchLink(newLink);
    }


    //change to ing and update ocm table
    private void updateOchResource(Link ochLink, boolean toAllocate) {

        Link newLink = updateOchImplStateToIng(ochLink, toAllocate);
        changedObject.addChangedOchLink(newLink);

        OcmUpdator ocmUpdator = new OcmUpdator(changedObject, ochLink);

        String ochLinkId = ochLink.getLinkId().getValue();
        if (toAllocate) {
            impactedSiteLink = removeDummyOchRelation(impactedSiteLink, ochLinkId);
            ocmUpdator.remove(ochLink);
        } else {
            impactedSiteLink = insertDummyOchRelation(impactedSiteLink, ochLinkId);
            ocmUpdator.insert(ochLink);
        }

        changedObject.addChangedSiteLink(impactedSiteLink);

        Collection<Node> nodeList = ocmUpdator.getUpdatedNode();
        for (Node node : nodeList) {
            changedObject.addChangedPhyNode(node);
        }

    }

    private Collection<Node> updateOcm(Link ochLink, boolean toAllocate) {
        OcmUpdator ocmUpdator = new OcmUpdator(changedObject, ochLink);
        if (toAllocate)
            ocmUpdator.remove(ochLink);
        else
            ocmUpdator.insert(ochLink);
        return ocmUpdator.getUpdatedNode();
    }

    private Link updateOchImplStateToIng(Link ochLink, boolean toAllocate) {
        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
        Link newLink = new LinkBuilder(ochLink).addAugmentation(Link1.class, new Link1Builder()
                .setOch(new OchBuilder(ochLinkAttr)
                        .setImplementState(toAllocate ? ImplementState.Deimplementing : ImplementState.Doimplementing)
                        .build())
                .build()).build();

        return newLink;
    }


    public Link insertDummyOchRelation(Link siteLink, String ochLinkId) {
        Site siteLinkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
        List<String> newDummyLinkList = new ArrayList<>();
        if (siteLinkAttr.getDummyLink() != null) {
            newDummyLinkList = new ArrayList<>(siteLinkAttr.getDummyLink());
        }
        newDummyLinkList.add(ochLinkId);
        return new LinkBuilder(siteLink).addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class, new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                        .setSite(new SiteBuilder(siteLinkAttr)
                                .setDummyLink(newDummyLinkList)
                                .build())
                        .build())
                .build();
    }


    public Link removeDummyOchRelation(Link siteLink, String ochLinkId) {
        Site siteLinkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();
        List<String> newDummyLinkList = new ArrayList<>(siteLinkAttr.getDummyLink());

        newDummyLinkList.removeIf(x -> x.equals(ochLinkId));
        return new LinkBuilder(siteLink).addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class, new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                        .setSite(new SiteBuilder(siteLinkAttr)
                                .setDummyLink(newDummyLinkList)
                                .build())
                        .build())
                .build();
    }
}
