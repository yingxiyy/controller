package net.flex.dci.otn.controller.nms.nms.component.amplifier;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteNodeDao;
import net.flex.dci.otn.controller.nms.nms.dto.link.OtsLinkAmplifierRefCache;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.DirectionAmplifierInfo;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.OtsLinkAmplifierCLInfo;
import net.flex.dci.otn.controller.nms.nms.dto.omslink.OtsLinkRamanInfo;
import net.flex.dci.otn.controller.nms.nms.enums.CustomAmplifierEquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.site.node.attributes.Site;
import org.springframework.stereotype.Component;

/**
 * 2025/8/11
 *
 * @author musa
 * @version 1.0
 **/
@Component
@Slf4j
@RequiredArgsConstructor
public class AmplifierHandlerImpl implements AmplifierHandler {

    private final PhyNodeDao phyNodeDao;

    private final SiteNodeDao siteNodeDao;


    @Override
    public OtsLinkAmplifierCLInfo getOtsLinkSourceBACLInfoByTp(String terminationPointId,
            OtsLinkAmplifierRefCache refCache) {
        log.debug("get ref ots link BAC BAL info by source tp :{}", terminationPointId);
        TpRefElementInfoDto tpRefElementInfoDto = getTpRefElementInfoByTp(terminationPointId,
                refCache);
        String neId = tpRefElementInfoDto.getNeId();

        List<CrossConnections> innerCrossConnections = tpRefElementInfoDto.getInnerCrossConnections();
        Equipments refEquipment = tpRefElementInfoDto.getEquipments();
        List<CrossConnections> amplifierCLXcs = innerCrossConnections.stream()
                .filter(xc -> xc.getAmplifier() != null)
                .filter(crossConnection -> crossConnection.getSourceTp().stream()
                        .map(sourceTp -> sourceTp.getTpRef().getValue())
                        .collect(Collectors.toList())
                        .contains(terminationPointId) ||
                        crossConnection.getDestinationTp().stream()
                                .map(destTp -> destTp.getTpRef().getValue())
                                .collect(Collectors.toList())
                                .contains(terminationPointId))
                .collect(Collectors.toList());
        CustomAmplifierEquipType customAmplifierEquipType = CustomAmplifierEquipType.fromEquipType(
                refEquipment.getEquipType());
        SiteInfo siteInfo = getRefSiteInfo(neId, refCache.getPhyNodeSiteNodeMap());
        //BAC
        CrossConnections BACXc = amplifierCLXcs.stream().filter(xc -> xc.getDescription().contains(
                customAmplifierEquipType.getSourceBACSuffix())).findAny().orElse(null);
        DirectionAmplifierInfo cAmplifierInfo = DirectionAmplifierInfo.builder()
                .name(BACXc.getDescription()).neId(neId)
                .crossConnectionId(BACXc.getCrossConnectionId().getValue())
                .amplifierXc(BACXc)
                .build();
        //BAL
        CrossConnections BALXc = customAmplifierEquipType.getSourceBALSuffix() == null ? null
                : amplifierCLXcs.stream().filter(xc -> xc.getDescription().contains(
                        customAmplifierEquipType.getSourceBALSuffix())).findAny().orElse(null);
        DirectionAmplifierInfo
                lAmplifierInfo = BALXc == null ? null : DirectionAmplifierInfo.builder()
                .name(BALXc.getDescription()).neId(neId)
                .crossConnectionId(BALXc.getCrossConnectionId().getValue())
                .amplifierXc(BALXc)
                .build();

        return OtsLinkAmplifierCLInfo.builder().tpId(terminationPointId).nodeId(neId)
                .neName(tpRefElementInfoDto.getNeFriendlyName())
                .tpName(tpRefElementInfoDto.getTpFriendlyName())
                .ip(tpRefElementInfoDto.getIp())
                .siteId(siteInfo.siteId)
                .siteName(siteInfo.siteName)
                .amplifierC(cAmplifierInfo)
                .amplifierL(lAmplifierInfo)
                .build();
    }

    private SiteInfo getRefSiteInfo(String neId, Map<String, Node> phyNodeSiteNodeMap) {
        String siteId = PhysicalNodeIdNamingRule.getSiteId(neId);
        Node refSite = phyNodeSiteNodeMap.get(siteId);
        Site sitePhysical = refSite.getAugmentation(
                        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.Node1.class)
                .getSite();
        String siteName = sitePhysical.getFriendlyName();
        return SiteInfo.builder().siteId(siteId).siteName(siteName).build();
    }


    @Override
    public OtsLinkAmplifierCLInfo getOtsLinkDestBACLInfoByTp(String terminationPointId,
            OtsLinkAmplifierRefCache refCache) {
        log.debug("get ref ots link BAC BAL info by destination tp :{}", terminationPointId);
        TpRefElementInfoDto tpRefElementInfoDto = getTpRefElementInfoByTp(terminationPointId,
                refCache);
        String neId = tpRefElementInfoDto.getNeId();
        SiteInfo siteInfo = getRefSiteInfo(neId, refCache.getPhyNodeSiteNodeMap());
        List<CrossConnections> innerCrossConnections = tpRefElementInfoDto.getInnerCrossConnections();
        List<CrossConnections> amplifierCLXcs = innerCrossConnections.stream()
                .filter(xc -> xc.getAmplifier() != null)
                .filter(crossConnection -> crossConnection.getSourceTp().stream()
                        .map(sourceTp -> sourceTp.getTpRef().getValue())
                        .collect(Collectors.toList())
                        .contains(terminationPointId) ||
                        crossConnection.getDestinationTp().stream()
                                .map(destTp -> destTp.getTpRef().getValue())
                                .collect(Collectors.toList())
                                .contains(terminationPointId))
                .collect(Collectors.toList());
        CustomAmplifierEquipType customAmplifierEquipType = CustomAmplifierEquipType.fromEquipType(
                tpRefElementInfoDto.equipments.getEquipType());
        //BAC
        CrossConnections BACXc = amplifierCLXcs.stream().filter(xc -> xc.getDescription().contains(
                customAmplifierEquipType.getDestBACSuffix())).findAny().orElse(null);
        DirectionAmplifierInfo cAmplifierInfo = DirectionAmplifierInfo.builder()
                .name(BACXc.getDescription()).neId(neId)
                .crossConnectionId(BACXc.getCrossConnectionId().getValue())
                .amplifierXc(BACXc)
                .build();
        //BAL
        CrossConnections BALXc = customAmplifierEquipType.getDestBALSuffix() == null ? null
                : amplifierCLXcs.stream().filter(xc -> xc.getDescription().contains(
                        customAmplifierEquipType.getDestBALSuffix())).findAny().orElse(null);

        DirectionAmplifierInfo lAmplifierInfo =
                BALXc == null ? null : DirectionAmplifierInfo.builder()
                        .name(BALXc.getDescription()).neId(neId)
                        .crossConnectionId(BALXc.getCrossConnectionId().getValue())
                        .amplifierXc(BALXc)
                        .build();
        return OtsLinkAmplifierCLInfo.builder().tpId(terminationPointId).nodeId(neId)
                .neName(tpRefElementInfoDto.getNeFriendlyName())
                .tpName(tpRefElementInfoDto.getTpFriendlyName())
                .ip(tpRefElementInfoDto.getIp())
                .siteId(siteInfo.siteId)
                .siteName(siteInfo.siteName)
                .amplifierC(cAmplifierInfo)
                .amplifierL(lAmplifierInfo)
                .build();
    }

    @Override
    public OtsLinkAmplifierCLInfo getOtsLinkSourcePACLInfoByTp(String terminationPointId,
            OtsLinkAmplifierRefCache refCache) {
        log.debug("get ref ots link BAC BAL info by source tp :{}", terminationPointId);
        TpRefElementInfoDto tpRefElementInfoDto = getTpRefElementInfoByTp(terminationPointId,
                refCache);
        String neId = tpRefElementInfoDto.getNeId();
        SiteInfo siteInfo = getRefSiteInfo(neId, refCache.getPhyNodeSiteNodeMap());
        List<CrossConnections> innerCrossConnections = tpRefElementInfoDto.getInnerCrossConnections();
        Equipments refEquipment = tpRefElementInfoDto.getEquipments();
        List<CrossConnections> amplifierCLXcs = innerCrossConnections.stream()
                .filter(crossConnections -> crossConnections.getSourceTp() != null)
                .filter(crossConnection -> crossConnection.getSourceTp().stream()
                        .map(sourceTp -> sourceTp.getTpRef().getValue())
                        .collect(Collectors.toList())
                        .contains(terminationPointId) ||
                        crossConnection.getDestinationTp().stream()
                                .map(destTp -> destTp.getTpRef().getValue())
                                .collect(Collectors.toList())
                                .contains(terminationPointId))
                .filter(xc -> xc.getAmplifier() != null)
                .collect(Collectors.toList());
        CustomAmplifierEquipType customAmplifierEquipType = CustomAmplifierEquipType.fromEquipType(
                refEquipment.getEquipType());
        if (customAmplifierEquipType.relayBoard()) {
            log.warn("current TP:{} ref card type is relayBoardCard,do not have PACL",
                    terminationPointId);
            return null;
        }
        //PAC
        CrossConnections PACXc = amplifierCLXcs.stream().filter(xc -> xc.getDescription().contains(
                customAmplifierEquipType.getSourcePACSuffix())).findAny().orElse(null);
        DirectionAmplifierInfo cAmplifierInfo = DirectionAmplifierInfo.builder()
                .name(PACXc.getDescription()).neId(neId)
                .crossConnectionId(PACXc.getCrossConnectionId().getValue())
                .amplifierXc(PACXc)
                .build();
        //PAL
        CrossConnections PALXc = customAmplifierEquipType.getSourcePALSuffix() == null ? null
                : amplifierCLXcs.stream().filter(xc -> xc.getDescription().contains(
                        customAmplifierEquipType.getSourcePALSuffix())).findAny().orElse(null);

        DirectionAmplifierInfo lAmplifierInfo =
                PALXc == null ? null : DirectionAmplifierInfo.builder()
                        .name(PALXc.getDescription()).neId(neId)
                        .crossConnectionId(PALXc.getCrossConnectionId().getValue())
                        .amplifierXc(PALXc)
                        .build();
        return OtsLinkAmplifierCLInfo.builder().tpId(terminationPointId).nodeId(neId)
                .neName(tpRefElementInfoDto.getNeFriendlyName())
                .tpName(tpRefElementInfoDto.getTpFriendlyName())
                .ip(tpRefElementInfoDto.getIp())
                .siteId(siteInfo.siteId)
                .siteName(siteInfo.siteName)
                .amplifierC(cAmplifierInfo)
                .amplifierL(lAmplifierInfo)
                .build();
    }

    @Override
    public OtsLinkAmplifierCLInfo getOtsLinkDestPACLInfoByTp(String terminationPointId,
            OtsLinkAmplifierRefCache refCache) {
        log.debug("get ref ots link BAC BAL info by source tp :{}", terminationPointId);
        TpRefElementInfoDto tpRefElementInfoDto = getTpRefElementInfoByTp(terminationPointId,
                refCache);
        String neId = tpRefElementInfoDto.getNeId();
        SiteInfo siteInfo = getRefSiteInfo(neId, refCache.getPhyNodeSiteNodeMap());
        List<CrossConnections> innerCrossConnections = tpRefElementInfoDto.getInnerCrossConnections();
        Equipments refEquipment = tpRefElementInfoDto.getEquipments();
        List<CrossConnections> amplifierCLXcs = innerCrossConnections.stream()
                .filter(xc -> xc.getAmplifier() != null)
                .filter(crossConnection -> crossConnection.getSourceTp().stream()
                        .map(sourceTp -> sourceTp.getTpRef().getValue())
                        .collect(Collectors.toList())
                        .contains(terminationPointId) ||
                        crossConnection.getDestinationTp().stream()
                                .map(destTp -> destTp.getTpRef().getValue())
                                .collect(Collectors.toList())
                                .contains(terminationPointId))

                .collect(Collectors.toList());
        CustomAmplifierEquipType customAmplifierEquipType = CustomAmplifierEquipType.fromEquipType(
                refEquipment.getEquipType());
        if (customAmplifierEquipType.relayBoard()) {
            log.warn("current TP:{} ref card type is relayBoardCard,do not have PACL",
                    terminationPointId);
            return null;
        }
        //PAC
        CrossConnections PACXc = amplifierCLXcs.stream().filter(xc -> xc.getDescription().contains(
                customAmplifierEquipType.getDestPACSuffix())).findAny().orElse(null);
        DirectionAmplifierInfo cAmplifierInfo = DirectionAmplifierInfo.builder()
                .name(PACXc.getDescription()).neId(neId)
                .crossConnectionId(PACXc.getCrossConnectionId().getValue())
                .amplifierXc(PACXc)
                .build();
        //PAL
        CrossConnections PALXc = customAmplifierEquipType.getDestPALSuffix() == null ? null
                : amplifierCLXcs.stream().filter(xc -> xc.getDescription().contains(
                        customAmplifierEquipType.getDestPALSuffix())).findAny().orElse(null);
        DirectionAmplifierInfo lAmplifierInfo =
                PALXc == null ? null : DirectionAmplifierInfo.builder()
                        .name(PALXc.getDescription()).neId(neId)
                        .crossConnectionId(PALXc.getCrossConnectionId().getValue())
                        .amplifierXc(PALXc)
                        .build();
        return OtsLinkAmplifierCLInfo.builder().tpId(terminationPointId).nodeId(neId)
                .neName(tpRefElementInfoDto.getNeFriendlyName())
                .tpName(tpRefElementInfoDto.getTpFriendlyName())
                .ip(tpRefElementInfoDto.getIp())
                .siteId(siteInfo.siteId)
                .siteName(siteInfo.siteName)
                .amplifierC(cAmplifierInfo)
                .amplifierL(lAmplifierInfo)
                .build();
    }

    @Override
    public OtsLinkRamanInfo getOtsLinkRamanInfoByTp(String ramanTpId,
            OtsLinkAmplifierRefCache refCache) {
        log.debug("get ref ots link raman info by source tp :{}", ramanTpId);
        if (ramanTpId == null) {
            return null;
        }
        TpRefElementInfoDto tpRefElementInfoDto = getTpRefElementInfoByTp(ramanTpId,
                refCache);
        String neId = tpRefElementInfoDto.getNeId();
        List<CrossConnections> innerCrossConnections = tpRefElementInfoDto.getInnerCrossConnections();
//        Equipments refEquipment = tpRefElementInfoDto.getEquipments();
        List<CrossConnections> amplifierXcs = innerCrossConnections.stream()
                .filter(xc -> xc.getAmplifier() != null)
                .filter(crossConnection -> crossConnection.getSourceTp().stream()
                        .map(sourceTp -> sourceTp.getTpRef().getValue())
                        .collect(Collectors.toList())
                        .contains(ramanTpId) ||
                        crossConnection.getDestinationTp().stream()
                                .map(destTp -> destTp.getTpRef().getValue())
                                .collect(Collectors.toList())
                                .contains(ramanTpId))

                .collect(Collectors.toList());
        CrossConnections ramanAmplifierXc = amplifierXcs.get(0);
        return OtsLinkRamanInfo.builder().neId(neId).name(ramanAmplifierXc.getDescription())
                .crossConnectionId(ramanAmplifierXc.getCrossConnectionId().getValue())
                .amplifierXc(ramanAmplifierXc)
                .build();
    }


    private TpRefElementInfoDto getTpRefElementInfoByTp(String terminationPointId,
            OtsLinkAmplifierRefCache refCache) {
        String neId = PhysicalTpIdNamingRule.getNodeId(terminationPointId);
        String equipId = PhysicalTpIdNamingRule.getEquipId(terminationPointId);
        Map<String, Node> phyNodeMap = refCache.getPhyNodeMap();
        Node ne = phyNodeMap.get(neId);
        if (ne == null) {
            log.error("the termination point :{} physically located on the ne:{} is not existed",
                    terminationPointId, neId);
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR, String.format(
                    "the termination point %s physically located on the ne is not existed",
                    terminationPointId));
        }
        Physical nePhysical = ne.getAugmentation(Node1.class).getPhysical();
        String neFriendlyName = nePhysical.getFriendlyName();
        String ip = nePhysical.getIp();
        List<CrossConnections> innerCrossConnections = nePhysical.getCrossConnections();
        List<Equipments> equipments = nePhysical.getEquipments();
        List<TerminationPoint> terminationPoints = ne.getTerminationPoint();
        TerminationPoint refTerminationPoint = terminationPoints.stream()
                .filter(terminationPoint -> terminationPoint.getTpId().getValue()
                        .equals(terminationPointId)).findAny().orElse(null);
        if (refTerminationPoint == null) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    String.format("The termination point %s is not founded", terminationPointId));
        }
        String tpFriendlyName = refTerminationPoint.getAugmentation(TerminationPoint1.class)
                .getPhysical().getFriendlyName();
        Equipments refEquipment = equipments.stream()
                .filter(equip -> equip.getEquipmentId().equals(equipId)).findAny().orElse(null);
        if (refEquipment == null) {
            log.error("the termination point :{} physically located on the card:{} is not existed",
                    terminationPointId, equipId);
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR, String.format(
                    "the termination point %s physically located on the card is not existed",
                    terminationPointId));
        }
        return TpRefElementInfoDto.builder().equipments(refEquipment).neId(neId)
                .neFriendlyName(neFriendlyName)
                .tpFriendlyName(tpFriendlyName)
                .ip(ip)
                .innerCrossConnections(innerCrossConnections)
                .build();
    }

    @Data
    @Builder
    public static class TpRefElementInfoDto {

        private String neId;
        private Equipments equipments;

        private String neFriendlyName;

        private String tpFriendlyName;

        private String ip;

        private List<CrossConnections> innerCrossConnections;
    }

    @Data
    @Builder
    public static class SiteInfo {

        private String siteId;

        private String siteName;
    }
}
