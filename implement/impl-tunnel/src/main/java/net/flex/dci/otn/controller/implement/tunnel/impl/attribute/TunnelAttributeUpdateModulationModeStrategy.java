/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.tunnel.impl.attribute;

import static net.flex.dci.otn.controller.implement.common.utils.CommonUtils.convert;
import static net.flex.dci.otn.controller.implement.common.utils.CommonUtils.logMessage;
import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.data.DataConvertors;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.TaskInfoMessage;
import net.flex.dci.otc.common.model.TaskInfoMessage.ActionType;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.controller.rpc.client.rpcs.NeManagerRpc;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.mongo.utils.MultipleTransaction;
import net.flex.dci.otn.controller.implement.tunnel.impl.util.TunnelRoute;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.eml.manager.rev180730.ConfigNeOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OduGranularity;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.Prot100GE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTU4;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTUc2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTUc3;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTUc4;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTUc6;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SignalProtocolType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.LinkId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Destination;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.Source;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.LinkBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.OchBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.och.AvailableBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTpBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuLineBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.UpdateTunnelInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRoute;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.ExplictRouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.RouteBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.Primary;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.route.PrimaryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class TunnelAttributeUpdateModulationModeStrategy extends
        AbstractTunnelAttributeUpdateStrategy {

//    private static TunnelAttributeUpdateModulationModeStrategy inst = null;

//    ChangedObject changedObject = null;
//    MultipleTransaction mongoTransaction;

    @Autowired
    private OchLinkDao ochLinkDao;

    @Autowired
    private MultipleTransaction mongoTransaction;


    @Override
    public boolean supports(UpdateTunnelInput updateTunnelInput) {
        return StringUtils.hasText(updateTunnelInput.getModulationMode());
    }

    @Override
    public void execute(String tunnelId, UpdateTunnelInput updateTunnelInput,
            TaskInfoMessage taskInfoMessage) {
        log.info("update tunnel:{} modulation mode:{}", tunnelId,
                updateTunnelInput.getModulationMode());
        Tunnel tunnel = tunnelDao.getTunnelById(tunnelId);
        if (null == tunnel) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("cannot find required tunnel %s.", tunnelId));
        }
        if (ImplementState.Allocate != tunnel.getImplementState()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("tunnel[%s] ImplementState must be allocate", tunnelId));
        }
        String modulationMode = updateTunnelInput.getModulationMode();
        String bandwidth = updateTunnelInput.getLineSignalRate();
        updateModulationMode(tunnel, modulationMode, bandwidth, taskInfoMessage);
    }

    @Override
    public ActionType taskActionType() {
        return ActionType.changeOpMode;
    }

    public void updateModulationMode(Tunnel tunnel, String modulationMode, String bandwidth,
            TaskInfoMessage taskInfoMessage)
            throws CommonException {
        TunnelUpdateModulationContext context = new TunnelUpdateModulationContext();
        ChangedObject changedObject = new ChangedObject();
        context.setChangedObject(changedObject);
        String tunnelName = tunnel.getFriendlyName();
        if (!StringUtils.hasText(bandwidth)) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "pls input LineSignalRate.");
        }

        String ochLinkId = TunnelRoute.getOchLinkId(tunnel);
        if (ochLinkId == null) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("Tunnel[%s] route error, cannot find out ochLink.",
                            tunnelName));
        }

        try {
            updateLPortModulationMode(ochLinkId, modulationMode, bandwidth, context);

            mongoTransaction.save(changedObject);

            logMessage(BroadCastConstant.UPDATE_TUNNEL_MODULE_MODE, tunnelName,
                    BLANK, taskInfoMessage);
        } catch (Exception e) {
            log.error("update tunnel modulation mode error", e);
            logMessage(BroadCastConstant.UPDATE_TUNNEL_MODULE_MODE, tunnelName,
                    e.getMessage(), taskInfoMessage);
        }
    }

    private void updateLPortModulationMode(String ochLinkId, String modulationMode,
            String newLinePortBandwidth, TunnelUpdateModulationContext context)
            throws CommonException {

        Link ochLink = ochLinkDao.getOchLinkByLinkId(ochLinkId);
        if (ochLink == null) {
            log.error("error ochLink info");
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("OchLink[%s] error info", ochLinkId));
        }

        //mode-id-target-rate:bandwidth:fec:modulation-rate:bound-rate
        log.info(String.format("update Modulation Mode[%s]", modulationMode));
        String[] tmp = modulationMode.split("-");
        String modeId = tmp[0];

        Och ochLinkAttr = ochLink.getAugmentation(Link1.class).getOch();

        Source src = ochLink.getSource();
        Destination des = ochLink.getDestination();

        String srcNeId = src.getSourceNode().getValue();
        String srcTpId = src.getSourceTp().getValue();
        String desNeId = des.getDestNode().getValue();
        String desTpId = des.getDestTp().getValue();
        Class<? extends SignalProtocolType> newLinePortSignalRate = getSignalRateByBandwidth(
                newLinePortBandwidth);

        boolean changeLineSignalRate = false;
        TerminationPoint srcTp = getTp(srcTpId, context);
        TerminationPoint dstTp = getTp(desTpId, context);

        if (srcTp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuLine()
                .getSignalRate() != newLinePortSignalRate &&
                dstTp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuLine()
                        .getSignalRate() != newLinePortSignalRate) {
            changeLineSignalRate = true;
        }

        if (changeLineSignalRate && ochLinkAttr.getImplementState() != ImplementState.Allocate) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    String.format("update ochLink [%s] Modulation Mode error.pls deImpl frist.",
                            ochLinkId));
        }
        if (!changeLineSignalRate) {
            //不需要修改signalRate L 口的带宽不变，只需要简单下发op-mode 变化
        } else {
            //*****
            //L 口带宽变化， 需要修改 xcID, xcSlot, och 属性 bandwidth，available, Tunnel route 中的XC 信息
            //*****
            OduGranularity newLineOduGranularity = getSlotGranularity(newLinePortSignalRate);
            List<AvailableBuilder> newAvailableBuilderList = getOchAvailables(
                    newLinePortSignalRate);
            int newSupportedTunnelNums = getNumByLineSignalRate(newLineOduGranularity);

            if (ochLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class)
                    .getSupportedTunnel() != null) {
                List<SupportedTunnel> tunnelRefs = ochLink.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class)
                        .getSupportedTunnel();

                for (SupportedTunnel st : tunnelRefs) {
                    Tunnel tunnel = tunnelDao.getTunnelById(st.getTunnelRef().getValue());
                    OduGranularity tunnelOdu = DataConvertors.getOduGranularity(
                            tunnel.getSignalRate());

                    for (AvailableBuilder ava : newAvailableBuilderList) {
                        if (ava.getSupportedOduj().equals(tunnelOdu)) {
                            updateTunnel(tunnel, newLineOduGranularity, ava, context);
                        }
                    }
                }

                List<Available> newAvailableList = newAvailableBuilderList.stream()
                        .map(x -> x.build()).collect(Collectors.toList());

                Link newOchLink = new LinkBuilder(ochLink)
                        .addAugmentation(Link1.class, new Link1Builder()
                                .setOch(new OchBuilder(ochLinkAttr)
                                        .setBandwidth(newSupportedTunnelNums + "")
                                        .setAvailable(newAvailableList)
                                        .setOdukType(newLineOduGranularity)
                                        .build())
                                .build())
                        .build();
                context.getChangedObject().addChangedOchLink(newOchLink);
            }

        }

        log.info(String.format("change tp op-mode directly {}, {}", srcTp, dstTp));
        writeNe(srcTp, modeId, newLinePortSignalRate, context);
        writeNe(dstTp, modeId, newLinePortSignalRate, context);
    }


    /**
     * @param tunnel
     * @param newLineOduGranularity
     * @param available
     * @return
     */
    private void updateTunnel(Tunnel tunnel, OduGranularity newLineOduGranularity,
            AvailableBuilder available, TunnelUpdateModulationContext context) {
        // 这个tunnel在L口 slot的位置 直接在available中修改
        List<Route> newRoute = new ArrayList<>();
        List<Integer> slotParam = new ArrayList<>();
        //一个Tunnel有两个C-L的交叉（位于两个电层网元上）， 他们的时隙是一样的，所以slotParam虽然会有两个值，但都一样
        //我们取slotParam.get(0) 就可以了

        ExplictRoute explictRoute = tunnel.getExplictRoute();
        for (Route route : explictRoute.getRoute()) {
            Primary primary = route.getPrimary();
            List<String> oldXcIdList = new ArrayList<>();

            List<CrossConnections> newXcList = new ArrayList<>();
            for (CrossConnections xc : primary.getCrossConnections()) {
                oldXcIdList.add(xc.getCrossConnectionId().getValue());

                CrossConnectionsBuilder newXcBuilder = new CrossConnectionsBuilder(xc)
                        .setSourceTp(updateXcSrcTpSlot(xc.getSourceTp(), newLineOduGranularity,
                                slotParam, context))
                        .setDestinationTp(
                                updateXcDstTpSlot(xc.getDestinationTp(), newLineOduGranularity,
                                        slotParam, context));

                if (slotParam.isEmpty()) {
                    //the XC of tunnel must have one slot in line port
                    context.getChangedObject().unsetTunnel(tunnel.getTunnelId().getValue());
                    return;
                }

                if (slotParam.size() == 1) {
                    //两个交叉的时隙都是一样的，只对第一个进行处理
                    if (!available.getAvailableOdujSlot().contains(slotParam.get(0).toString())) {
                        //原来L口是400G， 可以承载4条业务，现在变成200G， 只能承载2条业务，
                        //available中只有“1-2”， 通过比对，tunnel3, tunnel4不需要修改，保持原样
                        //the tunnel cannot be included in this och again
                        context.getChangedObject().unsetTunnel(tunnel.getTunnelId().getValue());
                        return;
                    }
                }
                newXcBuilder.setCrossConnectionId(PhysicalXcIdNamingRule.getXcId(newXcBuilder));
                newXcList.add(newXcBuilder.build());
                if (slotParam.size() == 1) {
                    removeAvaliable(available, slotParam.get(0));
                }
            }
            newRoute.add(new RouteBuilder(route)
                    .setPrimary(new PrimaryBuilder(primary).setCrossConnections(newXcList).build())
                    .build());

            updateNodeXcList(oldXcIdList, newXcList, context);
        }
        Tunnel newTunnel = new TunnelBuilder(tunnel)
                .setExplictRoute(new ExplictRouteBuilder(explictRoute).setRoute(newRoute).build())
                .build();
        context.getChangedObject().addChangedTunnel(newTunnel);
    }

    private void removeAvaliable(AvailableBuilder oldAva, Integer odujSlot) {
        String avaOdujString = oldAva.getAvailableOdujSlot();
        List<String> idsList = Arrays.asList(avaOdujString.split("-"))
                .stream().map(s -> s.trim()).collect(Collectors.toList());

        Iterator<String> iter = idsList.iterator();
        while (iter.hasNext()) {
            String id = iter.next();
            if (id.equals(odujSlot + "")) {
                iter.remove();
                break;
            }
        }

        String newAvaString = idsList.stream().map(n -> String.valueOf(n))
                .collect(Collectors.joining("-"));
        oldAva.setAvailableOdujSlot(newAvaString);
    }

    private void updateNodeXcList(List<String> oldXcIdList, List<CrossConnections> newXcList,
            TunnelUpdateModulationContext context) {
        for (String oldXcId : oldXcIdList) {
            //update Node xcList;
            String nodeId = PhysicalXcIdNamingRule.getNodeId(oldXcId);
            Node node = context.getChangedObject().getChangedPhyNode(nodeId);

            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections> xcs = node.getAugmentation(
                    Node1.class).getPhysical().getCrossConnections();
            xcs.removeIf(t -> t.getCrossConnectionId().getValue().equals(oldXcId));
            for (CrossConnections xc : newXcList) {
                if (PhysicalXcIdNamingRule.getNodeId(xc.getCrossConnectionId().getValue())
                        .equals(nodeId)) {
                    xcs.add(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder(
                            xc).build());
                }
            }
            Node newNode = new NodeBuilder(node)
                    .addAugmentation(Node1.class, new Node1Builder()
                            .setPhysical(new PhysicalBuilder(
                                    node.getAugmentation(Node1.class).getPhysical())
                                    .setCrossConnections(xcs)
                                    .build())
                            .build())
                    .build();
            context.getChangedObject().addChangedPhyNode(newNode);
        }
    }


    //"slot" : "/odu4x4=1/odu4=1"
    private List<DestinationTp> updateXcDstTpSlot(List<DestinationTp> destinationTps,
            OduGranularity newOduGranularity, List<Integer> slotParam,
            TunnelUpdateModulationContext context) {
        DestinationTp tp = destinationTps.get(0);
        String tpId = tp.getTpRef().getValue();
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        Node node = context.getChangedObject().getChangedPhyNode(nodeId);
        TerminationPoint phyTp = node.getTerminationPoint().stream()
                .filter(x -> x.getTpId().getValue().equals(tpId)).findFirst().get();
        if (phyTp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuLine() == null) {
            //this is not line port, doesn't need change
            return destinationTps;
        } else {
            List<DestinationTp> newTpList = new ArrayList<>();
            String oldSlotStr = tp.getSlot();
            String[] ids = oldSlotStr.split("\\/");
            String newSlotStr = String.format("/%s=1/%s", newOduGranularity.name().toLowerCase(),
                    ids[ids.length - 1]);
            newTpList.add(new DestinationTpBuilder(tp).setSlot(newSlotStr).build());

            ids = ids[ids.length - 1].split("=");
            slotParam.add(Integer.parseInt(ids[1]));
            return newTpList;
        }
    }

    //"slot" : "/odu4x4=1/odu4=1"
    private List<SourceTp> updateXcSrcTpSlot(List<SourceTp> sourceTps,
            OduGranularity newOduGranularity, List<Integer> slotParam,
            TunnelUpdateModulationContext context) {
        SourceTp tp = sourceTps.get(0);
        String tpId = tp.getTpRef().getValue();
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        Node node = context.getChangedObject().getChangedPhyNode(nodeId);
        TerminationPoint phyTp = node.getTerminationPoint().stream()
                .filter(x -> x.getTpId().getValue().equals(tpId)).findFirst().get();
        if (phyTp.getAugmentation(TerminationPoint1.class).getPhysical().getOtuLine() == null) {
            //this is not line port, doesn't need change
            return sourceTps;
        } else {
            List<SourceTp> newTpList = new ArrayList<>();
            String oldSlotStr = tp.getSlot();
            String[] ids = oldSlotStr.split("\\/");
            String newSlotStr = String.format("/%s=1/%s", newOduGranularity.name(),
                    ids[ids.length - 1]);
            newTpList.add(new SourceTpBuilder(tp).setSlot(newSlotStr).build());

            ids = ids[ids.length - 1].split("=");
            slotParam.add(Integer.parseInt(ids[1]));
            return newTpList;
        }
    }


    private TerminationPoint getTp(String tpId, TunnelUpdateModulationContext context)
            throws CommonException {
        String neId = PhysicalTpIdNamingRule.getNodeId(tpId);
        Node node = context.getChangedObject().getChangedPhyNode(neId);
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        Optional<TerminationPoint> tpOp = node.getTerminationPoint().stream()
                .filter(t -> t.getTpId().getValue().equals(tpId)).findFirst();
        if (tpOp.isPresent()) {
            return tpOp.get();
        } else {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot find required TP based on tpId " + tpId);
        }
    }

//    private Equipments getEq(String tpId) throws CommonException {
//        String eqId = PhysicalTpIdNamingRule.getEquipId(tpId);
//        String neId = PhysicalTpIdNamingRule.getNodeId(tpId);
//        Node node = changedObject.getChangedPhyNode(neId);
//        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
//        Optional<Equipments> eqOp = nodeAttr.getEquipments().stream()
//                .filter(t -> t.getEquipmentId().equals(eqId)).findFirst();
//        if (eqOp.isPresent()) {
//            return eqOp.get();
//        } else {
//            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
//                    "cannot find required EQ based on tpId " + tpId);
//        }
//    }


    private void writeNe(TerminationPoint tp, String modeId,
            Class<? extends SignalProtocolType> newSignalRate,
            TunnelUpdateModulationContext context) throws CommonException {
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tp.getTpId().getValue());
        Node node = context.getChangedObject().getChangedPhyNode(nodeId);

        //save change to configDB
        List<TerminationPoint> tps = new ArrayList<>();
        TerminationPoint uptTpMode = getUpdateTpWithProperty(node, tp, modeId, newSignalRate,
                context);
        tps.add(uptTpMode);

        if (tp.getAugmentation(TerminationPoint1.class).getPhysical().getImplementState()
                == ImplementState.Implement) {
            NeManagerRpc config = SpringBeanFinder.getBean(NeManagerRpc.class);

            //1. makeTpDown at first
            Node downNode = getUpdateTpWithState(nodeId, tp, AdminStatus.Down);
            ConfigNeOutput downResult = config.configNe(downNode);
            if (downResult.getFailObj() != null && downResult.getFailObj().getObject() != null) {
                throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                        convert(downResult.getFailObj()));
            }

            //2. change tp attribute
            Node1Builder node1Builder = new Node1Builder()
                    .setPhysical(new PhysicalBuilder().build());

            Node1 newNode = node1Builder.build();

            Node uptNode = new NodeBuilder()
                    .setNodeId(new NodeId(nodeId))
                    .setTerminationPoint(tps)
                    .addAugmentation(Node1.class, newNode).build();
            ConfigNeOutput uptResult = config.configNe(uptNode);
            if (downResult.getFailObj() != null && downResult.getFailObj().getObject() != null) {
                throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                        convert(uptResult.getFailObj()));
            }

            //3. make tp up again
            Node upNode = getUpdateTpWithState(nodeId, tp, AdminStatus.Up);
            ConfigNeOutput upResult = config.configNe(upNode);
            if (downResult.getFailObj() != null && downResult.getFailObj().getObject() != null) {
                throw new CommonException(CommonExceptionType.DEVICE_ERROR,
                        convert(upResult.getFailObj()));
            }
        }
    }

    private Node getUpdateTpWithState(String nodeId, TerminationPoint tp, AdminStatus status) {
        List<TerminationPoint> tps = new ArrayList<TerminationPoint>();
        TerminationPoint newTp = new TerminationPointBuilder()
                .setTpId(new TpId(tp.getTpId()))
                .setKey(new TerminationPointKey(new TpId(tp.getTpId())))
                .addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder()
                                .setPhysical(
                                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder()
                                                .setAdminState(status)
                                                .build())
                                .build())
                .build();

        tps.add(newTp);

        Node1Builder node1Builder = new Node1Builder()
                .setPhysical(new PhysicalBuilder().build());

        Node1 newNode = node1Builder.build();

        Node node = new NodeBuilder()
                .setNodeId(new NodeId(nodeId))
                .setTerminationPoint(tps)
                .addAugmentation(Node1.class, newNode).build();

        return node;
    }

    private String getOperationModeFromNe(Node ne, String modeId) {
        String operationMode = "";
        String mode = "\"mode-id\":" + modeId;

        Node1 node1 = ne.getAugmentation(Node1.class);
        if (node1.getPhysical() != null && node1.getPhysical().getProperties() != null) {
            boolean foundMode = false;
            for (Property pro : node1.getPhysical().getProperties().getProperty()) {
                if ("supportedOperationModes".equals(pro.getName())) {
                    foundMode = true;
                    String values = pro.getValue();
                    values = values.substring(1, values.length() - 1);

                    log.debug("supportedOperationModes:{}", values);

                    String[] tmp = values.split("},");

                    boolean found = false;
                    for (int i = 0; i < tmp.length; i++) {
                        if (tmp[i].indexOf(mode) != -1) {
                            found = true;
                            if (i != tmp.length - 1) {
                                operationMode = tmp[i] + "}";
                            } else {
                                operationMode = tmp[i];
                            }
                        }
                    }
                    if (!found) {
                        operationMode = "{\"mode-id\":" + modeId + "}";
                    }
                }
            }
            if (!foundMode) {
                operationMode = "{\"mode-id\":" + modeId + "}";
            }
        }
        return operationMode;
    }

    /**
     * export need changed poart for setting to NE
     *
     * @param ne
     * @param tp
     * @param modeId
     * @param newSignalRate
     * @return
     */
    private TerminationPoint getUpdateTpWithProperty(Node ne, TerminationPoint tp, String modeId,
            Class<? extends SignalProtocolType> newSignalRate,
            TunnelUpdateModulationContext context) {
        String operationMode = getOperationModeFromNe(ne, modeId);

        List<Property> updatedPros = new ArrayList<>();
        Property pro = new PropertyBuilder().setName("operationMode").setValue(operationMode)
                .build();
        updatedPros.add(pro);

        List<Property> newPros = new ArrayList<>();
        TerminationPoint1 tp1 = tp.getAugmentation(TerminationPoint1.class);
        if (tp1.getPhysical().getProperties() != null
                && tp1.getPhysical().getProperties().getProperty() != null) {
            Iterator<Property> iter = tp1.getPhysical().getProperties().getProperty().iterator();
            while (iter.hasNext()) {
                Property existedPro = iter.next();
                if ("operationMode".equals(existedPro.getName())) {
                    iter.remove();
                    break;
                }
            }
            newPros.addAll(tp1.getPhysical().getProperties().getProperty());
        }
        newPros.addAll(updatedPros);

        Properties newProperties = new PropertiesBuilder().setProperty(newPros).build();
        TerminationPoint mergeTp = new TerminationPointBuilder(tp)
                .addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder(tp1)
                                .setPhysical(
                                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                                                tp1.getPhysical())
                                                .setProperties(newProperties)
                                                .setOtuLine(new OtuLineBuilder(
                                                        tp1.getPhysical().getOtuLine())
                                                        .setSignalRate(newSignalRate)
                                                        .build())
                                                .build())
                                .build())
                .build();
        List<TerminationPoint> tps = ne.getTerminationPoint();
        tps.removeIf(t -> t.getTpId().equals(tp.getTpId()));
        tps.add(mergeTp);

        context.getChangedObject()
                .addChangedPhyNode(new NodeBuilder(ne).setTerminationPoint(tps).build());

        Properties updateProperties = new PropertiesBuilder().setProperty(updatedPros).build();
        TerminationPoint updatingTp = new TerminationPointBuilder(tp)
                .addAugmentation(TerminationPoint1.class,
                        new TerminationPoint1Builder()
                                .setPhysical(
                                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder()
                                                .setProperties(updateProperties)
                                                .setOtuLine(new OtuLineBuilder()
                                                        .setSignalRate(newSignalRate).build())
                                                .build())
                                .build())
                .build();

        log.debug(String.format("updateLPortModulationMode[%s]", updatingTp));
        return updatingTp;
    }

    private Class<? extends SignalProtocolType> getSignalRateByBandwidth(String bandwidth) {
        Class<? extends SignalProtocolType> signalType = null;
        if (bandwidth.equalsIgnoreCase("100G")) {
            signalType = Prot100GE.class;
        } else if (bandwidth.equalsIgnoreCase("200G")) {
            signalType = ProtOTUc2.class;
        } else if (bandwidth.equalsIgnoreCase("300G")) {
            signalType = ProtOTUc3.class;
        } else if (bandwidth.equalsIgnoreCase("400G")) {
            signalType = ProtOTUc4.class;
        } else if (bandwidth.equalsIgnoreCase("600G")) {
            signalType = ProtOTUc6.class;
        }
        return signalType;
    }

    private List<AvailableBuilder> getOchAvailables(
            Class<? extends SignalProtocolType> lineSignalRate) {
        String slot = "";
        if (lineSignalRate.equals(Prot100GE.class)) {
            slot = "1";
        } else if (lineSignalRate.equals(ProtOTU4.class)) {
            slot = "1";
        } else if (lineSignalRate.equals(ProtOTUc2.class)) {
            slot = "1-2";
        } else if (lineSignalRate.equals(ProtOTUc3.class)) {
            slot = "1-2-3";
        } else if (lineSignalRate.equals(ProtOTUc4.class)) {
            slot = "1-2-3-4";
        } else if (lineSignalRate.equals(ProtOTUc6.class)) {
            slot = "1-2-3-4-5-6";
        }
        List<AvailableBuilder> availables = new ArrayList<AvailableBuilder>();

        availables.add(new AvailableBuilder()
                .setSupportedOduj(OduGranularity.Odu4)
                .setAvailableOdujSlot(slot));
        return availables;
    }

    private OduGranularity getSlotGranularity(Class<? extends SignalProtocolType> lineSignalRate) {
        if (lineSignalRate.equals(Prot100GE.class)) {
            return OduGranularity.Odu4;
        } else if (lineSignalRate.equals(ProtOTU4.class)) {
            return OduGranularity.Odu4;
        } else if (lineSignalRate.equals(ProtOTUc2.class)) {
            return OduGranularity.Odu4x2;
        } else if (lineSignalRate.equals(ProtOTUc3.class)) {
            return OduGranularity.Odu4x3;
        } else if (lineSignalRate.equals(ProtOTUc4.class)) {
            return OduGranularity.Odu4x4;
        } else if (lineSignalRate.equals(ProtOTUc6.class)) {
            return OduGranularity.Odu4x6;
        } else {
            return null;
        }
    }

    private void updateOch(Link ochLink, String bandwidth, OduGranularity oduGranularity,
            List<Available> newAvailables, List<Available> oldAvailables, int supportedTunnelNums,
            TunnelUpdateModulationContext context) {
        List<Available> availables = new ArrayList<Available>();
        for (Available newAvailable : newAvailables) {
            for (Available oldAvailable : oldAvailables) {
                if (newAvailable.getSupportedOduj() == oldAvailable.getSupportedOduj()) {
                    String[] newOdujSlot = newAvailable.getAvailableOdujSlot().split("-");
                    String[] oldOdujSlot = oldAvailable.getAvailableOdujSlot().split("-");
                    String odujSlot = "";
                    int oldNum = 0;
                    if (oldOdujSlot.length == 1) {
                        if (!"".equals(oldOdujSlot[0])) {
                            oldNum = oldOdujSlot.length;
                        }
                    } else {
                        oldNum = oldOdujSlot.length;
                    }
                    int oldSlotLength = oldNum + supportedTunnelNums;
                    if (oldSlotLength > newOdujSlot.length) {//down
                        for (int i = 0; i < newOdujSlot.length; i++) {
                            for (int j = 0; j < oldOdujSlot.length; j++) {
                                if (newOdujSlot[i].equals(oldOdujSlot[j])) {
                                    if ("".equals(odujSlot)) {
                                        odujSlot = oldOdujSlot[j];
                                    } else {
                                        odujSlot = odujSlot + "-" + oldOdujSlot[j];
                                    }
                                }
                            }
                        }
                    } else if (oldSlotLength < newOdujSlot.length) {//up
                        for (int i = 0; i < oldOdujSlot.length; i++) {
                            for (int j = 0; j < newOdujSlot.length; j++) {
                                if (oldOdujSlot[i].equals(newOdujSlot[j])) {
                                    if ("".equals(odujSlot)) {
                                        odujSlot = oldOdujSlot[i];
                                    } else {
                                        odujSlot = odujSlot + "-" + oldOdujSlot[i];
                                    }
                                }
                            }
                        }

                        for (int i = oldSlotLength; i < newOdujSlot.length; i++) {
                            if ("".equals(odujSlot)) {
                                odujSlot = newOdujSlot[i];
                            } else {
                                odujSlot = odujSlot + "-" + newOdujSlot[i];
                            }
                        }
                    }

                    Available available = new AvailableBuilder()
                            .setSupportedOduj(newAvailable.getSupportedOduj())
                            .setAvailableOdujSlot(odujSlot).build();
                    availables.add(available);
                }
            }
        }
        Link1 link1 = ochLink.getAugmentation(Link1.class);

        Och och = new OchBuilder(link1.getOch()).setBandwidth(bandwidth)
                .setSlotGranularity(oduGranularity)
                .setAvailable(availables).build();

        context.getChangedObject().addChangedOchLink(new LinkBuilder(ochLink)
                .addAugmentation(Link1.class, new Link1Builder(link1)
                        .setOch(och)
                        .build())
                .build());
        MultipleTransaction mongoTransaction = SpringBeanFinder.getBean(MultipleTransaction.class);
//        ochLinkDao.updateLinkAttributeOch(ochConnId, och);
    }

    private void updateFrequencyForFlex(String ochConnId, List<LinkId> siteLinkIds,
            FrequencyType lowFrequency,
            FrequencyType uppFrequency, String eqpType, TunnelUpdateModulationContext context)
            throws CommonException {
        BigInteger interval = new BigInteger("0");
        if (uppFrequency.getValue().subtract(lowFrequency.getValue()).longValue()
                == interval.longValue()) {//don't need update frequency
            return;
        }
        ChangedObject changedObject = context.getChangedObject();
        Link ochLink = changedObject.getChangedOchLink(ochConnId);
        Node node = changedObject.getChangedPhyNode(ochLink.getSource().getSourceNode().getValue());
        NeYangModel yangModel = NeYangModel.getModel(
                node.getAugmentation(Node1.class).getPhysical());

        for (LinkId siteLinkId : siteLinkIds) {
            Link siteLink = changedObject.getChangedSiteLink(siteLinkId.getValue());
            if (siteLink != null) {
                //for CMux
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1 siteLink1 = siteLink
                        .getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class);
                GridType gridType = siteLink1.getSite().getGrid();
                if (GridType._0 == gridType) {
                    List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available> availables = siteLink1
                            .getSite().getAvailable();
                    List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available> newAvailables = rebuildAvailables(
                            lowFrequency.getValue(), uppFrequency.getValue(), availables);
                    String frequency = getFrequency(newAvailables, interval);

//                    TunnelAttributeUpdateFrequencyStrategy.instance()
//                            .updateOchFrequency(ochConnId, new Frequency(yangModel, frequency),
//                                    new TunnelFrequencyUpdateContext());
                }
            }
        }
    }

    private List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available> rebuildAvailables(
            BigInteger lowerValue,
            BigInteger upperValue,
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available> availables) {
        List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available> newAvailables
                = new ArrayList<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available>();
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available available : availables) {
            if (available.getLowerFrequency().getValue().longValue() == upperValue.longValue()) {
                upperValue = available.getUpperFrequency().getValue();
            } else if (available.getUpperFrequency().getValue().longValue()
                    == lowerValue.longValue()) {
                lowerValue = available.getLowerFrequency().getValue();
            } else {
                newAvailables.add(available);
            }
        }

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available available = new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableBuilder()
                .setKey(new AvailableKey(new FrequencyType(lowerValue)))
                .setLowerFrequency(new FrequencyType(lowerValue))
                .setUpperFrequency(new FrequencyType(upperValue))
                .build();
        newAvailables.add(available);
        return newAvailables;
    }

    private String getFrequency(
            List<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available> availables,
            BigInteger interval) {
        BigInteger maxUpper = new BigInteger("0");
        for (org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available available : availables) {
            BigInteger tmp = available.getUpperFrequency().getValue().subtract(interval);
            if (available.getUpperFrequency().getValue().longValue() > maxUpper.longValue()
                    && tmp.longValue() >= available.getLowerFrequency().getValue().longValue()) {
                maxUpper = available.getUpperFrequency().getValue();
            }
        }
        BigInteger lower = maxUpper.subtract(interval);

        //the format is channelID-lowerFrequecy,higherFrequecy.
        String frequency = "1-" + lower + "," + maxUpper;
        return frequency;
    }

    private int getNumByLineSignalRate(OduGranularity odukType) throws CommonException {
        switch (odukType) {
            case Odu4:
                return 1;
            case Odu4x2:
                return 2;
            case Odu4x3:
                return 3;
            case Odu4x4:
                return 4;
            case Odu4x6:
                return 6;
        }
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "unSupported OCH link bandwidth type (oduType) " + odukType.name());
    }

    @Data
    private static class TunnelUpdateModulationContext {

        private ChangedObject changedObject;
    }


}
