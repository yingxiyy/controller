package net.flex.dci.otn.controller.nms.nms.component.resource.frequency.impl;


import static net.flex.dci.otn.controller.nms.utils.CommonUtils.convert2Int;
import static net.flex.dci.otn.controller.nms.utils.CommonUtils.convert2String;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.enums.GridFrequency;
import net.flex.dci.otc.common.enums.MuxType;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.OtCardType;
import net.flex.dci.otc.common.util.frequency.FrequencyAvailable;
import net.flex.dci.otc.common.util.frequency.WDM_Band;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.SiteLinkIdNamingRule;
import net.flex.dci.otn.controller.nms.enums.FrequencyState;
import net.flex.dci.otn.controller.nms.nms.component.resource.frequency.FrequencyMapWrapper;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.MuxSpectrum;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.MUX.FrequencyRange;
import net.flex.dci.otn.controller.nms.nms.dto.FrequencyMapDto;
import net.flex.dci.otn.controller.nms.nms.dto.MuxGeneralInfoDto;
import net.flex.dci.otn.controller.nms.nms.dto.MuxSpectrumDto;
import net.flex.dci.otn.controller.nms.utils.CommonUtils;
import net.flex.dci.otn.controller.nms.utils.Constants;
import net.flex.dci.otn.controller.nms.utils.NMSUtils;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.Scope;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.ScopeBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.ScopeKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.grouping.Map;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.grouping.MapBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.grouping.MapKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.Spectrum;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.SpectrumBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.FrequencyType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.och.link.attributes.Och;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.supported.links.SupportedLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.Site;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.link.attributes.site.Available;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.types.rev180515.cross.connection.route.sequence.CrossConnections;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/6/23 10:18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FrequencyMapWrapperImpl implements FrequencyMapWrapper {

    private final NetconfTopology netconfTopology;

    private final MuxSpectrum muxSpectrum;

    private GridType otCardGrid = GridType._75;

    @Override
    public FrequencyMapDto getTpsFrequencyMap(String neId, String tpId) {
        log.info("start to get tp frequency map from ne:{},tpId is:{}", neId, tpId);
        Node node = netconfTopology.getNeNode(neId);
        if (node == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR, String
                    .format("cannot find required Node %s", neId));
        }
        Physical nodePhysical = node.getAugmentation(Node1.class).getPhysical();
        String neName = nodePhysical.getFriendlyName();
        List<TerminationPoint> terminationPoints = node.getTerminationPoint();
        List<Equipments> equipments = nodePhysical.getEquipments();
        Optional<TerminationPoint> tpOp = terminationPoints.stream()
                .filter(tp -> tp.getTpId().getValue().equals(tpId))
                .findAny();
        if (!tpOp.isPresent()) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR, String
                    .format("cannot find required TP on %s(%s), %s",
                            neName, neId, tpId));
        }
        //refactor 原来的算法错了，应该是基于这个TP找到OCH link, 然后看接的是什么MUX板卡。 然后直接给出map
        Link ochLink = netconfTopology.getOchLinkByLinePort(neId, tpId);
//        NeYangModel neYangModel = retrieveOpticalNeYangModelAndGrid(ochLink);
        MuxGeneralInfoDto muxGeneralInfoDto = getMuxGeneralInfoByOchLink(ochLink);
        List<Link> siteLinks = filterSupportSiteLink(ochLink);
        if (siteLinks == null || siteLinks.isEmpty()) {
            log.warn("this TP used on och Link directly, without siteLink!");
            siteLinks = new ArrayList<>();
        }

        FrequencyMapDto frequencyMapDto = constructTpRefFrequencyMap(
                muxGeneralInfoDto.getNeYangModel(),
                ochLink, siteLinks,
                muxGeneralInfoDto.getGridType());

        return frequencyMapDto;
    }

    /**
     * map.start / map.end	从连续 scope 中推导（结合 initalList 定义的频段）	频段范围 map.state	当前 scope 中的唯一
     * implement-state（分段前提）	状态段标签 map.scope	每个中心频点作为 scope，内含 centre, lower, upper, implement-state
     * 等	子频点 avaFrequencies	List<Long>	所有 free 的中心频点（依 grid 类型对齐）
     * initalList	List<Available>	定义整个频谱支持范围 ochLists	所有已分配/下发的 OCH，提取中心频率 + 状态	标记 allocated /
     * implemented
     *
     * @param siteLinkIds
     * @param otLinePortGrid
     * @return
     */
    @Override
    public FrequencyMapDto getSiteLinksFrequencyMapBySiteLinkAndGrid(List<String> siteLinkIds,
            WDM_Band lPort_band,
            GridType otLinePortGrid) {
        log.info("start to get site links frequency map on OT Line port grid {}, from {}",
                otLinePortGrid, siteLinkIds);

        List<Link> siteLinks = netconfTopology.listAllSiteLinkByIds(siteLinkIds);
        if (siteLinks.size() != siteLinkIds.size()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "provided site link has been removed");
        }

        //获取已经占用了的och link (频点）
        //这里需要把假波相关的ochLink 去除
        List<Link> ochLists = netconfTopology.getBusinessOchLinksUnderSiteLinks(siteLinkIds);
//        List<Link> ochLists = netconfTopology.getOchLinksUnderSiteLinks(siteLinkIds);

        NeYangModel yangModel = NMSUtils.getYangModelBySiteLinks(siteLinks);
        WDM_Band linkBand = NMSUtils.getWDMBandBySiteLinks(siteLinks);
        GridType basicGrid = siteLinks.get(0).getAugmentation(Link1.class).getSite().getGrid();

        //基于所有复用段（flexGrid/fixGrid) 找出了重叠的频点
        List<Long> avaFrequencies = FrequencyAvailable.getFreeCentFrequency(siteLinks,
                otLinePortGrid);

        //以业务进来的第一个复用段为标准， 获取频谱范围
        List<Available> initalList = FrequencyAvailable.getInitializedAvailableList(yangModel,
                linkBand.equals(WDM_Band.C_L) ? lPort_band : linkBand, basicGrid);

        FrequencyMapDto frequencyMapDto = constructSiteLinksRefFrequencyMap(avaFrequencies,
                ochLists, initalList, otLinePortGrid);

        return frequencyMapDto;
    }

    private FrequencyMapDto constructSiteLinksRefFrequencyMap(List<Long> avaFrequencies,
            List<Link> ochLists, List<Available> initalList, GridType grid) {
        log.debug("start to construct frequency map");
        List<Map> mapList = FrequencyMap.constructSiteLinksRefFrequencyMap(avaFrequencies,
                ochLists, initalList, grid);

        FrequencyMapDto dto = FrequencyMapDto.builder()
                .grid(grid)
                .mapList(mapList)
                .build();

        return dto;
    }

    /**
     * get mux general info like yang model and grid type
     *
     * @param ochLink
     * @return
     */
    private MuxGeneralInfoDto getMuxGeneralInfoByOchLink(Link ochLink) {
        log.debug("get the och link ref mux general info,och linkId is:{}", ochLink.getLinkId());
        Och ochLinkAttribute = ochLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                .getOch();
        List<SupportingLink> supportingLinks = ochLink.getSupportingLink();
        List<String> supportSiteLinkIds = supportingLinks.stream()
                .map(SupportingLink::getLinkRef).map(Uri::getValue)
                .filter(SiteLinkIdNamingRule::isSiteLink).collect(
                        Collectors.toList());
        MuxGeneralInfoDto muxGeneralInfoDto = null;

        //todo: dge model for link
        if (supportSiteLinkIds.isEmpty()) {
            muxGeneralInfoDto = getMuxGeneralInfoByDGEOchLinkRefTp(
                    ochLink.getSource().getSourceTp().getValue());
        } else {
            muxGeneralInfoDto = getMuxGeneralInfoBySiteLink(supportSiteLinkIds.get(0));
        }
        if (muxGeneralInfoDto.getNeYangModel().equals(NeYangModel.ByteDance)) {
            FrequencyType upperFrequency = ochLinkAttribute.getUpperFrequency();
            FrequencyType lowerFrequency = ochLinkAttribute.getLowerFrequency();
            GridType gridType = getGridTypeByUpperAndLowerFrequency(upperFrequency, lowerFrequency);
            muxGeneralInfoDto.setGridType(gridType);
        }

        return muxGeneralInfoDto;
    }

    /**
     * get gread type by upper and lower frequence
     *
     * @param upperFrequency
     * @param lowerFrequency
     * @return
     */
    private GridType getGridTypeByUpperAndLowerFrequency(FrequencyType upperFrequency,
            FrequencyType lowerFrequency) {
        log.debug("get grid type by the upper:{} and lower:{}", upperFrequency, lowerFrequency);
        BigInteger gridFrequency = upperFrequency.getValue().subtract(lowerFrequency.getValue());
        int gridMhz = gridFrequency.divide(BigInteger.valueOf(1000)).intValue();
        return GridType.forValue(gridMhz);
    }

    /**
     * och link tp id
     *
     * @param tpId
     * @return
     */
    private MuxGeneralInfoDto getMuxGeneralInfoByDGEOchLinkRefTp(String tpId) {
        log.debug("get mux general info by ref tp :{}", tpId);
        String otCardId = PhysicalTpIdNamingRule.getEquipId(tpId);
        String electricNodeId = PhysicalNodeIdNamingRule.getNodeId(tpId);
        Node opticalNode = netconfTopology.getNeNode(electricNodeId);
        Physical nodePhysical = opticalNode.getAugmentation(Node1.class).getPhysical();
        NeYangModel neYangModel = NeYangModel.getModel(nodePhysical);
        List<Equipments> equipments = nodePhysical.getEquipments();
        Optional<Equipments> equipmentsOptional = equipments.stream()
                .filter(equip -> equip.getEquipmentId().equals(otCardId)).findAny();
        if (equipmentsOptional.isPresent()) {
            Equipments muxCard = equipmentsOptional.get();
            GridType gridType = detectiveFrequencyWidthByOtCard(muxCard);
            return MuxGeneralInfoDto.builder().neYangModel(neYangModel).gridType(gridType).build();
        } else {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("cannot find MUX card in optical node %s (%s)",
                            nodePhysical.getFriendlyName(), otCardId));
        }
    }


    /**
     * get mux general info by optical layer site link start with mux card tp
     *
     * @param siteLinkId
     * @return
     */
    private MuxGeneralInfoDto getMuxGeneralInfoBySiteLink(String siteLinkId) {
        log.debug("get mux general info by optical layer site link :{}", siteLinkId);
        Link siteLink = netconfTopology.getSiteLink(siteLinkId);
        String sourceTpId = siteLink.getSource().getSourceTp().getValue();
        String opticalNodeId = PhysicalTpIdNamingRule.getNodeId(sourceTpId);
        String muxCardId = PhysicalTpIdNamingRule.getEquipId(sourceTpId);
        Node opticalNode = netconfTopology.getNeNode(opticalNodeId);
        Physical nodePhysical = opticalNode.getAugmentation(Node1.class).getPhysical();
        NeYangModel neYangModel = NeYangModel.getModel(nodePhysical);
        List<Equipments> equipments = nodePhysical.getEquipments();
        Optional<Equipments> equipmentsOptional = equipments.stream()
                .filter(equip -> equip.getEquipmentId().equals(muxCardId)).findAny();

        if (equipmentsOptional.isPresent()) {
            Equipments muxCard = equipmentsOptional.get();
            GridType gridType = detectiveFrequencyWidthByMuxCard(muxCard);
            return MuxGeneralInfoDto.builder().neYangModel(neYangModel).gridType(gridType).build();
        } else {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    String.format("cannot find MUX card in optical node %s (%s)",
                            nodePhysical.getFriendlyName(), opticalNodeId));
        }

    }

    /**
     * ochLink 的交叉是光层的交叉，这个可以反应光层网元的yang模型
     *
     * @param ochLink
     * @return
     */
    private NeYangModel retrieveOpticalNeYangModelAndGrid(Link ochLink) {
        Och ochLinkAttr = ochLink.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                .getOch();
        List<CrossConnections> xcList = ochLinkAttr.getExplictRoute().getRoute().get(0).getPrimary()
                .getCrossConnections();
        String opticalNodeId = xcList.get(0).getNodeRef().getValue();

        Node node = netconfTopology.getNeNode(opticalNodeId);
        Physical nodePhysical = node.getAugmentation(Node1.class).getPhysical();
        NeYangModel neYangModel = NeYangModel.getModel(nodePhysical);

        String muxCardId = getMuxCardId(ochLink.getSource().getSourceTp().getValue(), ochLink);
        Optional<Equipments> eqOp = nodePhysical.getEquipments().parallelStream()
                .filter(x -> x.getEquipmentId().equals(muxCardId)).findFirst();
        if (eqOp.isPresent()) {
            Equipments eq = eqOp.get();
            String muxType = eq.getEquipTypeConfiged();
            switch (muxType) {
                case Constant.EquipmentClass.MUX48:
                    otCardGrid = GridType._100;
                    break;
                case Constant.EquipmentClass.MUX96:
                    otCardGrid = GridType._50;
                    break;
                case Constant.EquipmentClass.MUX64:
                    otCardGrid = GridType._75;
                    break;
                default:
                    otCardGrid = GridType._75;
//                case Constant.EquipmentClass.MUXPANEL:
            }
        } else {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "cannot find MUX card in node " + node.getNodeId().getValue());
        }
        return neYangModel;
    }

    private String getMuxCardId(String tpId, Link ochLink) {
        for (SupportingLink sl : ochLink.getSupportingLink()) {
            String osLinkId = sl.getLinkRef().getValue();
            if (sl.getLinkRef().getValue().contains(tpId)) {
                String aTpId = PhysicalLinkIdNamingRule.getTpAId(osLinkId);
                String muxTpId = aTpId;
                if (aTpId.equals(tpId)) {
                    muxTpId = PhysicalLinkIdNamingRule.getTpZId(osLinkId);
                }
                return PhysicalTpIdNamingRule.getEquipId(muxTpId);
            }
        }
        throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                "cannot find MUX card on ochLink " + ochLink.getLinkId().getValue());
    }

    private List<Link> filterSupportSiteLink(Link ochLink) {
        log.debug("filter and get support site link for och link :{}",
                ochLink.getLinkId().getValue());
        List<SupportingLink> supportingLinks = ochLink.getSupportingLink();
        List<String> siteLinkIds = supportingLinks.stream()
                .filter(supportingLink -> SiteLinkIdNamingRule.isSiteLink(
                        supportingLink.getLinkRef().getValue()))
                .map(supportingLink -> supportingLink.getLinkRef().getValue())
                .collect(Collectors.toList());
        List<Link> siteLinks = new ArrayList<>();
        if (!siteLinkIds.isEmpty()) {
            siteLinks = netconfTopology.listAllSiteLinkByIds(siteLinkIds);
        }
        return siteLinks;
    }

    private GridType getGridTypeInAllSiteLinks(List<Link> siteLinks) {
        GridType gridType = null;

        for (Link siteLink : siteLinks) {
            Site siteLinkAttr = siteLink
                    .getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite();
            if (gridType == null) {
                gridType = siteLinkAttr.getGrid();
            } else if (!siteLinkAttr.getGrid().equals(gridType)) {
                if (!gridType.equals(GridType._0) && !siteLinkAttr.getGrid().equals(GridType._0)) {
                    throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                            "route info error, all siteLink should be same grid");
                }
            }
            if (siteLinkAttr.getGrid().equals(GridType._0)) {
                gridType = GridType._0;
            }
        }
        return gridType;
    }

    /**
     * construct frequency map
     *
     * @param ochLink
     * @param siteLinks
     * @param gridType
     * @return
     */
    private FrequencyMapDto constructTpRefFrequencyMap(NeYangModel model, Link ochLink,
            List<Link> siteLinks,
            GridType gridType) {
        log.debug("construct ref frequency map");

        java.util.Map<Long, Link> usedFrequencyList;
        List<Long> freeCentFrequencyList;
        Short fixedChannelId = null;

        GridType siteLinkGrid = null;
        if (siteLinks.isEmpty()) {//无光层业务提取频谱的情况， 直接用的75 的grid
            usedFrequencyList = new HashMap<>();
            Och ochLinkAttr = ochLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                    .getOch();

            siteLinkGrid = GridType._75;
            FrequencyAvailable frequencyAvailable = new FrequencyAvailable(siteLinkGrid);
            long centFactor = FrequencyAvailable.getCentFactor(gridType);
            usedFrequencyList.put(
                    ochLinkAttr.getLowerFrequency().getValue().longValue() + centFactor, ochLink);
            freeCentFrequencyList = frequencyAvailable.getAllPossibleCentFrequency(gridType);
        } else {
            usedFrequencyList = getUsedFrequency(siteLinks);
            freeCentFrequencyList = FrequencyAvailable.getFreeCentFrequency(siteLinks, gridType);

            List<Link> flexGridLinks = siteLinks.stream()
                    .filter(link -> link.getAugmentation(Link1.class).getSite().getGrid()
                            .equals(GridType._0))
                    .collect(Collectors.toList());

            if (!flexGridLinks.isEmpty()) {
                fixedChannelId = NMSUtils.getOchLinkRefMuxPortIndex(model, ochLink);
            }
        }

        List<Long> allFreqency = new ArrayList<>();
        allFreqency.addAll(freeCentFrequencyList);
        allFreqency.addAll(usedFrequencyList.keySet());
        allFreqency.sort(Comparator.reverseOrder());

        int step = GridFrequency.getFrequencyByGrid(gridType).getStep();
        List<Map> frequencyMapList = new ArrayList<>();

        Integer channelId = 0;
        for (Long freq : allFreqency) {
            channelId++;
            if (usedFrequencyList.containsKey(freq)) {
                Link usedOchLink = usedFrequencyList.get(freq);
                Och ochLinkAttr = usedOchLink.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                        .getOch();

                List<Scope> scopeList = new ArrayList<>();
                Scope usedFreq = new ScopeBuilder()
                        .setLower(freq.intValue() - step)
                        .setUpper(freq.intValue() + step)
                        .setCentre(freq.intValue())
                        .setIndex(fixedChannelId == null ? channelId : fixedChannelId)
                        .setImplementState(ochLinkAttr.getImplementState())
                        .setKey(new ScopeKey(freq.intValue()))
                        .setOchLinkId(usedOchLink.getLinkId().getValue())
                        .setOchLinkFriendlyName(ochLinkAttr.getFriendlyName())
                        .build();
                scopeList.add(usedFreq);

                frequencyMapList.add(new MapBuilder()
                        .setStart(usedFreq.getLower())
                        .setEnd(usedFreq.getUpper())
                        .setState(ImplementState.Implement)
                        .setScope(scopeList)
                        .setKey(new MapKey(usedFreq.getLower()))
                        .build());
            } else {
                List<Scope> scopeList = new ArrayList<>();
                Scope usedFreq = new ScopeBuilder()
                        .setLower(freq.intValue() - step)
                        .setUpper(freq.intValue() + step)
                        .setCentre(freq.intValue())
                        .setIndex(fixedChannelId == null ? channelId : fixedChannelId)
                        .setImplementState(ImplementState.Plan)
                        .setKey(new ScopeKey(freq.intValue()))
                        .setOchLinkId(null)
                        .setOchLinkFriendlyName(null)
                        .build();
                scopeList.add(usedFreq);

                frequencyMapList.add(new MapBuilder()
                        .setStart(freq.intValue() - step)
                        .setEnd(freq.intValue() + step)
                        .setState(ImplementState.Plan)
                        .setScope(scopeList)
                        .setKey(new MapKey(freq.intValue() - step))
                        .build());
            }
        }

        return FrequencyMapDto.builder().grid(gridType).mapList(frequencyMapList).build();
    }

//    private Integer getGridStep(GridType otCardGrid) {
//        Integer step;
//
//        if (otCardGrid.equals(GridType._50)) {
//            step = 50000 / 2;
//        } else if (otCardGrid.equals(GridType._100)) {
//            step = 100000 / 2;
//        } else {
//            step = 75000 / 2;
//        }
//        return step;
//    }

    /**
     * export all ochLinks in given siteLinks
     *
     * @param siteLinks
     * @return
     */
    private java.util.Map<Long, Link> getUsedFrequency(List<Link> siteLinks) {
        java.util.Map<Long, Link> ochLinkList = new HashMap<>();
        siteLinks.forEach(siteLink -> {
            List<SupportedLink> sLink = siteLink.getAugmentation(
                            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Link1.class)
                    .getSite().getSupportedLink();
            sLink.forEach(supportedLink -> {
                Link ochLink = netconfTopology.getOchLink(supportedLink.getLinkRef().getValue());
                Och ochLinkAttr = ochLink.getAugmentation(
                                org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1.class)
                        .getOch();
                long cent = ochLinkAttr.getLowerFrequency().getValue().longValue() + (
                        ochLinkAttr.getUpperFrequency().getValue().longValue()
                                - ochLinkAttr.getLowerFrequency().getValue().longValue()
                ) / 2;
                ochLinkList.put(cent, ochLink);
            });
        });

        return ochLinkList;
    }

    private List<Map> buildScopeMap(SpectrumScopeNode spectrumScopeNode, Integer channelId,
            int frequencyWidth, GridType gridType) {
        log.debug("build the scope map ");
        List<Map> maps = new ArrayList<>();
        SpectrumScopeNode node = spectrumScopeNode;
        while (node != null) {
            int start = node.getStart();
            int end = node.getEnd();
            if (start == end) {
                node = node.next;
                continue;
            }
            FrequencyState state = node.getFrequencyState();
            MapBuilder mapBuilder = new MapBuilder();
            mapBuilder.setState(
                    state.getState().equals(Constants.FREQUENCY_FREE) ? ImplementState.Implement
                            : ImplementState.Plan);
            mapBuilder.setStart(start);
            mapBuilder.setEnd(end);
            mapBuilder.setScope(translate2Scopes(node, channelId, frequencyWidth, gridType));
            maps.add(mapBuilder.build());
            node = node.next;
        }
        return maps;

    }

    private List<Scope> translate2Scopes(SpectrumScopeNode node, Integer channelId,
            int frequencyWidth,
            GridType gridType) {

        FrequencyState frequencyState = node.getFrequencyState();
        List<Scope> scopes = new ArrayList<>();
        if (frequencyState.equals(FrequencyState.Free)) {
            if (gridType == GridType._0) {
                scopes = generateFreeScopesForFlex(channelId, node.getStart(), node.getEnd(),
                        frequencyWidth);
            } else {
                List<Spectrum> spectrumScope = node.getScope();
                scopes = spectrumScope.stream().map(spectrum -> {
                    ScopeBuilder sb = new ScopeBuilder();
                    sb.setLower(CommonUtils.convert2Int(spectrum.getLowerFrequency()));
                    sb.setUpper(CommonUtils.convert2Int(spectrum.getUpperFrequency()));
                    sb.setCentre(CommonUtils.convert2Int(spectrum.getCenterFrequency()));
                    sb.setIndex(spectrum.getIndex());
                    sb.setImplementState(ImplementState.Plan);
                    sb.setKey(new ScopeKey(sb.getCentre()));
                    return sb.build();
                }).collect(Collectors.toList());
            }
        } else {
            List<Spectrum> spectrumScope = node.getScope();
            scopes = spectrumScope.stream().map(spectrum -> {
                ScopeBuilder sb = new ScopeBuilder();
                sb.setLower(CommonUtils.convert2Int(spectrum.getLowerFrequency()));
                sb.setUpper(CommonUtils.convert2Int(spectrum.getUpperFrequency()));
                sb.setCentre(CommonUtils.convert2Int(spectrum.getCenterFrequency()));
                sb.setIndex(spectrum.getIndex());
                sb.setImplementState(
                        spectrum.getOchLink() != null ? spectrum.getOchLink().getImplementState()
                                : ImplementState.Plan);
                sb.setKey(new ScopeKey(sb.getCentre()));
                return sb.build();
            }).collect(Collectors.toList());

        }
        return scopes;
    }

    private List<Scope> generateFreeScopesForFlex(Integer channelId, Integer start, Integer end,
            int width) {
        List<Scope> scopes = new LinkedList<>();
        int preEnd = end;
        end = end + width;
        int step = 6250;
        for (int pos = start; pos >= end; pos -= step) {
            ScopeBuilder sb = new ScopeBuilder();
            sb.setLower(pos - width);
            sb.setUpper(pos);
            sb.setCentre(pos - width / 2);
            sb.setIndex(channelId);
            sb.setImplementState(ImplementState.Plan);
            sb.setKey(new ScopeKey(sb.getCentre()));

            scopes.add(sb.build());
        }
        if (start < end) {
            ScopeBuilder sb = new ScopeBuilder();
            sb.setLower(preEnd);
            sb.setUpper(start);
            sb.setCentre((preEnd + start) / 2);
            sb.setIndex(channelId);
            sb.setImplementState(ImplementState.Plan);
            sb.setKey(new ScopeKey(sb.getCentre()));
            scopes.add(sb.build());
        }

        return scopes;
    }

    private SpectrumScopeNode retrieveSpectrumScopeNode(MuxSpectrumDto spectrumDto) {
        log.debug("retrieve the spectrum :{}", spectrumDto);
        List<SpectrumScopeDetailNode> detailNodes = buildScopeDetailNodeList(
                spectrumDto);
        SpectrumScopeNode node = retrieveSpectrumScopeNodeDetail(spectrumDto.getGridType(),
                detailNodes);
        return node.next;
    }

    private SpectrumScopeNode retrieveSpectrumScopeNodeDetail(GridType gridType,
            List<SpectrumScopeDetailNode> detailNodes) {
        log.debug("start to retrieve the spectrum scope node detail");
        if (gridType.equals(GridType._0)) {
            return retrieveFlexGridSpectrumScopeNodeDetail(detailNodes);
        } else {
            return retrieveFixedGridSpectrumScopeNodeDetail(detailNodes);
        }
    }

    /**
     * retrieve fixed grid spectrum scope
     *
     * @param detailNodes
     * @return
     */
    private SpectrumScopeNode retrieveFixedGridSpectrumScopeNodeDetail(
            List<SpectrumScopeDetailNode> detailNodes) {
        SpectrumScopeNode head = new SpectrumScopeNode();
        SpectrumScopeNode node = new SpectrumScopeNode();
        head = node;
        for (SpectrumScopeDetailNode detailNode : detailNodes) {
            SpectrumScopeNode spectrumScopeNode = createSpectrumScopeNode(detailNode.getScope());
            node.next = spectrumScopeNode;
            node = spectrumScopeNode;
        }
        return head;
    }


    /**
     * flex grid spectrum
     *
     * @param detailNodes
     * @return
     */
    private SpectrumScopeNode retrieveFlexGridSpectrumScopeNodeDetail(
            List<SpectrumScopeDetailNode> detailNodes) {
        log.debug("retrieve flex grid ");
        SpectrumScopeDetailNode pre = null;
        SpectrumScopeNode head = new SpectrumScopeNode();
        SpectrumScopeNode node = new SpectrumScopeNode();
        head = node;
        for (SpectrumScopeDetailNode detailNode : detailNodes) {
            if (pre == null) {
                pre = detailNode;
            } else {
                FrequencyState detailNodeState = detailNode.getFrequencyState();
                FrequencyState prefDetailNodeState = pre.getFrequencyState();
                if (!detailNodeState.equals(prefDetailNodeState)) {
                    if (detailNodeState.equals(FrequencyState.BUSY) && prefDetailNodeState.equals(
                            FrequencyState.Free) || detailNodeState.equals(FrequencyState.Free)
                            && prefDetailNodeState.equals(FrequencyState.BUSY)) {
                        SpectrumScopeNode spectrumScopeNode = createSpectrumScopeNode(pre,
                                detailNode,
                                FrequencyState.Free);
                        node.next = spectrumScopeNode;
                        node = spectrumScopeNode;
                        pre = detailNode;
                    }
                } else {
                    FrequencyState prevScopeNodeState = node.getFrequencyState();
                    //state is same
                    int prefFrequency = pre.frequency;
                    int currentFrequency = detailNode.frequency;
                    if (detailNodeState.equals(FrequencyState.Free)) {
                        if (prevScopeNodeState.equals(FrequencyState.Free)) {
                            //update the scope node
                            node.end = detailNode.frequency;

                        } else if (prevScopeNodeState.equals(FrequencyState.BUSY)) {
                            //new scope node
                            SpectrumScopeNode spectrumScopeNode = createSpectrumScopeNode(pre,
                                    detailNode,
                                    FrequencyState.Free);
                            node.next = spectrumScopeNode;
                            node = spectrumScopeNode;
                            pre = detailNode;
                        }


                    } else if (detailNodeState.equals(FrequencyState.BUSY)) {
                        if (prevScopeNodeState.equals(FrequencyState.BUSY)) {
                            Spectrum preSpectrum = pre.scope;
                            Spectrum currentSpectrum = detailNode.scope;
                            if (preSpectrum.getName().equals(currentSpectrum.getName())) {
                                node.end = detailNode.frequency;
                                node.scope.add(detailNode.scope);
                            } else {

                                SpectrumScopeNode spectrumScopeNode = createSpectrumScopeNode(
                                        pre,
                                        detailNode,
                                        FrequencyState.Free);
                                node.next = spectrumScopeNode;
                                node = spectrumScopeNode;
                                pre = detailNode;

                            }

                        } else if (prevScopeNodeState.equals(FrequencyState.Free)) {
                            SpectrumScopeNode spectrumScopeNode = createSpectrumScopeNode(pre,
                                    detailNode,
                                    FrequencyState.BUSY);
                            node.next = spectrumScopeNode;
                            node = spectrumScopeNode;
                            pre = detailNode;
                        }
                    }
                }
            }
        }

        return head;
    }

    private SpectrumScopeNode createSpectrumScopeNode(SpectrumScopeDetailNode pre,
            SpectrumScopeDetailNode detailNode, FrequencyState frequencyState) {
        SpectrumScopeNode spectrumScopeNode = new SpectrumScopeNode();
        spectrumScopeNode.setStart(pre.frequency);
        spectrumScopeNode.setEnd(detailNode.frequency);
        spectrumScopeNode.setFrequencyState(frequencyState);
        List<Spectrum> scope = new ArrayList<>();
        if (frequencyState.equals(FrequencyState.BUSY)) {
            scope.add(detailNode.scope);
        }
        spectrumScopeNode.setScope(scope);
        return spectrumScopeNode;
    }

    private List<SpectrumScopeDetailNode> buildScopeDetailNodeList(MuxSpectrumDto spectrumDto) {
        log.debug("build spectrum scope detail list");
        GridType gridType = spectrumDto.getGridType();
        List<SpectrumScopeDetailNode> detailNodes = new ArrayList<>();
        if (gridType.equals(GridType._0)) {
            //flex grid
            detailNodes = buildFlexGridScopeNode(spectrumDto);
        } else {
            //fix grid
            detailNodes = buildFixGridScopeNode(spectrumDto);
        }

        List<SpectrumScopeDetailNode> sortedDetailNodes = detailNodes.stream()
                .sorted((detail1, detail2) -> detail2.centreFrequency.compareTo(
                        detail1.centreFrequency)).collect(Collectors.toList());
        return sortedDetailNodes;
    }

    private List<SpectrumScopeDetailNode> buildFixGridScopeNode(MuxSpectrumDto spectrumDto) {
        log.debug("build the fixed grid spectrum");
        List<Spectrum> spectrumList = spectrumDto.getSpectrumList();
        List<SpectrumScopeDetailNode> spectrumScopeDetailNodes = spectrumList.stream()
                .map(spectrum -> {
                    return createSpectrumScopeDetailNode(
                            CommonUtils.convert2Int(spectrum.getLowerFrequency()), spectrum);
                })
                .collect(
                        Collectors.toList());
        return spectrumScopeDetailNodes;
    }

    private List<SpectrumScopeDetailNode> buildFlexGridScopeNode(MuxSpectrumDto spectrumDto) {
        log.debug("build the flex grid spectrum");
        List<SpectrumScopeDetailNode> detailNodes = new ArrayList<>();
        List<Spectrum> spectrumList = spectrumDto.getSpectrumList();
        FrequencyRange range = spectrumDto.getFrequencyRange();

        boolean usedRangeLower = false;
        boolean usedRangeUpper = false;
        for (Spectrum spectrum : spectrumList) {
            int upper = CommonUtils.convert2Int(spectrum.getUpperFrequency());
            int lower = CommonUtils.convert2Int(spectrum.getLowerFrequency());
            if (upper == range.upper || upper == range.lower) {
                usedRangeUpper = true;
            }
            if (lower == range.upper || lower == range.lower) {
                usedRangeUpper = true;
            }
            SpectrumScopeDetailNode upperNode = createSpectrumScopeDetailNode(upper, spectrum);
            SpectrumScopeDetailNode lowerNode = createSpectrumScopeDetailNode(lower, spectrum);
            detailNodes.add(upperNode);
            detailNodes.add(lowerNode);
        }
        //add lower add upper
        if (!usedRangeLower) {
            SpectrumScopeDetailNode rangeLowerNode = createSpectrumScopeDetailNode(range.lower,
                    new SpectrumBuilder().setCenterFrequency(convert2String(range.lower)).build());
            detailNodes.add(rangeLowerNode);
        }
        if (!usedRangeUpper) {
            SpectrumScopeDetailNode rangeUpperNode = createSpectrumScopeDetailNode(range.upper,
                    new SpectrumBuilder().setCenterFrequency(convert2String(range.upper)).build());
            detailNodes.add(rangeUpperNode);
        }
        return detailNodes;
    }

//    private SpectrumScopeNode retrieveSpectrumScopeNode(List<Spectrum> spectrumList) {
//        log.debug("retrieve the spectrum list :{}", spectrumList);
//        Spectrum firstSpectrum = spectrumList.get(0);
//        SpectrumScopeNode head = new SpectrumScopeNode();
//        SpectrumScopeNode node = createSpectrumScopeNode(firstSpectrum);
//        head.next = node;
//        for (int i = 1; i < spectrumList.size(); i++) {
//            Spectrum spectrum = spectrumList.get(i);
//            FrequencyState frequencyState = detectFrequencyState(spectrum);
//            if (frequencyState.equals(node.frequencyState)) {
//                int upper = CommonUtils.convert2Int(spectrum.getUpperFrequency());
//                int lower = CommonUtils.convert2Int(spectrum.getLowerFrequency());
//
//                int start = node.getStart();
//                int end = node.getEnd();
//                if (upper >= start) {
//                    node.start = upper;
//                }
//
//                if (lower <= end) {
//                    node.end = lower;
//                }
//
//                List<Spectrum> scope = node.getScope();
//                scope.add(spectrum);
//                node.setScope(scope);
//            } else {
//                SpectrumScopeNode newNode = createSpectrumScopeNode(spectrum);
//                node.next = newNode;
//                node = newNode;
//            }
//        }
//        return head.next;
//    }

    private SpectrumScopeNode createSpectrumScopeNode(Spectrum spectrum) {
        SpectrumScopeNode spectrumScopeNode = new SpectrumScopeNode();
        spectrumScopeNode.setStart(convert2Int(spectrum.getUpperFrequency()));
        spectrumScopeNode.setEnd(convert2Int(spectrum.getLowerFrequency()));
        List<Spectrum> scope = new ArrayList<>();
        scope.add(spectrum);
        spectrumScopeNode.setScope(scope);
        FrequencyState frequencyState = detectFrequencyState(spectrum);
        spectrumScopeNode.setFrequencyState(frequencyState);
        return spectrumScopeNode;
    }

    private SpectrumScopeDetailNode createSpectrumScopeDetailNode(Integer frequency,
            Spectrum spectrum) {
        SpectrumScopeDetailNode scopeDetailNode = new SpectrumScopeDetailNode();

        scopeDetailNode.setScope(spectrum);
        scopeDetailNode.setFrequency(frequency);
        scopeDetailNode.setCentreFrequency(convert2Int(spectrum.getCenterFrequency()));
        FrequencyState frequencyState = detectFrequencyState(spectrum);
        scopeDetailNode.setFrequencyState(frequencyState);
        return scopeDetailNode;
//        SpectrumScopeNode spectrumScopeNode = new SpectrumScopeNode();
//        spectrumScopeNode.setStart(convert2Int(spectrum.getUpperFrequency()));
//        spectrumScopeNode.setEnd(convert2Int(spectrum.getLowerFrequency()));
//        List<Spectrum> scope = new ArrayList<>();
//        scope.add(spectrum);
//        spectrumScopeNode.setScope(scope);
//        FrequencyState frequencyState = detectFrequencyState(spectrum);
//        spectrumScopeNode.setFrequencyState(frequencyState);
//        return spectrumScopeNode;
    }


    private FrequencyState detectFrequencyState(Spectrum spectrum) {
        FrequencyState frequencyState = FrequencyState.Free;
        if (spectrum == null) {
            return frequencyState;
        }
        if (spectrum.getOchLink() != null) {
            frequencyState = FrequencyState.BUSY;
        }
        return frequencyState;
    }

    /**
     * get frequency width from equipment
     *
     * @param equipment
     * @return
     */
    private GridType detectiveFrequencyWidthByMuxCard(Equipments equipment) {
        String equipType = equipment.getEquipClass() == null ? equipment.getEquipTypeConfiged()
                : equipment.getEquipClass();
        GridType grid = MuxType.getGridTypeByMuxName(equipType);
        return grid;
    }

    private GridType detectiveFrequencyWidthByOtCard(Equipments equipment) {
        String equipType = equipment.getEquipClass() == null ? equipment.getEquipTypeConfiged()
                : equipment.getEquipClass();
        GridType grid = OtCardType.getGrid(equipType);
        return grid;
    }

    private void splitRange(List<MapBuilder> mapBList, Spectrum spec) {
        int lower = convert2Int(spec.getLowerFrequency());
        int upper = convert2Int(spec.getUpperFrequency());
        int centre = convert2Int(spec.getCenterFrequency());

        List<MapBuilder> newList = new LinkedList<>();
        for (MapBuilder builder : mapBList) {
            int start = builder.getStart();
            int end = builder.getEnd();

            if (builder.getState().equals(ImplementState.Implement)) {
                if (start == lower) {
                    //extend when merge,
                    builder.setStart(upper);
                    updateBusySpec(builder, lower, upper, centre, spec);
                } else if (end == upper) {
                    //extend when merge,
                    builder.setEnd(lower);
                    updateBusySpec(builder, lower, upper, centre, spec);
                }
            }
            if (builder.getState().equals(ImplementState.Plan)) {
                if (lower >= end && upper <= start) {
                    if (lower == end && upper == start) {
                        //the free scope equal to used och, update it directly.
                        builder.setState(ImplementState.Implement);
                        updateBusySpec(builder, lower, upper, centre, spec);
                        break;
                    } else {
                        //check split when cover the range
                        if (start == upper) {
                            //reduce the range
                            builder.setStart(lower);
                        } else if (end == lower) {
                            //reduce the range
                            builder.setEnd(upper);
                        } else {
                            builder.setEnd(upper);

                            MapBuilder newBuilder = new MapBuilder();
                            newBuilder.setState(ImplementState.Implement);
                            newBuilder.setStart(lower);
                            newBuilder.setEnd(end);
                            newList.add(newBuilder);
                        }
                        if (isNewBusyScope(mapBList, lower, upper, centre, spec)) {
                            MapBuilder newBuilder = new MapBuilder();
                            newBuilder.setState(ImplementState.Implement);
                            newBuilder.setStart(upper);
                            newBuilder.setEnd(lower);
                            updateBusySpec(newBuilder, lower, upper, centre, spec);
                            newList.add(newBuilder);
                        }
                    }
                }
            }
        }

        mapBList.addAll(newList);
    }


    private boolean isNewBusyScope(List<MapBuilder> mapBList, int lower, int upper, int centre,
            Spectrum spec) {
        boolean isNew = true;
        for (MapBuilder builder : mapBList) {
            if (builder.getState().equals(ImplementState.Implement)) {
                if (builder.getEnd() == upper) {
                    //扩展现有频段
                    builder.setEnd(lower);
                    updateBusySpec(builder, lower, upper, centre, spec);
                    isNew = false;
                } else if (builder.getStart() == lower) {
                    //扩展现有频段
                    builder.setStart(upper);
                    updateBusySpec(builder, lower, upper, centre, spec);
                    isNew = false;
                }
            }
        }
        return isNew;
    }

    private void updateBusySpec(MapBuilder map, int lower, int upper, int centre, Spectrum spec) {
        //扩充Busy频段中占用的频率值
        if (map.getScope() == null) {
            map.setScope(new LinkedList<>());
        }

        List<Scope> scopes = map.getScope();
        ScopeBuilder sb = new ScopeBuilder();
        sb.setLower(lower);
        sb.setUpper(upper);
        sb.setCentre(centre);
        sb.setIndex(spec.getIndex());
        sb.setImplementState(spec.getOchLink().getImplementState());
        sb.setKey(new ScopeKey(sb.getCentre()));
        scopes.add(sb.build());
    }


    @Data
    private static class SpectrumScopeNode {

        FrequencyState frequencyState;

        List<Spectrum> scope;

        Integer start;

        Integer end;

        SpectrumScopeNode next;

        SpectrumScopeNode prev;

    }

    @Data
    private static class SpectrumScopeDetailNode {

        FrequencyState frequencyState;

        Spectrum scope;


        Integer frequency;

        Integer centreFrequency;
    }

}
