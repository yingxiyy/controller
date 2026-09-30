/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.constructs;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.util.MuxCardPortFormatting;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetWssChannelOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetWssChannelOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.Channel;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.ChannelBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.get.wss.channel.output.UpdateRangeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.properties.properties.Property;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.EquipType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.cross.connections.CrossConnections;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.wdm.attributes.WssChannelBuilder;

/**
 * @date: 2021/4/9
 */
@Slf4j
public class WssChannelConstructor {

    private final static TopologyId phyTopoId = new TopologyId(TopoNameConstants.Phy_Topo_Key);
    private final static String MaxDestToSourceVoa = "wss_max-dest-to-source-voa";
    private final static String MinDestToSourceVoa = "wss_min-dest-to-source-voa";
    private final static String MaxSourceToDestVoa = "wss_max-source-to-dest-voa";
    private final static String MinSourceToDestVoa = "wss_min-source-to-dest-voa";
    private final NodeId nodeId;
    private final NetconfTopology netconfTopology;

    public WssChannelConstructor(NodeId nodeId, NetconfTopology netconfTopology) {
        this.nodeId = nodeId;
        this.netconfTopology = netconfTopology;
    }

    public GetWssChannelOutput getChanels() throws Exception {
        GetWssChannelOutputBuilder output = new GetWssChannelOutputBuilder();

        Node ntNode = netconfTopology.getNode(phyTopoId, nodeId);
        if (ntNode == null) {
            throw new Exception("required node is not found");
        }

        Node1 phyNode = ntNode.getAugmentation(Node1.class);
        if (phyNode == null) {
            throw new Exception(
                    "the node hasn't physical structure");
        }

        boolean found = false;
        if (phyNode.getPhysical().getEquipments() == null) {
            throw new Exception(
                    "the node hasn't equipment");
        }

        for (Equipments eq : phyNode.getPhysical().getEquipments()) {
            if (eq.isEmpty() == null || eq.isEmpty() == true) {
                continue;
            }
            if (eq.getEquipType().equals(EquipType.CMUX64) || eq.getEquipType()
                    .equals(EquipType.WSS)) {
                output.setUpdateRange(new UpdateRangeBuilder()
                        .setMaxDestToSourceVoa(getRange(phyNode, MaxDestToSourceVoa))
                        .setMinDestToSourceVoa(getRange(phyNode, MinDestToSourceVoa))
                        .setMaxSourceToDestVoa(getRange(phyNode, MaxSourceToDestVoa))
                        .setMinSourceToDestVoa(getRange(phyNode, MinSourceToDestVoa))
                        .build()
                );
                found = true;
                break;
            }
        }

        if (found) {
            if (phyNode.getPhysical().getCrossConnections() == null) {
                throw new Exception(
                        "the node hasn't cross connection");
            }

            NeYangModel model = NeYangModel.getModel(phyNode.getPhysical());
            List<Channel> wssXcList = getAllWssXC(phyNode.getPhysical());
            Collections.sort(wssXcList, new SortByName(model));
            output.setChannel(wssXcList);
        }
        return output.build();
    }

    private BigDecimal getRange(Node1 phyNode, String key) {
        if (phyNode.getPhysical().getProperties() == null
                || phyNode.getPhysical().getProperties().getProperty() == null) {
            log.debug("cannot find required %s on node %s ", key, nodeId.getValue());
            return new BigDecimal(0);
        }
        for (Property prop : phyNode.getPhysical().getProperties().getProperty()) {
            if (prop.getKey().getName().equals(key)) {
                return new BigDecimal(prop.getValue());
            }
        }
        return new BigDecimal(0);
    }

    private List<Channel> getAllWssXC(Physical nodeAttr) {
        List<Channel> wssChannels = new LinkedList<>();
        NeYangModel model = NeYangModel.getModel(nodeAttr);
        MuxCardPortFormatting formatting = new MuxCardPortFormatting(model);
        for (CrossConnections xc : nodeAttr.getCrossConnections()) {
            if (xc.getCrossConnectionId().getValue().contains("frequency")) {

                Pattern pattern = Pattern.compile(formatting.getPortMatchingRegex());
                Matcher matcher = pattern.matcher(xc.getCrossConnectionId().getValue());
                if (matcher.find()) {
                    try {
                        Channel cb = new ChannelBuilder()
                                .setName(matcher.group(0))
                                .setWssChannel(new WssChannelBuilder()
                                        .setVoaUpdateModel(xc.getWssChannel().getVoaUpdateModel())
                                        .setDestToSourceVoa(xc.getWssChannel().getDestToSourceVoa())
                                        .setSourceToDestVoa(xc.getWssChannel().getSourceToDestVoa())
                                        .build())
                                .build();
                        wssChannels.add(cb);
                    } catch (NullPointerException e) {
                        log.debug("the XC %s is NOT contain wss channel.",
                                xc.getCrossConnectionId().getValue());
                    }
                }
            }
        }
        return wssChannels;
    }

    private class SortByName implements Comparator<Object> {

        MuxCardPortFormatting formatting;

        public SortByName(NeYangModel model) {
            formatting = new MuxCardPortFormatting(model);
        }

        @Override
        public int compare(Object arg0, Object arg1) {
            Channel a = (Channel) arg0;
            Channel b = (Channel) arg1;
            int aId = getIndex(a);
            int bId = getIndex(b);
            return aId - bId;
        }

        private int getIndex(Channel xc) {
            String[] ids = xc.getName().split(formatting.getKeyBeforChannelNo());
            return Integer.parseInt(ids[1]);
        }
    }

}
