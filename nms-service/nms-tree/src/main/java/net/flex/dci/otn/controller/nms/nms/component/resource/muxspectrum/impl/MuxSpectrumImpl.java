package net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.impl;

import static net.flex.dci.otn.controller.nms.utils.Constants.ASE_PREFIX;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalEqpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.nms.comparator.OchLinkSortComparator;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.MuxFactory;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.MuxSpectrum;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.MUX;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.Mux48;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.Mux64;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.Mux96;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.MuxFlex;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.MuxFlex32CL;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.Wss;
import net.flex.dci.otn.controller.nms.nms.dto.MuxSpectrumDto;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NMSUtils;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.Spectrum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.add.drop.link.lists.group.AddDropLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.AExternal;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/6/23 13:47
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MuxSpectrumImpl implements MuxSpectrum {

    private final NetconfTopology netconfTopology;

    private final MuxFactory muxFactory;

    @Override
    public MuxSpectrumDto getOpcNeSpectrum(String neId) {
        log.debug("start to get the ne spectrum,ne id is:{}", neId);
        Node opcNe = netconfTopology.getPhyNode(neId);
//        Physical nodeAttr = opcNe.getAugmentation(Node1.class).getPhysical();
//        model = NeYangModel.getModel(nodeAttr);

        Equipments muxEquipments = getMuxEquipmentsByNe(opcNe);
//        List<OchLink> refOchLink = getRefOchLinks(neId, muxEquipments);

        return null;
    }

    @Override
    public MuxSpectrumDto getSiteLinkSpectrum(Link siteLink) {
        log.debug("start to get detail for the site link spectrum ");
        String siteLinkId = siteLink.getLinkId().getValue();
        String sourceNeId = PhysicalTpIdNamingRule.getNodeId(
                siteLink.getSource().getSourceTp().getValue());

        Equipments muxEquipments = getMuxEquipmentsByNeId(sourceNeId);

        List<Link> ochLinks = netconfTopology.getOchLinksUnderSiteLinkId(siteLinkId);
        Physical nodeAttr = netconfTopology.getPhysical(sourceNeId);
        NeYangModel neYangModel = NeYangModel.getModel(nodeAttr);

        MUX mux = muxFactory.constructMux(neYangModel, muxEquipments, ochLinks);
        List<Spectrum> specList = extractSpectrum(mux);
        return MuxSpectrumDto.builder().gridType(mux.getGrid()).spectrumList(specList)
                .frequencyRange(mux.range()).mux(mux).build();
    }

    @Override
    public MuxSpectrumDto getSpectrumWithSiteLinks(List<String> siteLinkIds, GridType grid,
            WDM_Band band) {
        log.debug("start to get spectrum for siteLink: {}, grid is: {}, band is: {}", siteLinkIds,
                grid, band);
        List<Link> siteLinks = netconfTopology.listAllSiteLinkByIds(siteLinkIds);
        Equipments muxEquip = getMuxTypeBySiteLink(siteLinks.get(0));
//        List<Link> siteLinks = netconfTopology.listAllSiteLinkByIds(siteLinkIds);
        NeYangModel yangModel = NMSUtils.getYangModelBySiteLinks(siteLinks);
        List<Link> ochLinks = netconfTopology.getOchLinksUnderSiteLinks(siteLinkIds);
        MUX mux = muxFactory.constructMux(yangModel, muxEquip, ochLinks);
        List<Spectrum> specList = extractSpectrum(mux);
        return MuxSpectrumDto.builder().gridType(mux.getGrid()).spectrumList(specList)
                .frequencyRange(mux.range()).mux(mux).build();
    }


    /**
     * ira connect mux card with the external link
     *
     * @param siteLink
     * @return
     */
    private Equipments getMuxTypeBySiteLink(Link siteLink) {
        Site siteLinkPhysical = siteLink.getAugmentation(Link1.class).getSite();
        Equipments equipments = null;
        if (Objects.isNull(siteLinkPhysical.getAExternal())) {
            String sourceTpId = siteLink.getSource().getSourceTp().getValue();
            String equipmentId = PhysicalTpIdNamingRule.getEquipId(sourceTpId);
            equipments = netconfTopology.getEquipment(equipmentId);
        } else {
            equipments = getConnectedMuxEquipmentsFromExternalLink(siteLinkPhysical.getAExternal());
        }
        return equipments;
//        if (siteLink.getAugmentation(Link1.class).getSite().getAExternal() == null) {
//            //this is normal siteLink, get muxType from aTp of siteLink
//            String tpId = siteLink.getSource().getSourceTp().getValue();
//            String eqId = PhysicalTpIdNamingRule.getEquipId(tpId);
//            String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
//            Equipments eq = netconfTopology.getEquipment(nodeId, eqId);
//            return eq.getEquipType();
//        } else {
//            return siteLink.getAugmentation(Link1.class).getSite().getAExternal().getAddDropLink()
//                    .get(0).getConnnectorType();
//        }
    }


    @Override
    public MuxSpectrumDto getEquipRefSpectrum(String neId, String equipId) {
        log.debug("start to get spectrum for the equip:{} ,node is :{} spectrum", equipId, neId);
//        Node node = netconfTopology.getNeNode(neId);
        Physical nodeAttr = netconfTopology.getPhysical(neId);
//        model = NeYangModel.getModel(nodeAttr);
        NeYangModel neYangModel = NeYangModel.getModel(nodeAttr);
        Optional<Equipments> equipmentOptional = nodeAttr.getEquipments().stream()
                .filter(x -> x.getEquipmentId().equals(equipId)).findAny();
        if (equipmentOptional.isPresent()) {
            Equipments muxEquipment = equipmentOptional.get();
            List<Link> phyLinks = netconfTopology.getEquipmentRefPhyLinks(equipId);
            List<String> phyLinkIds = phyLinks.stream()
                    .map(phyLink -> phyLink.getLinkId().getValue())
                    .collect(
                            Collectors.toList());
            List<Link> ochLinks = netconfTopology.getOchLinksBasedOnPhyLinks(phyLinkIds);
            MUX mux = muxFactory.constructMux(neYangModel, muxEquipment, ochLinks);
            List<Spectrum> specList = extractSpectrum(mux);
            return MuxSpectrumDto.builder().gridType(mux.getGrid()).spectrumList(specList)
                    .frequencyRange(mux.range()).build();
        } else {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "cannot find required equip " + equipId);
        }
    }

    private Equipments getConnectedMuxEquipmentsFromExternalLink(AExternal aExternal) {
        List<String> muxConnectLinkIds = aExternal.getAddDropLink().stream()
                .map(AddDropLink::getLinkRef).collect(
                        Collectors.toList());
        String muxConnectLinkId = muxConnectLinkIds.get(0);
        String sourceTpId = PhysicalLinkIdNamingRule.getTpAId(muxConnectLinkId);
        String sourceEqId = PhysicalTpIdNamingRule.getEquipId(sourceTpId);
        Equipments muxEquipment = netconfTopology.getEquipment(sourceEqId);
        return muxEquipment;
    }


    private MUX constructMux(NeYangModel model, Equipments muxEquipments, List<Link> ochLinks) {
        log.debug("construct Mux by yang mode:{} equipment id:{}", model,
                muxEquipments.getEquipmentId());
        EquipType equipType = muxEquipments.getEquipType();

        String equipTypeConfig = muxEquipments.getEquipTypeConfiged();  //后期为了兼容其他厂家，这个地方应该用equipType
        if (equipTypeConfig.contains(Constants.WSS)) {
            List<CrossConnections> refCrossConnections = getRefEquipmentCrossConnections(
                    muxEquipments.getEquipmentId());
            return new Wss(model, muxEquipments.getEquipmentId(), ochLinks, refCrossConnections);
        }
        return constructMux(model, muxEquipments.getEquipmentId(), equipTypeConfig, ochLinks);
    }

    private MUX constructMux(NeYangModel model, String equipmentId, String equipTypeConfig,
            List<Link> ochLinks) {
        MUX mux = null;
        if (equipTypeConfig.contains(Constants.MUX_SPECTRUM_FLEX_32CL)) {
            ochLinks.sort(new OchLinkSortComparator(model));
            List<Link> realOchList = ochLinks.stream()
                    .filter(link -> !link.getAugmentation(
                                    org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                            .getOch().getFriendlyName()
                            .startsWith(ASE_PREFIX)).collect(Collectors.toList());
            mux = new MuxFlex32CL(model, equipmentId, realOchList);
        } else if (equipTypeConfig.contains(Constants.MUX_SPECTRUM_FLEX)) {
            ochLinks.sort(new OchLinkSortComparator(model));
            mux = new MuxFlex(model, equipmentId, ochLinks);
        } else if (equipTypeConfig.contains(Constants.MUX_SPECTRUM_48)) {
            mux = new Mux48(model, equipmentId, ochLinks);
        } else if (equipTypeConfig.contains(Constants.MUX_SPECTRUM_64)) {
            mux = new Mux64(model, equipmentId, ochLinks);
        } else if (equipTypeConfig.contains(Constants.MUX_SPECTRUM_96)) {
            mux = new Mux96(model, equipmentId, ochLinks);
        } else {
            String msg = String.format("unknown equip class %s", equipTypeConfig);
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR, msg);
        }
        return mux;
    }

    /**
     * get ref equipment cross connection for the equipment
     *
     * @param equipmentId
     * @return
     */
    private List<CrossConnections> getRefEquipmentCrossConnections(String equipmentId) {
        String refNodeId = PhysicalEqpIdNamingRule.getNodeId(equipmentId);
        Node phyNode = netconfTopology.getPhyNode(refNodeId);
        List<CrossConnections> crossConnections = phyNode.getAugmentation(
                Node1.class).getPhysical().getCrossConnections();
        List<CrossConnections> refCrossConnections = crossConnections.stream()
                .filter(xc -> xc.getCrossConnectionId().getValue().contains(equipmentId)).collect(
                        Collectors.toList());
        return refCrossConnections;
    }


    private Equipments getMuxEquipmentsByNeId(String neId) {
        log.debug("start to get ref mux equipments by neId :{}", neId);
        Node opcNe = netconfTopology.getPhyNode(neId);
        Physical nodeAttr = opcNe.getAugmentation(Node1.class).getPhysical();
//        model = NeYangModel.getModel(nodeAttr);
        Equipments muxEquipments = getMuxEquipmentsByNe(opcNe);
        return muxEquipments;
    }


    private Equipments getMuxEquipmentsByNe(Node ne) {
        List<Equipments> equipments = ne.getAugmentation(Node1.class).getPhysical().getEquipments();
        return equipments.stream()
                .filter(equipment -> equipment.getEquipType().equals(EquipType.MUXPANEL)
                        || equipment.getEquipType().equals(EquipType.MUX)).findAny().get();
    }

//    private List<OchLink> getRefOchLinks(String neId, Equipments muxEquipments) {
//        log.debug("start to get ref och link ,the neId :{},equipId:{}", neId,
//                muxEquipments.getEquipmentId());
//        List<Link> phyLinks = netconfTopology.getEquipmentRefPhyLinks(
//                muxEquipments.getEquipmentId());
//        List<String> phyLinkIds = phyLinks.stream().map(phyLink -> phyLink.getLinkId().getValue())
//                .collect(
//                        Collectors.toList());
//        List<Link> ochLinks = netconfTopology.getOchLinksUnderByPhyLinks(phyLinkIds);
//    }

    private List<Spectrum> extractSpectrum(MUX mux) {
        List<Spectrum> spectrums = new ArrayList<>();
        while (mux.hasNext()) {
            Spectrum frequency = mux.getNextFrequency();
            if (frequency != null) {
                spectrums.add(frequency);
            }
        }
        return spectrums;
    }

//    private class SortByName implements Comparator<Link> {
//
//        private MuxCardPortFormatting formatting;
//
//        public SortByName(NeYangModel model) {
//            formatting = new MuxCardPortFormatting(model);
//        }
//
//        @Override
//        public int compare(Link och0, Link och1) {
//
//            int aId = getIndex(och0);
//            int bId = getIndex(och1);
//            return aId - bId;
//        }
//
//        private int getIndex(Link link) {
//            if (link.getSupportingLink() != null) {
//                for (SupportingLink sLink : link.getSupportingLink()) {
//                    Pattern pattern = Pattern.compile(formatting.getPortMatcingRegex());
//                    Matcher matcher = pattern.matcher(sLink.getLinkRef().getValue());
//                    if (matcher.find()) {
//                        String name = matcher.group(0);
//                        String[] ids = name.split(formatting.getKeyBeforChannelNo());
//                        return Integer.parseInt(ids[1]);
//                    }
//                }
//            }
//            return 0;
//        }

}
