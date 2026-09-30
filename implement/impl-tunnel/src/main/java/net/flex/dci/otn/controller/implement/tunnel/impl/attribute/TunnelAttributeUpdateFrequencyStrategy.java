/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.tunnel.impl.attribute;

import static net.flex.dci.otn.controller.implement.common.utils.CommonUtils.logMessage;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.TerminationPointDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otc.tools.lock.impl.zk.ZkResourceLock;
import net.flex.dci.otn.controller.implement.common.utils.OpNodeMerger;
import net.flex.dci.otn.controller.implement.tunnel.impl.frequency.Frequency;
import net.flex.dci.otn.controller.implement.tunnel.impl.frequency.PhysicalLink;
import net.flex.dci.otn.controller.implement.tunnel.impl.util.OchLinkRoute;
import net.flex.dci.otn.controller.implement.tunnel.impl.util.TunnelRoute;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ConnectionStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinks;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinksBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.internal.links.InternalLinksKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuLineBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.SiteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.TunnelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class TunnelAttributeUpdateFrequencyStrategy extends AbstractTunnelAttributeUpdateStrategy {

    //    private static final String LowMUX64 = "191325000";
//    private static final String UppMUX64 = "196125000";
//
//    private static final Pattern SLOT_PATTERN = Pattern.compile(
//            "XC-(Site-\\d+#Ne-\\d+)#\\w+-\\d+-\\d+#PORT-(\\d+-\\d+).*");
    //    private static TunnelAttributeUpdateFrequencyStrategy inst = null;
    @Autowired
    private MultipleTransaction mongoTransaction;

    @Autowired
    private TerminationPointDao terminationPointDao;

//    private TunnelAttributeUpdateFrequencyStrategy() {
//    }
//
//    public static TunnelAttributeUpdateFrequencyStrategy instance() {
//        if (inst == null) {
//            inst = new TunnelAttributeUpdateFrequencyStrategy();
//        }
//        return inst;
//    }

    @Override
    public boolean supports(UpdateTunnelInput updateTunnelInput) {
        return StringUtils.hasText(updateTunnelInput.getFrequency());
    }

    @Override
    public void execute(String tunnelId, UpdateTunnelInput updateTunnelInput,
            TaskInfoMessage taskInfoMessage) {
        log.info("update tunnel:{} attribute frequency ", tunnelId);
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        if (null == tunnel) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("cannot find required tunnel %s.", tunnelId));
        }
        if (ImplementState.Allocate != tunnel.getImplementState()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("Tunnel[%s] ImplementState must be allocate.", tunnelId));
        }
        updateFrequency(tunnelId, updateTunnelInput.getFrequency(), taskInfoMessage);
    }

    @Override
    public ActionType taskActionType() {
        return ActionType.changeFrequency;
    }

    public void updateFrequency(String tunnelId, String frequencyStr,
            TaskInfoMessage taskInfoMessage) throws CommonException {
        TunnelFrequencyUpdateContext context = new TunnelFrequencyUpdateContext();
        ChangedObject changedObject = new ChangedObject();
        context.setChangedObject(changedObject);
        ZkResourceLock lock = new ZkResourceLock();
        Frequency frequency = checkParamAndGetFrequency(tunnelId, frequencyStr, context);

        try {
            lockResource(lock, tunnelId, context);

            List<TerminationPoint> otuLineTps = updateOchFrequency(context.getOchLinkId(), frequency, context);
            mongoTransaction.save(context.getChangedObject());

            updateOtuLineTpCentreFrequency(otuLineTps, frequency.getCentFreq(), context);

            logMessage(BroadCastConstant.UPDATE_TUNNEL_FREQUENCY, context.getTunnelName(), BLANK,
                    taskInfoMessage);

            //tmp solution for delivery
            Link ochLink = context.getChangedObject().getChangedOchLink(context.getOchLinkId());
            RouteInfo rInfo = new RouteInfo();
            rInfo.parse(ochLink.getAugmentation(Link1.class).getOch().getExplictRoute().getRoute());
            log.debug("after save to mongo, start merge to OP");
            OpNodeMerger opMerger = new OpNodeMerger();
            rInfo.getNodeIdList().forEach(opMerger::merge);
            log.debug("merge to OP done");

        } catch (Exception e) {
            log.error("update tunnel frequency error", e);
            logMessage(BroadCastConstant.UPDATE_TUNNEL_FREQUENCY, context.getTunnelName(),
                    e.getMessage(), taskInfoMessage);
        } finally {
            lock.unlock();
        }
    }

    private void lockResource(ZkResourceLock lock, String tunnelId,
            TunnelFrequencyUpdateContext context) {
        lock.addResource(tunnelId);
        lock.addResource(context.getOchLinkId());
        for (String siteLinkId : context.getSiteLinkIds()) {
            lock.addResource(siteLinkId);
        }
        // Frequency change rewrites OCH route and related phy nodes. Lock the nodes
        // as implement flow does, otherwise a long running ASE task may save an old
        // node snapshot after this change and remove the new WSS XC from DB.
        for (String nodeId : context.getNodeIds()) {
            lock.addResource(nodeId);
        }
        lock.getLock();
    }

    /**
     * 所有复用段都是一样的grid, 然后指定frequency在他们里面都是空闲的 最后只能相同的频谱范围才可以 （C波段/L波段）
     *
     * @param tunnelId
     * @param frequencyString
     * @param context
     * @return
     * @throws CommonException
     */
    private Frequency checkParamAndGetFrequency(String tunnelId, String frequencyString,
            TunnelFrequencyUpdateContext context) throws CommonException {
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        if (tunnel == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("Tunnel[%s] does not exist.", tunnelId));
        }

        if (tunnel.getImplementState() != ImplementState.Allocate) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("tunnel[%s] ImplementState must be allocate",
                            tunnel.getFriendlyName()));
        }

        context.setTunnelName(tunnel.getFriendlyName());

        String ochLinkId = TunnelRoute.getOchLinkId(tunnel);
        if (ochLinkId == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("Tunnel[%s] route error, cannot find out ochLink.",
                            tunnel.getFriendlyName()));
        }

        //the format is channelID:lowerFrequecy,higherFrequecy.
        Link ochLink = context.getChangedObject().getChangedOchLink(ochLinkId);
        if (ochLink == null) {
            log.error("cannot find out ochLink {}", ochLinkId);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("cannot find out OchLink[%s]", ochLinkId));
        }
        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
        if (ochLinkAttr.getImplementState() != ImplementState.Allocate) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "All tunnels over this L Port must be allocate");
        }

        context.setOchLinkId(ochLinkId);

        Node node = context.getChangedObject()
                .getChangedPhyNode(ochLink.getSource().getSourceNode().getValue());
        NeYangModel yangModel = NeYangModel.getModel(
                node.getAugmentation(Node1.class).getPhysical());
        context.setYangModel(yangModel);
        Frequency frequency = new Frequency(yangModel, frequencyString);

        List<String> siteLinkIds = OchLinkRoute.getSiteLinkIds(ochLink);
        context.setSiteLinkIds(siteLinkIds);
        RouteInfo ochRouteInfo = new RouteInfo();
        ochRouteInfo.parse(ochLinkAttr.getExplictRoute().getRoute());
        context.setNodeIds(ochRouteInfo.getNodeIdList());
        List<Link> siteLinkList = setGridTypeInAllSiteLinks(siteLinkIds, context);

        //在roadm环境中，frequency必须在所有siteLink中是free的
        List<Long> avaList = FrequencyAvailable.getFreeCentFrequency(siteLinkList,
                getGridType(ochLinkAttr.getUpperFrequency(), ochLinkAttr.getLowerFrequency()));
        long newCenter = frequency.getCentFreq().longValue();
        if (avaList.stream().noneMatch(x -> x.longValue() == newCenter)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the frequency has occupied");
        }

        checkBand(yangModel,
                ochLinkAttr.getLowerFrequency().getValue().longValue(),
                ochLinkAttr.getUpperFrequency().getValue().longValue(),
                Long.parseLong(frequency.getLowerFrequecy()),
                Long.parseLong(frequency.getUpperFrequecy()));
        return frequency;
    }

    private GridType getGridType(FrequencyType upperFrequency, FrequencyType lowerFrequency) {
        long width = upperFrequency.getValue().longValue() - lowerFrequency.getValue().longValue();
        int grid = (int) (width / 1000);
        switch (grid) {
            case 150:
                return GridType._150;
            case 100:
                return GridType._100;
            case 75:
                return GridType._75;
        }
        return GridType._50;
    }

    private void checkBand(NeYangModel yangModel, long oldLowFreq, long oldUpperFreq,
            long newLowFreq, long newUpperFreq) {
        if ((FrequencyAvailable.inBandC(yangModel, oldLowFreq, oldUpperFreq)
                && FrequencyAvailable.inBandC(yangModel, newLowFreq, newUpperFreq)) ||
                (!FrequencyAvailable.inBandC(yangModel, oldLowFreq, oldUpperFreq)
                        && !FrequencyAvailable.inBandC(yangModel, newLowFreq, newUpperFreq))) {
        } else {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot change frequency between C/L");
        }
    }

    /**
     * set global variable gridType, and return related siteLink obj
     *
     * @param siteLinkIds
     * @param context
     * @return
     */
    private List<Link> setGridTypeInAllSiteLinks(List<String> siteLinkIds,
            TunnelFrequencyUpdateContext context) {
        List<Link> siteLinkList = new ArrayList<>();
        GridType gridType = null;
        String ochLinkId = context.getOchLinkId();
        for (String siteLinkId : siteLinkIds) {
            Link siteLink = context.getChangedObject().getChangedSiteLink(siteLinkId);
            if (siteLink == null) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        String.format("OchLink[%s] route info error, cannot find out siteLink",
                                ochLinkId));
            }
            siteLinkList.add(siteLink);
            Site siteLinkAttr = siteLink
                    .getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite();
            if (gridType == null) {
                gridType = siteLinkAttr.getGrid();
            } else if (!siteLinkAttr.getGrid().equals(gridType)) {
                if (!gridType.equals(GridType._0) && !siteLinkAttr.getGrid().equals(GridType._0)) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            String.format(
                                    "OchLink[%s] route info error, all siteLink should be same grid",
                                    ochLinkId));
                }
            }
            if (siteLinkAttr.getGrid().equals(GridType._0)) {
                gridType = GridType._0;
            }
        }

        if (gridType == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("OchLink[%s] route info error, cannot find any siteLink",
                            ochLinkId));
        }
        context.setGridType(gridType);
        return siteLinkList;
    }

    /**
     * 基于所有siteLink，求取siteLink的availableFrequency的交集。 然后检查目的frequency是否在交集中
     *
     * @param siteLinkList
     * @param gridType
     * @param frequency
     */
    private void frequencyCheck(List<Link> siteLinkList, GridType gridType, Frequency frequency) {
        List<Long> freeFreqList = FrequencyAvailable.getFreeCentFrequency(siteLinkList, gridType);

        if (!freeFreqList.contains(frequency.getCentFreq().longValue())) {
            java.util.Map<Long, Link> usedFrequencyList = getUsedFrequency(siteLinkList);
            Link ochLink = usedFrequencyList.get(frequency.getCentFreq().longValue());
            if (ochLink != null) {
                Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        String.format("the target frequency %s has used in other OCH Link %s",
                                frequency.getCentFreq().longValue(),
                                ochLinkAttr.getFriendlyName()));
            }
        }
    }

    /**
     * export all ochLinks in given siteLinks
     *
     * @param siteLinks
     * @return
     */
    private java.util.Map<Long, Link> getUsedFrequency(List<Link> siteLinks) {
        java.util.Map<Long, Link> ochLinkList = new HashMap<>();
        OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);
        siteLinks.forEach(siteLink -> {
            List<SupportedLink> sLink = siteLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite().getSupportedLink();
            sLink.forEach(supportedLink -> {
                Link ochLink = ochLinkDao.getOchLinkByLinkId(supportedLink.getLinkRef().getValue());
                Och ochLinkAttr = ochLink.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                        .getOch();
                long cent = ochLinkAttr.getLowerFrequency().getValue().longValue() + (
                        ochLinkAttr.getUpperFrequency().getValue().longValue()
                                - ochLinkAttr.getLowerFrequency().getValue().longValue()
                ) / 2;
                ochLinkList.put(cent, ochLink);
            });
        });

        return ochLinkList;
    }

    public List<TerminationPoint> updateOchFrequency(String ochLinkId, Frequency frequency,
                                                     TunnelFrequencyUpdateContext context) throws CommonException {
        Link ochLink = context.getChangedObject().getChangedOchLink(ochLinkId);

        //for CMUX
        updateFrequency(ochLink, frequency, context.getSiteLinkIds(),
                GridType._0 != context.getGridType(), context);

        for (String tunnelId : OchLinkRoute.getSupportedTunnel(ochLink)) {
            updateTunnelProperty(tunnelId, frequency, context);
        }

        //REG mode, lot of OT card inside the route, their frequency should change
        List<String> supportingLinkIds = ochLink.getSupportingLink().stream()
                .map(SupportingLink::getLinkRef).map(Uri::getValue)
                .collect(
                        Collectors.toList());
        List<String> osLinkIds = supportingLinkIds.stream()
                .filter(PhysicalLinkIdNamingRule::isOsLink).collect(
                        Collectors.toList());
        List<String> tpIds = getTpIdsFromOsLink(osLinkIds);
        List<TerminationPoint> terminationPoints = terminationPointDao.getAllTerminationPointByIds(
                tpIds);
        List<TerminationPoint> otuLineTps = terminationPoints.stream()
                .filter(terminationPoint -> terminationPoint.getAugmentation(
                        TerminationPoint1.class).getPhysical().getPortType().equals(
                        PortType.OTULine)).collect(Collectors.toList());
//        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
//        RouteInfo rInfo = new RouteInfo();
//        rInfo.parse(ochLinkAttr.getExplictRoute().getRoute());
//        for (String tpId : rInfo.getTpIdList()) {
//            updateTpOtuLine(tpId, frequency.getCentFreq(), context);
//        }

        String srcTpId = ochLink.getSource().getSourceTp().getValue();
        String desTpId = ochLink.getDestination().getDestTp().getValue();

        updateOchNode_TPFrequency(srcTpId, frequency.getCentFreq(), context);
        updateOchNode_TPFrequency(desTpId, frequency.getCentFreq(), context);

        return otuLineTps;
    }

    private void updateOtuLineTpCentreFrequency(List<TerminationPoint> otuLineTps,
            BigInteger centFreq, TunnelFrequencyUpdateContext context) {
        List<String> otuLinkTpIds = otuLineTps.stream().map(TpAttributes::getTpId)
                .map(Uri::getValue).collect(
                        Collectors.toList());
        log.debug("update otuLineTpCentreFrequency {} on tpIds: {}",
                centFreq,
                otuLineTps.stream().map(tp->tp.getTpId().getValue()).collect(Collectors.toList()));
        for (TerminationPoint terminationPoint : otuLineTps) {
            String tpId = terminationPoint.getTpId().getValue();
            String phyNodeId = PhysicalTpIdNamingRule.getNodeId(
                    tpId);
            terminationPointDao.updateOtuLineTerminationPointFrequency(phyNodeId, tpId, centFreq);
        }

        for (String tpId : otuLinkTpIds) {
            updateOchNode_TPFrequency(tpId, centFreq, context);
        }
    }

    private List<String> getTpIdsFromOsLink(List<String> osLinkIds) {
        List<String> tpIds = new ArrayList<>();
        for (String osLink : osLinkIds) {
            String sourceTpId = PhysicalLinkIdNamingRule.getTpAId(osLink);
            String destTpId = PhysicalLinkIdNamingRule.getTpZId(osLink);
            tpIds.add(sourceTpId);
            tpIds.add(destTpId);
        }
        return tpIds;
    }

    private void updateFrequency(Link ochLink, Frequency frequency, List<String> siteLinkIds,
            boolean isFix, TunnelFrequencyUpdateContext context) throws CommonException {
        log.debug("UpdateFrequency {}, isFix {}", frequency.getLowerFrequecy(), isFix);

        FrequencyType oldLowerFrequency = ochLink.getAugmentation(Link1.class).getOch()
                .getLowerFrequency();
        FrequencyType oldUpperFrequency = ochLink.getAugmentation(Link1.class).getOch()
                .getUpperFrequency();

        if (isFix) {
            updateSiteLinkFixFrequency(siteLinkIds, oldLowerFrequency, oldUpperFrequency,
                    frequency, context);
        } else {
            updateSiteLinkFlexFrequency(siteLinkIds, oldLowerFrequency, oldUpperFrequency,
                    frequency, context);
        }

        Link newOchLink = updateOchLinkFrequency(ochLink, frequency);
        BigInteger oldCentFrequency = Frequency.getCentFreq(oldLowerFrequency.getValue(),
                oldUpperFrequency.getValue());

        boolean bandChanged = isBandChanged(oldLowerFrequency, oldUpperFrequency, frequency,
                context.getYangModel());
        if (bandChanged) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "not support change band");
        }

        OchLinkRoute ochLinkRoute = new OchLinkRoute(newOchLink);
        Map<String, CrossConnectionAttributes> newXcList = new HashMap<>(); //key is the xcID of old XC

        newOchLink = ochLinkRoute.replaceXC(newOchLink, oldCentFrequency, frequency, isFix,
                newXcList);

        replaceNodeNodeXc(newXcList, context);  //ochXC changed only at OPC node XC

        if (isFix) {
            //在fix下，OT板卡L口连接到MUX板卡的MuxChannel端口必须变化
            List<String> oldMuxChannelTpList = OchLinkRoute.getAllMuxChannelTps(ochLink, frequency);
            //fix 情况下 路由信息的修改，
            // 1. och route 中的TP， link的名称需要替换为新的TP点
            // 2. ochLink 本身的支撑 phyLink的TP点也需要修改
            // 3. 物理topo中物理连接的修改 (在replaceSupportingLink中获取到那些物理连接需要处理）
            newOchLink = ochLinkRoute.replaceTpAndLink(newOchLink, frequency);
            List<String> changedPhyLinkIdList = new ArrayList<>();
            newOchLink = ochLinkRoute.replaceSupportingLink(newOchLink, frequency,
                    changedPhyLinkIdList);

            PhysicalLink.replaceLink(context.getChangedObject(), changedPhyLinkIdList, frequency);

            //fix 情况下 网元的修改：
            // 1. 原来muxChannel TP connectionStatus->idel, 新的TP->busy
            // 2. 原来的网元内部连接删除，新加新TP的内部连接
            // 3. xc  前面replaceNodeNodeXc 以及处理
            updateNode_tpStauts(oldMuxChannelTpList, frequency, context);

            //oldMuxChannelTpList 中只有M?D?的端口，与之相连的L 口 也需要加入，一起修改internalLink
            oldMuxChannelTpList.add(ochLink.getSource().getSourceTp().getValue());
            oldMuxChannelTpList.add(ochLink.getDestination().getDestTp().getValue());
            updateNode_internalLink(oldMuxChannelTpList, frequency, context);
        }
        context.getChangedObject().addChangedOchLink(newOchLink);
    }

    private boolean isBandChanged(FrequencyType oldLowerFrequency, FrequencyType oldUpperFrequency,
            Frequency frequency, NeYangModel yangModel) {
        boolean oldIsC = FrequencyAvailable.inBandC(yangModel,
                oldLowerFrequency.getValue().longValue(), oldUpperFrequency.getValue().longValue());
        boolean newIsC = FrequencyAvailable.inBandC(yangModel,
                Long.parseLong(frequency.getLowerFrequecy()),
                Long.parseLong(frequency.getUpperFrequecy()));

        return !(oldIsC == newIsC);
    }


    private void replaceNodeNodeXc(Map<String, CrossConnectionAttributes> newXcList,
            TunnelFrequencyUpdateContext context) {
        for (String oldXcId : newXcList.keySet()) {
            CrossConnectionAttributes newXC = newXcList.get(oldXcId);

            Node cfgNode = context.getChangedObject()
                    .getChangedPhyNode(newXC.getNodeRef().getValue());

            cfgNode = replaceXc(cfgNode, oldXcId, newXC);
            context.getChangedObject().addChangedPhyNode(cfgNode);

//          不应该修改OP树上的值
//            Node opNode = changedObject.getChangedPhyOpNode(newXC.getNodeRef().getValue());
//            if (opNode != null) {
//                opNode = removeXc(opNode, oldXcId);
//                opNode = insertXc(opNode, newXC);
//                changedObject.addChangedPhyNode(opNode);
//            }
        }
    }

    private Node replaceXc(Node node, String oldXcId, CrossConnectionAttributes newXC) {
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        List<CrossConnections> replacedXc = new ArrayList<>();

        CrossConnections newOne = new CrossConnectionsBuilder(newXC).build();

        Iterator<CrossConnections> iter = nodeAttr.getCrossConnections().iterator();
        while (iter.hasNext()) {
            CrossConnections xc = iter.next();
            if (xc.getCrossConnectionId().getValue().equals(oldXcId)) {
                replacedXc.add(newOne);
            } else {
                replacedXc.add(xc);
            }
        }

        return new NodeBuilder(node)
                .addAugmentation(Node1.class,
                        new Node1Builder().setPhysical(
                                        new PhysicalBuilder(nodeAttr).setCrossConnections(replacedXc)
                                                .build())
                                .build())
                .build();
    }

    //same method happen in allocate.link.site.SiteLinkOchUpdater.java, updateSiteLinkAvailable
    private void updateSiteLinkFlexFrequency(List<String> siteLinkIds,
            FrequencyType oldLowerFrequency, FrequencyType oldUpperFrequency, Frequency newFreq,
            TunnelFrequencyUpdateContext context) {
        for (String siteLinkId : siteLinkIds) {
            Link siteLink = context.getChangedObject().getChangedSiteLink(siteLinkId);
            Site siteLinkAttr = siteLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite();
            FrequencyAvailable siteLinkFreeFrequency = new FrequencyAvailable(siteLink);

            Available newAva = new AvailableBuilder()
                    .setLowerFrequency(
                            new FrequencyType(new BigInteger(newFreq.getLowerFrequecy())))
                    .setUpperFrequency(
                            new FrequencyType(new BigInteger(newFreq.getUpperFrequecy())))
                    .setKey(new AvailableKey(
                            new FrequencyType(new BigInteger(newFreq.getLowerFrequecy()))))
                    .build();
            if (!siteLinkFreeFrequency.isFree(newAva)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        String.format("required frequency %s is NOT  available on siteLink %s",
                                newFreq.getCentFreq().toString(), siteLinkAttr.getFriendlyName()));
            }

            Available oldAva = new AvailableBuilder()
                    .setLowerFrequency(oldLowerFrequency)
                    .setUpperFrequency(oldUpperFrequency)
                    .setKey(new AvailableKey(oldLowerFrequency))
                    .build();
            siteLinkFreeFrequency.add(oldAva);

            siteLinkFreeFrequency.remove(newAva);

            Link newSiteLink = new LinkBuilder(siteLink)
                    .addAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class,
                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                                    .setSite(new SiteBuilder(siteLinkAttr)
                                            .setAvailable(siteLinkFreeFrequency.getAvailableList())
                                            .build()
                                    ).build()
                    ).build();

            context.getChangedObject().addChangedSiteLink(newSiteLink);
        }
    }

    private void updateSiteLinkFixFrequency(List<String> siteLinkIds,
            FrequencyType oldLowerFrequency, FrequencyType oldUpperFrequency, Frequency newFreq,
            TunnelFrequencyUpdateContext context)
            throws CommonException {
        for (String siteLinkId : siteLinkIds) {
            Link siteLink = context.getChangedObject().getChangedSiteLink(siteLinkId);
            Site siteLinkAttr = siteLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite();

            boolean found = false;
            List<Available> newAvaList = new ArrayList<>();
            for (Available ava : siteLinkAttr.getAvailable()) {
                if (ava.getLowerFrequency().getValue().toString()
                        .equals(newFreq.getLowerFrequecy())) {
                    found = true;
                    continue;  //remove new Frequency from siteLink free Frequency list
                } else {
                    newAvaList.add(ava);
                }
            }
            if (!found) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        String.format("required frequency %s is NOT  available on siteLink %s",
                                newFreq.getCentFreq().toString(), siteLinkAttr.getFriendlyName()));
            }

            //把老的频率放到siteLink free Frequency 中
            newAvaList.add(new AvailableBuilder()
                    .setLowerFrequency(oldLowerFrequency)
                    .setUpperFrequency(oldUpperFrequency)
                    .setKey(new AvailableKey(oldLowerFrequency))
                    .build());

            Link newSiteLink = new LinkBuilder(siteLink)
                    .addAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class,
                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1Builder()
                                    .setSite(new SiteBuilder(siteLinkAttr)
                                            .setAvailable(newAvaList)
                                            .build())
                                    .build())
                    .build();

            context.getChangedObject().addChangedSiteLink(newSiteLink);
        }
    }

    private Link updateOchLinkFrequency(Link ochLink, Frequency frequency) {
        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();
        return new LinkBuilder(ochLink)
                .addAugmentation(Link1.class, new Link1Builder()
                        .setOch(new OchBuilder(ochLinkAttr)
                                .setLowerFrequency(new FrequencyType(
                                        new BigInteger(frequency.getLowerFrequecy())))
                                .setUpperFrequency(new FrequencyType(
                                        new BigInteger(frequency.getUpperFrequecy())))
                                .build())
                        .build())
                .build();
    }


    private void updateNode_tpStauts(List<String> oldMuxChannelTpList, Frequency frequency,
            TunnelFrequencyUpdateContext context) {
        for (String oldTpId : oldMuxChannelTpList) {
            String nodeId = PhysicalTpIdNamingRule.getNodeId(oldTpId);
            Node node = context.getChangedObject().getChangedPhyNode(nodeId);
            String newTpId = frequency.replaceMuxChannelTpId(oldTpId);

            List<TerminationPoint> newTpList = new ArrayList<>();
            for (TerminationPoint tp : node.getTerminationPoint()) {
                if (tp.getTpId().getValue().equals(oldTpId)) {
                    newTpList.add(new TerminationPointBuilder(tp)
                            .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                    .setPhysical(
                                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                                                    tp.getAugmentation(TerminationPoint1.class)
                                                            .getPhysical())
                                                    .setConnectionStatus(ConnectionStatus.Idle)
                                                    .build())
                                    .build())
                            .build());
                } else if (tp.getTpId().getValue().equals(newTpId)) {
                    newTpList.add(new TerminationPointBuilder(tp)
                            .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                    .setPhysical(
                                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                                                    tp.getAugmentation(TerminationPoint1.class)
                                                            .getPhysical())
                                                    .setConnectionStatus(ConnectionStatus.Busy)
                                                    .build())
                                    .build())
                            .build());
                } else {
                    newTpList.add(tp);
                }
            }
            context.getChangedObject().addChangedPhyNode(
                    new NodeBuilder(node).setTerminationPoint(newTpList).build());
        }
    }

    private void updateNode_internalLink(List<String> oldMuxChannelTpList, Frequency frequency,
            TunnelFrequencyUpdateContext context) {
        for (String oldTpId : oldMuxChannelTpList) {
            String nodeId = PhysicalTpIdNamingRule.getNodeId(oldTpId);
            Node node = context.getChangedObject().getChangedPhyNode(nodeId);
            Physical phyNodeAttr = node.getAugmentation(Node1.class).getPhysical();

            List<InternalLinks> newInternalLinkList = new ArrayList<>();
            for (InternalLinks il : phyNodeAttr.getInternalLinks()) {
                if (il.getLinkRef().contains(oldTpId)) {
                    if (frequency.hasMuxChannel(il.getSrcTp())) {
                        newInternalLinkList.add(new InternalLinksBuilder(il)
                                .setLinkRef(frequency.replaceMuxChannelTpId(il.getLinkRef()))
                                .setLinkName(frequency.replaceMuxChannelTpId(il.getLinkName()))
                                .setSrcTp(frequency.replaceMuxChannelTpId(il.getSrcTp()))
                                .setKey(new InternalLinksKey(
                                        frequency.replaceMuxChannelTpId(il.getLinkName())))
                                .build());
                    } else {
                        newInternalLinkList.add(new InternalLinksBuilder(il)
                                .setLinkRef(frequency.replaceMuxChannelTpId(il.getLinkRef()))
                                .setLinkName(frequency.replaceMuxChannelTpId(il.getLinkName()))
                                .setDstTp(frequency.replaceMuxChannelTpId(il.getDstTp()))
                                .setKey(new InternalLinksKey(
                                        frequency.replaceMuxChannelTpId(il.getLinkName())))
                                .build());
                    }
                } else {
                    newInternalLinkList.add(il);
                }
            }
            context.getChangedObject().addChangedPhyNode(new NodeBuilder(node)
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder(phyNodeAttr)
                                    .setInternalLinks(newInternalLinkList)
                                    .build())
                            .build())
                    .build());
        }
    }

    private void updateTunnelProperty(String tunnelId, Frequency frequency,
            TunnelFrequencyUpdateContext context) throws CommonException {
        Tunnel dbTunnel = context.getChangedObject().getChangedTunnel(tunnelId);
        if (dbTunnel == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("get tunnel[%s] error", tunnelId));
        }

        Properties newProp = PropertyTool.addProperty(dbTunnel.getProperties(), "frequency",
                frequency.getFormattedFrequency());
        newProp = PropertyTool.addProperty(newProp, "centreFrequency",
                frequency.getCentFreq().longValue() + "");

        Tunnel newTunnel = new TunnelBuilder(dbTunnel).setProperties(newProp).build();

        context.getChangedObject().addChangedTunnel(newTunnel);
    }

    private void updateTpOtuLine(String tpId, BigInteger centFreq,
            TunnelFrequencyUpdateContext context) {
        boolean isElectric = updateNode_OtuLineFrequency(tpId, centFreq, context);

        if (isElectric) {
            updateOchNode_TPFrequency(tpId, centFreq, context);
        }
    }

    private void updateOchNode_TPFrequency(String tpId, BigInteger centFreq,
            TunnelFrequencyUpdateContext context) {
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        Node node = context.getChangedObject().getChangedOchNode(nodeId);
        if (node == null) {
            log.error("Cannot find the nodeId in Config-Och-node {}", nodeId);
            return;
        }

        int pos = 0;
        TerminationPoint newTp = null;
        Iterator<TerminationPoint> iter = node.getTerminationPoint().iterator();
        while (iter.hasNext()) {
            TerminationPoint tp = iter.next();
            if (tp.getTpId().getValue().equals(tpId)) {
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.tp.attributes.Och tpAttr = tp.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.TerminationPoint1.class)
                        .getOch();
                iter.remove();
                newTp = new TerminationPointBuilder(tp)
                        .addAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.TerminationPoint1.class,
                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.TerminationPoint1Builder()
                                        .setOch(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.tp.attributes.OchBuilder(
                                                tpAttr)
                                                .setCentralFrequency(new FrequencyType(centFreq))
                                                .build())
                                        .build())
                        .build();
                break;
            }
        }
        if (newTp != null) {
            node.getTerminationPoint().add(pos, newTp);
        }
        context.getChangedObject().addChangedOchNode(node);
    }

    /**
     * 这个只有电层网元才需要更新
     *
     * @param tpId
     * @param centFreq
     * @param context
     * @return 是电层网元，继续更新
     */
    private boolean updateNode_OtuLineFrequency(String tpId, BigInteger centFreq,
            TunnelFrequencyUpdateContext context) {
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        Node node = context.getChangedObject().getChangedPhyNode(nodeId);

        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        if (nodeAttr.getNodeType().equals(NodeType.OD)) {
            return false;
        }

        int pos = 0;
        TerminationPoint newTp = null;
        Iterator<TerminationPoint> iter = node.getTerminationPoint().iterator();
        while (iter.hasNext()) {
            TerminationPoint tp = iter.next();
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr = tp.getAugmentation(
                    TerminationPoint1.class).getPhysical();
            if (tp.getTpId().getValue().equals(tpId) && tpAttr.getOtuLine() != null) {
                iter.remove();
                newTp = new TerminationPointBuilder(tp)
                        .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                .setPhysical(
                                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                                                (tpAttr))
                                                .setOtuLine(new OtuLineBuilder(tpAttr.getOtuLine())
                                                        .setCentralFrequency(
                                                                new FrequencyType(centFreq))
                                                        .build())
                                                .build())
                                .build())
                        .build();
                break;
            }
        }
        if (newTp != null) {
            node.getTerminationPoint().add(pos, newTp);
        }
        context.getChangedObject().addChangedPhyNode(node);

        return true;
    }


    @Data
    public static class TunnelFrequencyUpdateContext {

        private String tunnelName;
        private String ochLinkId;
        private List<String> siteLinkIds;
        private List<String> nodeIds;
        private GridType gridType;
        private NeYangModel yangModel;
        private ChangedObject changedObject;
    }

}
