package net.flex.dci.otn.controller.implement.common;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.ChannelTargetPowerCalculator;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.common.util.frequency.Constant;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalXcIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.implement.common.config.ImplConfig;
import net.flex.dci.otn.controller.implement.common.utils.CommonUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.NodeType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1Builder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannelBuilder;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
public class OchXcProtoType {
    private final String Dst2SrcTargetPower = "target-source-port-output-optical-power";
    private final String Src2DstTargetPower = "target-dest-port-output-optical-power";

    private final ImplConfig implConfig = SpringBeanFinder.getBean(ImplConfig.class);
    private final PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDao.class);

    private Map<String, CrossConnections> protoMap;

    public OchXcProtoType() {
        protoMap = new HashMap<>();
    }

    /**
     * 把这些交叉缓存起来， 后面restore到新的xc
     *
     * @param xcIds
     */
    public void backup(ChangedObject changedObject, List<String> xcIds) {
        log.debug("backup XC attribute {}", xcIds);

        //should backup all wss XC
        xcIds.forEach(xcId-> {
            String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
            Node node = phyNodeDao.getOpPhyNodeById(nodeId);
            if (node == null) {
                Node cfgNode = changedObject.getChangedPhyNode(nodeId);
                Physical nodeAttr = cfgNode.getAugmentation(Node1.class).getPhysical();

                if (implConfig.isWriteWithoutIP()) {
                    return;
                }
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "the node hasn't been managed! " + nodeAttr.getIp());
            }

            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            CrossConnections xc = nodeAttr.getCrossConnections().stream()
                    .filter(x -> x.getCrossConnectionId().getValue().equals(xcId))
                    .findAny()
                    .orElse(null);

            if (xc == null) {
                log.error("cannot find XC with xcId:" + xcId);
                return;
            }
            if (xc.getWssChannel() != null) {
                if (!isValidProtoXc(xc)) {
                    xc = findOnePossible(xc, node);
                }
                log.debug("insert protoXC {}({}), \n properties{} ", xc.getDescription(),
                        xc.getCrossConnectionId().getValue(),
                        xc.getWssChannel().getProperties());

                protoMap.put(nodeId, xc);
            }
        });
    }

    private boolean getBusinessXcId(String xcId) {
        Pattern pattern = Pattern.compile("XC-[^,]*MUX[^,]*");
        Matcher matcher = pattern.matcher(xcId);
        return matcher.matches();
    }

    /**
     *
     * @param changedObject
     * @param xcIdList  //will update xc attribute for these xcIds
     * @param isAse
     */
    public void restore(ChangedObject changedObject, List<String> xcIdList, boolean isAse) {
        xcIdList.forEach(xcId -> {
            String nodeId = PhysicalXcIdNamingRule.getNodeId(xcId);
            CrossConnections protoXc = protoMap.get(nodeId);
            Node node = changedObject.getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            CrossConnections orgXc = nodeAttr.getCrossConnections().stream()
                    .filter(x -> x.getCrossConnectionId().getValue().equals(xcId))
                    .findAny()
                    .orElse(null);
            if (orgXc == null) {
                log.error("!!cannot find XC in changedObject with xcId {}", xcId);
                return;
            }

            if (protoXc == null) {
                if (nodeAttr.getNodeType().equals(NodeType.OD) && orgXc.getWssChannel() != null) {
                    // No device prototype is available; keep the normal copyAttr path by generating
                    // target powers from the channel calculation formula for this WSS XC.
                    log.error("Has NOT found protoXC for xcId {}, generate based on calculation", xcId);
                    protoXc = generateBasedOnCalcuation(orgXc, node);
                } else {
                    log.error("This is not a OD WSS XC, cannot generate protoXC for xcId {}", xcId);
                    return;
                }
            }

            if (orgXc.getWssChannel() != null) {
                log.debug("restrore XC attr on {} xc {}", nodeAttr.getIp(), xcId);

                boolean onDge = CommonUtils.onDGE(nodeAttr, orgXc);  //ILA 没有WSS XC
                CrossConnections newXc = copyAttr(protoXc, orgXc, isAse, onDge);
                if (shouldIgnoreAccelinkProtoPower(nodeAttr, newXc)) {
                    // 通过proto计算出来的值可能超过设备范围，那么采用算法计算结果.
                    log.warn("regenerate protoXC {} for target XC {} because restored target power pair is lower than -3, dest {}, source {}",
                            protoXc.getCrossConnectionId().getValue(), xcId,
                            PropertyTool.getValue(newXc.getWssChannel().getProperties(), Src2DstTargetPower),
                            PropertyTool.getValue(newXc.getWssChannel().getProperties(), Dst2SrcTargetPower));
                    protoXc = generateBasedOnCalcuation(orgXc, node);
                    newXc = copyAttr(protoXc, orgXc, isAse, onDge);
                }
                log.debug("Prototype properties are: {}", protoXc.getWssChannel().getProperties());
                log.debug("Copied properties are: {}", newXc.getWssChannel().getProperties());

                log.debug("protoXC  {}({}), \n" +
                                "\toranginal power of src2dst {}, dst2src {}\n" +
                                "\tnew power of src2dst {}, dst2src {}",
                        protoXc.getDescription(), protoXc.getCrossConnectionId().getValue(),
                        PropertyTool.getValue(protoXc.getWssChannel().getProperties(), Src2DstTargetPower),
                        PropertyTool.getValue(protoXc.getWssChannel().getProperties(), Dst2SrcTargetPower),
                        PropertyTool.getValue(newXc.getWssChannel().getProperties(), Src2DstTargetPower),
                        PropertyTool.getValue(newXc.getWssChannel().getProperties(), Dst2SrcTargetPower)
                );

                final CrossConnections restoredXc = newXc;
                List<CrossConnections> newXcList = nodeAttr.getCrossConnections().stream()
                        .map(xc -> {
                            if (xc.getCrossConnectionId().getValue().equals(xcId)) {
                                return restoredXc;
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
        });
    }

    /**
     * copy xc attribute from proto to org, and return a new XC
     *
     * @param protoXc
     * @param orgXc
     * @param isAse
     * @param onDge
     * @return
     */
    private CrossConnections copyAttr(CrossConnections protoXc, CrossConnections orgXc,
                                      boolean isAse, boolean onDge) {
        // s2DTargetPower, d2STargetPower
        Pair<String, String> newPower = getPower(protoXc, orgXc);

        Properties protoProp = protoXc.getWssChannel().getProperties();
        Properties newProp = orgXc.getWssChannel().getProperties();

        newProp = PropertyTool.addProperty(newProp, "auto-control-range",
                PropertyTool.getValue(protoProp, "auto-control-range"));
        newProp = PropertyTool.addProperty(newProp, "auto-control-active-threshold-dest",
                PropertyTool.getValue(protoProp, "auto-control-active-threshold-dest"));
        newProp = PropertyTool.addProperty(newProp, "ase-injection-threshold",
                PropertyTool.getValue(protoProp, "ase-injection-threshold"));
        newProp = PropertyTool.addProperty(newProp, "auto-control-active-threshold-source",
                PropertyTool.getValue(protoProp, "auto-control-active-threshold-source"));
//        newProp = PropertyTool.addProperty(newProp, "auto-control-range", PropertyTool.getValue(protoProp, "auto-control-range"));
        newProp = PropertyTool.addProperty(newProp, "dest-to-source-power-control-mode",
                PropertyTool.getValue(protoProp, "dest-to-source-power-control-mode"));
        newProp = PropertyTool.addProperty(newProp, "source-to-dest-power-control-mode",
                PropertyTool.getValue(protoProp, "source-to-dest-power-control-mode"));

        if (newPower.getLeft() != null) {
            newProp = PropertyTool.addProperty(newProp, Src2DstTargetPower, newPower.getLeft());  //target-source
        }
        if (newPower.getRight() != null) {
            if (onDge) {
                newProp = PropertyTool.addProperty(newProp, Dst2SrcTargetPower, newPower.getRight());
            } else {
                newProp = PropertyTool.addProperty(newProp, Dst2SrcTargetPower, "0.0");
            }
        }
        if (isAse) {
            newProp = PropertyTool.addProperty(newProp, "dest-to-source-power-control-mode",
                    PropertyTool.getValue(protoProp, "dest-to-source-power-control-mode"));
            newProp = PropertyTool.addProperty(newProp, "source-to-dest-power-control-mode",
                    PropertyTool.getValue(protoProp, "source-to-dest-power-control-mode"));
            newProp = PropertyTool.addProperty(newProp, "auto-control-range", "15");
            newProp = PropertyTool.addProperty(newProp, "ase-control-mode", "ASE_DISABLED");
            newProp = PropertyTool.addProperty(newProp, "auto-control-active-threshold-dest",
                    "0.5");
            newProp = PropertyTool.addProperty(newProp, "auto-control-active-threshold-source",
                    "0.5");

            if (orgXc.getCrossConnectionId().getValue().contains("EXP33")) {
                newProp = PropertyTool.addProperty(newProp, "source-to-dest-power-control-mode", "APC");
            }
            if (onDge) {
                newProp = PropertyTool.addProperty(newProp, "dest-to-source-power-control-mode",
                        PropertyTool.getValue(protoProp, "MANUAL"));
                newProp = PropertyTool.addProperty(newProp, "source-to-dest-power-control-mode",
                        PropertyTool.getValue(protoProp, "MANUAL"));
                newProp = PropertyTool.addProperty(newProp, "ase-control-mode", "ASE_DISABLED");
            }

            return new CrossConnectionsBuilder(orgXc)
                    .setWssChannel(new WssChannelBuilder(orgXc.getWssChannel())
                            .setSourceToDestVoa(protoXc.getWssChannel().getSourceToDestVoa())
                            .setDestToSourceVoa(protoXc.getWssChannel().getDestToSourceVoa())
                            .setProperties(newProp)
                            .build())
                    .build();
        } else {
            newProp = PropertyTool.addProperty(newProp, "ase-control-mode", "ASE_ENABLED");
            newProp = PropertyTool.addProperty(newProp, "dest-to-source-power-control-mode",
                    "MANUAL");
            newProp = PropertyTool.addProperty(newProp, "source-to-dest-power-control-mode", "APC");
            newProp = PropertyTool.addProperty(newProp, "auto-control-range", "6");
            newProp = PropertyTool.addProperty(newProp, "auto-control-active-threshold-dest",
                    "0.5");
            newProp = PropertyTool.addProperty(newProp, "auto-control-active-threshold-source",
                    "0.5");

            if (onDge) {
                newProp = PropertyTool.addProperty(newProp, "dest-to-source-power-control-mode",
                        PropertyTool.getValue(protoProp, "MANUAL"));
                newProp = PropertyTool.addProperty(newProp, "source-to-dest-power-control-mode",
                        PropertyTool.getValue(protoProp, "MANUAL"));
                newProp = PropertyTool.addProperty(newProp, "ase-control-mode", "ASE_DISABLED");
                return new CrossConnectionsBuilder(orgXc)
                        .setWssChannel(new WssChannelBuilder(orgXc.getWssChannel())
                                .setSourceToDestVoa(new BigDecimal(3))
                                .setDestToSourceVoa(new BigDecimal(3))
                                .setProperties(newProp)
                                .build())
                        .build();
            } else {
                return new CrossConnectionsBuilder(orgXc)
                        .setWssChannel(new WssChannelBuilder(orgXc.getWssChannel())
                                .setSourceToDestVoa(new BigDecimal(3))
                                .setDestToSourceVoa(new BigDecimal(0))
                                .setProperties(newProp)
                                .build())
                        .build();
            }
        }
    }

    /**
     * 基于protoXc 中的source2Dest, dest2Source 的targetPower, 和原有带宽，
     * 配合现在orgXc的带宽完成计算
     * 公式如下
     * When replace 100 GHz bandwidth ASE (Target output power PTMP(ASE)) to X GHz bandwidth service media-channel,
     * the media-channel target power PTMPS = PTMP(ASE) + 10 * lg(X/100)
     *
     * @param protoXc
     * @param orgXc
     * @return <source2Dest, dest2Source>
     */
    private Pair<String, String> getPower(CrossConnections protoXc, CrossConnections orgXc) {
        Pair<String, String> result;
        long protoBandwidth = protoXc.getWssChannel().getUpperFrequency().getValue().longValue()
                - protoXc.getWssChannel().getLowerFrequency().getValue().longValue();
        long orgBandwidth = orgXc.getWssChannel().getUpperFrequency().getValue().longValue()
                - orgXc.getWssChannel().getLowerFrequency().getValue().longValue();


        Properties protoProp = protoXc.getWssChannel().getProperties();

        String protoSrc2DstPower = PropertyTool.getValue(protoProp, Src2DstTargetPower);
        String protoDst2SrcPower = PropertyTool.getValue(protoProp, Dst2SrcTargetPower);

        log.info("original power in prototype xc src2Dst: {}, dst2Src: {}", protoSrc2DstPower, protoDst2SrcPower);
        {
            double src = 0;
            double dst = 0;
            if (protoSrc2DstPower != null) {
                src = Double.parseDouble(protoSrc2DstPower);
            }
            if (protoDst2SrcPower != null) {
                dst = Double.parseDouble(protoDst2SrcPower);
            }
            if (Math.abs(src) < 1e-9 && Math.abs(dst) < 1e-9) {
                log.error("!! find a prototype xc power both 0 on {}", protoXc.getCrossConnectionId().getValue() );
            }
        }
        if (protoBandwidth == orgBandwidth) {
            result = new ImmutablePair<>(protoSrc2DstPower, protoDst2SrcPower);
        } else {
            String newSrt2DstPower = calculatePower(protoSrc2DstPower, protoBandwidth, orgBandwidth);
            String newDst2SrcPower = calculatePower(protoDst2SrcPower, protoBandwidth, orgBandwidth);

            result = new ImmutablePair<>(newSrt2DstPower, newDst2SrcPower);
        }

        log.debug("replace wss target power as {}", result);
        return result;
    }

    private CrossConnections findOnePossible(CrossConnections protoXc, Node node) {
        log.debug("because both 0, findOnePossible +-3 scope");

        try {
            log.debug("checking node {}", node.getNodeId().getValue());

            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            List<CrossConnections> sortedXcList = nodeAttr.getCrossConnections().stream()
                    .filter(xc -> xc.getWssChannel() != null)
                    .sorted(Comparator.comparingLong(
                            xc -> xc.getWssChannel().getLowerFrequency().getValue().longValue()
                    ))
                    .collect(Collectors.toList());

            int index = IntStream.range(0, sortedXcList.size())
                    .filter(i -> sortedXcList.get(i).getCrossConnectionId().getValue().equals(protoXc.getCrossConnectionId().getValue()))
                    .findFirst()
                    .orElse(-1);
            log.debug("find the protoXC, index is {}", index);

            if (isValidProtoXc(protoXc)) {
                return protoXc;
            }

            // 按距离 1, 2, 3 依次向两边扩散判断
            CrossConnections possibleXc = null;
            for (int delta = 1; delta <= 3; delta++) {

                // 先检查当前的 -delta 位置 (前向)
                int prevIdx = index - delta;
                if (prevIdx >= 0) {
                    CrossConnections xc = sortedXcList.get(prevIdx);
                    if (isValidProtoXc(xc)) {
                        possibleXc = generateFakeXc(protoXc, xc);
                        break;
                    }
                }

                // 再检查当前的 +delta 位置 (后向)
                int nextIdx = index + delta;
                if (nextIdx < sortedXcList.size()) {
                    CrossConnections xc = sortedXcList.get(nextIdx);
                    if (isValidProtoXc(xc)) {
                        possibleXc = generateFakeXc(protoXc, xc);
                        break;
                    }
                }
            }

            if (!isValidProtoXc(possibleXc)) {
                possibleXc = generateBasedOnCalcuation(protoXc, node);
                log.debug("based on +- 3 XC, still invalid, have to based on calculation");
            } else {
                log.debug("based on +- 3 XC, find one proto");
            }
            return possibleXc;
        } catch (Exception e) {
            log.error("find on error", e);
        }
        return protoXc;
    }

    private boolean isValidProtoXc(CrossConnections xc) {
        if (xc == null) return false;
        return !isZero(PropertyTool.getValue(xc.getWssChannel().getProperties(), Src2DstTargetPower)) &&
                !isZero(PropertyTool.getValue(xc.getWssChannel().getProperties(), Dst2SrcTargetPower));
    }

    private boolean shouldIgnoreAccelinkProtoPower(Physical nodeAttr, CrossConnections xc) {
        if (nodeAttr.getVendorName() == null || !nodeAttr.getVendorName().equalsIgnoreCase("accelink")
                || xc == null || xc.getWssChannel() == null || xc.getWssChannel().getProperties() == null) {
            return false;
        }
        // Accelink现场存在OP原型XC target power计算异常偏低；跳过缓存让restore复用原有公式重算。
        return isLowerThan(PropertyTool.getValue(xc.getWssChannel().getProperties(), Src2DstTargetPower), -3.0)
                || isLowerThan(PropertyTool.getValue(xc.getWssChannel().getProperties(), Dst2SrcTargetPower), -3.0);
    }

    private boolean isLowerThan(String value, double threshold) {
        try {
            return value != null && Double.parseDouble(value) < threshold;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private CrossConnections generateBasedOnCalcuation(CrossConnections protoXc, Node node) {
        log.debug("based on formula to calculate for xc {}", protoXc);

        ChannelTargetPowerCalculator calculator = new ChannelTargetPowerCalculator();
        Long upper = protoXc.getWssChannel().getUpperFrequency().getValue().longValue();
        Long lower = protoXc.getWssChannel().getLowerFrequency().getValue().longValue();

        Map<Long, Double> adjustValue;
        double targetOutputPower;
        double targetOutputTilt;
        int totalSlots;
        long bandLower;
        long bandUpper;

        if (lower >= Long.parseLong(Constant.MaxLowerFrequency.MUX64_BD_C) &&
                upper <= Long.parseLong(Constant.MaxUpperFrequency.MUX64_BD_C)) {
            targetOutputPower = 22.0;
            targetOutputTilt = -1.5;
            totalSlots = ChannelTargetPowerCalculator.C_BAND_TOTAL_SLOTS;
            bandLower = Long.parseLong(Constant.MaxLowerFrequency.MUX64_BD_C);
            bandUpper = Long.parseLong(Constant.MaxUpperFrequency.MUX64_BD_C);
        } else {
            targetOutputPower = 21.0;
            targetOutputTilt = -0.5;
            totalSlots = ChannelTargetPowerCalculator.L_BAND_TOTAL_SLOTS;
            bandLower = Long.parseLong(Constant.MaxLowerFrequency.MUX64_BD_L);
            bandUpper = Long.parseLong(Constant.MaxUpperFrequency.MUX64_BD_L);
        }

        adjustValue = calculator.calculateTargetPower(
                bandLower, bandUpper,
                100_000L, totalSlots,
                bandLower, true,
                targetOutputPower, targetOutputTilt);

        // 根据 lower 找到对应的 adjustValue 中的 entry
        Long index = adjustValue.keySet().stream().sorted().filter(x -> lower >= x && lower < x + 100_000L)
                .findAny().orElse(null);

        if (index != null) {
            double value = adjustValue.get(index);
            String calculatedValStr = String.format("%.1f", value);

            long targetBandwidth = protoXc.getWssChannel().getUpperFrequency().getValue().longValue() -
                                    protoXc.getWssChannel().getLowerFrequency().getValue().longValue();
            String valueStr = calculatePower(calculatedValStr, 100_000L, targetBandwidth);

            log.debug("calculated value is {}, and after adjust {}", calculatedValStr, valueStr);
            Properties newProp = PropertyTool.addProperty(protoXc.getWssChannel().getProperties(),
                    Src2DstTargetPower, valueStr);

            CrossConnections fakeXc;
            if (isDGE(protoXc, node)) {
                newProp = PropertyTool.addProperty(newProp, Dst2SrcTargetPower, valueStr);
            }
            fakeXc = new CrossConnectionsBuilder(protoXc)
                    .setWssChannel(new WssChannelBuilder(protoXc.getWssChannel())
                            .setLowerFrequency(protoXc.getWssChannel().getLowerFrequency())
                            .setUpperFrequency(protoXc.getWssChannel().getUpperFrequency())
                            .setProperties(newProp)
                            .build())
                    .build();

            return fakeXc;
        }
        log.error("cannot find any matched based on calculation formula");
        return protoXc;
    }

    private boolean isDGE(CrossConnections protoXc, Node node) {
        if (protoXc.getCrossConnectionId().getValue().contains("WEST")) {
            String xcSTP = protoXc.getSourceTp().get(0).getTpRef().getValue();
            String eqId = PhysicalTpIdNamingRule.getEquipId(xcSTP);

            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
            Equipments eq = nodeAttr.getEquipments().stream()
                    .filter(x -> x.getEquipmentId().equals(eqId))
                    .findAny()
                    .orElseThrow(()-> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "cannot find the required EQ base on xcId " + protoXc.getCrossConnectionId().getValue()));

            if (eq.getEquipType().equals(EquipType.DGE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 有时候设备基于orgXC (protoType XC)的取值是0， 所以不得不找一个附近的作为新样本
     * @param protoXc
     * @param xc
     * @return
     */
    private CrossConnections generateFakeXc(CrossConnections protoXc, CrossConnections xc) {
        Properties newProp = PropertyTool.addProperty(protoXc.getWssChannel().getProperties(),
                Src2DstTargetPower,
                PropertyTool.getValue(xc.getWssChannel().getProperties(), Src2DstTargetPower));
        newProp = PropertyTool.addProperty(newProp,
                Dst2SrcTargetPower,
                PropertyTool.getValue(xc.getWssChannel().getProperties(), Dst2SrcTargetPower));

        CrossConnections fakeXc = new CrossConnectionsBuilder(protoXc)
                .setWssChannel(new WssChannelBuilder(protoXc.getWssChannel())
                        .setLowerFrequency(protoXc.getWssChannel().getLowerFrequency())
                        .setUpperFrequency(protoXc.getWssChannel().getUpperFrequency())
                        .setProperties(newProp)
                        .build())
                .build();
        return fakeXc;
    }

    private boolean isZero(String str) {
        if (str == null) {
            str = "0.0";
        }
        try {
            double value = Double.parseDouble(str);

            if (Math.abs(value) < 1e-9) {
                return true;
            }
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    // currentPower: PTMP(ASE), in dBm
    // currentBandwidth, currentPower 对应的带宽 MHz
    // targetBandwidth: X, in MHz
    private String calculatePower(String currentPower, long currentBandwidth, long targetBandwidth) {
        if (targetBandwidth <= 0) {
            log.error("Bandwidth must be positive when recalculate target power");
            return "0.0";
        }
        if (currentPower == null) {
            log.error("current power reading from prototype must be NOT null");
            return "0.0";
        }

        double douCurrentPower = Double.parseDouble(currentPower);
        if (Math.abs(douCurrentPower) < 1e-9) {
            return "0.0";
        }
        
        //100GHz 是个中间值，
        // target = 150GHz的时候带宽扩展，power上升，再次变成100GHz的时候需要原路减回来
        // target =  50GHz的时候带宽变小，power下降，再次变成100GHz的时候需要原路加回来
        double newPower = douCurrentPower +
                10.0 * Math.log10((double)targetBandwidth / currentBandwidth);

        log.debug("currentBandwidh {}, targetBandwidth {}, and currentPower {}, newPower {}",
                currentBandwidth, targetBandwidth, currentPower, newPower);
        return String.format("%.2f", newPower);
    }

    private boolean onDGE(Physical nodeAttr, CrossConnections dbXc) {
        String sTpId = dbXc.getSourceTp().get(0).getTpRef().getValue();
        String eqId = PhysicalTpIdNamingRule.getEquipId(sTpId);
        Equipments eq = nodeAttr.getEquipments().stream()
                .filter(x -> x.getEquipmentId().equals(eqId)).findAny()
                .orElseThrow(() -> new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                        "cannot find out eqId " + eqId));

        return eq.getEquipType().equals(EquipType.DGE);
    }

}
