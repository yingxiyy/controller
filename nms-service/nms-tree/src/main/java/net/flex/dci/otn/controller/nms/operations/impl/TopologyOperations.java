package net.flex.dci.otn.controller.nms.operations.impl;

import static net.flex.dci.otn.controller.nms.utils.Constants.COMMA;
import static net.flex.dci.otn.controller.nms.utils.Constants.MPO_SUFFIX;

import com.alibaba.fastjson.JSONObject;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.model.type.DataStoreType;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otn.controller.nms.enrich.TopologyOperationEnrichHandler;
import net.flex.dci.otn.controller.nms.enums.PathVariableType;
import net.flex.dci.otn.controller.nms.operations.ITopologyOperations;
import net.flex.dci.otn.controller.nms.operations.dto.TerminationPointDto;
import net.flex.dci.otn.controller.nms.properties.topo.TopologyAntPathMatcher;
import net.flex.dci.otn.controller.nms.properties.topo.TopologyAntPathMatcherMap;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import net.flex.dci.otn.controller.nms.utils.SerializeUtils;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

/**
 * @version 1.0
 * @date 10/16/2023 4:29 PM
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TopologyOperations implements ITopologyOperations {


    private final NetconfTopology netconfTopology;


    private final TopologyOperationEnrichHandler enrichHandler;

    private final TopologyAntPathMatcherMap topologyAntPathMatcherMap;


    @Override
    public String getNetworkTopology(String identifier) throws UnsupportedEncodingException {
        log.debug("get network topology the identifier is:{}", identifier);
        TopologyAntPathMatcher topologyAntPathMatcher = topologyAntPathMatcherMap.getTopologyAntPathMatcherByIdentifier(
                identifier);
        String topology = null;
        if (topologyAntPathMatcher == null) {
            topology = generalGetNetworkTopology(identifier);
        } else {
            PathVariableType variableType = topologyAntPathMatcher.getType();
            if (variableType.equals(PathVariableType.PHY_TP)) {
                topology = getTopologyInfoForTerminalPoint(identifier, topologyAntPathMatcher);
            } /*else if (variableType.equals(PathVariableType.SITE_LINK)) {
                topology = getTopologyInfoForSiteLink(identifier, topologyAntPathMatcher);
            } */ else {
                topology = generalGetNetworkTopology(identifier);
            }
        }
        return topology;
    }

    private String getTopologyInfoForSiteLink(String identifier,
            TopologyAntPathMatcher topologyAntPathMatcher) {
        log.debug("get topology info for the siteLink the info is:{}",
                topologyAntPathMatcher);
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        String variable = topologyAntPathMatcher.getPathVariable();
        List<String> variables = Arrays.asList(variable.split(COMMA));
        Map<String, String> uriVariable = antPathMatcher.extractUriTemplateVariables(
                topologyAntPathMatcher.getAntPathMatcher(),
                identifier);
        String siteLinkId = uriVariable.get(variables.get(0));
        Link siteLink = netconfTopology.getSiteLink(siteLinkId);
        siteLink = (Link) enrichHandler.enrichProperties(siteLink);
        String result = SerializeUtils.serializeDataObject2Json(identifier, siteLink);
        return result;
    }

    /**
     * get topology info for the termination point
     *
     * @param topologyAntPathMatcher
     * @return
     */
    private String getTopologyInfoForTerminalPoint(String identifier,
            TopologyAntPathMatcher topologyAntPathMatcher) {
        log.debug("get topology info for the termination point the info is:{}",
                topologyAntPathMatcher);
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        String variable = topologyAntPathMatcher.getPathVariable();
        List<String> variables = Arrays.asList(variable.split(COMMA));

        Map<String, String> uriVariable = antPathMatcher.extractUriTemplateVariables(
                topologyAntPathMatcher.getAntPathMatcher(),
                identifier);
        String terminalPointId = uriVariable.get(variables.get(1));
        String phyEqId = PhysicalTpIdNamingRule.getEquipId(terminalPointId);
        Equipments refEquip = netconfTopology.getEquipment(phyEqId);
        EquipType equipType = refEquip.getEquipType();
        String result = null;
        if (equipType.equals(EquipType.CMUX64) || equipType.equals(EquipType.MUXPANEL)) {
            result = getMPOTpInformation(topologyAntPathMatcher, terminalPointId);
        } else {
            TerminationPoint terminationPoint = netconfTopology.getTerminationPoint(
                    terminalPointId);
            result = SerializeUtils.serializeDataObject2Json(identifier, terminationPoint);
        }
        return result;
    }

    private String getMPOTpInformation(TopologyAntPathMatcher topologyAntPathMatcher,
            String terminalPointId) {
        String result = null;
        if (terminalPointId.endsWith(MPO_SUFFIX)) {
            result = generateLogicalTerminationPoint(topologyAntPathMatcher, terminalPointId);
        } else {
            TerminationPoint terminationPoint = netconfTopology.getTerminationPoint(
                    terminalPointId);
            String nodeId = PhysicalTpIdNamingRule.getNodeId(terminalPointId);
            String identifier = topologyAntPathMatcher.getPathIdentifier(nodeId, terminalPointId);
            result = SerializeUtils.serializeDataObject2Json(identifier, terminationPoint);
        }
        return result;
    }

    private String generateLogicalTerminationPoint(TopologyAntPathMatcher topologyAntPathMatcher,
            String terminalPointId) {
        log.debug("logical generate the termination point for tp :{}", terminalPointId);
        List<TerminationPoint> realTps = netconfTopology.getTerminationPointIdRegex(
                terminalPointId);
        if (realTps.isEmpty()) {
            throw new CommonException(CommonExceptionType.NOT_FOUND_ERROR,
                    String.format("can not find the tp,tp id is:%s", terminalPointId));
        }
        String nodeId = PhysicalTpIdNamingRule.getNodeId(terminalPointId);
        List<String> results = realTps.stream().map(realTp -> {
            String tpId = realTp.getTpId().getValue();
            String identifier = topologyAntPathMatcher.getPathIdentifier(nodeId, tpId);
            return SerializeUtils.serializeDataObject2Json(identifier, realTp);
        }).collect(Collectors.toList());
        TerminationPointDto terminationPoint = new TerminationPointDto();
        List<Object> tpInfo = results.stream()
                .map(result -> JSONObject.parseObject(result, TerminationPointDto.class)
                        .getTerminationPoint())
                .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);
        terminationPoint.setTerminationPoint(tpInfo);
        return JSONObject.toJSONString(terminationPoint);
    }


    private String generalGetNetworkTopology(String identifier) {
        InstanceIdentifier<?> iid = SerializeUtils.fromString2InstanceIdentitfier(identifier);
        DataObject dataObject = null;
        if (iid.getTargetType() == NetworkTopology.class) {
            dataObject = netconfTopology.getNetworkTopology(DataStoreType.CONFIG);
        } else if (iid.getTargetType() == Topology.class) {
            dataObject = netconfTopology
                    .getTopology(iid.firstKeyOf(Topology.class).getTopologyId());
        } else {
            dataObject = netconfTopology.getNetworkTopology(iid);
        }
        if (dataObject == null) {
            log.warn("Topology not found for identifier: {}", identifier);
            return "{}";
        }
        dataObject = enrichHandler.enrichProperties(dataObject);
        return SerializeUtils.serializeDataObject2Json(identifier, dataObject);
    }
}
