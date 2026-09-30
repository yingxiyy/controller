package net.flex.dci.otn.controller.implement.physical.nbi.impl;


import static net.flex.dci.otn.controller.implement.common.utils.Constants.BLANK;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.constants.BroadCastConstant;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.CommonUtil;
import net.flex.dci.otc.common.util.PropertyTool;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.mongo.dao.OchLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.dto.LinkStateDto;
import net.flex.dci.otn.controller.implement.common.impl.TpUpdator;
import net.flex.dci.otn.controller.implement.common.nbi.impl.BaseImpl;
import net.flex.dci.otn.controller.implement.common.utils.AsynchronousExecutor;
import net.flex.dci.otn.controller.implement.common.utils.Constants;
import net.flex.dci.otn.controller.implement.physical.util.PhysicalNodeUtils;
import org.apache.commons.lang3.StringUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.TerminationPoint1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointOutput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.update.termination.point.input.Tps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @version 1.0
 * @date 2022/8/21 11:46
 */
@Component
@Slf4j
public class TerminationPointNeImpl extends BaseImpl {

    private static final String DELAY_MEASUREMENT_MODE = "delay-measurement-mode";

    @Autowired
    private TpUpdator tpUpdator;

    @Autowired
    private TunnelDao tunnelDao;

    @Autowired
    private OchLinkDao ochLinkDao;

    @Override
    public UpdateTerminationPointOutput updateTerminationPoint(UpdateTerminationPointInput input) {
        log.debug("start to update the termination point state,the input is :{}", input);
        validateParam(input);
        AsynchronousExecutor.execute(() -> {
            configTerminationPoint(input);
        });
        return super.updateTerminationPoint(input);
    }


    /**
     * asynchronous executor the config termination point
     *
     * @param input
     */
    private void configTerminationPoint(UpdateTerminationPointInput input) {

        log.info("update the termination point the input is:{}", input);
        List<Tps> tps = input.getTps();
        String refNodeId = tps.stream().map(tp -> tp.getPhysical().getNodeRef()).findFirst().get();
        Node refNode = phyNodeDao.getPhyNodeById(refNodeId);

        Physical phNodeAttr = refNode.getAugmentation(Node1.class).getPhysical();
        String neName = phNodeAttr.getFriendlyName();
        if (StringUtils.isBlank(neName)) {
            neName = refNodeId;
        }
        taskInfoMessage.setResourceId(refNodeId);

        for (Tps tp : input.getTps()) {
            String tpName = getTpName(refNode, tp);
            String resourceName = neName + "/" + tpName;
            try {
                tpUpdator.configTerminationPoint(tp);
                logMessage(BroadCastConstant.MODIFY_TP, resourceName, BLANK);
            } catch (Exception e) {
                log.error("update tp", e);
                logMessage(BroadCastConstant.MODIFY_TP, resourceName, e.getMessage());
            }
        }

    }

    private String getTpName(Node refNode, Tps tp) {
        Optional<TerminationPoint> opData = refNode.getTerminationPoint().parallelStream()
                .filter(x -> x.getTpId().getValue().equals(tp.getTpId().getValue())).findAny();
        if (opData.isPresent()) {
            if (opData.get().getAugmentation(TerminationPoint1.class) != null
                    && opData.get().getAugmentation(TerminationPoint1.class).getPhysical()
                    != null) {
                return opData.get().getAugmentation(TerminationPoint1.class).getPhysical()
                        .getFriendlyName();
            }
        }
        return null;
    }


    /**
     * validate the param for the input for updating termination point parameter
     *
     * @param input
     */
    private void validateParam(UpdateTerminationPointInput input) {
        log.debug("valid the param for update the termination point param");
        List<Tps> tps = input.getTps();
        HashMap<String, List<Tps>> map = new HashMap<>();
        for (Tps tp : tps) {
            List<Tps> tmps = map.getOrDefault(tp.getPhysical().getNodeRef(), new ArrayList<>());
            tmps.add(tp);
            map.put(tp.getPhysical().getNodeRef(), tmps);
        }
        if (map.size() != 1) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "this command only support action on one Node");
        }
        //check the tp is on the node
        //assume there have only one node for each update termination point
        String refNodeId = (String) map.keySet().toArray()[0];
        Node refNode = phyNodeDao.getConfigPhyNodeById(refNodeId);
        if (refNode == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the ref node id is invalided");
        }
        List<Tps> refTps = map.get(refNodeId);
        Set<String> missMatchTps = checkTpsIsInNode(refNode, refTps);
        if (!missMatchTps.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the update termination point id is invalided");
        }
        Set<String> refEquipmentIds = refTps.stream()
                .map(refTp -> PhysicalTpIdNamingRule.getEquipId(refTp.getTpId().getValue()))
                .collect(
                        Collectors.toSet());
        boolean rebooting = PhysicalNodeUtils.checkEquipmentsRebooting(refNode, refEquipmentIds);
        if (rebooting) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "The TP related equipment is rebooting");
        }
        checkDelayMeasurementMode(refNode, refTps);
        checkCustomInfoAndDescription(refTps);

    }

    private void checkDelayMeasurementMode(Node refNode, List<Tps> refTps) {
        for (Tps refTp : refTps) {
            if (!isEnableDelayMeasurementMode(refTp.getPhysical())) {
                continue;
            }

            TerminationPoint dbTp = getTp(refNode, refTp.getTpId().getValue());
            org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical dbPhysical =
                    dbTp.getAugmentation(TerminationPoint1.class).getPhysical();

            // Delay measurement is mutually exclusive between the two ends of one tunnel/OCH.
            if (dbPhysical.getOtuClient() != null) {
                checkTunnelPeerDelayMeasurement(refTp.getTpId().getValue());
            }
            if (dbPhysical.getOtuLine() != null) {
                checkOchPeerDelayMeasurement(refTp.getTpId().getValue());
            }
        }
    }

    private boolean isEnableDelayMeasurementMode(org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical physical) {
        return physical != null
                && "true".equalsIgnoreCase(
                PropertyTool.getValue(physical.getProperties(), DELAY_MEASUREMENT_MODE));
    }

    private void checkTunnelPeerDelayMeasurement(String tpId) {
        List<LinkStateDto> tunnelStates = tunnelDao.getAllTunnelsStateUnderTp(tpId);
        for (LinkStateDto tunnelState : tunnelStates) {
            Tunnel tunnel = tunnelDao.getTunnelById(tunnelState.getId());
            String peerTpId = getPeerTunnelTpId(tunnel, tpId);
            if (peerTpId != null && isTpDelayMeasurementModeEnabled(peerTpId)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "The peer tunnel TP already has delay-measurement-mode enabled");
            }
        }
    }

    private String getPeerTunnelTpId(Tunnel tunnel, String tpId) {
        String sourceTpId = tunnel.getSourceTp().get(0).getTpRef().getValue();
        String destinationTpId = tunnel.getDestinationTp().get(0).getTpRef().getValue();
        if (tpId.equals(sourceTpId)) {
            return destinationTpId;
        }
        if (tpId.equals(destinationTpId)) {
            return sourceTpId;
        }
        return null;
    }

    private void checkOchPeerDelayMeasurement(String tpId) {
        List<Link> ochLinks = ochLinkDao.queryWithTp(tpId);
        for (Link ochLink : ochLinks) {
            String peerTpId = getPeerOchTpId(ochLink, tpId);
            if (peerTpId != null && isTpDelayMeasurementModeEnabled(peerTpId)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "The peer OCH TP already has delay-measurement-mode enabled");
            }
        }
    }

    private String getPeerOchTpId(Link ochLink, String tpId) {
        String sourceTpId = ochLink.getSource().getSourceTp().getValue();
        String destinationTpId = ochLink.getDestination().getDestTp().getValue();
        if (tpId.equals(sourceTpId)) {
            return destinationTpId;
        }
        if (tpId.equals(destinationTpId)) {
            return sourceTpId;
        }
        return null;
    }

    private boolean isTpDelayMeasurementModeEnabled(String tpId) {
        String nodeId = PhysicalTpIdNamingRule.getNodeId(tpId);
        Node node = phyNodeDao.getConfigPhyNodeById(nodeId);
        TerminationPoint tp = getTp(node, tpId);
        org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.tp.attributes.Physical physical =
                tp.getAugmentation(TerminationPoint1.class).getPhysical();
        return "true".equalsIgnoreCase(
                PropertyTool.getValue(physical.getProperties(), DELAY_MEASUREMENT_MODE));
    }

    private TerminationPoint getTp(Node node, String tpId) {
        return node.getTerminationPoint().stream()
                .filter(tp -> tp.getTpId().getValue().equals(tpId))
                .findAny().get();
    }

    private void checkCustomInfoAndDescription(List<Tps> refTps) {
        log.debug("check custom info and description for tps:{}", refTps);
        List<TpProperty> tpProperties = refTps.stream()
                .filter(tp -> tp.getPhysical().getProperties() != null
                        && tp.getPhysical().getProperties().getProperty() != null
                        && tp.getPhysical().getProperties().getProperty().size() != 0)
                .map(tp -> TpProperty.builder().tpId(tp.getTpId().getValue())
                        .properties(tp.getPhysical().getProperties().getProperty()).build())
                .collect(Collectors.toList());
        Map<String, TpTextInfo> tpTextInfoMap = new HashMap<>();
        for (TpProperty tpProperty : tpProperties) {
            Map<String, Object> keyProperty = tpProperty.properties.stream().collect(HashMap::new,
                    (map, property) -> map.put(property.getKey().getName(), property.getValue()),
                    HashMap::putAll);
            String tpId = tpProperty.tpId;
            TpTextInfo tpTextInfo = TpTextInfo.builder().customInfo((String) keyProperty.get(
                            Constants.CUSTOM_INFO))
                    .description((String) keyProperty.get(Constants.DESCRIPTION)).build();
            tpTextInfoMap.put(tpId, tpTextInfo);
        }
        List<String> invalidTextInfoIds = validateTextInfo(tpTextInfoMap);
        if (!invalidTextInfoIds.isEmpty()) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "port's custom-info/description should be smaller than "
                            + Constants.CUSTOM_INFO_LENGTH + " characters");
        }
    }

    private List<String> validateTextInfo(Map<String, TpTextInfo> tpTextInfoMap) {
        List<String> invalidTpIds = new ArrayList<>();
        tpTextInfoMap.keySet().forEach(tp -> {
            TpTextInfo tpTextInfo = tpTextInfoMap.get(tp);
            if (tpTextInfo.customInfo != null) {
                if (tpTextInfo.customInfo.trim().length() > Constants.CUSTOM_INFO_LENGTH) {
                    log.error("tp :{},custom-info is larger than {} characters", tp,
                            Constants.CUSTOM_INFO);
                    invalidTpIds.add(tp);
                }

            }

            if (tpTextInfo.description != null) {
                if (tpTextInfo.description.trim().length() > Constants.DESCRIPTION_LENGTH) {
                    log.error("tp :{},description is larger than {} characters", tp,
                            Constants.DESCRIPTION_LENGTH);
                    if (!invalidTpIds.contains(tp)) {
                        invalidTpIds.add(tp);
                    }
                }

            }
        });
        return invalidTpIds;
    }


    private Set<String> checkTpsIsInNode(Node refNode, List<Tps> refTps) {
        log.debug("check the tp id is in the node");
        Node node = phyNodeDao.getPhyNodeById(refNode.getNodeId().getValue());
        Set<String> refTpIds = refTps.stream().map(refTp -> refTp.getTpId().getValue()).collect(
                Collectors.toSet());
        Set<String> configTpIds = node.getTerminationPoint().stream()
                .map(terminationPoint -> terminationPoint.getTpId().getValue()).collect(
                        Collectors.toSet());
        Set<String> missMatch = CommonUtil.getDifferenceSetByGuava(refTpIds, configTpIds);
        return missMatch;
    }

    @Data
    @Builder
    public static class TpProperty implements Serializable {

        private String tpId;

        private List<Property> properties;
    }


    @Data
    @Builder
    public static class TpTextInfo implements Serializable {

        private String customInfo;

        private String description;
    }
}
