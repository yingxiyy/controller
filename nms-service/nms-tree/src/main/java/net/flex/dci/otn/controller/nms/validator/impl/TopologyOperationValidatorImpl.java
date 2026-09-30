package net.flex.dci.otn.controller.nms.validator.impl;


import static net.flex.dci.otn.controller.nms.utils.Constants.COMMA;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.OchLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalLinkIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalNodeIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.PhysicalTpIdNamingRule;
import net.flex.dci.otc.common.util.namingrule.ViewNodeNamingRule;
import net.flex.dci.otn.controller.nms.enums.PathVariableType;
import net.flex.dci.otn.controller.nms.properties.topo.TopologyAntPathMatcher;
import net.flex.dci.otn.controller.nms.properties.topo.TopologyAntPathMatcherMap;
import net.flex.dci.otn.controller.nms.validator.TopologyOperationValidator;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

/**
 * @version 1.0
 * @date 2022/4/27 13:54
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class TopologyOperationValidatorImpl implements TopologyOperationValidator {

    private final TopologyAntPathMatcherMap topologyAntPathMatcherMap;

    @SneakyThrows
    @Override
    public void validateIdentifier(String identifier) {
        log.debug("start to validate the identifier");

//        Map<String, TopologyAntPathMatcher> pathMatcherMap = topologyAntPathMatcherMap.getPathMatcherMap();
        TopologyAntPathMatcher relativeTopologyPathMatcher = topologyAntPathMatcherMap.getTopologyAntPathMatcherByIdentifier(
                identifier);
        if (relativeTopologyPathMatcher != null) {
            validateIdentifier(identifier, relativeTopologyPathMatcher);
        }
    }

//    private TopologyAntPathMatcher getRelativePathMatcher(String identifier,
//            Map<String, TopologyAntPathMatcher> pathMatcherMap) {
//        AntPathMatcher antPathMatcher = new AntPathMatcher();
//        Set<String> antPaths = pathMatcherMap.keySet();
//        Optional<String> path = antPaths.stream()
//                .filter(antPath -> antPathMatcher.match(antPath, identifier))
//                .findAny();
//        return path.map(pathMatcherMap::get).orElse(null);
//    }

    /**
     * validate identifier
     *
     * @param identifier
     * @param relativeTopologyPathMatcher
     */
    private void validateIdentifier(String identifier,
            TopologyAntPathMatcher relativeTopologyPathMatcher) {
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        String antPath = relativeTopologyPathMatcher.getAntPathMatcher();
        String variable = relativeTopologyPathMatcher.getPathVariable();
        PathVariableType validateResourceType = relativeTopologyPathMatcher.getType();
        List<String> elements = Arrays.asList(variable.split(COMMA));
        Map<String, String> uriVariable = antPathMatcher.extractUriTemplateVariables(antPath,
                identifier);
        validateElement(validateResourceType, elements, uriVariable);
    }

    private void validateElement(PathVariableType validateResourceType, List<String> elements,
            Map<String, String> uriVariable) {
        if (validateResourceType.equals(PathVariableType.SITE_NODE)) {
            String siteNodeId = uriVariable.get(elements.get(0));
            if (null == siteNodeId) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "the site id should not be null");
            }


        } else if (validateResourceType.equals(PathVariableType.VIEW_NODE)) {
            String viewNodeId = uriVariable.get(elements.get(0));
            if (null == viewNodeId) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "the view node id should not be null");
            }
            if (!ViewNodeNamingRule.isViewNodeId(viewNodeId)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "invalid view node id");
            }

        } else if (validateResourceType.equals(PathVariableType.SITE_LINK)) {
            String siteLinkId = uriVariable.get(elements.get(0));
            if (null == siteLinkId) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "the siteLink id should not be null");
            }

        } else if (validateResourceType.equals(PathVariableType.PHY_NODE)) {
            String phyNodeId = uriVariable.get(elements.get(0));
            if (null == phyNodeId) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "the physical node id should not be null");
            }
            //validate phy node id
            validatePhyNodeId(phyNodeId);

        } else if (validateResourceType.equals(PathVariableType.PHY_TP)) {
            String nodeId = uriVariable.get(elements.get(0));
            String phyTpId = uriVariable.get(elements.get(1));
            if (null == nodeId) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "the phy node id should not be null");
            }

            if (null == phyTpId) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "the terminationPoint id should not be null");
            }
            validatePhyNodeId(nodeId);
            validatePhyTpId(phyTpId);
        } else if (validateResourceType.equals(PathVariableType.PHY_LINK)) {
            String phyLinkId = uriVariable.get(elements.get(0));
            if (null == phyLinkId) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "the physical link id should not be null");
            }
            validatePhyLinkId(phyLinkId);
        } else if (validateResourceType.equals(PathVariableType.OCH_LINK)) {
            String ochLinkId = uriVariable.get(elements.get(0));
            if (null == ochLinkId) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "the och link id should not be null");
            }
            if (!OchLinkIdNamingRule.isOchLink(ochLinkId)) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "the och link id is invalid");
            }
        }
    }

    private void validatePhyLinkId(String phyLinkId) {
        if (!PhysicalLinkIdNamingRule.isPhysicalLinkId(phyLinkId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the physical link id is invalid");
        }
    }

    private void validatePhyTpId(String phyTpId) {
        if (!PhysicalTpIdNamingRule.isTpId(phyTpId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the physical TP id is invalid");
        }
    }

    /**
     * validate phy node id
     *
     * @param phyNodeId
     */
    private void validatePhyNodeId(String phyNodeId) {
        if (!PhysicalNodeIdNamingRule.isPhyNodeId(phyNodeId)) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "the physical node id is invalid");
        }
    }
}
