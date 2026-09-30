package net.flex.dci.otn.controller.implement.common.ase.ne.bytedance;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.ase.ne.SpecificParamNode;
import net.flex.dci.otn.controller.implement.common.ase.ne.SpecificalParam;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.Aps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.ApsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otu.client.attributes.Client;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.otu.client.attributes.ClientBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuClient;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuClientBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.physical.OtuLineBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.supported.tunnels.SupportedTunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.explict.route.explict.route.Route;

@Slf4j
public class TdOtmSpecfic extends SpecificParamNode implements SpecificalParam {
    private static final PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);

    public TdOtmSpecfic(ChangedObject changedObject, Node node, RouteInfo rInfo) {
        super(changedObject, node, rInfo);
    }

    //change default value and stored in changedObject
    public void set() {
        log.debug("start to set TdOtmSpecfic for node:{}", node.getNodeId().getValue());
        //配置CHASSIS-1-1的子框类型为BONE_EPC
        changeChassisType();

        removeDefaultTpAttributes();
        //removed because related logic has impl at allocate
        //changeClientPortFecStatus();
        changeLinePortLaserAndOuterPutPower();
        changeClientPortAls();
        changeAps();

        done();
    }

    /** 精确电层仅恢复本次客户口的预处理，不修改公共 L/APS/EQ，也不扩展路由。 */
    public void setPrivateClientPortParams() {
        removeDefaultTpAttributes(true);
        changeClientPortAls();
        done();
    }

    private void changeAps() {
        for (String xcId : rInfo.getXcIdList()) {
            String nodeId = node.getNodeId().getValue();
            if (xcId.contains(nodeId)) {
                CrossConnections xc = nodeAttr.getCrossConnections().stream()
                        .filter(x -> x.getCrossConnectionId().getValue().equals(xcId)).findAny()
                        .orElse(null);
                if (xc == null) {
                    log.error("cannot find xc {} on node {}", xcId, nodeId);
                    changedObject.unsetPhyNode(nodeId);
                    node = changedObject.getChangedPhyNode(nodeId);
                    nodeAttr = node.getAugmentation(Node1.class).getPhysical();
                    xc = nodeAttr.getCrossConnections().stream()
                            .filter(x -> x.getCrossConnectionId().getValue().equals(xcId)).findAny()
                            .orElse(null);

                    if (xc == null) {
                        log.error("have to check on opNode");
                        xc = xcRepair(nodeId, xcId);
                    }
                }
                if (xc.getAps() != null) {
                    log.debug("change aps cross connection id:{} ({})", xc.getCrossConnectionId(), xc.getDescription());
                    Aps aps = xc.getAps();
                    String apsName = aps.getName() == null ? xc.getDescription() : aps.getName();
                    String[] parts = apsName.split("-");
                    String apsKey = parts[parts.length - 1];

                    // Preserve APS member states set during allocate/bind before adding TD defaults.
                    Properties newProps = aps.getProperties();
                    newProps = PropertyTool.addProperty(newProps, "absolute-switch-hysteresis",
                            "1");
                    newProps = PropertyTool.addProperty(newProps, "relative-switch-threshold", "3");
                    newProps = PropertyTool.addProperty(newProps, "relative-switch-hysteresis",
                            "3");
                    newProps = PropertyTool.addProperty(newProps, "och-ber-switch-mode", "true");
                    newProps = PropertyTool.addProperty(newProps, "och-ber-switch-threshold",
                            "0.021");
                    newProps = PropertyTool.addProperty(newProps,
                            "digital-diagnosis-auto-restore-mode", "true");
                    newProps = PropertyTool.addProperty(newProps,
                            "digital-diagnosis-auto-wait-to-restore-time", "600");
                    newProps = PropertyTool.addProperty(newProps,
                            "initial-digital-diagnosis-auto-downgrade-time", "100");
                    newProps = PropertyTool.addProperty(newProps,
                            "uncertain-digital-diagnosis-auto-downgrade-time", "600");
                    newProps = PropertyTool.addProperty(newProps,
                            "initial-digital-diagnosis-auto-restore-times", "5");
                    newProps = PropertyTool.addProperty(newProps,
                            apsKey + "A.absolute-switch-threshold", "-25");
                    newProps = PropertyTool.addProperty(newProps,
                            apsKey + "B.absolute-switch-threshold", "-25");
                    newProps = PropertyTool.addProperty(newProps,
                            apsKey + "C.absolute-switch-threshold", "-25");
                    newProps = PropertyTool.addProperty(newProps,
                            apsKey + "A.relative-switch-threshold-offset", "0");
                    newProps = PropertyTool.addProperty(newProps,
                            apsKey + "B.relative-switch-threshold-offset", "0");
                    newProps = PropertyTool.addProperty(newProps,
                            apsKey + "C.relative-switch-threshold-offset", "0");

                    Properties finalNewProps = newProps;
                    CrossConnections finalXc = xc;
                    List<CrossConnections> newXcList = nodeAttr.getCrossConnections().stream()
                            .map(x -> {
                                if (x.getCrossConnectionId().getValue().equals(xcId)) {
                                    return new CrossConnectionsBuilder(finalXc).setAps(
                                                    new ApsBuilder(finalXc.getAps())
                                                            .setApsMode(Aps.ApsMode.ABSOLUTE)
                                                            .setRevertive(false)
                                                            .setWaitToRestoreTime(1800000L)
                                                            .setHoldOffTime(0L)
                                                            .setProperties(finalNewProps)
                                                            .build())
                                            .build();
                                } else {
                                    return x;
                                }
                            }).collect(Collectors.toList());

                    node = new NodeBuilder(node).addAugmentation(Node1.class,
                                    new Node1Builder().setPhysical(
                                                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(
                                                            nodeAttr)
                                                            .setCrossConnections(newXcList)
                                                            .build())
                                            .build())
                            .build();

                    return;
                }
            }
        }
    }

    private CrossConnections xcRepair(String nodeId, String xcId) {
        Node opNode = phyNodeDao.getOpPhyNodeById(nodeId);
        if (opNode == null) {
            log.error("The node hasn't been managed by Adapter " + nodeId);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "The node hasn't been managed by Adapter " + nodeId);
        }

        CrossConnections xc = opNode.getAugmentation(Node1.class).getPhysical().getCrossConnections().stream()
                .filter(x -> x.getCrossConnectionId().getValue().equals(xcId)).findAny().orElse(null);
        if (xc == null) {
            log.error("cannot find xc {} on op node {}", xcId, nodeId);
            xc = continueXcRepair(xcId);
        }

        CrossConnections newXc;
        if (xc.getAps() != null) {
            newXc = new CrossConnectionsBuilder(xc).setAps(new ApsBuilder(xc.getAps())
                    .setProperties(null)
                    .build()).build();
        } else {
            newXc = new CrossConnectionsBuilder(xc)
                    .setProperties(null)
                    .build();
        }
        List<CrossConnections> newXcList = new ArrayList<>(nodeAttr.getCrossConnections());
        newXcList.add(newXc);

        Node newNode = new NodeBuilder(node).addAugmentation(Node1.class,
                        new Node1Builder().setPhysical(
                                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(
                                                nodeAttr)
                                                .setCrossConnections(newXcList)
                                                .build())
                                .build())
                .build();
        node = newNode;
        nodeAttr = newNode.getAugmentation(Node1.class).getPhysical();
        changedObject.addChangedPhyNode(newNode);
        log.debug("find aps on op node, start to repair xc {}", xcId);

        return newXc;
    }

    private CrossConnections continueXcRepair(String xcId) {
        OchLinkDao ochLinkDao = SpringBeanFinder.getBean(OchLinkDao.class);

        for (String svrId : rInfo.getLogicServerLinkIdList()) {
            if (OchLinkIdNamingRule.isOchLink(svrId)) {
                String ochLinkId = svrId;
                Link ochLink = ochLinkDao.getOchLinkByLinkId(ochLinkId);

                ArrayList<CrossConnectionAttributes> xcList = new ArrayList<>();
                Route route = ochLink.getAugmentation(Link1.class).getOch().getExplictRoute().getRoute().get(0);

                xcList.addAll(route.getPrimary().getCrossConnections());
                if (route.getSecondary() != null) {
                    xcList.addAll(route.getSecondary().getCrossConnections());
                }
                if (route.getThird() != null) {
                    route.getThird().forEach(x -> xcList.addAll(x.getCrossConnections()));
                }

                CrossConnectionAttributes xc = xcList.stream().filter(x -> x.getCrossConnectionId().getValue().equals(xcId))
                        .findAny().orElse(null);
                if (xc != null) {
                    log.debug("find xc {} from logic server link id {}", xcId, svrId);
                    return new CrossConnectionsBuilder(xc).build();
                } else {
                    log.error("cannot find xc on ochLink {}, {}", xcId, svrId);

                    //checking in Tunnel;
                    return continueXcRepairInTunnel(xcId, ochLink);
                }
            }
        }
        if (rInfo.getLogicServerLinkIdList() == null || rInfo.getLogicServerLinkIdList().isEmpty()) {
            return continueXcRepairInTunnel(xcId);
        }

        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "Cannoot find the xc in ochLink route! " + xcId);
    }

    private CrossConnections continueXcRepairInTunnel(String xcId) {
        TunnelDao tunnelDao = SpringBeanFinder.getBean(TunnelDao.class);
        String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);

        List<Tunnel> tunnels = tunnelDao.queryWithNode(nodeId);
        return continueXcRepairInTunnelList(xcId, tunnels);
    }

    private CrossConnections continueXcRepairInTunnelList(String xcId, List<Tunnel> tunnels) {
        for (Tunnel tunnel : tunnels) {
            Route route = tunnel.getExplictRoute().getRoute().get(0);
            ArrayList<CrossConnectionAttributes> xcList = new ArrayList<>();
            xcList.addAll(route.getPrimary().getCrossConnections());

            CrossConnectionAttributes xc = xcList.stream().filter(x -> x.getCrossConnectionId().getValue().equals(xcId))
                    .findAny().orElse(null);
            if (xc != null) {
                log.debug("find xc {} from tunnel id {}", xcId, tunnel.getTunnelId().getValue());
                return new CrossConnectionsBuilder(xc).build();
            }
        }
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "Cannoot find the xc in tunnels");
    }

    private CrossConnections continueXcRepairInTunnel(String xcId, Link ochLink) {
        log.debug("start to find xc {} in tunnels which over ochLink {}", xcId, ochLink.getLinkId().getValue());

        TunnelDao tunnelDao = SpringBeanFinder.getBean(TunnelDao.class);

        List<SupportedTunnel> tunnelList = ochLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.Link1.class).getSupportedTunnel();
        List<Tunnel> tunnels = tunnelList.stream()
                .map(t -> tunnelDao.getTunnelById(t.getTunnelRef().getValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return continueXcRepairInTunnelList(xcId, tunnels);
    }

    private void changeClientPortAls() {
        for (String tpId : rInfo.getTpIdList()) {
            String nodeId = node.getNodeId().getValue();
            if (tpId.contains(nodeId)) {
                TerminationPoint tp = node.getTerminationPoint().stream()
                        .filter(x -> x.getTpId().getValue().equals(tpId)).findAny().orElse(null);
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr = tp.getAugmentation(
                        TerminationPoint1.class).getPhysical();
                if (tpAttr.getOtuClient() != null) {
                    log.debug("change als and tx_laser on client port:{}", tpAttr.getFriendlyName());
                    List<TerminationPoint> newTpList = node.getTerminationPoint().stream()
                            .map(x -> {
                                if (x.getTpId().getValue().equals(tp.getTpId().getValue())) {
                                    Properties newProps = PropertyTool.addProperty(tpAttr.getProperties(), "als_ch_0", "false");
                                    newProps = PropertyTool.addProperty(newProps, "client-als", "ETHERNET");
                                    newProps = PropertyTool.addProperty(newProps, "als-delay", "200");
                                    newProps = PropertyTool.delProperty(newProps, "tx_laser");
                                    newProps = PropertyTool.delProperty(newProps, "tx_laser_0");
                                    newProps = PropertyTool.delProperty(newProps, "tx_laser_1");
                                    newProps = PropertyTool.delProperty(newProps, "tx_laser_2");
                                    newProps = PropertyTool.delProperty(newProps, "tx_laser_3");
                                    return new TerminationPointBuilder(tp).addAugmentation(
                                                    TerminationPoint1.class,
                                                    new TerminationPoint1Builder().setPhysical(
                                                                    new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                                                                            tpAttr)
                                                                            .setProperties(newProps)
                                                                            .build())
                                                            .build())
                                            .build();
                                } else {
                                    return x;
                                }
                            }).collect(Collectors.toList());

                    node = new NodeBuilder(node).setTerminationPoint(newTpList)
                            .build();
                }
            }
        }
    }

    //HW L port the lowest output power is 1, but defualt is -1
    private void changeLinePortLaserAndOuterPutPower() {
        for (String tpId : rInfo.getTpIdList()) {
            String nodeId = node.getNodeId().getValue();
            if (tpId.contains(nodeId)) {
                TerminationPoint tp = node.getTerminationPoint().stream()
                        .filter(x -> x.getTpId().getValue().equals(tpId)).findAny().orElse(null);
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr = tp.getAugmentation(
                        TerminationPoint1.class).getPhysical();
                if (tpAttr.getOtuLine() != null) {
                    BigDecimal power = new BigDecimal(-1);
                    if (nodeAttr.getVendorName().equalsIgnoreCase("HUAWEI")) {
                        power = new BigDecimal(1);
                    } else {
                        power = getMaxOutputPower(nodeId, tpId);
                    }
                    
                    log.debug("remove tx_laser and targetOutputPower on line port:{}", tpAttr.getFriendlyName());
                    BigDecimal finalPower = power;
                    List<TerminationPoint> newTpList = node.getTerminationPoint().stream()
                            .map(x -> {
                                if (x.getTpId().getValue().equals(tp.getTpId().getValue())) {
                                    Properties newProps = PropertyTool.delProperty(tpAttr.getProperties(), "tx_laser");
                                    newProps = PropertyTool.delProperty(newProps, "tx_laser_0");
                                    newProps = PropertyTool.delProperty(newProps, "tx_laser_1");
                                    newProps = PropertyTool.delProperty(newProps, "tx_laser_2");
                                    newProps = PropertyTool.delProperty(newProps, "tx_laser_3");

                                    return new TerminationPointBuilder(tp).addAugmentation(
                                                TerminationPoint1.class,
                                                new TerminationPoint1Builder().setPhysical(
                                                        new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(tpAttr)
                                                                .setOtuLine(new OtuLineBuilder(tpAttr.getOtuLine())
                                                                        .setTargetOutputPower(finalPower)
                                                                        .build())
                                                                .setProperties(newProps)
                                                                .build())
                                                .build())
                                            .build();
                                } else {
                                    return x;
                                }
                            }).collect(Collectors.toList());

                    node = new NodeBuilder(node).setTerminationPoint(newTpList).build();
                }
            }
        }
    }

    private BigDecimal getMaxOutputPower(String nodeId, String tpId) {
        Node opNode = phyNodeDao.getOpPhyNodeById(nodeId);

        Node cfgNode = changedObject.getChangedPhyNode(nodeId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical nodeAttr =
                cfgNode.getAugmentation(Node1.class).getPhysical();

        if (opNode == null || nodeAttr.getIp() == null) {
            log.error("The node hasn't been managed by Adapter or hasn't IP yet " + nodeAttr.getFriendlyName());
            return new BigDecimal(-1);
        }

        TerminationPoint tp = opNode.getTerminationPoint().stream().filter(x -> x.getTpId().getValue().equals(tpId))
                .findAny().orElseThrow(() -> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "cannot find out tpId " + tpId));

        Physical tpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();
        String value = PropertyTool.getValue(tpAttr.getProperties(), "max-output-power");
        log.debug("find max-output-power on tp {} = {}", tpId, value);
        if (value == null || StringUtils.isEmpty(value)) {
            return new BigDecimal(-1);
        }

        try {
            double power = Double.parseDouble(value);
            if (power > 5.0) {
                power = 5.0;
            }
            return new BigDecimal(power);
        } catch (java.lang.NumberFormatException e) {
            log.error("invalid max-output-power, return default value:-1", e);
            return new BigDecimal(-1);
        }
    }

    private void changeClientPortFecStatus() {
        for (String tpId : rInfo.getTpIdList()) {
            String nodeId = node.getNodeId().getValue();
            if (tpId.contains(nodeId)) {
                TerminationPoint tp = node.getTerminationPoint().stream()
                        .filter(x -> x.getTpId().getValue().equals(tpId)).findAny().orElse(null);
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr = tp.getAugmentation(
                        TerminationPoint1.class).getPhysical();
                if (tpAttr.getOtuClient() != null) {
                    log.debug("change FEC attribute on client port:{}", tpAttr.getFriendlyName());

                    OtuClient otuClient = tpAttr.getOtuClient();
                    Client client = otuClient.getClient();

                    List<TerminationPoint> newTpList;
                    List<Equipments> newEqList;
                    if (client != null && client.getEthComplianceCode() != null && client.getEthComplianceCode()
                            .getSimpleName().endsWith("LR4")) {
                        log.debug("this port {} is LR4 {}", tpAttr.getFriendlyName(), client.getEthComplianceCode().getSimpleName());
                        newTpList = setFecStatus2Disabled(tp, otuClient, true);
                        newEqList = setFecStatus2Disabled(tpId, tpAttr.getPortType(), true);
                    } else {
                        log.debug("this port {} is NOT LR4 {}", tpAttr.getFriendlyName(), client.getEthComplianceCode().getSimpleName());
                        newTpList = setFecStatus2Disabled(tp, otuClient, false);
                        newEqList = setFecStatus2Disabled(tpId, tpAttr.getPortType(), false);
                    }

                    node = new NodeBuilder(node).setTerminationPoint(newTpList)
                            .addAugmentation(Node1.class, new Node1Builder().setPhysical(
                                            new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder(
                                                    nodeAttr)
                                                    .setEquipments(newEqList)
                                                    .build())
                                    .build())
                            .build();

                    return;
                }
            }
        }
    }

    private void removeDefaultTpAttributes() {
        removeDefaultTpAttributes(false);
    }

    private void removeDefaultTpAttributes(boolean clientOnly) {
        List<TerminationPoint> newTpList = node.getTerminationPoint().stream().map(tp -> {
            if (!rInfo.getTpIdList().contains(tp.getTpId().getValue())) {
                return tp;
            }

            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr =
                    tp.getAugmentation(TerminationPoint1.class).getPhysical();
            Properties newProps = null;
            if (tpAttr.getOtuClient() != null) {
                newProps = PropertyTool.delProperty(tpAttr.getProperties(), "tti-msg-auto");
                newProps = PropertyTool.delProperty(newProps, "loopback-mode");
                newProps = PropertyTool.delProperty(newProps, "test-signal");
            } else if (!clientOnly && tpAttr.getOtuLine() != null) {
                newProps = PropertyTool.delProperty(tpAttr.getProperties(), "tti-msg-auto");
            }

            if (newProps != null) {
                TerminationPoint newTp = new TerminationPointBuilder(tp).addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                                .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(tpAttr)
                                        .setProperties(newProps)
                                        .build())
                                .build())
                        .build();

                return newTp;
            } else {
                return tp;
            }
        }).collect(Collectors.toList());

        node = new NodeBuilder(node).setTerminationPoint(newTpList).build();
    }

    private List<Equipments> setFecStatus2Disabled(String tpId, PortType portType, boolean yes) {
        String eqId = PhysicalTpIdNamingRule.getTransceiverId(tpId, portType);

        List<Equipments> newEqList = nodeAttr.getEquipments().stream().map(x -> {
            if (x.getEquipmentId().equals(eqId)) {
                String value;
                if (yes) {
                    value = "DISABLED";
                } else {
                    value = "ENABLED";
                }
                Properties newProps = PropertyTool.addProperty(x.getProperties(), "FEC_MODE",
                        value);
                Equipments newEq = new EquipmentsBuilder(x).setProperties(newProps).build();
                return newEq;
            } else {
                return x;
            }
        }).collect(Collectors.toList());

        return newEqList;
    }

    private List<TerminationPoint> setFecStatus2Disabled(TerminationPoint tp, OtuClient otuClient, boolean yes) {
        List<TerminationPoint> newTpList = node.getTerminationPoint().stream().map(x -> {
            if (x.getTpId().getValue().equals(tp.getTpId().getValue())) {
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr = tp.getAugmentation(
                        TerminationPoint1.class).getPhysical();
                Client client = otuClient.getClient();
                Client newClient;
                if (yes) {
                    newClient = new ClientBuilder(client).setFecMode(FecMode.Disable).build();
                } else {
                    newClient = new ClientBuilder(client).setFecMode(FecMode.Enable).build();
                }
                TerminationPoint newTp = new TerminationPointBuilder(tp).addAugmentation(
                                TerminationPoint1.class,
                                new TerminationPoint1Builder().setPhysical(
                                                new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(
                                                        tpAttr)
                                                        .setOtuClient(new OtuClientBuilder(otuClient)
                                                                .setClient(newClient)
                                                                .build())
                                                        .build())
                                        .build())
                        .build();
                return newTp;
            } else {
                return x;
            }
        }).collect(Collectors.toList());

        return newTpList;
    }

}
