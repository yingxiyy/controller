package net.flex.dci.otc.controller.utils.bytedance;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otc.optical.tool.OpticalPathCalculator;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FIXEDGAINRANGE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GAINRANGE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.HIGHGAINRANGE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LOWGAINRANGE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.amplifier.attributes.Amplifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.amplifier.attributes.AmplifierBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class OaCardVoaGain {
    private static final double VOA_UPDATE_DELTA = 0.1;
    private static final String EGRESS_VOA = "egress-voa-atten";

    private final PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);
    private ChangedObject changedObject;

    public OaCardVoaGain(ChangedObject changedObject) {
        this.changedObject = changedObject;
    }

    public void updateTargetGain(List<OpticalPathCalculator.SegmentCalculationResult> resultList, boolean isAz) {
        for (OpticalPathCalculator.SegmentCalculationResult result : resultList) {
            double voaA;
            double targetGainZ;
            double gainRangeBoundaryThresholdZ;
            String gainRange;
            String nodeAId;
            String nodeZId;

            voaA = result.getVoaA();
            targetGainZ = result.getTargetGainZ();
            gainRangeBoundaryThresholdZ = result.getGainRangeBoundaryThresholdZ();
            gainRange = result.getNameOfGainRangeZ();
            nodeAId = result.getSegment().getNodeAId();
            nodeZId = result.getSegment().getNodeZId();

            log.info("calculated optical params: direction={}, voaA={}, targetGainZ={}, gainRange={}, gainRangeBoundaryThreshold={}, nodeA={}, nodeZ={}",
                    isAz ? "A->Z" : "Z->A", voaA, targetGainZ, gainRange, gainRangeBoundaryThresholdZ, getNodeInfo(nodeAId), getNodeInfo(nodeZId));
            updateNodeVoa(nodeAId, voaA, isAz);
            updateNodeGain(nodeZId, targetGainZ, gainRange, gainRangeBoundaryThresholdZ, isAz);
        }
    }

    private void updateNodeGain(String nodeZId, double targetGainZ, final String gainRange,
                                double gainRangeBoundaryThreshold, boolean isAz) {
        Node node = changedObject.getChangedPhyNode(nodeZId);
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        List<CrossConnections> newXcList = nodeAttr.getCrossConnections()
                .stream().map(xc -> {
                    String newGainRange = gainRange;

                    if (xc.getAmplifier() != null) {
                        if (isAz && !xc.getDescription().contains("WEST") && !xc.getDescription().contains("PA")) {
                            return xc;
                        }
                        if (!isAz && !xc.getDescription().contains("EAST") && !xc.getDescription().contains("PA")) {
                            return xc;
                        }
                        if (xc.getDescription().contains("PA")) {
                            BigDecimal paTageGain = BigDecimal.valueOf(targetGainZ).setScale(1, RoundingMode.HALF_UP);
                            Class<? extends GAINRANGE> paGainRange = getPaGainRange(paTageGain);
                            log.info("For PA,:{} , target gain:{},gain range:{}", xc.getDescription(), paTageGain, paGainRange);
                            return new CrossConnectionsBuilder(xc)
                                    .setAmplifier(new AmplifierBuilder(xc.getAmplifier())
                                            .setTargetGain(paTageGain)
                                            .setGainRange(paGainRange)
                                            .build())
                                    .build();
                        }
                        log.info("apply targetGain/gainRange: direction={}, xc={}, targetGain={}, gainRange={}",
                                isAz ? "A->Z" : "Z->A", xc.getDescription(), targetGainZ, newGainRange);

                        if (xc.getDescription().contains("BA")) {
                            if (!newGainRange.toLowerCase().contains("fix")) {
                                log.info("the BA only support fixGainRange, change it to fixed");
                                newGainRange = "fixed";
                            }
                        }

                        return new CrossConnectionsBuilder(xc)
                                .setAmplifier(new AmplifierBuilder(xc.getAmplifier())
                                        .setTargetGain(BigDecimal.valueOf(targetGainZ).setScale(1, RoundingMode.HALF_UP))
                                        .setGainRange(getSafeGainRange(nodeZId, xc, getGainRange(newGainRange), gainRangeBoundaryThreshold))
                                        .build())
                                .build();
                    } else {
                        return xc;
                    }
                }).collect(Collectors.toList());

        Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(nodeAttr)
                                .setCrossConnections(newXcList)
                                .build())
                        .build())
                .build();

        changedObject.addChangedPhyNode(newNode);
    }

    private Class<? extends GAINRANGE> getPaGainRange(BigDecimal paTagGain) {
        BigDecimal PA_GAIN_THRESHOLD = new BigDecimal("20");
        return paTagGain.compareTo(PA_GAIN_THRESHOLD) > 0
                ? HIGHGAINRANGE.class
                : LOWGAINRANGE.class;
    }

    private Class<? extends GAINRANGE> getGainRange(String gainRange) {
        gainRange = gainRange.toLowerCase();
        if (gainRange.contains("fixed")) {
            // fixed means the gain-range enum remains FIXEDGAINRANGE instead of being converted to high/low.
            return FIXEDGAINRANGE.class;
        } else if (gainRange.contains("low")) {
            return LOWGAINRANGE.class;
        }
        return HIGHGAINRANGE.class;
    }

    private Class<? extends GAINRANGE> getSafeGainRange(String nodeId, CrossConnections cfgXc,
                                                        Class<? extends GAINRANGE> targetGainRange,
                                                        double gainRangeBoundaryThreshold) {
        Amplifier cfgAmplifier = cfgXc.getAmplifier();
        Node opNode = getOpNode(nodeId);
        if (opNode == null) {
            // No OP snapshot means there is no known device-side gainRange to protect.
            // In this case keep the original behavior and allow the calculated range.
            log.info("allow amplifier gainRange switch because OP node is missing, nodeId: {}, xc: {}, to: {}",
                    nodeId, cfgXc.getDescription(), targetGainRange == null ? null : targetGainRange.getSimpleName());
            return targetGainRange;
        }

        CrossConnections opXc = getOpXc(opNode, cfgXc);
        Amplifier opAmplifier = opXc.getAmplifier();
        Class<? extends GAINRANGE> currentGainRange = opAmplifier.getGainRange();

        if (currentGainRange == null || targetGainRange == null || currentGainRange.equals(targetGainRange)) {
            return targetGainRange;
        }

        BigDecimal opTargetGain = opAmplifier.getTargetGain();

        // Device validates gain-range with the current target-gain when only gain-range is merged.
        // Do not switch the range until OP confirms the current target-gain is already safe for it.
        if (!isTargetGainSafeForRange(opTargetGain, targetGainRange, gainRangeBoundaryThreshold)) {
            log.warn("skip amplifier gainRange switch because OP targetGain is out of target range, nodeId: {}, xc: {}, opTargetGain: {}, from: {}, to: {}",
                    nodeId, cfgXc.getDescription(), opTargetGain, currentGainRange.getSimpleName(), targetGainRange.getSimpleName());
            return currentGainRange;
        }

        return targetGainRange;
    }

    private boolean isTargetGainSafeForRange(BigDecimal targetGain, Class<? extends GAINRANGE> gainRange,
                                             double gainRangeBoundaryThreshold) {
        if (HIGHGAINRANGE.class.equals(gainRange)) {
            if (gainRangeBoundaryThreshold <= 0) {
                // Without a boundary from optical-tool we cannot prove that the NE
                // accepts a high range switch with its current target-gain.
                return false;
            }
            BigDecimal threshold = BigDecimal.valueOf(gainRangeBoundaryThreshold);
            return targetGain.compareTo(threshold) >= 0;
        }
        if (LOWGAINRANGE.class.equals(gainRange)) {
            if (gainRangeBoundaryThreshold <= 0) {
                // Without a boundary from optical-tool we cannot prove that the NE
                // accepts a low range switch with its current target-gain.
                return false;
            }
            BigDecimal threshold = BigDecimal.valueOf(gainRangeBoundaryThreshold);
            return targetGain.compareTo(threshold) < 0;
        }
        return true;
    }

    private Node getOpNode(String nodeId) {
        return phyNodeDao.getOpPhyNodeById(nodeId);
    }

    private CrossConnections getOpXc(Node opNode, CrossConnections cfgXc) {
        String xcId = cfgXc.getCrossConnectionId().getValue();
        return opNode.getAugmentation(Node1.class).getPhysical().getCrossConnections().stream()
                .filter(xc -> xcId.equals(xc.getCrossConnectionId().getValue()))
                .findAny()
                .orElseThrow(() -> new IllegalStateException(
                        "cannot find OP amplifier XC " + xcId + " on node " + opNode.getNodeId().getValue()));
    }


    private void updateNodeVoa(String nodeAId, double voaA, boolean isAz) {
        Node node = changedObject.getChangedPhyNode(nodeAId);
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        List<CrossConnections> newXcList = nodeAttr.getCrossConnections()
                .stream().map(xc -> {
                    if (xc.getAmplifier() != null) {
                        if (isAz && !xc.getDescription().contains("EAST") && !xc.getDescription().contains("BA")) {
                            return xc;
                        }
                        if (!isAz && !xc.getDescription().contains("WEST") && !xc.getDescription().contains("BA")) {
                            return xc;
                        }
                        Properties prop = xc.getAmplifier().getProperties();
                        if (!shouldUpdateVoa(prop, voaA)) {
                            log.info("skip VOA update: direction={}, xc={}, currentVoa={}, calculatedVoa={}, deltaThreshold={}dB",
                                    isAz ? "A->Z" : "Z->A", xc.getDescription(), PropertyTool.getValue(prop, EGRESS_VOA), voaA, VOA_UPDATE_DELTA);
                            return xc;
                        }
                        log.info("apply VOA: direction={}, xc={}, currentVoa={}, newVoa={}",
                                isAz ? "A->Z" : "Z->A", xc.getDescription(), PropertyTool.getValue(prop, EGRESS_VOA), voaA);
                        prop = PropertyTool.addProperty(prop, EGRESS_VOA, String.format("%.1f", voaA));
                        return new CrossConnectionsBuilder(xc)
                                .setAmplifier(new AmplifierBuilder(xc.getAmplifier())
                                        .setProperties(prop)
                                        .build())
                                .build();
                    } else {
                        return xc;
                    }
                }).collect(Collectors.toList());

        Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                        .setPhysical(new PhysicalBuilder(nodeAttr)
                                .setCrossConnections(newXcList)
                                .build())
                        .build())
                .build();

        changedObject.addChangedPhyNode(newNode);
    }

    private boolean shouldUpdateVoa(Properties prop, double calculatedVoa) {
        if (prop == null || prop.getProperty() == null) {
            return true;
        }

        String currentValue = PropertyTool.getValue(prop, EGRESS_VOA);
        if (currentValue == null || currentValue.trim().isEmpty()) {
            return true;
        }

        try {
            double currentVoa = Double.parseDouble(currentValue);
            return Math.abs(currentVoa - calculatedVoa) >= VOA_UPDATE_DELTA;
        } catch (NumberFormatException e) {
            log.warn("invalid current VOA value {}, will update with calculated value {}", currentValue, calculatedVoa);
            return true;
        }
    }

    private String getNodeInfo(String nodeId) {
        Node node = changedObject.getChangedPhyNode(nodeId);
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

        return String.format("%s(%s)", nodeAttr.getFriendlyName(), nodeAttr.getIp());
    }
}
