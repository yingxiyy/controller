/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.ne;

import com.google.common.collect.ImmutableList;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.model.JsonYangConverter;
import net.flex.dci.otn.controller.allocate.designer.namingrule.NEIdGenerator;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import net.flex.dci.otn.controller.allocate.ne.Param;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.AdminStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.LinkDirection;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.OperStatus;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.PropertiesBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.PropertyBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.ApsPath;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.CrossConnectionAttributes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.VoaUpdateModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.amplifier.attributes.Amplifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.amplifier.attributes.AmplifierBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.Aps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.Aps.ApsMode;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.aps.attributes.ApsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.DestinationTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connection.attributes.SourceTp;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannelBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class XCRepo {

    public static final String PROTECTED = "protected";
    public static final String SHELF = "1";
    public static final String JSON_SLOT = "<slot>";
    public static final String JSON_FROM = "<from>";
    public static final String JSON_TO = "<to>";
    public static final String JSON_CENTRAL_FREQUENCY = "<central-frequency>";
    public static final String APS_MODE_ABSULOTE = "ABSOLUTE";
    public static String AMPLIFIER = "AMPLIFIER";
    public static ImmutableList<String> APS_XC_TYPES = ImmutableList.of("APS");


    @Autowired
    private TpRepo tpRepo;
    @Autowired
    private JsonYangConverter jsonYangConverter;

    /**
     * TPC的XC ID包含时隙信息， 例如： "XC-Site-1637822513176#Ne-1637823014069#LINECARD-1-1#PORT-1-1-C1/odu4=1-Site-1637822513176#Ne-1637823014069#LINECARD-1-1#PORT-1-1-L1/odu4=1"
     *
     * @param nodeId
     * @param crossConnection
     * @param sTPs
     * @param dTPs
     * @return
     */
    public CrossConnections createOTXC(String nodeId, CrossConnection crossConnection, List<SourceTp> sTPs, List<DestinationTp> dTPs, String description) throws NeDesignerException {

        List<String> tpIdList = Stream.concat(sTPs.stream().map(stp -> stp.getTpRef().getValue().concat(stp.getSlot())), dTPs.stream()
                        .map(stp -> stp.getTpRef().getValue().concat(stp.getSlot())))
                .collect(Collectors.toList());
        return createXC(tpIdList, nodeId, crossConnection, sTPs, dTPs, false, description);
    }


    public CrossConnections createXC(String nodeId, CrossConnection crossConnection, List<SourceTp> sTPs, List<DestinationTp> dTPs, Boolean isProtected) throws NeDesignerException {
        List<String> tpIdList = getTpIdList(sTPs, dTPs);
        return createXC(tpIdList, nodeId, crossConnection, sTPs, dTPs, isProtected, null);

    }

    public CrossConnections createXC(String nodeId, CrossConnection crossConnection, List<SourceTp> sTPs, List<DestinationTp> dTPs, Boolean isProtected, String description)
            throws NeDesignerException {
        List<String> tpIdList = getTpIdList(sTPs, dTPs);
        return createXC(tpIdList, nodeId, crossConnection, sTPs, dTPs, isProtected, description);

    }

    private List<String> getTpIdList(List<SourceTp> sTPs, List<DestinationTp> dTPs) {
        return Stream.concat(sTPs.stream().map(stp -> stp.getTpRef().getValue()), dTPs.stream().map(stp -> stp.getTpRef().getValue()))
                .collect(Collectors.toList());
    }

    public CrossConnections createXC(List<String> tpIdList, String nodeId, CrossConnection crossConnection, List<SourceTp> sTPs, List<DestinationTp> dTPs, Boolean isProtected, String description)
            throws NeDesignerException {

        Amplifier amplifier = getAmplifier(crossConnection);
        String slot = NEIdGenerator.getSlotFromTp(sTPs.get(0).getTpRef());
        description = description == null ? getXCDescription(crossConnection.getDescription(), slot) : description;
        Aps aps = getAps(crossConnection.getType(), isProtected, slot, description, crossConnection.getAps());

        String xcId;
        // Raman spans physical SIG/LINE ports; unlike EDFA, it has no C/L layer suffix.
        if (amplifier != null && !"RAMAN".equals(crossConnection.getType())) {
            xcId = NEIdGenerator.createXCIdWithLayer(tpIdList, crossConnection);
        } else {
            xcId = NEIdGenerator.createXCId(tpIdList, crossConnection.getBiDirection());
        }

        WssChannel wssChannel = null;
        net.flex.dci.otn.controller.allocate.ne.WssChannel wssChannelDefinition = crossConnection.getWssChannel();
        if (wssChannelDefinition != null) {
            WssChannelBuilder wssChannelBuilder = new WssChannelBuilder()
                    .setDestToSourceVoa(new BigDecimal(wssChannelDefinition.getDestToSourceVoa())).
                    setSourceToDestVoa(new BigDecimal(wssChannelDefinition.getSourceToDestVoa())).
                    setVoaUpdateModel(VoaUpdateModel.valueOf(wssChannelDefinition.getVoaUpdateModel().value()));
            if (wssChannelDefinition.getLowerFrequency() != null) {
                // e.g.: "slot": "/frequency=191400000,191475000"
                String frequencyString = sTPs.get(0).getSlot();
                String lowerFrequency = frequencyString.substring(frequencyString.indexOf("=") + 1, frequencyString.indexOf(","));
                String upperFrequency = frequencyString.substring(frequencyString.indexOf(",") + 1);
                wssChannelBuilder.setLowerFrequency(new FrequencyType(new BigInteger(lowerFrequency)));
                wssChannelBuilder.setUpperFrequency(new FrequencyType(new BigInteger(upperFrequency)));
            }
            List<Param> wssChannelDefinitionParams = wssChannelDefinition.getParams();
            if (wssChannelDefinitionParams != null && !wssChannelDefinitionParams.isEmpty()) {
                PropertiesBuilder propertiesBuilder = new PropertiesBuilder();
                List<Property> properties = new ArrayList<>();
                for (Param param : wssChannelDefinitionParams) {
                    properties.add(new PropertyBuilder()
                            .setName(param.getName())
                            .setValue(param.getValue()).build());

                }
                propertiesBuilder.setProperty(properties);
                wssChannelBuilder.setProperties(propertiesBuilder.build());

            }
            wssChannel = wssChannelBuilder.build();
        }
        CrossConnections xc = new CrossConnectionsBuilder()
                .setAdminState(AdminStatus.Unknown)
                .setImplementState(ImplementState.Allocate)
                .setOperationalState(OperStatus.Unknown)
                .setCrossConnectionId(new Uri(xcId))
//                .setDescription(getXCDescription(nodeId, crossConnection.getType(), slot, aps))
                .setDescription(description)
                .setDestinationTp(dTPs)
                .setDirection(
                        crossConnection.getBiDirection() ? LinkDirection.Bidirection
                                : LinkDirection.Unidirection)
                .setFixed(crossConnection.getIsFixed())
                .setKey(new CrossConnectionsKey(new Uri(xcId)))
                .setNodeRef(new NodeId(nodeId))
                .setSourceTp(sTPs)
                .setAmplifier(amplifier)
                .setAps(aps)
                .setWssChannel(wssChannel)
                .build();

        return xc;

    }


    private Aps getAps(String xcType, Boolean isProtected, String slot, String description, net.flex.dci.otn.controller.allocate.ne.Aps aps) {

        if (!APS_XC_TYPES.contains(xcType)) {
            return null;
        }

        return isProtected ? createProtectedAps(xcType, slot, description, aps) : createNonProtectedAps(xcType, slot, description);
    }

    public CrossConnections updateApsMemberEnable(CrossConnections xc, boolean cEnable) {
        if (xc == null || xc.getAps() == null) {
            return xc;
        }

        return new CrossConnectionsBuilder(xc)
                .setAps(updateAps(xc, cEnable))
                .build();
    }

    public org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections updateApsMemberEnable(
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections xc,
            boolean cEnable) {
        if (xc == null || xc.getAps() == null) {
            return xc;
        }

        return new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnectionsBuilder(xc)
                .setAps(updateAps(xc, cEnable))
                .build();
    }

    private Aps updateAps(CrossConnectionAttributes xc, boolean cEnable) {

        Properties properties = xc.getAps().getProperties();
        List<TpId> memberTps = new ArrayList<>();
        if (xc.getSourceTp() != null) {
            xc.getSourceTp().forEach(tp -> memberTps.add(tp.getTpRef()));
        }
        if (xc.getDestinationTp() != null) {
            xc.getDestinationTp().forEach(tp -> memberTps.add(tp.getTpRef()));
        }

        // APS member switches use the device-model ".enabled" suffix;
        for (TpId tpId : memberTps) {
            String member = getApsMember(tpId);
            if (member == null || member.endsWith("SIG")) {
                continue;
            }
            if (member.endsWith("A") || member.endsWith("B")) {
                properties = PropertyTool.addProperty(properties, member + ".enabled", "true");
            } else if (member.endsWith("C")) {
                properties = PropertyTool.addProperty(properties, member + ".enabled",
                        cEnable ? "true" : "false");
            }
        }

        return new ApsBuilder(xc.getAps()).setProperties(properties).build();
    }

    private String getApsMember(TpId tpId) {
        if (tpId == null || tpId.getValue() == null) {
            return null;
        }
        String value = tpId.getValue();
        return value.substring(value.lastIndexOf('-') + 1);
    }


    public List<String> getTpIdListWithCentralFrequency(List<SourceTp> sTPs, List<DestinationTp> dTPs, BigInteger cenFrequency) {

        String centFreq = String.format("/%d", cenFrequency.longValue());

        return Stream.concat(sTPs.stream().map(stp -> stp.getTpRef().getValue().concat(centFreq)), dTPs.stream()
                        .map(stp -> stp.getTpRef().getValue().concat(centFreq)))
                .collect(Collectors.toList());
    }

    public Amplifier getAmplifier(CrossConnection xcInfo) throws NeDesignerException {
        if (xcInfo.getDescription().toUpperCase().startsWith(AMPLIFIER) || xcInfo.getAmplifier() != null) {
           /* if (this.neInfo.getVendorName().equals("II-VI")) {
                gainRange = LOWGAINRANGE.class;
            } else {
                gainRange = FIXEDGAINRANGE.class;
            }*/
            AmplifierBuilder amplifierBuilder = new AmplifierBuilder();
//                    .setAmpMode(AmpMode.CONSTANTGAIN);
//                    .setTargetAttenuation(crossConn.getAttenuation())
//                    .setTargetGain(crossConn.getGain())

//                    .setTargetGainTilt(new BigDecimal("0"))
//                    .setGainRange(gainRange)
//                    .setEnable(true)
//                    .setAutoPowerReduction(null);
            if (xcInfo.getAmplifier() != null) {
                net.flex.dci.otn.controller.allocate.ne.Amplifier xcAmplifierInfo = xcInfo.getAmplifier();
                if ("RAMAN".equals(xcInfo.getType())) {
                    validateRamanGain(xcAmplifierInfo);
                }
                if (xcAmplifierInfo.getTargetAttenuation() != null) {
                    amplifierBuilder.setTargetAttenuation(new BigDecimal(xcAmplifierInfo.getTargetAttenuation()));
                }
                if (xcAmplifierInfo.getTargetGainTilt() != null) {
                    amplifierBuilder.setTargetGainTilt(new BigDecimal(xcAmplifierInfo.getTargetGainTilt()));
                }
                if (xcAmplifierInfo.getVoaRange() != null) {
                    amplifierBuilder.setVoaRange(new BigDecimal(xcAmplifierInfo.getVoaRange()));
                }
                if (xcAmplifierInfo.getAutoPowerReduction() != null) {
                    amplifierBuilder.setAutoPowerReduction(xcAmplifierInfo.getAutoPowerReduction());
                }
                if (xcAmplifierInfo.getEnable() != null) {
                    amplifierBuilder.setEnable(xcAmplifierInfo.getEnable());
                }
                if (xcAmplifierInfo.getTargetGain() != null) {
                    amplifierBuilder.setTargetGain(new BigDecimal(xcAmplifierInfo.getTargetGain()));
                }
                if (xcAmplifierInfo.getGainRange() != null) {
                    amplifierBuilder.setGainRange(jsonYangConverter.getGainRange(xcAmplifierInfo.getGainRange()));
                }
                if (xcAmplifierInfo.getAmpMode() != null) {
                    amplifierBuilder.setAmpMode(jsonYangConverter.getAmpMode(xcAmplifierInfo.getAmpMode()));
                }
                if (xcAmplifierInfo.getParams() != null) {
                    amplifierBuilder.setProperties(jsonYangConverter.getProperties(xcAmplifierInfo.getParams()));
                }
            }

            return amplifierBuilder.build();
        }
        return null;

    }


    private void validateRamanGain(net.flex.dci.otn.controller.allocate.ne.Amplifier amplifier)
            throws NeDesignerException {
        BigDecimal lower = null;
        BigDecimal upper = null;
        try {
            if (amplifier.getParams() != null) {
                for (Param param : amplifier.getParams()) {
                    if ("target-gain-range-lower".equals(param.getName())) {
                        lower = new BigDecimal(param.getValue());
                    } else if ("target-gain-range-higher".equals(param.getName())) {
                        upper = new BigDecimal(param.getValue());
                    }
                }
            }
            BigDecimal gain = amplifier.getTargetGain() == null ? null
                    : BigDecimal.valueOf(amplifier.getTargetGain());
            if (gain == null || lower == null || upper == null
                    || gain.compareTo(lower) < 0 || gain.compareTo(upper) > 0) {
                throw new NeDesignerException("Raman target gain must be within model range ["
                        + lower + ", " + upper + "], actual: " + gain);
            }
        } catch (NumberFormatException e) {
            throw new NeDesignerException("Invalid Raman gain or model gain range", e);
        }
    }

    private Aps createProtectedAps(String type, String slot, String description, net.flex.dci.otn.controller.allocate.ne.Aps aps) {
        ApsBuilder apsBuilder = new ApsBuilder();
        if (aps != null) {
            if (aps.getApsMode() != null) {
                if (aps.getApsMode().equals(APS_MODE_ABSULOTE)) {
                    apsBuilder.setApsMode(ApsMode.ABSOLUTE);
                } else {
                    apsBuilder.setApsMode(ApsMode.RELATIVE);
                }
            }
            if (aps.getHoldOffTime() != null) {
                apsBuilder.setHoldOffTime(aps.getHoldOffTime().longValue());
            }
            if (aps.getRevertive() != null) {
                apsBuilder.setRevertive(aps.getRevertive());
            }
            if (aps.getWaitToRestoreTime() != null) {
                apsBuilder.setWaitToRestoreTime(aps.getWaitToRestoreTime().longValue());
            }
            if (aps.getParams() != null) {
                apsBuilder.setProperties(jsonYangConverter.getProperties(aps.getParams()));
            }
            return apsBuilder.build();
        }

        //legacy for non-byte
        if (APS_XC_TYPES.contains(type)) {
            String apsName = description;
            List<Property> apsPros = new ArrayList<Property>();
            apsPros.add(new PropertyBuilder().setName("protected").setValue("true")
                    .build());

            apsPros.add(new PropertyBuilder().setName("primary-switch-threshold").setValue("-25.0")
                    .build());
            apsPros.add(new PropertyBuilder().setName("primary-switch-hysteresis").setValue("1.0")
                    .build());
            apsPros.add(
                    new PropertyBuilder().setName("secondary-switch-threshold").setValue("-25.0")
                            .build());
            apsBuilder.setApsMode(ApsMode.ABSOLUTE)
                    .setForceToPort(ApsPath.NONE)
                    .setHoldOffTime(Long.valueOf(0))
                    .setName(apsName)
                    .setRevertive(false)
                    .setProperties(new PropertiesBuilder().setProperty(apsPros).build());
        }
        return apsBuilder.build();

    }

    public static String getApsName(String slot) {
        return String.format("APS-1-%S-1", slot);
    }

    private Aps createNonProtectedAps(String type, String slot, String description) {
        ApsBuilder apsBuilder = new ApsBuilder();

        if (APS_XC_TYPES.contains(type)) {
            String apsName = description;
            List<Property> apsPros = new ArrayList<Property>();
            apsPros.add(new PropertyBuilder().setName("protected").setValue("false")
                    .build());

            apsPros.add(new PropertyBuilder().setName("primary-switch-threshold").setValue("-25.0")
                    .build());
            apsPros.add(new PropertyBuilder().setName("primary-switch-hysteresis").setValue("1.0")
                    .build());
            apsPros.add(
                    new PropertyBuilder().setName("secondary-switch-threshold").setValue("-25.0")
                            .build());
            apsBuilder.setApsMode(ApsMode.ABSOLUTE)
                    .setForceToPort(ApsPath.PRIMARY)
                    .setHoldOffTime(Long.valueOf(0))
                    .setName(apsName)
                    .setRevertive(false)
                    .setProperties(new PropertiesBuilder().setProperty(apsPros).build());
        }
        return apsBuilder.build();
    }

    private String getXCDescription(String descriptionDefinition, String slot) {
        return descriptionDefinition.replace(JSON_SLOT, slot);
    }

    public CrossConnections updateSlaveXc(CrossConnections xc) {
        Aps oldAps = xc.getAps();
        Aps newAps = getSlaveAps(oldAps);
        return new CrossConnectionsBuilder(xc).setAps(newAps).build();
    }

    private Aps getSlaveAps(Aps oldAps) {
        ApsBuilder apsBuilder = new ApsBuilder(oldAps);
        List<Property> apsPros = oldAps.getProperties().getProperty();
        int size = apsPros.size();
        for (int i = 0; i < size; i++) {
            Property property = apsPros.get(i);
            if (property.getName().equals(PROTECTED)) {
                Property newProperty = new PropertyBuilder().setName("protected").setValue("true")
                        .build();
                apsPros.set(i, newProperty);
                break;
            }
        }
        return apsBuilder
                .setForceToPort(ApsPath.NONE)
                .setProperties(new PropertiesBuilder().setProperty(apsPros).build())
                .build();


    }


    /**
     * If no central frequency definition, return null.
     *
     * @param crossConnection
     * @return
     */
    public ArrayList<String> getCenFrequencyList(CrossConnection crossConnection) {
        String centralFrequencyDefinition = crossConnection.getCentralFrequency();
        if (centralFrequencyDefinition == null) {
            return null;
        }
        ArrayList<String> result = new ArrayList<>();
        String[] centralFrequencyConfig = centralFrequencyDefinition.split(",");
        long start = Long.parseLong(centralFrequencyConfig[0]);
        long step = Long.parseLong(centralFrequencyConfig[2]);
        int length = crossConnection.getMultiple();

        for (int i = 1; i <= length; i++) {
            long centralFrequency = start - step * (i - 1);
            result.add(String.valueOf(centralFrequency));
        }
        return result;
    }


    public String getXCDescription(CrossConnection crossConnection, String tpId, String fromPort, String toPort, String cenFrequency) {
        String slot = PhysicalTpIdNamingRule.getSlotFromTp(tpId);
        String result = crossConnection.getDescription().replace(JSON_SLOT, slot);
        if (fromPort != null) {
            result = result.replace(JSON_FROM, fromPort);
        }
        if (toPort != null) {
            result = result.replace(JSON_TO, toPort);
        }
        if (cenFrequency != null) {
            result = result.replace(JSON_CENTRAL_FREQUENCY, cenFrequency);
        }
        return result;
    }
}
