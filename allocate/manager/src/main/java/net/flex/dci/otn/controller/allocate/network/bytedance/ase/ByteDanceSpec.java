package net.flex.dci.otn.controller.allocate.network.bytedance.ase;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.RouteInfo;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otc.controller.utils.bytedance.OaCardVoaGain;
import net.flex.dci.otc.mongo.mdoel.ChangedObject;
import net.flex.dci.otn.controller.allocate.common.ByteDanceSpecConfig;
//import net.flex.dci.otc.optical.tool.MongoSpanLossProvider;
//import net.flex.dci.otc.optical.tool.bytedance.OpticalPrams;
import net.flex.dci.otc.optical.tool.MongoSpanLossProvider;
import net.flex.dci.otc.optical.tool.OpticalPathCalculator;
import net.flex.dci.otc.optical.tool.RamanTransparentRoute;
import net.flex.dci.otc.optical.tool.SpanLossProvider;
import net.flex.dci.otc.optical.tool.bytedance.OpticalPrams;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.PortType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.Properties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.provider.info.Provider;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.*;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.amplifier.attributes.Amplifier;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.amplifier.attributes.AmplifierBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnectionsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.PhysicalBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.EquipmentsBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


//ILA指amplfier类型为ILA，ILA-CL和DGE-CL内都是ILA amplifier。
// switch gain指有highGainRange和lowerGainRange切换的amplifier
// PA/ILA是switch gain，BA是Fixed gain
@Slf4j
public class ByteDanceSpec {
    @FunctionalInterface
    public interface GainValueProvider {
        float get();
    }

    private RouteInfo rInfo;
    private RamanTransparentRoute transparentRoute;
    private Link siteLink;
    private List<String>otmSiteList;  //useless now
    private WDM_Band currentWdmBand;
    private GridType currentGrid;

    private ChangedObject sourceChangedObject;
    private ChangedObject defaultParamChanges;
    private final ByteDanceSpecConfig config = ByteDanceSpecConfig.load();


    public ByteDanceSpec(String siteLinkId, ChangedObject changedObject) {
        this(siteLinkId, changedObject, changedObject);
    }

    // sourceChangedObject carries the just-created network context for route reads.
    // defaultParamChanges is a small delta saved after the first DB write, avoiding a full re-save.
    public ByteDanceSpec(String siteLinkId, ChangedObject sourceChangedObject, ChangedObject defaultParamChanges) {
        otmSiteList = new ArrayList<>();
        this.sourceChangedObject = sourceChangedObject;
        this.defaultParamChanges = defaultParamChanges;
        initSiteLink(siteLinkId);
    }

    private void initSiteLink(String siteLinkId) {
        this.siteLink = sourceChangedObject.getChangedSiteLink(siteLinkId);

        otmSiteList.add(SiteLinkIdNamingRule.getNodeA(siteLinkId));
        otmSiteList.add(SiteLinkIdNamingRule.getNodeZ(siteLinkId));
    }

    private Node getChangedPhyNode(String nodeId) {
        Node defaultParamNode = defaultParamChanges.getChangedPhyNodeList().get(nodeId);
        if (defaultParamNode != null) {
            return defaultParamNode;
        }
        return sourceChangedObject.getChangedPhyNode(nodeId);
    }

    private Link getChangedPhyLink(String linkId) {
        return sourceChangedObject.getChangedPhyLink(linkId);
    }

    public void start() {
        rInfo = new net.flex.dci.otc.common.util.RouteInfo();
        Site siteLinkAttr = siteLink.getAugmentation(Link1.class).getSite();
        rInfo.parse(siteLinkAttr.getExplictRoute().getRoute());
        currentWdmBand = WDM_Band.fromString(siteLinkAttr.getLinkGroup());
        currentGrid = siteLinkAttr.getGrid();

        // 三个模块共用透明路由，实际配置路由仍保留 RAMAN 资源。
        transparentRoute = new RamanTransparentRoute(siteLink, sourceChangedObject);

        oaCardRelated();

        //(后级OA增益 -（ 前级VOA + OSC_SPAN_LOSS）)< Delta (0.3)
        SpanLossProvider provider = new ChangedObjectSpanLossProvider(new MongoSpanLossProvider());
        OpticalPathCalculator.OpticalCalculationResult result = new OpticalPrams(siteLink.getLinkId().getValue(), sourceChangedObject).getResult(provider);

        OaCardVoaGain oaCardParm = new OaCardVoaGain(defaultParamChanges);
        oaCardParm.updateTargetGain(result.getAzResult(), true);
        oaCardParm.updateTargetGain(result.getZaResult(), false);
    }

    private void oaCardRelated() {
        rInfo.getEqIdList().forEach(id -> {
            String nodeId = PhysicalEqpIdNamingRule.getNodeId(id);
            Node node = getChangedPhyNode(nodeId);
            Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();

            Optional<Equipments> eqOp = nodeAttr.getEquipments().stream().filter(eq -> eq.getEquipmentId().equals(id)).findAny();
            if (!eqOp.isPresent()) {
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Impossible, the eqID must be in node. " + id);
            }

            Equipments eq = eqOp.get();
            // RAMAN 保留模板参数，板卡自身及端口均不参与初始光放参数计算。
            if ("RAMAN".equals(String.valueOf(eq.getEquipType()))) {
                return;
            }

            List<CrossConnections> newXcList = nodeAttr.getCrossConnections().stream()
                .map(xc -> {
                    if (xc.getCrossConnectionId().getValue().contains(eq.getEquipmentId()) &&
                            xc.getAmplifier() != null) {
                        CrossConnections newXC = null;
                        switch (eq.getEquipType()) {
                            case OA:
                                if (otmSiteList.contains(nodeId)) {
                                    newXC = otmOA(eq, xc);
                                } else {
                                    newXC = roadmOA(eq, xc);
                                }
                                break;
                            case IRA:
                                if (otmSiteList.contains(nodeId)) {
                                    newXC = otmIRA(eq, xc);
                                } else {
                                    newXC = roadmIRA(eq, xc);
                                }
                                iraAseRelated(eq);
                                break;
                            case TILA:
                            case ILA:
                                newXC = ila(eq, xc);
                                break;
                            case DGE:
                                newXC = dge(eq, xc);
                                break;
                            default:
                                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Impossible, find one unkown OA card type in siteLink. " + eq.getEquipType().name());
                        }
                        return newXC;
                    } else {
                        return xc;
                    }
                }).collect(Collectors.toList());

            List<TerminationPoint> updatedTpList = updateOTSLine(node.getTerminationPoint(), eq);

            Node newNode = new NodeBuilder(node)
                .addAugmentation(Node1.class, new Node1Builder()
                    .setPhysical(new PhysicalBuilder(nodeAttr)
                        .setCrossConnections(newXcList)
                        .build())
                    .build())
                .setTerminationPoint(updatedTpList)
                .build();

            defaultParamChanges.addChangedPhyNode(newNode);
        });
    }

    private List<TerminationPoint> updateOTSLine(List<TerminationPoint> tpList, Equipments eq) {
        List<TerminationPoint> newTpList = tpList.stream().map(tp-> {
            if (tp.getTpId().getValue().contains(eq.getEquipmentId())) {
                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical tpAttr = tp.getAugmentation(TerminationPoint1.class).getPhysical();
                PortType portType = tpAttr.getPortType();
                if (portType.equals(PortType.OALine) || portType.equals(PortType.ILALINEA) || portType.equals(PortType.ILALINEB)) {
                    Link phyLink = getPhyLinkWithTp(tp.getTpId().getValue());

                    Provider provider = getProvider(phyLink);
                    if (provider == null) {
                        // TILA has two line-side ports; the OMS/MUX-facing side has no span provider and keeps its original properties.
                        log.debug("skip OTS line default params for internal line tp {}, phyLink {}", tp.getTpId().getValue(),
                            phyLink.getLinkId().getValue());
                        return tp;
                    }

                    BigDecimal ingressDistance = null;
                    BigDecimal egressDistance = null;
                    BigDecimal egressAttenuation = null;
                    // 规划长度与出向损耗采用有效端口的方向，不再用 RAMAN 物理端口匹配 IRA。
                    String[] effectiveTps = transparentRoute.getEndpoints().get(phyLink.getLinkId().getValue());
                    boolean sourceSide = effectiveTps == null
                            ? phyLink.getSource().getSourceTp().getValue().equals(tp.getTpId().getValue())
                            : effectiveTps[0].equals(tp.getTpId().getValue());
                    if (sourceSide) {
                        ingressDistance = provider.getDistanceZa();
                        egressDistance = provider.getDistanceAz();
                        egressAttenuation = provider.getContractAttenuationAz();
                    } else {
                        ingressDistance = provider.getDistanceAz();
                        egressDistance = provider.getDistanceZa();
                        egressAttenuation = provider.getContractAttenuationZa();
                    }

                    Properties newProp = tpAttr.getProperties();
                    Map<String, String> otsLineOverrides = new LinkedHashMap<>();
                    otsLineOverrides.put("fiber-length-ingress-planning", ingressDistance.toString());
                    otsLineOverrides.put("fiber-length-egress-planning", egressDistance.toString());
                    otsLineOverrides.put("span-loss-adaptation.auto-control-range.oms-c", egressAttenuation.toString());
                    otsLineOverrides.put("span-loss-adaptation.auto-control-range.oms-l", egressAttenuation.toString());
                    newProp = addConfiguredProperties(newProp,
                        config.getTerminationPointProperties("OTS_LINE_BASE", currentWdmBand, currentGrid), otsLineOverrides);

                    if (eq.getEquipType().equals(EquipType.IRA)) {
                        newProp = addConfiguredProperties(newProp,
                            config.getTerminationPointProperties("OTS_LINE_IRA", currentWdmBand, currentGrid));
                    } else if (eq.getEquipType().equals(EquipType.DGE)) {
                        newProp = addConfiguredProperties(newProp,
                            config.getTerminationPointProperties("OTS_LINE_DGE", currentWdmBand, currentGrid));
                    }

                    return new TerminationPointBuilder(tp)
                        .addAugmentation(TerminationPoint1.class, new TerminationPoint1Builder()
                            .setPhysical(new org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.PhysicalBuilder(tpAttr)
                                .setProperties(newProp).build())
                            .build())
                        .build();
                }
            }

            return tp;

        }).collect(Collectors.toList());

        return newTpList;
    }

    private Link getPhyLinkWithTp(String tpId) {
        // IRA 与 OTS 之间有 RAMAN 时，用透明路由找到真正的跨段，不能取中间 OMS。
        for (Map.Entry<String, String[]> entry : transparentRoute.getEndpoints().entrySet()) {
            if ((tpId.equals(entry.getValue()[0]) && !tpId.equals(PhysicalLinkIdNamingRule.getTpAId(entry.getKey())))
                    || (tpId.equals(entry.getValue()[1]) && !tpId.equals(PhysicalLinkIdNamingRule.getTpZId(entry.getKey())))) {
                return getChangedPhyLink(entry.getKey());
            }
        }
        List<Link> candidateLinks = rInfo.getPhyLinkIdList().stream()
            .filter(x -> x.contains(tpId))
            .map(this::getChangedPhyLink)
            .collect(Collectors.toList());
        if (candidateLinks.isEmpty()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Impossible, cannot get physical link based on tpID. " + tpId);
        }
        return selectProviderLink(candidateLinks);
    }

    static Link selectProviderLink(List<Link> candidateLinks) {
        // A TILA/OA line TP can appear on both an internal OMS link and the external OTS fiber link.
        return candidateLinks.stream()
            .filter(link -> getProvider(link) != null)
            .findFirst()
            .orElse(candidateLinks.get(0));
    }

    private static Provider getProvider(Link phyLink) {
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1 phyLinkAttr =
            phyLink.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class);
        if (phyLinkAttr == null || phyLinkAttr.getPhysical() == null) {
            return null;
        }
        return phyLinkAttr.getPhysical().getProvider();
    }

    private class ChangedObjectSpanLossProvider implements SpanLossProvider {
        private final SpanLossProvider delegate;

        ChangedObjectSpanLossProvider(SpanLossProvider delegate) {
            this.delegate = delegate;
        }

        @Override
        public double[] getSpanLoss(String linkId) {
            try {
                return delegate.getSpanLoss(linkId);
            } catch (RuntimeException e) {
                Link link = getChangedPhyLink(linkId);
                Provider provider = getProvider(link);
                if (provider != null && getAttenuationAz(provider) != null && getAttenuationZa(provider) != null) {
                    return new double[]{getAttenuationAz(provider).doubleValue(), getAttenuationZa(provider).doubleValue()};
                }
                if (isInternalPhyLink(link)) {
                    // Bone2.0 adds internal OTS sections such as OLP3-3 1B to TILA west; they
                    // have no physical span loss and should not fail default VOA/gain calculation.
                    return new double[]{0D, 0D};
                }
                throw e;
            }
        }

        private boolean isInternalPhyLink(Link link) {
            if (link == null || link.getSource() == null || link.getDestination() == null) {
                return false;
            }
            String srcSite = PhysicalTpIdNamingRule.getSiteId(link.getSource().getSourceTp().getValue());
            String destSite = PhysicalTpIdNamingRule.getSiteId(link.getDestination().getDestTp().getValue());
            return srcSite != null && srcSite.equals(destSite);
        }

        private BigDecimal getAttenuationAz(Provider provider) {
            return provider.getAttenuationAz() != null ? provider.getAttenuationAz() : provider.getContractAttenuationAz();
        }

        private BigDecimal getAttenuationZa(Provider provider) {
            return provider.getAttenuationZa() != null ? provider.getAttenuationZa() : provider.getContractAttenuationZa();
        }
    }




    private CrossConnections ila(Equipments eq, CrossConnections axmplifierXC) {
        CrossConnectionsBuilder newXCB = new CrossConnectionsBuilder(axmplifierXC);
        AmplifierBuilder newAmB = new AmplifierBuilder(axmplifierXC.getAmplifier());
        Amplifier newAm = null;
        String desc = axmplifierXC.getDescription();
        if (desc.contains("ILAC_EAST")) {
            newAm = getIlaEastC(newXCB, newAmB, eq);
        } else if (desc.contains("ILAL_EAST")) {
            newAm = getIlaEastL(newXCB, newAmB, eq);
        } else if (desc.contains("ILAC_WEST")) {
            newAm = getIlaWestC(newXCB, newAmB, eq);
        } else if (desc.contains("ILAL_WEST")) {
            newAm = getIlaWestL(newXCB, newAmB, eq);
        } else if (desc.contains("ILA_EAST")) {
            newAm = getIlaWestC(newXCB, newAmB, eq);
        } else if (desc.contains("ILA_WEST")) {
            newAm = getIlaWestC(newXCB, newAmB, eq);
        }
        newXCB.setAmplifier(newAm);
        return newXCB.build();
    }

    private Amplifier getIlaWestL(CrossConnectionsBuilder newXCB, AmplifierBuilder newAmB, Equipments eq) {
        return getDgeWestL(newXCB, newAmB, eq);
    }

    private Amplifier getIlaWestC(CrossConnectionsBuilder newXCB, AmplifierBuilder newAmB, Equipments eq) {
        return getDgeWestC(newXCB, newAmB, eq);
    }

    private Amplifier getIlaEastL(CrossConnectionsBuilder newXCB, AmplifierBuilder newAmB, Equipments eq) {
        return getDgeEastL(newXCB, newAmB, eq);
    }

    private Amplifier getIlaEastC(CrossConnectionsBuilder newXCB, AmplifierBuilder newAmB, Equipments eq) {
        return getDgeEastC(newXCB, newAmB, eq);
    }


    private CrossConnections dge(Equipments eq, CrossConnections axmplifierXC) {
        CrossConnectionsBuilder newXCB = new CrossConnectionsBuilder(axmplifierXC);
        AmplifierBuilder newAmB = new AmplifierBuilder(axmplifierXC.getAmplifier());
        Amplifier newAm = null;
        String desc = axmplifierXC.getDescription();
        if (desc.contains("ILAC_EAST")) {
            newAm = getDgeEastC(newXCB, newAmB, eq);
        } else if (desc.contains("ILAL_EAST")) {
            newAm = getDgeEastL(newXCB, newAmB, eq);
        } else if (desc.contains("ILAC_WEST")) {
            newAm = getDgeWestC(newXCB, newAmB, eq);
        } else if (desc.contains("ILAL_WEST")) {
            newAm = getDgeWestL(newXCB, newAmB, eq);
        } else if (desc.contains("ILA_EAST")) {
            newAm = getDgeWestC(newXCB, newAmB, eq);
        } else if (desc.contains("ILA_WEST")) {
            newAm = getDgeWestL(newXCB, newAmB, eq);
        }
        newXCB.setAmplifier(newAm);
        return newXCB.build();
    }

    private Amplifier getDgeWestL(CrossConnectionsBuilder newXCB, AmplifierBuilder newAmB, Equipments eq) {
        return getDgeEastL(newXCB, newAmB, eq);
    }

    private Amplifier getDgeWestC(CrossConnectionsBuilder newXCB, AmplifierBuilder newAmB, Equipments eq) {
        return getDgeEastC(newXCB, newAmB, eq);
    }

    private Amplifier getDgeEastL(CrossConnectionsBuilder newXCB, AmplifierBuilder newAmB, Equipments eq) {
        ByteDanceSpecConfig.AmplifierProfile profile = config.getAmplifierProfile("DGE_EAST_L", currentWdmBand, currentGrid);
        Amplifier newAm = buildAmplifier("DGE_EAST_L", newAmB, profile, () -> getAmplifierGain(eq.getEquipmentId()));
        Properties newProp = addConfiguredProperties(newXCB.getProperties(), profile.properties);
        newXCB.setProperties(newProp);
        return newAm;
    }

    private Amplifier getDgeEastC(CrossConnectionsBuilder newXCB, AmplifierBuilder newAmB, Equipments eq) {
        ByteDanceSpecConfig.AmplifierProfile profile = config.getAmplifierProfile("DGE_EAST_C", currentWdmBand, currentGrid);
        Amplifier newAm = buildAmplifier("DGE_EAST_C", newAmB, profile, () -> getAmplifierGain(eq.getEquipmentId()));
        Properties newProp = addConfiguredProperties(newXCB.getProperties(), profile.properties);
        newXCB.setProperties(newProp);
        return newAm;
    }

    private CrossConnections otmOA(Equipments eq, CrossConnections xc) {
        return otmOa(eq, xc);
    }

    private CrossConnections roadmOA(Equipments eq, CrossConnections xc) {
        return otmOa(eq, xc);
    }

    private CrossConnections otmOa(Equipments eq, CrossConnections axmplifierXC) {
        CrossConnectionsBuilder newXCB = new CrossConnectionsBuilder(axmplifierXC);
        AmplifierBuilder newAmB = new AmplifierBuilder(axmplifierXC.getAmplifier());
        Amplifier newAm = null;
        String desc = axmplifierXC.getDescription();
        if (desc.endsWith("BAC")) {
            newAm = getOaBAC(newAmB);
        } else if (desc.endsWith("PAC")) {
            newAm = getOaPAC(newAmB, eq);
        } else if (desc.endsWith("BA")) {
            newAm = getOaBAC(newAmB);
        } else if (desc.endsWith("PA")) {
            newAm = getOaPAC(newAmB, eq);
        }
        newXCB.setAmplifier(newAm);
        return newXCB.build();
    }

    private CrossConnections roadmIRA(Equipments eq, CrossConnections xc) {
        return otmIRA(eq, xc);
    }

    private CrossConnections otmIRA(Equipments eq, CrossConnections axmplifierXC) {
        CrossConnectionsBuilder newXCB = new CrossConnectionsBuilder(axmplifierXC);
        AmplifierBuilder newAmB = new AmplifierBuilder(axmplifierXC.getAmplifier());
        Amplifier newAm = null;
        String desc = axmplifierXC.getDescription();
        if (desc.contains("BAC")) {
            newAm = getIraBAC(newXCB, newAmB);
        } else if (desc.contains("BAL")) {
            newAm = getIraBAL(newXCB, newAmB);
        } else if (desc.contains("PAC")) {
            newAm = getIraPAC(newXCB, newAmB, eq);
        } else if (desc.contains("PAL")) {
            newAm = getIraPAL(newXCB, newAmB, eq);
        } else if (desc.contains("BA")) {
            newAm = getIraBAC(newXCB, newAmB);
        } else if (desc.contains("PA")) {
            newAm = getIraPAC(newXCB, newAmB, eq);
        }
        newXCB.setAmplifier(newAm);
        return newXCB.build();
    }

    private Amplifier getOaBAC(AmplifierBuilder newAmB) {
        ByteDanceSpecConfig.AmplifierProfile profile = config.getAmplifierProfile("OA_BAC", currentWdmBand, currentGrid);
        newAmB.setProperties(null);
        return buildAmplifier("OA_BAC", newAmB, profile, null);
    }

    private Amplifier getOaPAC(AmplifierBuilder newAmB, Equipments eq) {
        ByteDanceSpecConfig.AmplifierProfile profile = config.getAmplifierProfile("OA_PAC", currentWdmBand, currentGrid);
        newAmB.setProperties(null);
        return buildAmplifier("OA_PAC", newAmB, profile, () -> getAmplifierGain(eq.getEquipmentId()));
    }

    private Amplifier getIraBAL(CrossConnectionsBuilder newXCB, AmplifierBuilder newAmB) {
        return getIraBAC(newXCB, newAmB);
    }

    private Amplifier getIraPAL(CrossConnectionsBuilder newXCB, AmplifierBuilder newAmB, Equipments eq) {
        return getIraPAC(newXCB, newAmB, eq);
    }

    private Amplifier getIraPAC(CrossConnectionsBuilder newXCB, AmplifierBuilder newAmB, Equipments eq) {
        ByteDanceSpecConfig.AmplifierProfile profile = config.getAmplifierProfile("IRA_PAC", currentWdmBand, currentGrid);
        Amplifier newAm = buildAmplifier("IRA_PAC", newAmB, profile, () -> getAmplifierGain(eq.getEquipmentId()));
        Properties newProp = addConfiguredProperties(newXCB.getProperties(), profile.properties);
        newXCB.setProperties(newProp);
        return newAm;
    }

    private float getAmplifierGain(String equipmentId) {
        List<String> plIds = rInfo.getPhyLinkIdList().stream().filter(x -> x.contains(equipmentId)).collect(Collectors.toList());
        if (plIds.isEmpty()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Impossible, cannot get physical link based on EqId. " + equipmentId);
        }
        List<String> otsLinkIds = plIds.stream().filter(PhysicalLinkIdNamingRule::isOtsLink).collect(Collectors.toList());
        if (transparentRoute.hasTransparentEndpoints()) {
            otsLinkIds = transparentRoute.getOtsLinkIds(equipmentId);
        }
        if (otsLinkIds.isEmpty()) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, "Impossible, cannot get OTS link based on EqId. " + equipmentId);
        }
        for (String otsLinkId : otsLinkIds) {
            Link pl = getChangedPhyLink(otsLinkId);
            if (pl == null || pl.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class) == null
                    || pl.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class).getPhysical() == null) {
                continue;
            }
            Provider provider = pl.getAugmentation(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Link1.class)
                .getPhysical().getProvider();
            if (provider == null) {
                continue;
            }
            // 透明穿越后仍按原 OTS 的 A/Z 方向取入向规划损耗，不能用 RAMAN ID 判断 IRA 方向。
            boolean sourceSide = transparentRoute.hasTransparentEndpoints()
                    ? transparentRoute.isSourceEquipment(otsLinkId, equipmentId)
                    : pl.getSource().getSourceTp().getValue().contains(equipmentId);
            BigDecimal attenuation = sourceSide
                    ? provider.getContractAttenuationZa() : provider.getContractAttenuationAz();
            if (attenuation != null) {
                // Bone2.0 TILA can have internal OTS links without span loss; use the real span link for gain.
                return attenuation.floatValue();
            }
        }
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "Impossible, cannot get OTS attenuation based on EqId. " + equipmentId);
    }

    private Amplifier getIraBAC(CrossConnectionsBuilder newXCB, AmplifierBuilder newAmB) {
        ByteDanceSpecConfig.AmplifierProfile profile = config.getAmplifierProfile("IRA_BAC", currentWdmBand, currentGrid);
        Amplifier newAm = buildAmplifier("IRA_BAC", newAmB, profile, null);
        Properties newProp = addConfiguredProperties(newXCB.getProperties(), profile.properties);
        newXCB.setProperties(newProp);
        return newAm;
    }

    private void iraAseRelated(Equipments eq) {
        String nodeId = PhysicalEqpIdNamingRule.getNodeId(eq.getEquipmentId());

        Node node = getChangedPhyNode(nodeId);
        Physical nodeAttr = node.getAugmentation(Node1.class).getPhysical();
        Optional<TerminationPoint> tpOp = node.getTerminationPoint().stream().filter(tp -> tp.getTpId().getValue().contains("EXP33")).findAny();
        if (!tpOp.isPresent()) {
            log.debug("cannot get EXP33 TP based on EqId. this is not an ASE supported" + eq.getEquipType().name());
            return;
        }
        TerminationPoint tp = tpOp.get();
        rInfo.getTpIdList().add(tp.getTpId().getValue());

        Properties newProp = addConfiguredProperties(eq.getProperties(), config.getEquipmentProperties("IRA_ASE"));

        Properties finalNewProp = newProp;
        List<Equipments> newEqList = nodeAttr.getEquipments().stream().map(x-> {
            if (x.getEquipmentId().equals(eq.getEquipmentId())) {
                return new EquipmentsBuilder(eq).setProperties(finalNewProp).build();
            } else {
                return x;
            }
        }).collect(Collectors.toList());

        Node newNode = new NodeBuilder(node).addAugmentation(Node1.class, new Node1Builder()
                .setPhysical(new PhysicalBuilder(nodeAttr).setEquipments(newEqList).build())
                .build())
            .build();

        defaultParamChanges.addChangedPhyNode(newNode);
    }

    private Amplifier buildAmplifier(String profileKey, AmplifierBuilder newAmB, ByteDanceSpecConfig.AmplifierProfile profile,
                                     GainValueProvider gainValueProvider) {
        profile.validate(profileKey);

        float targeGain = profile.resolveTargetGain(profileKey, gainValueProvider);
        newAmB.setTargetGain(BigDecimal.valueOf(targeGain));
        newAmB.setGainRange(profile.resolveGainRange(profileKey, targeGain));

        if (profile.targetGainTilt != null) {
            newAmB.setTargetGainTilt(BigDecimal.valueOf(profile.targetGainTilt));
        }
        newAmB.setAmpMode(profile.resolveAmpMode(profileKey));
        if (profile.autoPowerReduction != null) {
            newAmB.setAutoPowerReduction(profile.autoPowerReduction);
        }
        if (profile.targetAttenuation != null) {
            newAmB.setTargetAttenuation(BigDecimal.valueOf(profile.targetAttenuation));
        }
        if (profile.enable != null) {
            newAmB.setEnable(profile.enable);
        }
        return newAmB.build();
    }

    private Properties addConfiguredProperties(Properties baseProperties, Map<String, String> propertyMap) {
        return addConfiguredProperties(baseProperties, propertyMap, null);
    }

    private Properties addConfiguredProperties(Properties baseProperties, Map<String, String> propertyMap, Map<String, String> overrideValues) {
        Properties newProp = baseProperties;
        for (Map.Entry<String, String> entry : propertyMap.entrySet()) {
            String value = overrideValues != null && overrideValues.containsKey(entry.getKey())
                ? overrideValues.get(entry.getKey()) : entry.getValue();
            newProp = PropertyTool.addProperty(newProp, entry.getKey(), value);
        }
        return newProp;
    }

}
