/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.allocate.designer.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.TopoPath;
import net.flex.dci.otc.serialization.JsonUtil;
import net.flex.dci.otn.controller.allocate.designer.model.site.ReallocateDataModel;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.site.topology.rev180426.reallocate.info.ReallocateEquipment;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier.InstanceIdentifierBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JsonOutputer {

    private ObjectMapper objectMapper;

    @Autowired
    JsonUtil jsonUtil;


    @PostConstruct
    private void init() {
        objectMapper = new ObjectMapper();
        // 允许注释
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        // 允许使用无引号属性名
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        // 允许单引号包住属性名和值
        objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
        // 允许非引号控制字符
        objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
        // null值不序列化
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        // 没有映射到的属性不抛出异常
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public String format(Object input) {
        try {
            return objectMapper.writeValueAsString(input);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert to json format for {}, return raw content instead.", input, e);
            return input.toString();
        }
    }

    public String formatNode(Node node) {
        try {
            InstanceIdentifierBuilder<Node> iid = TopoPath.getNtPhyNodePath(node.getNodeId().getValue());
            return jsonUtil.fromDataObjectToJson(jsonUtil.fromInstanceIdentifierToString(iid.build()), node);
        } catch (Exception e) {
            log.warn("Failed to convert to json format for {}, return raw content instead.", node, e);
            return node.toString();
        }
    }

    public String formatRouteInfo(RouteInfo routeInfo) {
        JsonObject output = new JsonObject();
        try {
            output.add("main", formatRoute(routeInfo.getMain()));
            output.add("slave", formatRoute(routeInfo.getSlave()));
            return output.toString();
        } catch (Exception e) {
            log.warn("Failed to convert to json format for {}, return raw content instead.", routeInfo, e);
            return routeInfo.toString();
        }
    }

    private JsonObject formatRoute(Route route) {
        if (route == null) {
            return null;
        }
        JsonObject output = new JsonObject();
        output.add("nodes", formatNodes(route.getNodes()));
        output.add("links", formatLinks(route.getLinks()));
        output.add("xcs", formatXcs(route.getXcs()));
        return output;
    }

    private JsonArray formatXcs(List<CrossConnections> xcs) {
        if (xcs == null) {
            return null;
        }
        JsonArray output = new JsonArray();
        for (CrossConnections xc : xcs) {
            output.add(new Gson().fromJson(formatXc(xc), JsonObject.class));
        }
        return output;
    }

    private String formatXc(CrossConnections xc) {
        try {
            InstanceIdentifierBuilder<CrossConnections> iid = TopoPath.getPhyNodeXcPath(xc.getNodeRef().getValue(), xc.getCrossConnectionId());
            return jsonUtil.fromDataObjectToJson(jsonUtil.fromInstanceIdentifierToString(iid.build()), xc);
        } catch (Exception e) {
            log.warn("Failed to convert to json format for {}, return raw content instead.", xc, e);
            return xc.toString();
        }
    }

    private JsonElement formatLinks(List<Link> links) {
        if (links == null) {
            return null;
        }
        JsonArray output = new JsonArray();
        for (Link link : links) {
            output.add(new Gson().fromJson(formatLink(link), JsonObject.class));
        }
        return output;
    }

    private JsonArray formatNodes(List<Node> nodes) {
        if (nodes == null) {
            return null;
        }
        JsonArray output = new JsonArray();
        for (Node node : nodes) {
            output.add(new Gson().fromJson(formatNode(node), JsonObject.class));
        }
        return output;
    }

    public String formatLink(Link link) {
        try {
            InstanceIdentifierBuilder<Link> iid = TopoPath.getPhyLinkPath(link.getLinkId().getValue());
            return jsonUtil.fromDataObjectToJson(jsonUtil.fromInstanceIdentifierToString(iid.build()), link);
        } catch (Exception e) {
            log.warn("Failed to convert to json format for {}, return raw content instead.", link, e);
            return link.toString();
        }
    }

    public String formatReallocateEquipments(ReallocateDataModel reallocateEquipments) {
        if (reallocateEquipments == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(reallocateEquipments);
        } catch (JsonProcessingException e) {
            log.warn("Failed to convert to json format for {}, return raw content instead.", reallocateEquipments, e);
            return reallocateEquipments.toString();
        }
    }

    public String fromOchLinkToJson(Link ochLink) {
        try {
            InstanceIdentifierBuilder<Link> iid = TopoPath.getOchLinkPath(ochLink.getLinkId().getValue());
            return jsonUtil.fromDataObjectToJson(jsonUtil.fromInstanceIdentifierToString(iid.build()), ochLink);
        } catch (Exception e) {
            log.warn("Failed to convert to json format for {}, return raw content instead.", ochLink, e);
            return ochLink.toString();
        }
    }
}
