/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model;

import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.ne.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.get.ot.card.capability.output.CardType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.get.ot.card.capability.output.CardTypeBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.get.ot.card.capability.output.card.type.ServiceType;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.get.ot.card.capability.output.card.type.ServiceTypeBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.get.ot.card.capability.output.card.type.service.type.ClientPortBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.ne.capability.rev211110.get.ot.card.capability.output.card.type.service.type.LinePortBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.*;
import org.opendaylight.yangtools.yang.binding.BaseIdentity;

import java.util.*;
import java.util.stream.Collectors;

import static net.flex.dci.otn.controller.allocate.ne.SupportedSignal.*;

//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.SERVICETYPE;

@Slf4j
public class NeInfoUtil {

    public static final String NAME_SEPERATOR = ",";

    private static final java.util.Map<String, String> GRID_MAP;
    public static final String COMMON_OTN_TYPES = "common-otn-types:";
    public static final String OTU_CLIENT = "OTU-Client";
    public static final String OTU_LINE = "OTU-Line";
    private static final BiMap<SupportedSignal, Class<? extends BaseIdentity>> supportedSignalTunnelSignalRateMapping;
    public static final Set<Class<? extends ETHERNETCOMPLIANCECODE>> FEC_DISABLE = ImmutableSet.of(
            ETH100GBASELR4.class,
            ETH100GBASEFR.class,
            ETH100GBASEDR.class
    );

    static {
        final com.google.common.collect.ImmutableMap.Builder<String, String> b = com.google.common.collect.ImmutableMap.builder();

        b.put("CMUX", "Flex-Grid");
        b.put("MUXPANEL", "Flex-Grid");
//        b.put("MUXPANEL_32C32L", "Flex-Grid");
        b.put("MUX96", "50GHz");
        b.put("MUX64", "75GHz");
        b.put("MUX_C64", "75GHz");
        b.put("MUX48", "100GHz");
        b.put("MUX32", "150GHz");
        b.put("MUX_32", "150GHz");

        GRID_MAP = b.build();

        //init SupportedSignal to yang model mapping
        supportedSignalTunnelSignalRateMapping = EnumHashBiMap.create(SupportedSignal.class);
        supportedSignalTunnelSignalRateMapping.put(_10_GB_E, Prot10GE.class);
        supportedSignalTunnelSignalRateMapping.put(_10_GE_WAN, Prot10GEWan.class);
        supportedSignalTunnelSignalRateMapping.put(_100_GB_E, Prot100GE.class);
        supportedSignalTunnelSignalRateMapping.put(_400_GB_E, Prot400GE.class);
        supportedSignalTunnelSignalRateMapping.put(_100_GE_FLEX_E, Prot100GEFlexE.class);
        supportedSignalTunnelSignalRateMapping.put(OTU_2, ProtOTU2.class);
        supportedSignalTunnelSignalRateMapping.put(OTU_4, ProtOTU4.class);
        supportedSignalTunnelSignalRateMapping.put(OTU_4_X_2, ProtOTUc2.class);
        supportedSignalTunnelSignalRateMapping.put(OTU_4_X_3, ProtOTUc3.class);
        supportedSignalTunnelSignalRateMapping.put(OTU_4_X_4, ProtOTUc4.class);
        supportedSignalTunnelSignalRateMapping.put(OTU_4_X_6, ProtOTUc6.class);
        supportedSignalTunnelSignalRateMapping.put(OTU_4_X_8, ProtOTUc8.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_100_GBASE_SR_10, ETH100GBASESR10.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_100_GBASE_SR_4, ETH100GBASESR4.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_100_GBASE_LR_4, ETH100GBASELR4.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_100_GBASE_ER_4_L, ETH100GBASEER4L.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_100_GBASE_ER_4, ETH100GBASEER4.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_100_GBASE_CWDM_4, ETH100GBASECWDM4.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_100_GBASE_CLR_4, ETH100GBASECLR4.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_100_GBASE_PSM_4, ETH100GBASEPSM4.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_100_GBASE_CR_4, ETH100GBASECR4.class);
//        supportedSignalTunnelSignalRateMapping.put(ETH_100_GBASE_FR, ETH100GBASEFR.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_100_GBASE_FR_1_2_KM, ETH100GBASEFR.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_100_GBASE_DR, ETH100GBASEDR.class);

        supportedSignalTunnelSignalRateMapping.put(ETH_200_GBASE_FR_4, ETH200GBASEFR4.class);

//        supportedSignalTunnelSignalRateMapping.put(ETH_100_GBASELR_4, ETH100GBASELR4.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_400_GBASE_ZR, ETH400GBASEZR.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_400_GBASE_LR_4, ETH400GBASELR4.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_400_GBASE_FR_4, ETH400GBASEFR4.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_400_GBASE_LR_8, ETH400GBASELR8.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_400_GBASE_DR_4, ETH400GBASEDR4.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_400_GMSA_PSM_4, ETH400GMSAPSM4.class);

        supportedSignalTunnelSignalRateMapping.put(STM_64, ProtStm64.class);
       /* supportedSignalTunnelSignalRateMapping.put(OMS, "OMS");
        supportedSignalTunnelSignalRateMapping.put(OTS, "OTS");
        supportedSignalTunnelSignalRateMapping.put(OCH, "OCH");*/
        supportedSignalTunnelSignalRateMapping.put(ETH_10_GB_ZR, ETH10GBASEZR.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_10_GB_SR, ETH10GBASESR.class);
        supportedSignalTunnelSignalRateMapping.put(ETH_10_GB_LR, ETH10GBASELR.class);
        supportedSignalTunnelSignalRateMapping.put(XI_64, STM64XI64.class);
        supportedSignalTunnelSignalRateMapping.put(XL_64, STM64XL64.class);
        supportedSignalTunnelSignalRateMapping.put(XS_64, STM64XS64.class);


    }


    public static List<String> getNameList(String name) throws NeDesignerException {
        if (name.contains("?")) {
            List<String> result = new ArrayList<>();
            try {
                String[] nameFormat = name.split(",");
                String nameTemplate = nameFormat[0];
                Integer nameStart = Integer.parseInt(nameFormat[1]);
                Integer nameEnd = Integer.parseInt(nameFormat[2]);
                Integer nameStep = Integer.parseInt(nameFormat[3]);
                String muxName;
                for (int i = nameStart; i <= nameEnd; i += nameStep) {
                    muxName = nameTemplate.replace("?", String.valueOf(i));
                    result.add(muxName);
                }
                return result;
            } catch (IndexOutOfBoundsException | NumberFormatException e) {
                throw new NeDesignerException(
                        "Failed to create port name, with the Port name:  " + name, e);
            }
        }
        return Arrays.asList(name.split(NAME_SEPERATOR));
    }

    public static List<String> getXcLayers(String layer) throws NeDesignerException {
        return getNameList(layer);
    }

    public static List<String> getLPortXcLayers(Card otCardInfo) throws NeDesignerException {
        String layer = otCardInfo.getCrossConnections().get(0).getTo().getLayer();

        return getNameList(layer);
    }


    public static List<String> getOtCardXcFromNamesByTo(Card otCardInfo, String lPortName) throws NeDesignerException {
        for (CrossConnection xc : otCardInfo.getCrossConnections()) {
            if (!xc.getTo().getPort().equals(lPortName)) {
                continue;
            }
            return getNameList(xc.getFrom().getPort());
        }
        throw new NeDesignerException(String.format("Failed to get xc from name by to name: %s, from card: %s, please check the json.", lPortName, otCardInfo.getCardType()));
    }


    /**
     * 1. 这个方法只是提供给OT card使用。
     * <p>
     * 2. 这个方法的前提是： OT card的交叉，里面的to，只有一个端口； 根据lineSignalRate和to的端口，就可以唯一确定一个交叉定义
     *
     * @param otCardInfo
     * @param lPortName
     * @param toSupportedSignal
     * @param fromSupportedSignal
     * @return
     * @throws NeDesignerException
     */
    public static CrossConnection getOtXcInfo(Card otCardInfo, String lPortName, SupportedSignal toSupportedSignal, SupportedSignal fromSupportedSignal, SERVICETYPE servicetype)
            throws NeDesignerException {

        for (CrossConnection crossConnection : otCardInfo.getCrossConnections()) {

            if (!isSameServiceType(crossConnection, servicetype)) {
                continue;
            }

            //交叉上面的SupportedSignal都是指L口的
            if (crossConnection.getSupportedSignal().contains(toSupportedSignal)) {

                //默认情况下，C口只支持一种SupportedSignal，这里就不配置。只有当同一张板卡的C口支持不同的速率，这里才需要配置。
                if (crossConnection.getFrom().getSupportedSignal() != null && !crossConnection.getFrom().getSupportedSignal().isEmpty()) {
                    if (!crossConnection.getFrom().getSupportedSignal().contains(fromSupportedSignal)) {
                        continue;
                    }
                }

                String toName = crossConnection.getTo().getPort();
                List<String> toNames = getNameList(toName);
                if (toNames.contains(lPortName)) {
                    return crossConnection;
                }
            }
        }

        log.error("Failed to get crossConnection definition for :  lPort:{}, lineSignalRate:{}; tunnelSignalRate:{}; from card:{}", lPortName, toSupportedSignal, fromSupportedSignal, otCardInfo);
        String msg = String.format("Failed to get crossConnection definition for :  lPort:%s, lineSignalRate:%s; tunnelSignalRate:%s; serviceType:%s; from card:%s", lPortName, toSupportedSignal,
                fromSupportedSignal, servicetype
                        .name(),
                otCardInfo.getCardType());

        throw new NeDesignerException(msg);
    }

    private static Boolean isSameServiceType(CrossConnection crossConnection, SERVICETYPE servicetype) {
        if (servicetype == null) {
            return true;
        }

        String serviceTypeXc = crossConnection.getServiceType();
        if (serviceTypeXc == null) {
            return false;
        }

        return isSameServiceType(servicetype, serviceTypeXc);
    }

    private static boolean isSameServiceType(SERVICETYPE servicetype, String serviceTypeJson) {
        if (serviceTypeJson == null || serviceTypeJson.isEmpty()) return false;
        return serviceTypeJson.replace("_", "").toUpperCase().equals(servicetype.name().toUpperCase());
    }

    public static String getLPortXcLayersByCPortName_OT(CrossConnection xcInfo, String cPortName) throws NeDesignerException {
        List<String> fromNames = NeInfoUtil.getNameList(xcInfo.getFrom().getPort());
        int cPortIndex = fromNames.indexOf(cPortName);
        List<String> lPortLayers = NeInfoUtil.getXcLayers(xcInfo.getTo().getLayer());
        return lPortLayers.get(cPortIndex);
    }

    public static String getCPortXcLayers_OT(CrossConnection xcInfo) {
        return xcInfo.getFrom().getLayer();
    }

    public static String getSupportedFrequency(String card) throws NeDesignerException {
        String frequency = GRID_MAP.get(card);
        if (frequency == null) {
            throw new NeDesignerException("Failed to get grid info from card: " + card);
        }
        return frequency;
    }

    /**
     * For OT card only
     *
     * @param card
     * @param vendor
     * @param productType
     * @return
     */
    public static CardType getOtCardCapability(Card card, String vendor, String productType) throws NeDesignerException {

        Map<String, List<CrossConnection>> xcInfoMap = card.getCrossConnections()
                .stream()
                .filter(xc -> xc.getType().equals("ODU") && xc.getServiceType() != null)
                .collect(Collectors.groupingBy(CrossConnection::getServiceType, HashMap::new, Collectors.toList()));
        List<ServiceType> serviceTypeList = new ArrayList<>();

        for (Map.Entry<String, List<CrossConnection>> xcEntry : xcInfoMap.entrySet()) {
            String serviceType = xcEntry.getKey();

            //clientSignal
            Set<SupportedSignal> clientSignalsJson = xcEntry.getValue()
                    .stream()
                    .flatMap(xc -> xc.getFrom().getSupportedSignal().stream())
                    .collect(Collectors.toSet());

            List<Class<? extends SignalProtocolType>> clientSignal = convertToYangSignals(clientSignalsJson);

            //transceiver
            Set<SupportedSignal> transcversJson = xcEntry.getValue()
                    .stream()
                    .flatMap(xc -> xc.getFrom().getTransceiverSupportedSignal().stream())
                    .collect(Collectors.toSet());
            List<Class<? extends ETHERNETCOMPLIANCECODE>> transceiverModule = convertToTransceiverYangSignals(transcversJson);

            ClientPortBuilder clientPortBuilder = new ClientPortBuilder().setClientSignal(clientSignal)
                    .setTransceiverModule(new ArrayList<>(transceiverModule));

            //line signal
            Set<SupportedSignal> lineSignalsJson = xcEntry.getValue()
                    .stream()
                    .flatMap(xc -> xc.getSupportedSignal().stream())
                    .collect(Collectors.toSet());

            List<Class<? extends SignalProtocolType>> lineSignal = convertToYangSignals(lineSignalsJson);

            LinePortBuilder linePortBuilder = new LinePortBuilder().setRate(lineSignal);
            Set<Integer> gridSet = xcEntry.getValue().get(0).getOpModes().stream().map(opMode -> opMode.getGrid()).collect(Collectors.toSet());

            List<GridType> gridTypes = convertToGridTypes(gridSet);
            serviceTypeList.add(new ServiceTypeBuilder().setClientPort(clientPortBuilder.build())
                    .setLinePort(linePortBuilder.build())
                    .setGridType(gridTypes)
                    .setServiceType(serviceType)
                    .build());
        }

        return new CardTypeBuilder()
                .setCardType(card.getCardType())
                .setComments(card.getComments())
                .setWDMBand(card.getWdmBand())
//                .setKey(new CardTypeKey(card.getCardType()))
                .setServiceType(serviceTypeList)
                .setProductType(productType)
                .setVendorName(vendor)
                .build();
    }

    private static List<GridType> convertToGridTypes(Set<Integer> gridSet) {
        List<GridType> gridTypeList = new ArrayList<>();
        for (Integer grid : gridSet) {
            gridTypeList.add(GridType.forValue(grid));
        }
        return gridTypeList;
    }

    public static Class<? extends BaseIdentity> convertToYangSingnal(SupportedSignal supportedSignal) throws NeDesignerException {
        Class<? extends BaseIdentity> yangSignal = supportedSignalTunnelSignalRateMapping.get(supportedSignal);
        if (yangSignal == null) {
            throw new NeDesignerException("Failed to convert supportedSignal to yang model: " + supportedSignal.name());
        }
        return yangSignal;
    }

    public static List<Class<? extends SignalProtocolType>> convertToYangSignals(Set<SupportedSignal> supportedSignals) throws NeDesignerException {
        List<Class<? extends SignalProtocolType>> result = new ArrayList<>();
        for (SupportedSignal s : supportedSignals) {
            result.add((Class<? extends SignalProtocolType>) convertToYangSingnal(s));
        }
        return result;

    }

    public static List<Class<? extends ETHERNETCOMPLIANCECODE>> convertToTransceiverYangSignals(Set<SupportedSignal> supportedSignals) throws NeDesignerException {
        List<Class<? extends ETHERNETCOMPLIANCECODE>> result = new ArrayList<>();
        for (SupportedSignal s : supportedSignals) {
            result.add((Class<? extends ETHERNETCOMPLIANCECODE>) convertToYangSingnal(s));
        }
        return result;

    }

    public static SupportedSignal convertToSupportedSignal(Class<? extends SignalProtocolType> tunnelSignalRate) throws NeDesignerException {
        SupportedSignal supportedSignal = supportedSignalTunnelSignalRateMapping.inverse().get(tunnelSignalRate);
        if (supportedSignal == null) {
            throw new NeDesignerException("Failed to convert to SupportedSignal from tunnelSignalRate: " + tunnelSignalRate);
        }
        return supportedSignal;
    }

    public static List<ImmutablePair<String, String>> getTpInfoNamePairList(TpModel tpModel) throws NeDesignerException {
        List<ImmutablePair<String, String>> output = new ArrayList<>();

        List<String> tpInfos = tpModel.getTpInfo();
        List<String> friendlyNames = tpModel.getTpFriendlyName();
        int size = tpInfos.size();
        for (int i = 0; i < size; i++) {
            String tpInfo = tpInfos.get(i);
            String friendlyName;
            try {
                friendlyName = friendlyNames.get(i);
            } catch (IndexOutOfBoundsException e) {
                friendlyName = friendlyNames.get(0);
            }
            output.add(new ImmutablePair<String, String>(tpInfo, friendlyName));
        }
        return output;
    }

    public static Integer getCPortNumberByTunnelSignalRate(Card otCardInfo, Class<? extends SignalProtocolType> tunnelSignalRate) throws NeDesignerException {
        SupportedSignal supportedSignal = convertToSupportedSignal(tunnelSignalRate);
        Integer matchedCPortsNumber = otCardInfo.getPorts()
                .stream()
                .filter(port -> port.getPortType().equals(OTU_CLIENT) && port.getSupportedSignal().contains(supportedSignal))
                .map(port -> {
                    try {
                        return getNameList(port.getName()).size();
                    } catch (NeDesignerException e) {
                        throw new RuntimeException(e);
                    }
                })
                .reduce(0, Integer::sum);

        if (matchedCPortsNumber == 0) {
            String msg = String.format("There is no available C port matched the supportedSignal: %s, from tunnelSignalRate:%s", supportedSignal.name(), tunnelSignalRate);
            throw new NeDesignerException(msg);
        }
        return matchedCPortsNumber;

    }


    public static Integer parserOduMux(SERVICETYPE serviceType, Card card, OduGranularity tunnelOdu, Class<? extends SignalProtocolType> linePortSignalRate) throws NeDesignerException {
        Optional<CrossConnection> xcInfo = card.getCrossConnections()
                .stream()
                .filter(crossConnection -> isSameServiceType(crossConnection, serviceType))
                .findAny();
        if (!xcInfo.isPresent()) {
            String msg = String.format("There is no available CrossConnection for serviceType:%s", serviceType);
            throw new NeDesignerException(msg);
        }
        return xcInfo.get().getMultiple();

    }

    public static OduGranularity parserOduGranularity(SERVICETYPE serviceType, Card card) {
        return OduGranularity.Odu4;//todo: how to define 400G
    }

    public static Map<String, String> getCPortNameMappingByServiceType(Card cardInfo, SERVICETYPE serviceType) {
        return cardInfo.getCrossConnections()
                .stream()
                .filter(xcInfo -> isSameServiceType(xcInfo, serviceType))
                .flatMap(xcInfo -> {
                    try {
                        Map<String, String> temMap = new HashMap<>();
                        List<String> portNames = getNameList(xcInfo.getFrom().getPort());
                        List<String> friendlyNames = getNameList(xcInfo.getFrom().getPortFriendlyName());
                        if (portNames.size() != friendlyNames.size()) {
                            throw new NeDesignerException("The size is not the same:portNames,FriendlyNames");
                        }
                        // 创建键值对流
                        List<AbstractMap.SimpleEntry<String, String>> entries = new ArrayList<>();
                        for (int i = 0; i < portNames.size(); i++) {
                            entries.add(new AbstractMap.SimpleEntry<>(portNames.get(i), getFriendlyName(portNames.get(i), friendlyNames.get(i))));
                        }
                        return entries.stream();
                    } catch (NeDesignerException e) {
                        throw new RuntimeException(e);
                    }
                }).collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

    }

    private static String getFriendlyName(String portName, String friendlyName) {
        return friendlyName.replace("<port>", portName);
    }

    public static Map<String, String> getLPortNameMappingByServiceType(Card cardInfo, SERVICETYPE serviceType) throws NeDesignerException {
        if (serviceType.name().startsWith("REG")) {
            Optional<Reg> regOptional = cardInfo.getRegs().stream().filter(r -> isSameServiceType(serviceType, r.getServiceType())).findAny();
            if (!regOptional.isPresent()) {
                String msg = String.format("Failed to get getLPortNameMappingByServiceType for serviceType:%s,card:%s", serviceType, cardInfo.getCardType());
                throw new NeDesignerException(msg);
            }

            return regOptional.get().getPorts().stream().collect(Collectors.toMap(RegPort::getPort, RegPort::getFriendlyName));
        }
        return cardInfo.getCrossConnections()
                .stream()
                .filter(xcInfo -> isSameServiceType(xcInfo, serviceType))
                .collect(Collectors.toMap(xcInfo -> xcInfo.getTo().getPort(), xcInfo -> getFriendlyName(xcInfo.getTo().getPort(), xcInfo.getTo()
                        .getPortFriendlyName())));

    }

    public static String getOpMode(Card card, SERVICETYPE serviceType, int grid) throws NeDesignerException {
        Optional<CrossConnection> xcInfoOptional = card.getCrossConnections()
                .stream()
                .filter(xcInfo -> isSameServiceType(xcInfo, serviceType))
                .findAny();
        if (!xcInfoOptional.isPresent()) {
            throw new NeDesignerException("Failed to find opMode for card:" + card.getCardType() + ",serviceType:" + serviceType.name());
        }
        Optional<OpMode> opOptional = xcInfoOptional.get()
                .getOpModes()
                .stream()
                .filter(opMode -> opMode.getGrid() == grid)
                .findAny();
        if (!opOptional.isPresent()) {
            throw new NeDesignerException("Failed to find opMode for card:" + card.getCardType() + ",serviceType:" + serviceType.name() + "grid:" + grid);
        }
        return opOptional.get().getName();


    }

    public static String getMpoPortName(Card muxPanelCardInfo, String mdPortName) {
        if (muxPanelCardInfo.getInternalLinks() == null || muxPanelCardInfo.getInternalLinks().isEmpty()) {
            return null;// for fix75,no muxpanel
        }
        return muxPanelCardInfo.getInternalLinks().stream().filter(internalLink -> {
            try {
                return getNameList(internalLink.getFrom()).contains(mdPortName);
            } catch (NeDesignerException e) {
                throw new RuntimeException(e);
            }
        }).findAny().get().getTo();
    }

    public static Set<String> getMDPortName(Card muxPanelCardInfo, String expPortName) throws NeDesignerException {
        Set<String> result = new HashSet<>();
        List<ExternalLink> externalLinks = muxPanelCardInfo.getExternalLinks();
        for (ExternalLink externalLink : externalLinks) {
            for (ExternalLinkTo externalLinkTo : externalLink.getTo()) {
                List<String> expNameList = getNameList(externalLinkTo.getPort());
                if (expNameList.contains(expPortName)) {
                    List<String> mdNameList = getNameList(externalLink.getMdPort());
                    String mdName = mdNameList.get(expNameList.indexOf(expPortName));
                    result.add(mdName);
                    break;
                }
            }
        }
        return result;
    }

    public static CrossConnection getOchXcInfo(Card iraCardInfo, String expPortName) throws NeDesignerException {
        Optional<CrossConnection> xcInfoOptional = iraCardInfo.getCrossConnections()
                .stream()
                .filter(xcInfo -> xcInfo.getType().equals("OCH") && !xcInfo.getInitiated())
                .findAny();
        if (!xcInfoOptional.isPresent()) {
            String msg = String.format("Failed to get OCH crossConnection definition for port:%s,card:%s", expPortName, iraCardInfo.getCardType());

            throw new NeDesignerException(msg);
        }

        return xcInfoOptional.get();
    }

    public static SERVICETYPE getRegServiceType(Card otCardInfo, Class<? extends SignalProtocolType> lineSignalRate) throws NeDesignerException {
        SupportedSignal supportedSignal = convertToSupportedSignal(lineSignalRate);
        Optional<Reg> regOptional = otCardInfo.getRegs().stream().filter(reg -> reg.getSupportedSignal().contains(supportedSignal)).findAny();
        if (!regOptional.isPresent()) {
            String msg = String.format("Failed to get reg serviceType definition for lineSignalRate:%s,card:%s", lineSignalRate, otCardInfo.getCardType());
            throw new NeDesignerException(msg);
        }
        return getYangServiceType(regOptional.get().getServiceType());
    }

    private static SERVICETYPE getYangServiceType(String regServiceType) {
        return SERVICETYPE.valueOf(regServiceType.replace("_", "").toUpperCase());
    }

    public static int getLPortNameSizeReg(Card otCardInfo, SERVICETYPE serviceType) throws NeDesignerException {
        Optional<Reg> regOptional = otCardInfo.getRegs().stream().filter(r -> isSameServiceType(serviceType, r.getServiceType())).findAny();
        if (!regOptional.isPresent()) {
            String msg = String.format("Failed to get reg serviceType definition for service:%s,card:%s", serviceType, otCardInfo.getCardType());
            throw new NeDesignerException(msg);
        }
        return regOptional.get().getPorts().size();
    }

    public static CrossConnection getRegXcInfo(Card otCardInfo, SERVICETYPE regServiceType) throws NeDesignerException {
        Optional<CrossConnection> xcOptional = otCardInfo.getCrossConnections()
                .stream()
                .filter(crossConnection -> isSameServiceType(regServiceType, crossConnection.getServiceType()))
                .findAny();
        if (!xcOptional.isPresent()) {
            String msg = String.format("Failed to get CrossConnection definition for service:%s,card:%s", regServiceType, otCardInfo.getCardType());
            throw new NeDesignerException(msg);
        }
        return xcOptional.get();
    }

    //目前xc的定义就是一个L口一个
    public static long getLPortCount(Card cardInfo, SERVICETYPE serviceType) {
        return cardInfo.getCrossConnections()
                .stream()
                .filter(xcInfo -> isSameServiceType(xcInfo, serviceType))
                .count();

    }

}
