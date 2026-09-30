package net.flex.dci.otn.controller.implement.common.utils;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.ChannelTargetPowerCalculator;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.frequency.Constant;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannelBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class WssChannelChannelPowerCalculator {

    private final ImplConfig implConfig = SpringBeanFinder.getBean(ImplConfig.class);


    private final static String TARGET_OUTPUT_POWER_C = "span-loss-adaptation.target-output-optical-power.oms-c";
    private final static String TARGET_OUTPUT_TILT_C = "tilt-control.target-output-tilt.oms-c";
    private final static String TARGET_OUTPUT_POWER_L = "span-loss-adaptation.target-output-optical-power.oms-l";
    private final static String TARGET_OUTPUT_TILT_L = "tilt-control.target-output-tilt.oms-l";

    private final static ChannelTargetPowerCalculator calculator = new ChannelTargetPowerCalculator();
    private ChangedObject changedObject;
    private boolean isLBand;
    private boolean isASE;

    public void updatePower(ChangedObject changedObject, RouteInfo rInfo, boolean isAse) {
        this.changedObject = changedObject;
        this.isLBand = isLBand(rInfo);
        this.isASE = isAse;

        updateWSSPower(rInfo);
    }

    private boolean isLBand(RouteInfo rInfo) {
        for (String xcId : rInfo.getXcIdList()) {
            String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
            Node node = changedObject.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            CrossConnections xc = nodeAttr.getCrossConnections().stream()
                    .filter(x -> x.getCrossConnectionId().getValue().equals(xcId))
                    .findAny()
                    .orElse(null);

            if (xc == null) {
                log.error("cannot find xc {}}", xcId);
                return false;
            }
            if (xc.getWssChannel() != null) {
                return PhysicalXcIdNamingRule.isLBandWssChannel(xc);
            }
        }
        return false;
    }

    private final String Channel_Target_Dest_Port_Optical_Power = "target-dest-port-output-optical-power";
    private final String Channel_Target_Source_Port_Optical_Power = "target-source-port-output-optical-power";
    //This is new imported based on calculate
    private void updateWSSPower(RouteInfo rInfo) {
        rInfo.getXcIdList().forEach(xcId-> {
            String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
            Node node = changedObject.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            if (implConfig.isWriteWithoutIP() && nodeAttr.getIp() == null) {
                return;
            }

            CrossConnections xc = nodeAttr.getCrossConnections().stream()
                    .filter(x -> x.getCrossConnectionId().getValue().equals(xcId))
                    .findAny()
                    .orElseThrow(() -> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, String.format("cannot find xc %s on node %s", xcId, nodeId)));

            if (xc.getWssChannel() != null) {
                String sTpId;
                String dTpId;
                boolean onDge = CommonUtils.onDGE(nodeAttr, xc);
                if (onDge) {
                    sTpId = xc.getSourceTp().get(0).getTpRef().getValue();
                    dTpId = xc.getDestinationTp().get(0).getTpRef().getValue();
                } else {
                    //IRA only has LINE port
                    dTpId = xc.getDestinationTp().get(0).getTpRef().getValue();
                    sTpId = dTpId;
                }
                Node opNode = changedObject.getChangedPhyOpNode(nodeId);
                if (opNode == null) {
                    throw new CommonException(CommonExceptionType.DEVICE_ERROR, String.format("the node hasn't been managed %s(%s)", nodeAttr.getFriendlyName(), nodeAttr.getIp()));
                }
                Double sPower = getPower(opNode, sTpId, xc.getWssChannel());
                Double dPower = getPower(opNode, dTpId, xc.getWssChannel());
                changedObject.unsetAllPhyOpNode();

                log.debug("the calculated value is {} {}", sPower, dPower);
                if (!onDge) {
                    log.debug("this is OTM NE, ZA power set to 0");
                    sPower = 0.0;
                }
                Properties newProp = xc.getWssChannel().getProperties();
                if (sPower != null) {
                    PropertyTool.addProperty(newProp, Channel_Target_Source_Port_Optical_Power, getValue(sPower).toString());
                }
                if (dPower != null) {
                    PropertyTool.addProperty(newProp, Channel_Target_Dest_Port_Optical_Power, getValue(dPower).toString());
                }
                CrossConnections newXc = new CrossConnectionsBuilder(xc)
                        .setWssChannel(new WssChannelBuilder(xc.getWssChannel())
                                .setProperties(newProp)
                                .build())
                        .build();

                Node newNode = new NodeBuilder(node)
                        .addAugmentation(Node1.class, new Node1Builder()
                                .setPhysical(new PhysicalBuilder(nodeAttr)
                                        .setCrossConnections(replaceXcInList(nodeAttr.getCrossConnections(), newXc))
                                        .build())
                                .build())
                        .build();

                changedObject.addChangedPhyNode(newNode);
            }
        });

    }

    private List<CrossConnections> replaceXcInList(List<CrossConnections> crossConnections, CrossConnections newXc) {
        List<CrossConnections> newXcList = crossConnections.stream().map(existXc -> {
            if (existXc.getCrossConnectionId().getValue().equals(newXc.getCrossConnectionId().getValue())) {
                return newXc;
            } else {
                return existXc;
            }
        }).collect(Collectors.toList());

        return newXcList;
    }

    private BigDecimal getValue(double voa) {
        BigDecimal bd = new BigDecimal(voa);
        bd = bd.setScale(1, BigDecimal.ROUND_HALF_UP);
        return bd;
    }

    /**
     * @param node
     * @param tpId
     * @param wssChannel
     * @return
     */
    private Double getPower(Node node, String tpId, WssChannel wssChannel) {
        long lower = wssChannel.getLowerFrequency().getValue().longValue();
        long upper = wssChannel.getUpperFrequency().getValue().longValue();
        log.debug("getPower on tp {}, {}-{}", tpId, lower, upper);

        TerminationPoint tp = node.getTerminationPoint().stream()
                .filter(x -> x.getTpId().getValue().equals(tpId)).findAny()
                .orElseThrow(() -> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        String.format("cannot find tp %s on node %s", tpId, node.getNodeId().getValue())));

        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();
        if (tpAttr.getPortType().equals(PortType.OALine) ||
                tpAttr.getPortType().equals(PortType.ILALINEA) || tpAttr.getPortType().equals(PortType.ILALINEB)) {

            long bandwidth = upper - lower;

            boolean isCBand;
            Properties tpProps = tpAttr.getProperties();
            double targetPower;
            double targetTilt;
            if (isLBand) {
                String value = PropertyTool.getValue(tpProps, TARGET_OUTPUT_POWER_L);
                if (value == null) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            String.format("cannot find property %s on tp %s", TARGET_OUTPUT_POWER_L, tpId));
                }
                targetPower = Double.parseDouble(value);

                value = PropertyTool.getValue(tpProps, TARGET_OUTPUT_TILT_L);
                if (value == null) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            String.format("cannot find property %s on tp %s", TARGET_OUTPUT_TILT_L, tpId));
                }
                targetTilt = Double.parseDouble(value);
                isCBand = false;
            } else {
                String value = PropertyTool.getValue(tpProps, TARGET_OUTPUT_POWER_C);
                if (value == null) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            String.format("cannot find property %s on tp %s", TARGET_OUTPUT_POWER_C, tpId));
                }
                targetPower = Double.parseDouble(value);

                value = PropertyTool.getValue(tpProps, TARGET_OUTPUT_TILT_C);
                if (value == null) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            String.format("cannot find property %s on tp %s", TARGET_OUTPUT_TILT_C, tpId));
                }
                targetTilt = Double.parseDouble(value);
                isCBand = true;
            }
            log.debug("{} targetOutputPower {}, targetOutputTilt{} on TP {}",
                    isCBand ? "C Band" : "L Band", targetPower, targetTilt, tpId);

            Map<Long, Double> adjustValue = calculator.calculateTargetPower(
                    lower, upper, bandwidth,
                    isCBand ? ChannelTargetPowerCalculator.C_BAND_TOTAL_SLOTS : ChannelTargetPowerCalculator.L_BAND_TOTAL_SLOTS,
                    isCBand ? Long.parseLong(Constant.MaxLowerFrequency.MUX64_BD_C) : Long.parseLong(Constant.MaxLowerFrequency.MUX64_BD_L),
                    isASE, targetPower, targetTilt);

            return adjustValue.get(lower);
        }

        return null;
    }

}
