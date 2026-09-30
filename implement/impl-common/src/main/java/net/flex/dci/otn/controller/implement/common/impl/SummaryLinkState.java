package net.flex.dci.otn.controller.implement.common.impl;

import com.google.common.util.concurrent.FutureCallback;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.DataTimeConvert;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.recorder.StepRecord;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.link.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.TunnelBuilder;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

@Slf4j
public class SummaryLinkState implements FutureCallback<List<StepRecord>> {
    protected final BiConsumer<Boolean, Throwable> onFinish;


    //following constant format should same as ConfigNeSequence
    private final static String FORMAT_BASE_INFO = "baseInfo@%s@";
    private final static String FORMAT_IL = "link@%s@";
    private final static String FORMAT_EQ = "equipment@%s@";
    private final static String FORMAT_XC = "xc@%s@";
    private final static String FORMAT_TP = "tp@%s@";
    private final static String FORMAT_OCM = "ocmInfo@%s@";

    protected String linkId;
    private LinkImplementState.LinkType linkType;
    protected ImplActionType actionType;
    private RouteInfo rInfo;

    private ChangedObject changedObject;

    public SummaryLinkState(ChangedObject changedObject, LinkImplementState.LinkType linkType, String linkId,
                            RouteInfo rInfo, ImplActionType actionType,
                            BiConsumer<Boolean, Throwable> onFinish) {
        this.linkType = linkType;
        this.linkId = linkId;
        this.rInfo = rInfo;
        this.actionType = actionType;
        this.onFinish = onFinish;

        this.changedObject = changedObject;
    }

    @Override
    public void onSuccess(@Nullable List<StepRecord> stepRecords) {
        List<StepRecord.Property> allResults = new ArrayList<>();
        ImplementState latestState = null;
        String msg = "";
        try {
            Gson gson = new Gson();
            for (StepRecord stepRecord : stepRecords) {
                log.debug("action has done {} ({}) \n {} ", stepRecord.getNodeId(), stepRecord.getFriendlyName(), gson.toJson(stepRecord.getProperties().getPropertyList()));
                allResults.addAll(stepRecord.getProperties().getPropertyList());
            }

            latestState = updateLinkStates(allResults);
        } catch (Exception e) {
            msg = String.format("internal error %s", e.getMessage());
            log.error(msg, e);
        }

        long fail = allResults.stream().filter(e -> e.getValue().equals(ConfigNeSequence.STATUS_FAILURE)).count();
        if (fail > 0) {
            log.error("error happen when writeing to ne");
        }

        if (onFinish != null) {
            if (fail > 0) {
                onFinish.accept(false, new Throwable("write device failure"));
            } else if (! msg.isEmpty()) {
                onFinish.accept(false, new Throwable(msg));
            } else {
                onFinish.accept(true, null);  // 成功
            }
        }

        log.info("update implementState has done. {} to {} ", latestState, linkId);
    }

    protected ImplementState updateLinkStates(List<StepRecord.Property> allResults) {

        ImplementState state = ImplementState.Plan;
        try {
            if (linkType.equals(LinkImplementState.LinkType.SiteLink)) {
                state = updateSiteLinkImplState(linkId, allResults);
            } else if (linkType.equals(LinkImplementState.LinkType.Tunnel)) {
                state = updateTunnelImplState(linkId, allResults);
            } else if (linkType.equals(LinkImplementState.LinkType.OchLink)) {
                state = updateOchLinkImplState(linkId, allResults);
            } else if (linkType.equals(LinkImplementState.LinkType.PhyLink)) {
                state = updatePhyLinkImplState(linkId, allResults);
            } else if (linkType.equals(LinkImplementState.LinkType.ProtectionLeg)) {
                // 减腿的设备阶段只改变目标腿的物理资源。OCH 路由、SiteLink 关联以及所有
                // 客户 Tunnel 的状态必须留给 Binder 在设备成功后的 trim 阶段统一处理。
                state = checkLinkResourceWithAllResults(rInfo, allResults);
            } else {
                log.error("IMPOSSIBLE error, {} change implState isn't support yet.", linkType.name());
            }
        } catch (Exception e) {
            log.error("error happen ", e);
        }
        for (Link link : changedObject.getChangedPhyLinkList().values()) {
            log.debug("{} {}, {} ", link.getLinkId().getValue(), link.getAugmentation(Link1.class).getPhysical().getImplementState(), link.getAugmentation(Link1.class).getPhysical().getFriendlyName());
        }

        return state;
    }

    private ImplementState updateTunnelImplState(String tunnelId, List<StepRecord.Property> allResults) {
        log.debug("update tunnel impl state with all results");

        Tunnel tunnel = changedObject.getChangedTunnel(tunnelId);

        ImplementState endState;
        if (actionType.equals(ImplActionType.Deimplement)) {
            endState = ImplementState.Allocate;
        } else {
            endState = ImplementState.Implement;
        }
        ImplementState updatedState = endState;
        for (String srvLinkId : rInfo.getLogicServerLinkIdList()) {
            if (OchLinkIdNamingRule.isOchLink(srvLinkId)) {
                ImplementState newState = updateOchLinkImplState(srvLinkId, allResults);
                updatedState = acuumulateLinkImpltate(endState, newState);
            }
            if (SiteLinkIdNamingRule.isSiteLink(srvLinkId)) {
                updateSiteLinkImplState(changedObject.getChangedSiteLink(srvLinkId), ImplementState.Implement);
            }
        }
        if (updatedState.equals(endState)) {
            for (String srvLinkId : rInfo.getPhyLinkIdList()) {
                ImplementState newState = updatePhyLinkImplState(srvLinkId, allResults);
                updatedState = acuumulateLinkImpltate(endState, newState);
            }
        }

        if (endState.equals(updatedState)) {
            updatedState = checkLinkResourceWithAllResults(rInfo, allResults);
        }

        return updateTunnelImplState(tunnel, updatedState);
    }

    private ImplementState updateTunnelImplState(Tunnel tunnel, ImplementState summaryImplState) {
        log.debug("update Tunnel link {} {}", summaryImplState, tunnel.getTunnelId().getValue());
        if (tunnel.getImplementState().equals(summaryImplState)) {
            // The DB may already be ...ing even if this cached object is terminal.
            // Force store2DB to rewrite the unchanged terminal object without changing activationTime.
            changedObject.unsetTunnel(tunnel.getTunnelId().getValue());
            changedObject.addChangedTunnel(tunnel);
            return tunnel.getImplementState();
        } else {
            //update tunnel impl status.
            Tunnel newTunnel = new TunnelBuilder(tunnel)
                    .setImplementState(summaryImplState)
                    .setAdminState(summaryImplState.equals(ImplementState.Allocate) ? AdminStatus.Down : AdminStatus.Up)
                    .setActivationTime(summaryImplState.equals(ImplementState.Allocate) ? null : DataTimeConvert.convertToDateAndTime(DataTimeConvert.long2date(System.currentTimeMillis())))
                    .build();
            changedObject.addChangedTunnel(newTunnel);
            return summaryImplState;
        }
    }

    private ImplementState updateOchLinkImplState(String ochLinkId, List<StepRecord.Property> allResults) {
        log.debug("update OCH link implState with all results");

        ImplementState endState;
        if (actionType.equals(ImplActionType.Deimplement)) {
            endState = ImplementState.Allocate;
        } else {
            endState = ImplementState.Implement;
        }

        Link ochLink = changedObject.getChangedOchLink(ochLinkId);
        if (ochLink == null) {
            log.error("Here has bug, the ochLinkID {} has NOT OCH link, patch with original", ochLinkId);
            return endState;
        }

        ImplementState updateState = endState;
        for (String srvLinkId : rInfo.getLogicServerLinkIdList()) {
            if (SiteLinkIdNamingRule.isSiteLink(srvLinkId)) {
                //因为在och link 变化的时候对siteLink的状态，保持Implement 除非有错误

                Link siteLink = changedObject.getChangedSiteLink(srvLinkId);
                if (updateState.equals(ImplementState.PartialImplement)) {
                    updateSiteLinkImplState(siteLink, updateState);
                } else {
                    updateSiteLinkImplState(siteLink, ImplementState.Implement);
                }
            }
        }
        if (updateState.equals(endState)) {
            for (String srvLinkId : rInfo.getPhyLinkIdList()) {
                ImplementState newState = updatePhyLinkImplState(srvLinkId, allResults);
                updateState = acuumulateLinkImpltate(endState, newState);
            }
        }

        if (updateState.equals(endState)) {
            updateState = checkLinkResourceWithAllResults(rInfo, allResults);
        }

        return updateOchLinkImplState(ochLink, updateState);
    }

    private ImplementState updateOchLinkImplState(Link ochLink, ImplementState summaryImplState) {
        log.debug("update OCH link {} {}", summaryImplState, ochLink.getLinkId().getValue());

        Och ochLinkAttr = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class).getOch();
        if (ochLinkAttr.getImplementState().equals(summaryImplState)) {
            // The DB may already be ...ing even if this cached object is terminal.
            changedObject.unsetOchLink(ochLink.getLinkId().getValue());
            changedObject.addChangedOchLink(ochLink);
            return ochLinkAttr.getImplementState();
        } else {
            //update link impl status.
            Link newLink = new LinkBuilder(ochLink)
                    .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class, new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder()
                            .setOch(new OchBuilder(ochLinkAttr)
                                    .setImplementState(summaryImplState)
                                    .setAdminState(summaryImplState.equals(ImplementState.Allocate) ? AdminStatus.Down : AdminStatus.Up)
                                    .setActivationTime(summaryImplState.equals(ImplementState.Allocate) ? null : DataTimeConvert.convertToDateAndTime(DataTimeConvert.long2date(System.currentTimeMillis())))
                                    .build())
                            .build())
                    .build();
            changedObject.addChangedOchLink(newLink);
            return summaryImplState;
        }
    }

    //根据某层link的路由资源，查找allResults, 如果在allResult中没有错，说明这一层数据下发成功
    private ImplementState checkLinkResourceWithAllResults(RouteInfo linkRouteInfo, List<StepRecord.Property> allResults) {
        for (String nodeId : linkRouteInfo.getNodeIdList()) {
            String name = String.format(FORMAT_BASE_INFO, nodeId);
            for (StepRecord.Property prop : allResults) {
                if (prop.getName().startsWith(name)) {
                    if (prop.getValue().equals(ConfigNeSequence.STATUS_FAILURE)) {

                        return ImplementState.PartialImplement;
                    }
                }
            }
        }
        for (String tpId : linkRouteInfo.getTpIdList()) {
            String name = String.format(FORMAT_TP, tpId);
            for (StepRecord.Property prop : allResults) {
                if (prop.getName().startsWith(name)) {
                    if (prop.getValue().equals(ConfigNeSequence.STATUS_FAILURE)) {

                        return ImplementState.PartialImplement;
                    }
                }
            }
        }

        for (String eqId : linkRouteInfo.getEqIdList()) {
            String name = String.format(FORMAT_EQ, eqId);
            for (StepRecord.Property prop : allResults) {
                if (prop.getName().startsWith(name)) {
                    if (prop.getValue().equals(ConfigNeSequence.STATUS_FAILURE)) {

                        return ImplementState.PartialImplement;
                    }
                }
            }
        }

        for (String linkId : linkRouteInfo.getPhyLinkIdList()) {
            String name = String.format(FORMAT_IL, linkId);
            for (StepRecord.Property prop : allResults) {
                if (prop.getName().startsWith(name)) {
                    if (prop.getValue().equals(ConfigNeSequence.STATUS_FAILURE)) {

                        return ImplementState.PartialImplement;
                    }
                }
            }
        }


        for (String xcId : linkRouteInfo.getXcIdList()) {
            String name = String.format(FORMAT_XC, xcId);
            for (StepRecord.Property prop : allResults) {
                if (prop.getName().startsWith(name)) {
                    if (prop.getValue().equals(ConfigNeSequence.STATUS_FAILURE)) {

                        return ImplementState.PartialImplement;
                    }
                }
            }
        }


        for (String nodeId : linkRouteInfo.getNodeIdList()) {
            String ocmName = String.format(FORMAT_OCM, nodeId);
            for (StepRecord.Property prop : allResults) {
                if (prop.getName().startsWith(ocmName)) {
                    if (prop.getValue().equals(ConfigNeSequence.STATUS_FAILURE)) {

                        return ImplementState.PartialImplement;
                    }
                }
            }
        }
        if (actionType.equals(ImplActionType.Deimplement)) {
            return ImplementState.Allocate;
        } else {
            return ImplementState.Implement;
        }
    }

    private ImplementState updateSiteLinkImplState(String siteLinkId, List<StepRecord.Property> allResults) {
        log.debug("update SITE link implState with all results");

        Link siteLink = changedObject.getChangedSiteLink(siteLinkId);

        ImplementState endState;
        if (actionType.equals(ImplActionType.Deimplement)) {
            endState = ImplementState.Allocate;
        } else {
            endState = ImplementState.Implement;
        }
        ImplementState updateState = endState;
        for (String phyLinkId : rInfo.getPhyLinkIdList()) {
            ImplementState newState = updatePhyLinkImplState(phyLinkId, allResults);
            updateState = acuumulateLinkImpltate(endState, newState);
        }

        if (updateState == endState) {
            //管理用的Phylink 不在route 中，单独处理
            for (SupportingLink sl : siteLink.getSupportingLink()) {
                String linkId = sl.getLinkRef().getValue();
                if (PhysicalLinkIdNamingRule.isCableLink(linkId)) {
                    rInfo.getPhyLinkIdList().add(linkId);
                    rInfo.getTpIdList().add(PhysicalLinkIdNamingRule.getTpAId(linkId));
                    rInfo.getTpIdList().add(PhysicalLinkIdNamingRule.getTpZId(linkId));
                }
            }


            updateState = checkLinkResourceWithAllResults(rInfo, allResults);
        }
        return updateSiteLinkImplState(siteLink, updateState);
    }

    private ImplementState acuumulateLinkImpltate(ImplementState endState, ImplementState newState) {
        if (endState.equals(ImplementState.PartialImplement)) {
            return endState;
        } else if (endState.equals(newState)) {
            return endState;
        } else {
            return ImplementState.PartialImplement;
        }
    }

    private ImplementState updateSiteLinkImplState(Link siteLink, ImplementState summaryImplState) {
        log.debug("update SITE link {} {}", summaryImplState, siteLink.getLinkId().getValue());
        Site siteLinkAttr = siteLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class).getSite();

        Link newLink;
        if (!siteLinkAttr.getImplementState().equals(summaryImplState)) {
            //update link impl status.
            newLink = new LinkBuilder(siteLink)
                    .addAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class, new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                            .setSite(new SiteBuilder(siteLinkAttr)
                                    .setImplementState(summaryImplState)
                                    .setAdminState(summaryImplState.equals(ImplementState.Allocate) ? AdminStatus.Down : AdminStatus.Up)
                                    .setActivationTime(summaryImplState.equals(ImplementState.Allocate) ? null : DataTimeConvert.convertToDateAndTime(DataTimeConvert.long2date(System.currentTimeMillis())))
                                    .build())
                            .build())
                    .build();
        } else {
            // The DB may already be ...ing even if this cached object is terminal.
            changedObject.unsetSiteLink(siteLink.getLinkId().getValue());
            newLink = siteLink;
        }

        changedObject.addChangedSiteLink(newLink);

        log.debug("the sitelink status change to {}", summaryImplState);
        return summaryImplState;
    }

    private ImplementState updatePhyLinkImplState(String phyLinkId, List<StepRecord.Property> allResults) {
        log.debug("update PHY link implState with all results");

        ImplementState target;
        if (actionType.equals(ImplActionType.Deimplement)) {
            target = ImplementState.Allocate;
        } else {
            target = ImplementState.Implement;
        }
        Link phyLink = changedObject.getChangedPhyLink(phyLinkId);
        if (phyLink == null) {
            try {
                throw new NullPointerException("impossible, database has issue");
            } catch (NullPointerException e) {
                log.error("!!! phyLink: {}", phyLinkId, e);
                return target;
            }
        }
        Physical phyLinkAttr = phyLink.getAugmentation(Link1.class).getPhysical();
        if (phyLinkAttr.getImplementState().equals(actionType))
            return target;

        ImplementState newState = checkLinkResourceWithAllResults(rInfo, allResults);
        //checking internalLink not consider.

        return updatePhyLinkImplState(phyLink, newState);
    }

    private ImplementState updatePhyLinkImplState(Link phyLink, ImplementState summaryImplState) {
        log.debug("update PHY link {} {}", summaryImplState, phyLink.getLinkId().getValue());
        ImplementState endState = summaryImplState;
        Physical phyLinkAttr = phyLink.getAugmentation(Link1.class).getPhysical();
        if (phyLinkAttr.getImplementState().equals(summaryImplState)) {
            // The DB may already be ...ing even if this cached object is terminal.
            changedObject.unsetPhyLink(phyLink.getLinkId().getValue());
            changedObject.addChangedPhyLink(phyLink);
            return summaryImplState;
        } else {
            //update link impl status.
            Link newLink = new LinkBuilder(phyLink)
                    .addAugmentation(Link1.class, new Link1Builder()
                            .setPhysical(new PhysicalBuilder(phyLinkAttr)
                                    .setImplementState(summaryImplState)
                                    .setAdminState(summaryImplState.equals(ImplementState.Implement) ? AdminStatus.Up : AdminStatus.Down)
                                    .setActivationTime(summaryImplState.equals(ImplementState.Allocate) ? null : DataTimeConvert.convertToDateAndTime(DataTimeConvert.long2date(System.currentTimeMillis())))
                                    .build())
                            .build())
                    .build();
            changedObject.addChangedPhyLink(newLink);
            endState = summaryImplState;
        }
        return endState;
    }

    @Override
    public void onFailure(Throwable throwable) {
        log.error("error happen {}", throwable.getMessage());
        if (onFinish != null) {
            onFinish.accept(false, throwable);  // 失败
        }
    }
}

