/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.site.nbi.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.allocate.link.site.SiteLinkNodeOperationState;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import net.flex.dci.otn.controller.implement.common.enums.BusinessLinkType;
import net.flex.dci.otn.controller.implement.common.lifecycle.LifeCycleSevice;
import net.flex.dci.otn.controller.implement.common.opList.OpType;
import net.flex.dci.otn.controller.implement.common.opList.TypeUtils;
import net.flex.dci.otn.controller.implement.common.service.MyExecutor;
import net.flex.dci.otn.controller.implement.common.utils.ActionNotificationMessage;
import net.flex.dci.otn.controller.implement.common.utils.NeManagementChecker;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.RpcResultType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.topology.type.OchTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.UpdateLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 */
@Slf4j
@Component
public class SiteLinkImplSyncService {

    @Autowired
    private ImplConfig implConfig;

    @Autowired
    private PhyNodeDao phyNodeDao;

    @Autowired
    private TunnelDao tunnelDao;

    @Autowired
    private NeManagementChecker neManagementChecker;

    //===================================================
    //
    // init env
    //
    //===================================================
    public UpdateLinkOutput start(UpdateLinkInput input, String who) throws CommonException {
        log.info("start to update sitelink sync input:{}", input);

        if (input.getImplementState().equals(ImplementState.Allocate) || input.getAdminState()
                .equals(AdminStatus.Down)) {
            if (implConfig.isDisableDeImplSiteLink()) {
                return new UpdateLinkOutputBuilder()
                        .setReturnCode(RpcResultType.ResourceAccessFail)
                        .setReturnMessage("禁止删除复用段配置")
                        .build();
            }
        }

        Link siteLink = checkParam(input);
        UpdateLinkOutput nodeOperationStateFailure = nodeOperationStateFailure(siteLink);
        if (nodeOperationStateFailure != null) {
            return nodeOperationStateFailure;
        }
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();

        ActionType actionType =
            ImplementState.Implement.equals(input.getImplementState()) ? ActionType.implement
                : ActionType.deimplement;

        if (input.getImplementState().equals(siteLinkAttr.getImplementState())) {
            log.debug("the link has worked in wanted state {}", input.getImplementState());

                ActionNotificationMessage
                    .sendLinkImplementMethodDoneNotification(BusinessLinkType.SITE_LINK, siteLinkAttr.getFriendlyName(),
                        actionType);

            return new UpdateLinkOutputBuilder()
                    .setReturnCode(RpcResultType.Success)
                    .build();
        }

        LifeCycleSevice lifeService = new LifeCycleSevice();
        lifeService.logStartLinkImpl(siteLink.getLinkId().getValue(),
                TaskInfoMessage.ResourceType.siteLink,
                siteLinkAttr.getFriendlyName(),
                actionType,
                who, null);

        SiteLinkImplementor implementor = new SiteLinkImplementor(siteLink.getLinkId().getValue(),
                input.getImplementState(), lifeService);

        // 异步队列只保留 ID、名称和目标状态，避免闭包持有复用段完整路由及请求对象。
        String siteLinkId = siteLink.getLinkId().getValue();
        String friendlyName = siteLinkAttr.getFriendlyName();
        ImplementState targetState = input.getImplementState();
        //由于耗时, 把这个同步命令改为异步
        MyExecutor executor = SpringBeanFinder.getBean(MyExecutor.class);
        executor.lazyDo(new Runnable() {
            @Override
            public void run() {
                try {
                    log.debug("async {} working on siteLink {}", targetState.name(),
                            siteLinkId);
                    ActionNotificationMessage.sendLinkImplementMethodStartNotification(
                            BusinessLinkType.SITE_LINK, friendlyName,
                            actionType);
                    implementor.startSyncAction();
                } catch (Exception e) {
                    log.error("error on impl", e);
                }

            }
        });

        return new UpdateLinkOutputBuilder()
                .setReturnCode(RpcResultType.AcceptAndStartAsync)
                .build();
    }

    /**
     * @param input
     * @return which siteLink will be operated
     * @throws CommonException
     */
    private Link checkParam(UpdateLinkInput input) throws CommonException {
        ChangedObject changedObject = new ChangedObject();
        if (input.getImplementState() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "implementState is mandatory");
        }

        OpType opType = TypeUtils.fromImplementState(input.getImplementState());
        if (opType == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "implement-state must be allocate or implement");
        }

        if (input.getLinkId() == null || input.getLinkId().isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "sitelinkId is mandatory");
        }

        Link siteLink = changedObject.getChangedSiteLink(input.getLinkId());
        if (siteLink == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find required siteLink " + input.getLinkId());
        }
        if (opType.equals(OpType.IMPLEMENT)) {
            RouteInfo rInfo = new RouteInfo();
            Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
            rInfo.parse(siteLinkAttr.getExplictRoute().getRoute());

            neManagementChecker.checking(rInfo.getNodeIdList());
        }

        if (opType.equals(OpType.DEIMPLEMENT)) {
            //does all tunnels has deimpl
            List<Tunnel> tunnelList = getTunnels(siteLink);
            for (Tunnel tunnel : tunnelList) {
                if (tunnel.getImplementState().getIntValue()
                        != ImplementState.Allocate.getIntValue()) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "tunnel is still working " + tunnel.getFriendlyName());
                }
            }

            //checkingOscIP(siteLink);
        }
        return siteLink;
    }

    static UpdateLinkOutput nodeOperationStateFailure(Link siteLink) {
        String nodeOperationState = SiteLinkNodeOperationState.get(siteLink);
        if (nodeOperationState == null) {
            return null;
        }
        return new UpdateLinkOutputBuilder()
                .setReturnCode(RpcResultType.ResourceAccessFail)
                .setReturnMessage(String.format(
                        "siteLink cannot be implemented or deimplemented while node-operation-state is %s",
                        nodeOperationState))
                .build();
    }

    private void checkingOscIP(Link siteLink) {
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
        RouteInfo rInfo = new RouteInfo();
        rInfo.parse(siteLinkAttr.getExplictRoute().getRoute());
        for (String tpId : rInfo.getTpIdList()) {
            String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
            TerminationPoint tp = phyNodeDao.getOpPhyTpById(nodeId, tpId);
            if (tp != null) {
                Physical tpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();
                if (tpAttr.getDcn() != null && tpAttr.getDcn().getIp() != null) {
                    throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                            "this site link related OSC has IP address, cannot deImplement this siteLink");
                }
            }
        }
    }

    private List<Tunnel> getTunnels(Link siteLink) throws CommonException {
//        ChangedObject changedObject = new ChangedObject();
        log.debug("start to get the site link:{} related tunnels", siteLink.getLinkId().getValue());
        List<Tunnel> tunnels = new ArrayList<>();
        if (siteLink.getAugmentation(Link1.class) != null
                && siteLink.getAugmentation(Link1.class).getSite() != null
                && siteLink.getAugmentation(Link1.class).getSite().getSupportedLink() != null
                && !siteLink.getAugmentation(Link1.class).getSite().getSupportedLink().isEmpty()) {
            List<SupportedLink> supportedLinks = siteLink.getAugmentation(Link1.class).getSite()
                    .getSupportedLink();
            List<String> supportedOchLinkIds = supportedLinks.stream()
                    .filter(spl -> spl.getTopologyRef().getValue().equals(
                            OchTopology.QNAME.getLocalName()))
                    .map(spl -> spl.getLinkRef().getValue()).collect(
                            Collectors.toList());
            List<Tunnel> tunnelList = tunnelDao.getAllTunnelsUnderOchLink(supportedOchLinkIds);
            tunnels.addAll(tunnelList);
//            for (SupportedLink supportedLink : siteLink.getAugmentation(Link1.class).getSite()
//                    .getSupportedLink()) {
//                if (supportedLink.getTopologyRef().getValue()
//                        .equals(OchTopology.QNAME.getLocalName())) {
//                    Link ochLink = changedObject.getChangedOchLink(
//                            supportedLink.getLinkRef().getValue());
//
//                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1 ochLink1 = ochLink
//                            .getAugmentation(
//                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class);
//                    if (ochLink1.getSupportedTunnel() != null && !ochLink1.getSupportedTunnel()
//                            .isEmpty()) {
//                        for (SupportedTunnel supportedTunnel : ochLink1.getSupportedTunnel()) {
//                            Tunnel tunnel = changedObject.getChangedTunnel(
//                                    supportedTunnel.getTunnelRef().getValue());
//
//                            if (tunnel != null) {
//                                tunnels.add(tunnel);
//                            }
//                        }
//                    }
//                }
//            }
        }
        return tunnels;
    }
}
