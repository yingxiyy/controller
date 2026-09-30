/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.component.impl;


import static net.flex.dci.otn.controller.nms.utils.Constants.COMMA;
import static net.flex.dci.otn.controller.nms.utils.Constants.SUCCESS;

import com.alibaba.fastjson.JSON;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.namingrule.ViewNodeNamingRule;
import net.flex.dci.otn.controller.nms.component.ITopologyTree;
import net.flex.dci.otn.controller.nms.component.dto.ViewNodeWrapper;
import net.flex.dci.otn.controller.nms.enums.PathVariableType;
import net.flex.dci.otn.controller.nms.model.RestResult;
import net.flex.dci.otn.controller.nms.operations.ITopologyOperations;
import net.flex.dci.otn.controller.nms.properties.topo.TopologyAntPathMatcher;
import net.flex.dci.otn.controller.nms.properties.topo.TopologyAntPathMatcherMap;
import net.flex.dci.otn.controller.nms.utils.HttpUtils;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import net.flex.dci.otn.controller.nms.utils.SerializeUtils;
import net.flex.dci.otn.controller.nms.validator.TopologyOperationValidator;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.Node1Builder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.View;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.view.topology.rev180718.view.node.attributes.ViewBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

/**
 * @author: musa
 * @date: 2021/3/24
 */
@Slf4j
@Component
public class TopologyTree implements ITopologyTree {


    @Autowired
    private TopologyOperationValidator validator;

    @Autowired
    private ITopologyOperations topologyOperations;

    @Autowired
    private NetconfTopology netconfTopology;

    @Autowired
    private TopologyAntPathMatcherMap topologyAntPathMatcherMap;


    @Override
    public String getNetWorkTopology(String identifier) throws CommonException {
        log.debug("start to get network topology....");

        try {

            validator.validateIdentifier(identifier);
            String result = topologyOperations.getNetworkTopology(identifier);
            return result;
//            InstanceIdentifier<?> iid = SerializeUtils.fromString2InstanceIdentitfier(identifier);
//            DataObject dataObject = null;
//            if (iid.getTargetType() == NetworkTopology.class) {
//                dataObject = netconfTopology.getNetworkTopology(DataStoreType.CONFIG);
//            } else if (iid.getTargetType() == Topology.class) {
//                dataObject = netconfTopology
//                        .getTopology(iid.firstKeyOf(Topology.class).getTopologyId());
//            } else {
//                dataObject = netconfTopology.getNetworkTopology(iid);
//            }
//            if (dataObject == null) {
//                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
//                        "there have no network topology info ");
//            }
//            dataObject = enrichHandler.enrichProperties(dataObject);
//            return SerializeUtils.serializeDataObject2Json(identifier, dataObject);
        } catch (Exception ex) {
            log.error("failed to get networkTopology from the netconf tree {}", ex.getMessage(),
                    ex);
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to get network topology info :" + ex.getMessage(), ex);
        }
    }

    @Override
    public String mergeNetworkTopology(HttpServletRequest request,
            String identifier) throws CommonException {

        log.info("start to merge the network topology tree");
        //todo:validate the method
        try {
            validator.validateIdentifier(identifier);
            TopologyAntPathMatcher antMatcher = topologyAntPathMatcherMap.getTopologyAntPathMatcherByIdentifier(
                    identifier);
            String requestBody = HttpUtils.getRequestBody(request);
            if (antMatcher.getType().equals(PathVariableType.VIEW_NODE)) {
                coordinateUpdateViewNode(requestBody, identifier, antMatcher);
            } else {
                DataObject dataObject = SerializeUtils.serializeDataObject(identifier, requestBody);
                InstanceIdentifier<?> iid = SerializeUtils.fromString2InstanceIdentitfier(
                        identifier);
                netconfTopology.updateTopology(iid, dataObject);
            }
            return operationOk();
        } catch (Exception e) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR,
                    "failed to merge network topology:" + e.getMessage(), e);
        }

    }

    /**
     * special method to coordinate update view node
     *
     * @param requestBody
     */
    private void coordinateUpdateViewNode(String requestBody, String identified,
            TopologyAntPathMatcher antMatcher) {
        AntPathMatcher antPathMatcher = new AntPathMatcher();
        Map<String, String> antPathMap = antPathMatcher.extractUriTemplateVariables(
                antMatcher.getAntPathMatcher(), identified);
        String variable = antMatcher.getPathVariable();
        List<String> elements = Arrays.asList(variable.split(COMMA));
        String viewNodeId = antPathMap.get(elements.get(0));
        log.info("coordinate update view node :{}", viewNodeId);
        ViewNodeWrapper viewNodeWrapper = JSON.parseObject(requestBody, ViewNodeWrapper.class);
        String viewNodeRefSiteId = ViewNodeNamingRule.extractSiteId(viewNodeId);
        List<Node> viewNodes = netconfTopology.listAllViewNodesBySiteId(viewNodeRefSiteId);
        viewNodes.forEach(node -> {
            log.debug("update view node:{}", node.getNodeId().getValue());
            View oldView = node.getAugmentation(Node1.class).getView();
            NodeBuilder nodeBuilder = new NodeBuilder(node);
            Node1Builder node1Builder = new Node1Builder();
            ViewBuilder viewBuilder = new ViewBuilder(oldView);
            viewBuilder.setPosX(viewNodeWrapper.getView().getPosX());
            viewBuilder.setPosY(viewNodeWrapper.getView().getPosY());
            node1Builder.setView(viewBuilder.build());
            nodeBuilder.addAugmentation(Node1.class, node1Builder.build());
            netconfTopology.updateViewNode(nodeBuilder.build());
        });
    }

    private String operationOk() {
        RestResult res = new RestResult();
        res.setCode(200);
        res.setMessage(SUCCESS);
        return JSON.toJSONString(res);
    }


}
