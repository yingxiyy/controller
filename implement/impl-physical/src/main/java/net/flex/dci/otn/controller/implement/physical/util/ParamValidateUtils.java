/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.implement.physical.util;

import static net.flex.dci.otc.common.constants.Constants.POUND;
import static net.flex.dci.otn.controller.implement.physical.util.Constants.CUSTOMER_DESCRIPTION_LENGTH;
import static net.flex.dci.otn.controller.implement.physical.util.Constants.CUSTOMER_INFO_LENGTH;
import static net.flex.dci.otn.controller.implement.physical.util.Constants.CUSTOM_INFO;
import static net.flex.dci.otn.controller.implement.physical.util.Constants.DESCRIPTION;
import static net.flex.dci.otn.controller.implement.physical.util.Constants.NE_SYSTEM_CURRENT_TIME_PROPERTIES;
import static net.flex.dci.otn.controller.implement.physical.util.Constants.NE_YANG_VERSION_PROPERTIES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.GetNeDataInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateEquipInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateNodeInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.UpdateTerminationPointInput;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.update.node.input.Nodes;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.update.termination.point.input.Tps;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.tunnel.rev180515.tunnel.Tunnel;

import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.SpringBeanFinder;
import net.flex.dci.otc.mongo.dao.PhyNodeDao;
import net.flex.dci.otc.mongo.dao.SiteLinkDao;
import net.flex.dci.otc.mongo.dao.TunnelDao;
import net.flex.dci.otc.mongo.dao.impl.PhyNodeDaoImpl;

/**
 * @version 1.0
 * @date 2021/11/9 17:00
 */
@Slf4j
public class ParamValidateUtils {


    private static final List<String> neProperties = new ArrayList<>();

    private static final Map<String, Integer> tpValidateLengthMap = new HashMap<>();


    static {
        neProperties.add(NE_YANG_VERSION_PROPERTIES);
        neProperties.add(NE_SYSTEM_CURRENT_TIME_PROPERTIES);

        tpValidateLengthMap.put(CUSTOM_INFO, CUSTOMER_INFO_LENGTH);
        tpValidateLengthMap.put(DESCRIPTION, CUSTOMER_DESCRIPTION_LENGTH);

    }

    /**
     * check get ne data input
     */
    public static void checkGetNeDataInput(GetNeDataInput input) throws CommonException {
        log.debug("to check get ne data input :{}", input);
        if (input.getProperties() == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "hasn't point out which attribute want to retrieve");
        }
        List<Property> propList = input.getProperties().getProperty();
        List<Property> diffProperty = new ArrayList<>();
        for (Property property : propList) {
            if (!neProperties.contains(property.getName())) {
                diffProperty.add(property);
            }
        }
        if (!diffProperty.isEmpty()) {
            StringBuilder stringBuilder = new StringBuilder();
            for (Property property : diffProperty) {
                stringBuilder.append(property.getName()).append(POUND);
            }
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "unsupported get ne data properties :" + stringBuilder.subSequence(0,
                            stringBuilder.length() - 1)
            );
        }

    }

    /**
     * check underLayer link like site link and tunnel
     */
    public static void checkUnderLayerLink(NodeId nodeId) {
        log.debug("find is there have link under the node :{}", nodeId.getValue());
        SiteLinkDao sitelinkDao = SpringBeanFinder.getBean(SiteLinkDao.class);
        List<Link> links = sitelinkDao.queryWithNode(nodeId.getValue());
        if (links != null && !links.isEmpty()) {
          throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format(
        		  "This Node[%s] is on SiteLink,pls remove sitelink resource first",
        		  nodeId.getValue()));
        }
        
        TunnelDao tunnelDao = SpringBeanFinder.getBean(TunnelDao.class);
        List<Tunnel> tunnels = tunnelDao.queryWithNode(nodeId.getValue());
        if (tunnels != null && !tunnels.isEmpty()) {
          throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format(
	          "This Node[%s] is on Tunnel,pls remove Tunnel resource first",
	          nodeId.getValue()));
        }
//        NmsRpc nmsService = SpringBeanFinder.getBean(NmsRpc.class);
//        GetSiteLinkPagedInput siteLinkInput = new GetSiteLinkPagedInputBuilder()
//                .setTopologyRef(new TopologyId(TopoNameConstants.Phy_Topo_Key))
//                .setNodeRef(nodeId)
//                .setHowMany(20)
//                .setStartPos(0)
//                .build();
//
//        GetSiteLinkPagedOutput siteLinkOutput = nmsService
//                .getSiteLinkPaged(siteLinkInput);
//        if (siteLinkOutput.getLink() != null && siteLinkOutput.getLink().size() > 0) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format(
//                    "This Node[%s] is on SiteLink,pls remove sitelink resource first",
//                    nodeId.getValue()));
//        }

//        GetTunnelPagedInput tunnelInput = new GetTunnelPagedInputBuilder()
//                .setTopologyRef(new TopologyId(TopoNameConstants.Phy_Topo_Key))
//                .setNodeRef(nodeId)
//                .setHowMany(20)
//                .setStartPos(0)
//                .build();
//        GetTunnelPagedOutput tunnelOutput = nmsService.getTunnelPaged(tunnelInput);
//        if (tunnelOutput.getTunnel() != null && tunnelOutput.getTunnel().size() > 0) {
//            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format(
//                    "This Node[%s] is on Tunnel,pls remove Tunnel resource first",
//                    nodeId.getValue()));
//        }
    }

    /**
     * check update equipment input
     *
     * @param input
     */
    public static void checkUpdateEquipmentInput(UpdateEquipInput input) {
        log.debug("check the update equipment input :{}", input);


    }


    public static void checkRemoveNeIp(Node phyNode) {
        log.debug("check the node can or can not remove the ne ip ");
        ImplementState state = phyNode.getAugmentation(Node1.class).getPhysical()
                .getImplementState();
        if (!(state.equals(ImplementState.Allocate) || state.equals(ImplementState.Plan))) {
            throw new CommonException(CommonExceptionType.NOT_SUPPORT_ERROR,
                    "cannot remove IP when the  node implement state isn't allocate ");
        }
    }

    public static void checkTerminationPointInput(UpdateTerminationPointInput input) {
        log.debug("start to check termination point input:{}", input);
        for (Tps tp : input.getTps()) {
            if (tp.getPhysical().getProperties() != null
                    && tp.getPhysical().getProperties().getProperty() != null
                    && tp.getPhysical().getProperties().getProperty().size() != 0) {
                for (Property pro : tp.getPhysical().getProperties().getProperty()) {
                    String proName = pro.getName();
                    String proValue = pro.getValue();
                    if (tpValidateLengthMap.containsKey(proName)) {
                        int validateLength = tpValidateLengthMap.get(proName);
                        if (proValue.length() > validateLength) {
                            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                                    "port's " + proName + " should be smaller than "
                                            + validateLength + " characters");
                        }

                    } else {
                        throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                                "tp can not support " + proName + " edit");
                    }
                }
            }
        }
    }


    public static void checkUpdateNeParam(UpdateNodeInput input) {

        if (input.getNodes() == null || input.getNodes().size() == 0) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                    "Failed to config phy node because node list is empty");
        }
        List<Nodes> nodeList = input.getNodes();

        //check validation of inputing node's parameter
        for (Nodes node : nodeList) {
            if (node.getNodeId() == null || node.getNodeId().getValue() == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "Failed to config phy node because node id is null");
            }
            if (node.getPhysical() == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "Failed to config phy node because physical is null");
            }
            PhyNodeDao phyNodeDao = SpringBeanFinder.getBean(PhyNodeDaoImpl.class);
            Node mongoNode = phyNodeDao.getConfigPhyNodeById(node.getNodeId().getValue());
            if (mongoNode == null) {
                throw new CommonException(CommonExceptionType.INVALID_PARAMETER,
                        "cannot find node " + node.getNodeId().getValue());
            }
        }
    }
}
