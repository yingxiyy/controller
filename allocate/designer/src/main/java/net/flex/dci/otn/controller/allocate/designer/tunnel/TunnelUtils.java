/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.tunnel;

import static org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType._150;

import com.google.common.collect.Sets;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.common.util.frequency.FrequencyInterval;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.allocate.designer.NeDesignerException;
import net.flex.dci.otn.controller.allocate.designer.config.ProductTypeResolver;
import net.flex.dci.otn.controller.allocate.designer.model.JsonYangConverter;
import net.flex.dci.otn.controller.allocate.designer.model.NeInfoUtil;
import net.flex.dci.otn.controller.allocate.designer.ne.TpRepo;
import net.flex.dci.otn.controller.allocate.ne.Card;
import net.flex.dci.otn.controller.allocate.ne.CrossConnection;
import net.flex.dci.otn.controller.allocate.ne.SupportedSignal;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yangtools.yang.common.QName;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ETHERNETCOMPLIANCECODE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.Prot100GE;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTU4;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTUc2;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTUc4;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ProtOTUc6;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.SignalProtocolType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AvailableBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class TunnelUtils {

    public static final String L_PORT_MODE_DEFAULT = "{\"mode-id\":0}";
    public static final String L_PORT_MODE_200G = "{\"mode-id\":3}";
    public static final String L_PORT_MODE_300G = "{\"mode-id\":8}";
    public static final String L_PORT_MODE_400G = "{\"mode-id\":1}";
    public static final String OPA = "OP-A";
    public static final String OPB = "OP-B";
    public static final String OPC = "OP-C";
    @Autowired
    private JsonYangConverter jsonYangConverter;

    public static final String GRID_0 = "0";
    public static final String GRID_50 = "50";
    public static final String GRID_75 = "75";
    public static final String GRID_100 = "100";
    public static final String GRID_150 = "150";
    public static final String BANDWIDTH_100G = "100G";
    public static final String BANDWIDTH_200G = "200G";
    public static final String BANDWIDTH_300G = "300G";
    public static final String BANDWIDTH_400G = "400G";
    public static final String BANDWIDTH_600G = "600G";
    public static final String BANDWIDTH_800G = "800G";
    public static final String WSS_LINK_PREFIX = "WssLink";
    public static final String LINK_MODEL = "model";
    public static final String LINK_MODEL_OMSP = "4";
    public static final String LINK_MODEL_BONE20_ONE_TO_TWO = "10";

    public boolean isWssLink(String linkId) {
        return linkId.startsWith(WSS_LINK_PREFIX);
    }

    public String resolvePeerSiteId(String siteLinkId, String currentSiteId) {
        String siteA = PhysicalLinkIdNamingRule.getSiteAId(siteLinkId);
        return siteA.equals(currentSiteId) ? PhysicalLinkIdNamingRule.getSiteZId(siteLinkId) : siteA;
    }

    public String resolveTpIdBySiteId(String siteLinkId, String siteId) {
        String tpAId = PhysicalLinkIdNamingRule.getTpAId(siteLinkId);
        return tpAId.contains(siteId) ? tpAId : PhysicalLinkIdNamingRule.getTpZId(siteLinkId);
    }

    public String resolveEquipIdBySiteId(String siteLinkId, String siteId) {
        String tpId = resolveTpIdBySiteId(siteLinkId, siteId);
        return PhysicalTpIdNamingRule.getEquipId(tpId);
    }


    public String getRegSite(String firstLinkId, String secondLinkId) throws NeDesignerException {
        Set<String> firstSites = Sets.newHashSet(PhysicalLinkIdNamingRule.getSiteAId(firstLinkId), PhysicalLinkIdNamingRule.getSiteZId(firstLinkId));
        Set<String> secondSites = Sets.newHashSet(PhysicalLinkIdNamingRule.getSiteAId(secondLinkId), PhysicalLinkIdNamingRule.getSiteZId(secondLinkId));
        secondSites.retainAll(firstSites);
        if (secondSites.size() != 1) {
            log.error("Invalid path,,bcause failed to getRegs between {} and {}", firstLinkId, secondLinkId);
            throw new NeDesignerException("Invalid path, failed to get REG site between sitelinks");
        }
        return secondSites.iterator().next();
    }

    public CrossConnection getOtXcInfo(Card otCardInfo, String lPortName, Class<? extends SignalProtocolType> lineSignalRate, Class<? extends SignalProtocolType> tunnelSignalRate,
            SERVICETYPE servicetype)
            throws NeDesignerException {
        SupportedSignal lSupportedSignal = NeInfoUtil.convertToSupportedSignal(lineSignalRate);
        SupportedSignal tSupportedSignal = NeInfoUtil.convertToSupportedSignal(tunnelSignalRate);
        return NeInfoUtil.getOtXcInfo(otCardInfo, lPortName, lSupportedSignal, tSupportedSignal, servicetype);
    }

    public FrequencyInterval getFrequencyInterval(Link siteLink, Class<? extends SignalProtocolType> lineSignalRate, Card cardInfo) throws NeDesignerException {
        GridType grid = siteLink.getAugmentation(Link1.class).getSite().getGrid();
        String type = siteLink.getAugmentation(Link1.class).getSite().getProductType();

        if (lineSignalRate.equals(ProtOTUc4.class) || lineSignalRate.equals(ProtOTUc6.class)) {
            if (!grid.equals(GridType._0) && !grid.equals(GridType._75)) {
                throw new NeDesignerException("Failed to get frequencyInterval, because only flex grid support 400G or higher line signal rate ");
            }
        }
        switch (grid) {
            case _0:
                return getFlexFrequencyInterval(lineSignalRate, cardInfo);
            case _50:
                return FrequencyInterval.MUX96;
            case _75:
                return FrequencyInterval.MUX64;
            case _100:
                return FrequencyInterval.MUX48;
            case _150:
                return FrequencyInterval.MUX32C;
            default:
                throw new NeDesignerException("Failed to get frequencyInterval, because unsupported grid: " + grid);
        }

    }

    public FrequencyInterval getFlexFrequencyInterval(Class<? extends SignalProtocolType> lineSignalRate, Card cardInfo) throws NeDesignerException {
        String bandwidth = getBandwidthBySignalRate(lineSignalRate);
        if (bandwidth.equals(BANDWIDTH_400G)) {
            if (cardInfo.getCardType().startsWith("L1X12C8")) {
                return FrequencyInterval.MUX32C;
            } else {
                return FrequencyInterval.MUX64;
            }
        } else if (bandwidth.equals(BANDWIDTH_200G)) {
            if (cardInfo.getVendorType().equals("CH4L2E8") || cardInfo.getVendorType().equals("TMUX")) {
                //26 400G板卡，L口的间隔需要75GHz
                return FrequencyInterval.MUX64;
            } else {
                return FrequencyInterval.MUX96;
            }
        } else if (bandwidth.equals(BANDWIDTH_800G)) {
            return FrequencyInterval.MUX32C;
        } else if (bandwidth.equals(BANDWIDTH_600G)) {
            return FrequencyInterval.MUX32C;
        }
        throw new NeDesignerException("Failed to get frequencyInterval for flex grid, because unsupported lineSignalRate: " + lineSignalRate.getSimpleName());
    }

    public String getBandwidthBySignalRate(Class<? extends SignalProtocolType> lineSignalRate) throws NeDesignerException {
        if (lineSignalRate.equals(Prot100GE.class) || lineSignalRate.equals(ProtOTU4.class)) {
            return BANDWIDTH_100G;
        }
        if (lineSignalRate.equals(ProtOTUc2.class)) {
            return BANDWIDTH_200G;
        }
        if (lineSignalRate.equals(ProtOTUc3.class)) {
            return BANDWIDTH_300G;
        }
        if (lineSignalRate.equals(ProtOTUc4.class)) {
            return BANDWIDTH_400G;
        } else if (lineSignalRate.equals(ProtOTUc6.class)) {
            return BANDWIDTH_600G;
        } else if (lineSignalRate.equals(ProtOTUc8.class)) {
            return BANDWIDTH_800G;
        }
        throw new NeDesignerException("Failed to get bandwidth, because unsupported lineSignalRate: " + lineSignalRate.getSimpleName());
    }

    public Available pickFreeFrequency(Link siteLink, Class<? extends SignalProtocolType> lineSignalRate, FrequencyAvailable frequencyAvailable, Card cardInfo) throws NeDesignerException {
        FrequencyInterval frequencyInterval = getFrequencyInterval(siteLink, lineSignalRate, cardInfo);
        Available output = frequencyAvailable.getFree(frequencyInterval);
        frequencyAvailable.remove(output);
        return output;
    }

    /**
     * @param frequency
     * @return e.g. /frequency=196025000,19675000
     */
    public String getTpSlotFrequencyString(Available frequency) {
        return String.format("/frequency=%d,%d", frequency.getLowerFrequency().getValue().longValue(), frequency.getUpperFrequency().getValue().longValue());
    }

    /**
     * @param tpPhysical
     * @return e.g  e.g. /frequency=196025000,19675000
     */
    public String getTpSlotBTp(Physical tpPhysical) {
        Properties property = tpPhysical.getProperties();
        if (property == null) {
            return null;
        }

        List<Property> propertyList = property.getProperty();
        if (propertyList == null || propertyList.isEmpty()) {
            return null;
        }

        for (Property propertyItem : propertyList) {
            if (propertyItem.getName().equals(TpRepo.SLOT)) {
                return propertyItem.getValue();
            }
        }
        return null;
    }

    public BigInteger getCentFreq(Available freeFrequency) {
        return freeFrequency.getUpperFrequency().getValue().add(freeFrequency.getLowerFrequency().getValue()).divide(new BigInteger("2"));
    }

    public String getClientMediumLocalName(Class<? extends ETHERNETCOMPLIANCECODE> clientMedium) throws NeDesignerException {

        Field field = null;
        try {
            field = clientMedium.getField("QNAME");
            field.setAccessible(true);
            QName qName = (QName) field.get(null);
            return qName.getLocalName();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            log.error("Failed get the local name for class:{}.", clientMedium.getSimpleName(), e);
            throw new NeDesignerException("No supported clientMedium type" + clientMedium.getSimpleName());
        }
    }

    public String getLPortOperationMode(Class<? extends SignalProtocolType> lineSignalRate) {
        try {
            String bandwidth = getBandwidthBySignalRate(lineSignalRate);
            switch (bandwidth) {
                case BANDWIDTH_200G:
                    return L_PORT_MODE_200G;
                case BANDWIDTH_300G:
                    return L_PORT_MODE_300G;
                case BANDWIDTH_400G:
                    return L_PORT_MODE_400G;
                default:
                    log.error("Unsupported bandwidth:{}, return default mode:{}", bandwidth, L_PORT_MODE_DEFAULT);
                    return L_PORT_MODE_DEFAULT;


            }
        } catch (NeDesignerException e) {
            log.error("Failed to get operation Mode, return the default value.", L_PORT_MODE_DEFAULT, e);
        }

        return L_PORT_MODE_DEFAULT;
    }

    public Available convertToUpperLowerFreq(GridType grid, Long cenFrequency) throws NeDesignerException {
        long step = getStepByFixGrid(grid);
        long centFactor = step / 2;
        long lowerFrequency = cenFrequency - centFactor;
        long upperFrequency = cenFrequency + centFactor;
        return new AvailableBuilder()
                .setLowerFrequency(new FrequencyType(BigInteger.valueOf(lowerFrequency)))
                .setUpperFrequency(new FrequencyType(BigInteger.valueOf(upperFrequency)))
                .build();
    }

    public long getStepByFixGrid(GridType grid) throws NeDesignerException {
        BigInteger step;
        switch (grid) {
            case _50: {
                step = FrequencyInterval.MUX96.getGridSpan();
                break;
            }
            case _75: {
                step = FrequencyInterval.MUX64.getGridSpan();
                break;
            }
            case _100: {
                step = FrequencyInterval.MUX48.getGridSpan();
                break;
            }
            case _150: {
                step = FrequencyInterval.MUX32C.getGridSpan();
                break;
            }
            default:
                throw new NeDesignerException("Unsupported grid:" + grid.name());
        }
        return step.longValue();
    }

    public GridType getGridFromSiteLink(Link siteLink) {
        return siteLink.getAugmentation(Link1.class).getSite().getGrid();
    }

    public String getOpPrimaryTp(String op6SigPortTp, Card opCardInfo) throws NeDesignerException {
        return getOpTpByPortTypeAndOpSig(op6SigPortTp, opCardInfo, OPA);
    }

    /**
     * Use XC config to get the targe port in the same group for OP.
     *
     * @param opSigPortTp
     * @param opCardInfo
     * @param portType
     * @return
     */
    private String getOpTpByPortTypeAndOpSig(String opSigPortTp, Card opCardInfo, String portType) throws NeDesignerException {
        String sigPortName = PhysicalTpIdNamingRule.getPortNameByTpId(opSigPortTp);
        Set<String> targetPortNames = opCardInfo.getPorts().stream().filter(port -> port.getPortType().equals(portType)).flatMap(port -> {
            try {
                return NeInfoUtil.getNameList(port.getName()).stream();
            } catch (NeDesignerException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toSet());
        CrossConnection xcInfo = opCardInfo.getCrossConnections().stream().filter(xc -> xc.getFrom().getPort().equals(sigPortName)).findAny().get();
        String targetPortName = NeInfoUtil.getNameList(xcInfo.getTo().getPort())
                .stream()
                .filter(portName -> targetPortNames.contains(portName))
                .findAny()
                .get();
        return opSigPortTp.replace(sigPortName, targetPortName);
    }

    public String getOpSecondaryTp(String opSigPortTp, Card opCardInfo) throws NeDesignerException {
        return getOpTpByPortTypeAndOpSig(opSigPortTp, opCardInfo, OPB);

    }

    public String getOpThirdTp(String opSigPortTp, Card opCardInfo) throws NeDesignerException {
        return getOpTpByPortTypeAndOpSig(opSigPortTp, opCardInfo, OPC);
    }

    public GridType getGridByString(String gridString) throws NeDesignerException {
        switch (gridString) {
            case GRID_0:
                return GridType._0;
            case GRID_50:
                return GridType._50;
            case GRID_75:
                return GridType._75;
            case GRID_100:
                return GridType._100;
            case GRID_150:
                return _150;
            default:
                throw new NeDesignerException("Unsupported grid: " + gridString);

        }
    }

    public String getGridString(GridType gridType) throws NeDesignerException {
        switch (gridType) {
            case _75:
                return "75";
            case _0:
                return "0";
            case _50:
                return "50";
            case _100:
                return "100";
            default:
                throw new NeDesignerException("Unsupported grid: " + gridType);
        }
    }


    public Boolean isReg(SERVICETYPE servicetype) {
        return servicetype.name().startsWith("REG");
    }

    public String resolveTpIdByEquipId(Link link, String equipId) {
        if (PhysicalTpIdNamingRule.getEquipId(link.getSource().getSourceTp().getValue()).equals(equipId)) {
            return link.getSource().getSourceTp().getValue();
        }
        return link.getDestination().getDestTp().getValue();
    }

    public WDM_Band getWdmBand(Link siteLink) {
        return WDM_Band.fromString(siteLink.getAugmentation(Link1.class).getSite().getLinkGroup());
    }

    public List<String> getMatchedWdmBands(WDM_Band wdmBand) {
        switch (wdmBand) {
            case C:
                return Arrays.asList("C+L", "C");
            case L:
                return Arrays.asList("C+L", "L");
            default:
                return Arrays.asList("C+L", "C", "L");
        }
    }

    public boolean isOmsp(Link siteLink) {
        Site siteAttr = siteLink.getAugmentation(Link1.class).getSite();
        if (!siteAttr.getProtectionType().equals(ProtectionBidir1To1.class)) {
            return false;
        }
        return siteAttr.getProperties().getProperty().stream()
                .filter(property -> property.getName().equals(LINK_MODEL) && property.getValue().equals(LINK_MODEL_OMSP)).findAny().isPresent();
    }

    public boolean isBone20OneToTwo(Link siteLink) {
        Site siteAttr = siteLink.getAugmentation(Link1.class).getSite();
        if (siteAttr.getProperties() == null || siteAttr.getProperties().getProperty() == null) {
            return false;
        }

        String model = getPropertyValue(siteAttr, LINK_MODEL);
        // Bone2.0 model=10 keeps protection legs inside one siteLink instead of separate siteLinks.
        return LINK_MODEL_BONE20_ONE_TO_TWO.equals(model)
                && ProductTypeResolver.isBone20ProductType(siteAttr.getVendorName(),
                siteAttr.getProductType());
    }

    private String getPropertyValue(Site siteAttr, String name) {
        return siteAttr.getProperties().getProperty().stream()
                .filter(property -> name.equals(property.getName()))
                .map(Property::getValue)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public Boolean isAdditionalMux(Link siteLink, String muxMdTpId) {
        Boolean hasMux51 = PhysicalTpIdNamingRule.getEquipId(muxMdTpId).contains("51");//目前写死51为addtioanl
        if (!hasMux51) {
            return false;
        }

        //判断siteLInk的supporting link是不是同事包含了50和51. 因为也可能51是单独的siteLink
        Pattern pattern = Pattern.compile("MUX-1-\\d+");
        long muxPanelCount = siteLink.getSupportingLink().stream().
                map(supportingLink -> supportingLink.getLinkRef().getValue())
                .map(name -> {
                    Matcher m = pattern.matcher(name); return m.find() ? m.group() : null;
                })
                .filter(Objects::nonNull).distinct().count();

        if (muxPanelCount < 2) {
            return false;
        }
        return true;
    }

    public boolean isSameDirection(String srcSiteId, String destSiteId, String tunnelId) {
        String tunnelSiteA = PhysicalLinkIdNamingRule.getSiteAId(tunnelId);
        String tunnelSiteZ = PhysicalLinkIdNamingRule.getSiteZId(tunnelId);
        return  (srcSiteId.equals(tunnelSiteA) && destSiteId.equals(tunnelSiteZ))
                || (srcSiteId.equals(tunnelSiteZ) && destSiteId.equals(tunnelSiteA));

    }
}
