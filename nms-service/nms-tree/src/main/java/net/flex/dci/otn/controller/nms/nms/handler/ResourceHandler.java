/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.handler;

import static net.flex.dci.otn.controller.nms.nms.enums.NMSResourceType.CHASSIS;
import static net.flex.dci.otn.controller.nms.utils.Constants.API_VERSION;
import static net.flex.dci.otn.controller.nms.utils.Constants.CHASSIS_INFIX;
import static net.flex.dci.otn.controller.nms.utils.Constants.CONTROLLER_VERSION;
import static net.flex.dci.otn.controller.nms.utils.Constants.DEFAULT_BAND;
import static net.flex.dci.otn.controller.nms.utils.Constants.SUPPORT_ADAPTER_VERSION;
import static net.flex.dci.otn.controller.nms.utils.Constants.TRANSCEIVER_INFIX;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.constructs.NEResourceHolder;
import net.flex.dci.otn.controller.nms.nms.component.object.NmsObjectDetailHandler;
import net.flex.dci.otn.controller.nms.nms.component.resource.frequency.FrequencyMap;
import net.flex.dci.otn.controller.nms.nms.component.resource.frequency.FrequencyMapWrapper;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.MuxSpectrum;
import net.flex.dci.otn.controller.nms.nms.component.resource.wsschannel.WssChannel;
import net.flex.dci.otn.controller.nms.nms.component.terminationPoint.NMSScanTerminationPointHandler;
import net.flex.dci.otn.controller.nms.nms.convertors.NmsOutputConverters;
import net.flex.dci.otn.controller.nms.nms.dto.FrequencyMapDto;
import net.flex.dci.otn.controller.nms.nms.dto.MuxSpectrumDto;
import net.flex.dci.otn.controller.nms.nms.dto.VoaThresholdDto;
import net.flex.dci.otn.controller.nms.nms.dto.WssChannelDto;
import net.flex.dci.otn.controller.nms.nms.enums.NMSResourceType;
import net.flex.dci.otn.controller.nms.nms.handler.impl.connections.PhyLink;
import net.flex.dci.otn.controller.nms.nms.handler.impl.nodes.PhyNe;
import net.flex.dci.otn.controller.nms.nms.handler.impl.resource.alarm.AlarmResourceLocator;
import net.flex.dci.otn.controller.nms.utils.NMSUtils;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import net.flex.dci.otn.controller.nms.utils.PagedList;
import net.flex.dci.otn.controller.nms.utils.SotnConfigurationUtils;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.get.ot.card.capability.output.CardType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetBoardLldpInfoInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetCardTypeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetEnvPropertyOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetEnvPropertyOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetEquipmentInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetEquipmentPagedInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFiberAffectionInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFiberAffectionOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFiberAffectionOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFrequencyMapInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFrequencyMapSiteLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetFriendlyNameInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumWithSiteLinkInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumWithSiteLinkOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumWithSiteLinkOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetObjectDetailsInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetResourceByOrderIdInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetResourceByOrderIdOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetResourceByOrderIdOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetScanTpInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetScanTpOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetScanTpOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetVersionOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetVersionOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetWssChannelInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetWssChannelOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetWssChannelOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.LocateResourcesByAlarmInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.LocateResourcesByAlarmOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.OtdrPortDirection;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.RecycleResourceByOrderIdInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.RecycleResourceByOrderIdOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.RecycleResourceByOrderIdOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.RemoveResourceByOrderIdInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ResynchronizeNeInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.ResynchronizeNeOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.SyncEquipmentInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.SyncEquipmentOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.SyncTerminationPointInput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.SyncTerminationPointOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.fiber.affection.attributes.Customer;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.fiber.affection.attributes.CustomerBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.Object;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.ObjectBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.ObjectKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.board.lldp.info.output.LldpInfos;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.board.lldp.info.output.LldpInfosBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.UpdateRange;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.UpdateRangeBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.nodes.Node;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.detail.list.ObjectDetail;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.detail.list.ObjectDetailBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.detail.list.ObjectDetailKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtectionUnprotected;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.lldp.attributes.Lldp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.ExplicitRouteObjects;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explicit.route.objects.explicit.route.objects.PathRouteObject;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

/**
 * @date: 2021/4/8
 */
@Slf4j
@Component
public class ResourceHandler extends AbstractBaseHandler {

    @Autowired
    private NEResourceHolder neResourceHolder;


    @Autowired
    private NmsObjectDetailHandler nmsObjectDetailHandler;

    @Autowired
    private MuxSpectrum muxSpectrum;

    @Autowired
    private WssChannel wssChannel;

    @Autowired
    private FrequencyMapWrapper frequencyMapWrapper;

    @Autowired
    private NmsOutputConverters nmsOutputConverters;

    @Autowired
    private NMSScanTerminationPointHandler nmsScanTerminationPointHandler;

    @Autowired
    private AlarmResourceLocator alarmResourceLocator;

    public ResourceHandler(
            NetconfTopology netconfTopology) {
        super(netconfTopology);
    }


    @Override
    public void removeResourceByOrderId(RemoveResourceByOrderIdInput input) throws Exception {
        String orderId = input.getOrderId();
        this.neResourceHolder.removeResourceByOrderId(orderId);
    }

    @Override
    public RecycleResourceByOrderIdOutput recycleResourceByOrderId(
            RecycleResourceByOrderIdInput input) throws Exception {
        RecycleResourceByOrderIdOutputBuilder outputBuilder = new RecycleResourceByOrderIdOutputBuilder();
        outputBuilder.setNode(this.neResourceHolder.getNodes(input.getOrderId(), false));
        return outputBuilder.build();
    }

    //todo:error
    @Override
    public GetResourceByOrderIdOutput getResourceByOrderId(GetResourceByOrderIdInput input)
            throws Exception {
        List<Node> nodes = this.neResourceHolder.getNodes(input.getOrderId(), false);
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link> links = this.neResourceHolder
                .getLinks(input.getOrderId(), false);
        GetResourceByOrderIdOutputBuilder builder = new GetResourceByOrderIdOutputBuilder();
        builder.setLink(links);
        builder.setNode(nodes);
        return builder.build();
    }

    @Override
    public GetEnvPropertyOutput getEnvProperty() throws Exception {
        log.debug("start to get env property output");
//        List<String> list = new ArrayList<>();
//        String regionIdStr = System.getenv("regionId");
//        if (StringUtils.isBlank(regionIdStr)) {
//            regionIdStr = "1";
//        }
//        Long regionId = Long.parseLong(regionIdStr);
//        String regionName = SpringBeanFinder.getBean(RegionService.class)
//                .getRegionNameById(regionId);
//        if (StringUtils.isBlank(regionName)) {
//            throw new Exception("region cannot be empty");
//        }
        GetEnvPropertyOutputBuilder builder = new GetEnvPropertyOutputBuilder();
        builder.setRegion("中国");
        builder.setRiskPlane(
                StringUtils.isBlank(System.getenv("riskPlane")) ? "DefaultRiskPlane"
                        : System.getenv("riskPlane"));
        return builder.build();
    }

    @Override
    public GetFiberAffectionOutput getFiberAffection(GetFiberAffectionInput input)
            throws Exception {
        GetFiberAffectionOutputBuilder outputBuilder = new GetFiberAffectionOutputBuilder();

        TopologyId topologyRef = input.getTopologyRef();
        LinkId linkRef = input.getLinkRef();
        if (topologyRef.getValue().contains(TopoNameConstants.Phy_Topo_Key)) {
            List<Link> phyLinks = new PhyLink(netconfTopology).getPhyLinks(topologyRef, linkRef);
            if (phyLinks.size() != 1) {
                throw new Exception(
                        "not supported parameter compose.");
            }
            Link phyLink = phyLinks.get(0);
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node srcNode = new PhyNe(
                    netconfTopology)
                    .getSiteNodes(topologyRef, phyLink.getSource().getSourceNode()).get(0);
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node desNode = new PhyNe(
                    netconfTopology)
                    .getSiteNodes(topologyRef, phyLink.getDestination().getDestNode()).get(0);

            List<Link> siteLinks = new PhyLink(netconfTopology).getSiteLinks(topologyRef, linkRef);
            if (siteLinks.size() != 1) {
                throw new Exception(
                        "supported siteLink error.");
            }
            Link siteLink = siteLinks.get(0);
            Site site = siteLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite();

            List<Tunnel> tunnels = new PhyLink(netconfTopology).getTunnels(topologyRef, linkRef);

            List<Customer> customers = new ArrayList<Customer>();
            String desSiteName = desNode.getAugmentation(Node1.class).getSite().getFriendlyName();
            String friendlyName = phyLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                    .getPhysical()
                    .getFriendlyNameDisplay();
            Integer totalTunnel = 0;
            String planeName = site.getPlaneName();
            String riskGroupName = site.getRiskGroupName();
            String srcSiteName = srcNode.getAugmentation(Node1.class).getSite().getFriendlyName();
            Integer totalGigbit = 0;
            Boolean isOmsp = true;
            Boolean isPrimary = checkPrimary(site, linkRef);

            if (site.getProtectionType() == null
                    || site.getProtectionType().getName()
                    .equals(ProtectionUnprotected.class.getName())) {
                isOmsp = false;
            }

            phyLink.getSource().getSourceNode();
            for (Tunnel tunnel : tunnels) {
                if (tunnel.getImplementState() == ImplementState.Implement) {
                    totalTunnel++;
                    String custName = tunnel.getCustomer();
                    Integer gigbit = NMSUtils.getGigbit(tunnel.getSignalRate());
                    totalGigbit = totalGigbit + gigbit;
                    countCustomer(customers, custName, gigbit);
                }
            }

            outputBuilder.setCustomer(customers)
                    .setDstSiteName(desSiteName)
                    .setFiberFriendlyName(friendlyName)
                    .setImpactedTunnelNumber(totalTunnel)
                    .setIsOmsp(isOmsp)
                    .setIsPrimary(isPrimary)
                    .setPlaneName(planeName)
                    .setRiskGroupName(riskGroupName)
                    .setSrcSiteName(srcSiteName)
                    .setTotalImpactedGigbit(totalGigbit);
        } else {
            throw new Exception(
                    "not supported parameter compose.");
        }

        return outputBuilder.build();
    }

    @Override
    public GetScanTpOutput getScanTp(GetScanTpInput input) throws Exception {
        List<TerminationPoint> tpList;
        if (input.getSiteRef() != null && input.getTpRef() != null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "only support siteNode or port from ILA/OA/WSS");
        }
        if (input.getSiteRef() != null) {
            tpList = neResourceHolder.getScanTpsInSite(input.getSiteRef(), input.getPortType());
        } else if (input.getTpRef() != null) {
            tpList = neResourceHolder.getScanTpByTp(input.getTpRef(), input.getPortType(),
                    input.getDirection());
        } else {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "only support based on siteNode or port from ILA/OA/WSS");
        }

        return new GetScanTpOutputBuilder()
                .setTerminationPoint(convertTp2Output(tpList))
                .build();

    }

    @Override
    public GetScanTpOutput getScanTerminationPoint(GetScanTpInput input) throws Exception {
        log.info("get scan termination point by input:{}", input);
        String siteRef = input.getSiteRef() == null ? null : input.getSiteRef().getValue();
        String tpRef = input.getTpRef();
        PortType portType = input.getPortType();
        OtdrPortDirection direction = input.getDirection();
        List<TerminationPoint> terminationPoints = nmsScanTerminationPointHandler.getElementRefScanTerminationPoints(
                siteRef, tpRef, portType, direction);
        GetScanTpOutputBuilder getScanTpOutputBuilder = new GetScanTpOutputBuilder();
        getScanTpOutputBuilder.setTerminationPoint(
                nmsOutputConverters.convert2NmsOutput(terminationPoints));
        return getScanTpOutputBuilder.build();
    }

    private List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.scan.tp.output.TerminationPoint> convertTp2Output(
            List<TerminationPoint> tpList) {
        if (null == tpList || tpList.isEmpty()) {
            return new ArrayList<>();
        }
        return tpList.parallelStream().map(tp -> {
            org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.scan.tp.output.TerminationPoint newTp =
                    new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.scan.tp.output.TerminationPointBuilder()
                            .setTpId(tp.getTpId())
                            .setKey(new org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.scan.tp.output.TerminationPointKey(
                                    tp.getTpId()))
                            .setPhysical(tp.getAugmentation(TerminationPoint1.class).getPhysical())
                            .build();
            return newTp;
        }).collect(Collectors.toList());
    }

    @Override
    public List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.Object> getFriendName(
            GetFriendlyNameInput input) throws Exception {

        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.id.list.Object> objects = input
                .getObject();
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.Object> output = objects.stream()
                .map(friendNameObject -> {
                    ObjectBuilder objectBuilder = new ObjectBuilder();
                    objectBuilder.setTopologyRef(friendNameObject.getTopologyRef());
                    objectBuilder.setObjectType(friendNameObject.getObjectType());
                    objectBuilder.setObjectId(friendNameObject.getObjectId());
                    objectBuilder.setKey(
                            new ObjectKey(objectBuilder.getObjectId(),
                                    objectBuilder.getTopologyRef()));
                    return nmsObjectDetailHandler.getObjectFriendName(objectBuilder.build());
                }).sorted(new SortByName()).collect(Collectors.toList());
        return output;
    }

    @Override
    public List<ObjectDetail> getObjectsDetails(GetObjectDetailsInput input) {
        List<org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.object.id.list.Object> objects = input
                .getObject();
        List<ObjectDetail> objectDetails = objects.stream().map(object -> {
            ObjectDetailBuilder objectBuilder = new ObjectDetailBuilder();
            objectBuilder.setTopologyRef(object.getTopologyRef());
            objectBuilder.setObjectType(object.getObjectType());
            objectBuilder.setObjectId(object.getObjectId());
            objectBuilder.setKey(
                    new ObjectDetailKey(objectBuilder.getObjectId(),
                            objectBuilder.getTopologyRef()));

            return nmsObjectDetailHandler.getObjectDetail(objectBuilder.build());
        }).sorted(new Comparator<ObjectDetail>() {
            @Override
            public int compare(ObjectDetail o1, ObjectDetail o2) {
                return o1.getFriendlyName().compareTo(o2.getFriendlyName());
            }
        }).collect(Collectors.toList());
        return objectDetails;
    }

    @Override
    public GetMuxSpectrumOutput getMuxSpectrum(GetMuxSpectrumInput input) throws Exception {
        log.debug("start to get mux spectrum the input is :{}", input);
        MuxSpectrumDto muxSpectrumDto = muxSpectrum.getEquipRefSpectrum(
                input.getNodeId().getValue(), input.getEquipmentRef());
        GetMuxSpectrumOutputBuilder outputBuilder = new GetMuxSpectrumOutputBuilder();
        outputBuilder.setSpectrum(muxSpectrumDto.getSpectrumList());
        return outputBuilder.build();
    }

    @Override
    public GetMuxSpectrumWithSiteLinkOutput getMuxSpectrumWithSiteLinks(
            GetMuxSpectrumWithSiteLinkInput input) throws Exception {
        log.debug("start to get mux spectrum with site links: {}", input);
        String bandStr =
                StringUtils.isBlank(input.getWDMBand()) ? DEFAULT_BAND : input.getWDMBand();
        MuxSpectrumDto muxSpectrumDto = muxSpectrum.getSpectrumWithSiteLinks(
                input.getSiteLinkIds(), input.getGrid(), WDM_Band.fromString(bandStr));
        GetMuxSpectrumWithSiteLinkOutputBuilder outputBuilder = new GetMuxSpectrumWithSiteLinkOutputBuilder();
        outputBuilder.setSpectrum(muxSpectrumDto.getSpectrumList());
        return outputBuilder.build();
    }

    @Override
    public GetWssChannelOutput getWssChannel(GetWssChannelInput input) {
        try {
            log.debug("start to get wss channel for the input:{}", input);
//        WssChannel wssChannel = SpringBeanFinder.getBean(WssChannel.class);
            WssChannelDto wssChannelDto = wssChannel.retrieveRefEquipAllChannels(
                    input.getNodeId().getValue(), input.getEquipmentRef());
            VoaThresholdDto voaThresholdDto = wssChannelDto.getVoaThresholdDto();
            UpdateRange updateRange = null;
            if (voaThresholdDto != null) {
                updateRange = new UpdateRangeBuilder()
                        .setMaxDestToSourceVoa(voaThresholdDto.getMaxDestToSourceVoa())
                        .setMinDestToSourceVoa(voaThresholdDto.getMinDestToSourceVoa())
                        .setMaxSourceToDestVoa(voaThresholdDto.getMaxSourceToDestVoa())
                        .setMinSourceToDestVoa(voaThresholdDto.getMinSourceToDestVoa())
                        .build();
            }
            return new GetWssChannelOutputBuilder().setChannel(wssChannelDto.getWssChannels())
                    .setUpdateRange(updateRange).build();
        } catch (Exception ex) {
            log.error("failed to get wss channel {}", ex.getMessage(), ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get wss channel :" + ex.getMessage(), ex);
        }
    }

    @Override
    public FrequencyMap getFrequencyMap(GetFrequencyMapInput input) throws Exception {
        FrequencyMap frequencyMap = new FrequencyMap(netconfTopology);
        return frequencyMap.getMap(input.getNodeRef(), input.getTpRef());
    }

    /**
     * to support api for nms rpc nms:get-frequency-map
     *
     * @param input
     * @return
     * @throws Exception
     */
    @Override
    public FrequencyMapDto getFrequencyMapDto(GetFrequencyMapInput input) throws Exception {
        log.info("get node:{},tp :{} frequency map", input.getNodeRef().getValue(),
                input.getTpRef().getValue());
        FrequencyMapDto frequencyMapDto = frequencyMapWrapper.getTpsFrequencyMap(
                input.getNodeRef().getValue(), input.getTpRef().getValue());
        return frequencyMapDto;
    }

    /**
     * to support api for nms rpc nms:get-frequency-map-site-link
     *
     * @param input
     * @return
     * @throws Exception
     */
    @Override
    public FrequencyMapDto getFrequencyMapBySiteLink(GetFrequencyMapSiteLinkInput input)
            throws Exception {
        log.info("get frequency map on SiteLinks {}", input.getSiteLinkIds());
        FrequencyMapDto frequencyMapDto = frequencyMapWrapper.getSiteLinksFrequencyMapBySiteLinkAndGrid(
                input.getSiteLinkIds(), WDM_Band.fromString(input.getBandType()), input.getGrid());
        return frequencyMapDto;
    }

    @Override
    public List<CardType> getOtCardCapability() {
//        List<CardType> list = new ArrayList<>();
//
//        List<String> clientSignal = new ArrayList<>();
//        clientSignal.add("common-otn-types:" + Prot100GE.QNAME.getLocalName());
//
//        List<String> transceiverModule = new ArrayList<>();
//        transceiverModule.add("common-otn-types:" + ETH100GBASECWDM4.QNAME.getLocalName());
//        transceiverModule.add("common-otn-types:" + ETH100GBASELR4.QNAME.getLocalName());
//
//        List<String> c4SupportedRate = new ArrayList<>();
//        c4SupportedRate.add("common-otn-types:" + ProtOTUc2.QNAME.getLocalName());
//        c4SupportedRate.add("common-otn-types:" + ProtOTU4.QNAME.getLocalName());
//
//        ClientPortBuilder c4cp = new ClientPortBuilder()
//                .setClientSignal(clientSignal)
//                .setTransceiverModule(transceiverModule);
//
//        LinePortBuilder c4lp = new LinePortBuilder().setRate(c4SupportedRate);
//        CardTypeBuilder c4 = new CardTypeBuilder()
//                .setCardType("T2X2C4")
//                .setKey(new CardTypeKey("T2X2C4"))
//                .setClientPort(c4cp.build())
//                .setLinePort(c4lp.build());
//
//        //############ for T2X4C8 #######################
//        List<String> c8SupportedRate = new ArrayList<>();
//        c8SupportedRate.add("common-otn-types:" + ProtOTUc4.QNAME.getLocalName());
//        c8SupportedRate.add("common-otn-types:" + ProtOTUc2.QNAME.getLocalName());
////    c8SupportedRate.add("common-otn-types:" + ProtOTU4.QNAME.getLocalName());
//        LinePortBuilder c8lp = new LinePortBuilder().setRate(c8SupportedRate);
//        CardTypeBuilder c8 = new CardTypeBuilder()
//                .setCardType("T2X4C8")
//                .setKey(new CardTypeKey("T2X4C8"))
//                .setClientPort(c4cp.build())
//                .setLinePort(c8lp.build());
//
//        list.add(c4.build());
//        list.add(c8.build());

        return null;
    }

    @Override
    public List<String> getCardType(GetCardTypeInput input) {
        List<String> list = new ArrayList<String>();
        list.add(Constant.EquipmentClass.T2X2C4);
        list.add(Constant.EquipmentClass.T2X4C8);
        return list;
    }

    @Override
    public List<Node> getEquipment(GetEquipmentInput input) {
        TopologyId topologyRef = input.getTopologyRef();
        NodeId nodeRef = input.getNodeRef();
        String rackRef = input.getRackRef();
        String equipRef = input.getEquipmentRef();
        TpId tpRef = input.getTpRef();
        String tunnelRef = input.getTunnelRef();
        LinkId linkRef = input.getLinkRef();

        return getEquipment(topologyRef, nodeRef, rackRef, equipRef, tpRef, linkRef, tunnelRef);
    }

    @Override
    public PagedList getEquipmentPaged(GetEquipmentPagedInput input) throws Exception {
        TopologyId topologyRef = input.getTopologyRef();
        NodeId nodeRef = input.getNodeRef();
        String rackRef = input.getRackRef();
        String equipRef = input.getEquipmentRef();
        TpId tpRef = input.getTpRef();
        String tunnelRef = input.getTunnelRef();
        LinkId linkRef = input.getLinkRef();
        List<Node> equips = this
                .getEquipment(topologyRef, nodeRef, rackRef, equipRef, tpRef, linkRef, tunnelRef);
        PagedList pagedList = new PagedList(equips);
        pagedList.setFilter(input.getFilter());
        pagedList.sort(input.getSortInfos());
        return pagedList;
    }

    @Override
    public LocateResourcesByAlarmOutput locateResourceByAlarm(LocateResourcesByAlarmInput input) {
        log.debug("locate resource by alarm :{}", input);
        String resourceId = input.getAlarmResourceId();
        if (StringUtils.isBlank(resourceId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the alarm resource id should not be null");
        }
        //element locate
        boolean isTpId = PhysicalTpIdNamingRule.isTpId(resourceId);
        boolean isEquipId = PhysicalEqpIdNamingRule.isEquipId(resourceId);
        boolean isNeId = PhysicalNodeIdNamingRule.isPhyNodeId(resourceId);
        LocateResourcesByAlarmOutput locateResourcesByAlarmOutput = null;
        NMSResourceType nmsResourceType = null;
        if (isTpId) {
            nmsResourceType = NMSResourceType.TERMINATIONPOINT;
        } else if (isEquipId) {
            if (resourceId.contains(CHASSIS_INFIX)) {
                nmsResourceType = CHASSIS;
            } else if (resourceId.contains(TRANSCEIVER_INFIX)) {
                nmsResourceType = NMSResourceType.TRANSCEIVER;
            } else {
                nmsResourceType = NMSResourceType.CARD;
            }
        } else if (isNeId) {
            nmsResourceType = NMSResourceType.NODE;
        }
        log.debug("current alarm resource object type is:{}", nmsResourceType);
        locateResourcesByAlarmOutput = alarmResourceLocator.locateAlarmResource(resourceId,
                nmsResourceType);
        return locateResourcesByAlarmOutput;
    }

    /**
     * sync equipment eml service todo:invoking eml service
     *
     * @param input
     * @return
     */
    @Override
    public SyncEquipmentOutputBuilder syncEquipment(SyncEquipmentInput input) {
        return null;
    }

    /**
     * sync termination point todo: invoking eml service
     *
     * @param input
     * @return
     */
    @Override
    public SyncTerminationPointOutput syncTerminationPoint(SyncTerminationPointInput input) {
        return null;
    }

    /**
     * resynchronize ne todo:invoking eml service
     *
     * @param input
     * @return
     */
    @Override
    public ResynchronizeNeOutput resynchronizeNe(ResynchronizeNeInput input) {
        return null;
    }

    @Override
    public List<LldpInfos> getBoardLldpInfos(GetBoardLldpInfoInput input) {
        log.debug("get board lldp infos input is:{}", input);
        NodeId nodeId = input.getNodeId();
        String equipmentId = input.getEquipmentId();
        if (nodeId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "ref ne id should not be null");
        }
        if (equipmentId == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "ref board id should not be null");
        }
        String neId = nodeId.getValue();
        Equipments equipment = netconfTopology.getEquipment(neId, equipmentId);
        if (equipment == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("board %s is not existed on ne:%s", equipmentId, neId));
        }
        List<TerminationPoint> terminationPoints = new ArrayList<>();
        if ((equipment.getEquipTypeVendorSpecific() != null
                && equipment.getEquipTypeVendorSpecific().equals(CHASSIS.name()))
                || (equipment.getEquipTypeInstalled().equals(CHASSIS.name()))) {
            terminationPoints = netconfTopology.getTerminationPointIdRegex(neId);
        } else {
            terminationPoints = netconfTopology.getTerminationPointIdByRefEquipIds(
                    equipmentId);
        }
        List<LldpInfos> lldpInfos = getTerminationPointRefLLdpInfos(terminationPoints);
        return lldpInfos;
    }

    /**
     * get ref termination points lldps
     *
     * @param terminationPoints
     * @return
     */
    private List<LldpInfos> getTerminationPointRefLLdpInfos(
            List<TerminationPoint> terminationPoints) {
        log.debug("get termination point with lldp info");
        List<TerminationPoint> tpWithLldps = terminationPoints.stream()
                .filter(tp -> tp.getAugmentation(TerminationPoint1.class).getPhysical().getLldp()
                        != null)
                .filter(tp -> !CollectionUtils.isEmpty(
                        tp.getAugmentation(TerminationPoint1.class).getPhysical().getLldp()
                                .getNeighbor())).collect(
                        Collectors.toList());
        List<LldpInfos> lldpInfos = tpWithLldps.stream().map(this::buildLldpInfos)
                .collect(Collectors.toList());
        return lldpInfos;
    }

    private LldpInfos buildLldpInfos(TerminationPoint tp) {
        log.debug("build lldp info by tpId:{}", tp.getTpId());
        String tpId = tp.getTpId().getValue();
        Physical terminationPointPhysical = tp.getAugmentation(TerminationPoint1.class)
                .getPhysical();
        String tpName = terminationPointPhysical.getFriendlyName();
        Lldp lldp = terminationPointPhysical.getLldp();
        LldpInfosBuilder lldpInfosBuilder = new LldpInfosBuilder();
        lldpInfosBuilder.setTpId(TpId.getDefaultInstance(tpId));
        lldpInfosBuilder.setTpName(tpName);
        lldpInfosBuilder.setNeighbor(lldp.getNeighbor());
        return lldpInfosBuilder.build();
    }

    /**
     * get equipment
     *
     * @param topologyRef
     * @param nodeRef
     * @param rackRef
     * @param equipRef
     * @param tpRef
     * @param linkRef
     * @param tunnelRef
     * @return
     */
    private List<Node> getEquipment(TopologyId topologyRef, NodeId nodeRef, String rackRef,
            String equipRef, TpId tpRef, LinkId linkRef, String tunnelRef) {
        log.debug(
                "start get all PHY Equipment, topology:{}, node:{}, rack:{}, equip:{}, tp:{}, link:{}, tunnel:{}",
                topologyRef == null ? "null" : topologyRef.getValue(),
                nodeRef == null ? "null" : nodeRef.getValue(),
                rackRef == null ? "null" : rackRef, equipRef == null ? "null" : equipRef,
                tpRef == null ? "null" : tpRef.getValue(),
                linkRef == null ? "null" : linkRef.getValue(),
                tunnelRef == null ? "null" : tunnelRef);

        return new LinkedList<>();
    }

    @Override
    public GetVersionOutput getVersion() throws Exception {
        try {
            String apiVersion = SotnConfigurationUtils.getInstance().getString(API_VERSION);
            String controlleVersion = SotnConfigurationUtils.getInstance()
                    .getString(CONTROLLER_VERSION);
            String supportedAdapterApiVersionStr = SotnConfigurationUtils.getInstance()
                    .getString(SUPPORT_ADAPTER_VERSION);
            List<String> adapterVersionList = new ArrayList<String>(
                    Arrays.asList(supportedAdapterApiVersionStr.split(",")));
            GetVersionOutput output = new GetVersionOutputBuilder().setApiVersion(apiVersion)
                    .setControllerVersion(controlleVersion)
                    .setSupportedAdapterApiVersion(adapterVersionList)
                    .build();
            return output;
        } catch (Exception e) {
            log.error("Failed to get controller version info", e);
            throw new Exception("Failed to get controller version info");
        }
    }

    private Boolean checkPrimary(Site site, LinkId phyLinkId) {
        for (Route route : site.getExplictRoute().getRoute()) {
            Primary primary = route.getPrimary();
            for (ExplicitRouteObjects ero : primary.getExplicitRouteObjects()) {
                for (PathRouteObject pro : ero.getPathRouteObject()) {
                    if (pro.getResourceType().getImplementedInterface().getName()
                            .equals(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link.class
                                    .getName())) {
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link linkHop = (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.resource.type.resource.type.Link) pro
                                .getResourceType();
                        if (linkHop.getLinkHop().getLinkRef().getValue()
                                .equals(phyLinkId.getValue())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void countCustomer(List<Customer> customers, String custName, Integer gigbit) {
        List<Customer> customerList = new ArrayList<Customer>();
        boolean found = false;
        for (Customer customer : customers) {
            if (custName.equals(customer.getName())) {
                found = true;
                Integer number = customer.getImpactedTunnelNumber() + 1;
                Integer impGigbit = gigbit + customer.getTotalImpactedGigbit();
                Customer newCustomer = new CustomerBuilder()
                        .setImpactedTunnelNumber(number)
                        .setName(custName)
                        .setTotalImpactedGigbit(impGigbit)
                        .build();
                customerList.add(newCustomer);
            } else {
                customerList.add(customer);
            }
        }

        if (!found) {
            Customer newCustomer = new CustomerBuilder()
                    .setImpactedTunnelNumber(1)
                    .setName(custName)
                    .setTotalImpactedGigbit(gigbit)
                    .build();
            customerList.add(newCustomer);
        }

        customers.clear();
        customers.addAll(customerList);
    }

    private class SortByName implements Comparator<Object> {

        @Override
        public int compare(Object arg0, Object arg1) {
            {
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.Object a = arg0;
                org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.friendly.name.list.Object b = arg1;
                return a.getFriendlyName().compareTo(b.getFriendlyName());
            }
        }
    }


}
