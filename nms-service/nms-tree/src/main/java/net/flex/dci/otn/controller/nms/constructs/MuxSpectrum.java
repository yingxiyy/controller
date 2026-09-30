/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.constructs;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.MuxCardPortFormatting;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.MUX;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.MUX.FrequencyRange;
import net.flex.dci.otn.controller.nms.nms.handler.LinkHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumOutputBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.Scope;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.Spectrum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.Node1;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.Physical;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;

/**
 * @author: xinyzhao
 * @date: 2021/4/9
 */
@Slf4j
public class MuxSpectrum {

    private final static TopologyId phyTopoId = new TopologyId(TopoNameConstants.Phy_Topo_Key);
    private List<Link> ochList = null;
    private MUX mux = null;
    private NodeId nodeId;
    private Equipments equip = null;
    private NetconfTopology netconfTopology;

    public MuxSpectrum(NetconfTopology netconfTopology, NodeId nodeId, String equipRef)
            throws Exception {
        log.debug("get spectrum for {}, {}", nodeId.getValue(), equipRef);
        this.nodeId = nodeId;
        this.netconfTopology = netconfTopology;

        Node ntNode = netconfTopology.getNeNode(nodeId.getValue());
        if (ntNode == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "cannot find required node: " + nodeId.getValue());
        }
        Physical phyNode = ntNode.getAugmentation(Node1.class).getPhysical();
        for (Equipments eq : phyNode.getEquipments()) {
            if (eq.getEquipmentId().equals(equipRef) && eq.getEquipType().name().contains("MUX")) {
                equip = eq;
                break;
            }
        }
        if (equip == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format(
                    "cannot find required equip on %s(%s): " + phyNode.getFriendlyName(), equipRef));
        }
        init(NeYangModel.getModel(phyNode));
    }

    public MuxSpectrum(NetconfTopology netconfTopology, NodeId nodeId) throws Exception {
        log.debug("get spectrum for {}", nodeId.getValue());
        this.nodeId = nodeId;
        this.netconfTopology = netconfTopology;

        Node ntNode = netconfTopology.getNode(phyTopoId, nodeId);
        if (ntNode == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, "cannot find required node: " + nodeId.getValue());
        }
        Physical phyNode = ntNode.getAugmentation(Node1.class).getPhysical();
        for (Equipments eq : phyNode.getEquipments()) {
            if (eq.getEquipType().name().contains("MUX")) {
                equip = eq;
                break;
            }
        }
        if (equip == null) {
            throw new CommonException(CommonExceptionType.INVALID_PARAMETER, String.format(
                    "cannot find required MUX equip on %s(%s): " + phyNode.getFriendlyName(), nodeId.getValue()));
        }

        init(NeYangModel.getModel(phyNode));
    }

    private void init(NeYangModel model) throws Exception {
        log.debug("equip config is {}", equip.getEquipTypeConfiged());
        ochList = new LinkHandler(netconfTopology)
                .getOchLinks(phyTopoId, nodeId, null, equip.getEquipmentId(), null, null, null);
//        if (equip.getEquipType().name().contains("MUX")) {
//            if (equip.getEquipTypeConfiged().contains("96")) {
//                mux = new Mux96(ochList);
//            } else if (equip.getEquipTypeConfiged().contains("64")) {
//                mux = new Mux64(ochList);
//            } else if (equip.getEquipTypeConfiged().contains("PANEL")) {
//                mux = new MuxFlex(ochList);
//            } else {
//                String msg = String.format("unknown equip class %s", equip.getEquipTypeConfiged());
//                throw new Exception(msg);
//            }
//        }

        if (equip.getEquipTypeConfiged().contains("PANEL")) {
            Collections.sort(ochList, new SortByName(model));
        }
    }

    public FrequencyRange getMuxSpectrumRange() {
        return mux.range();
    }

    public GridType getGrid() {
        return mux.getGrid();
    }

    public GetMuxSpectrumOutput getSpectrum() {
        List<Spectrum> specList = new LinkedList<>();
        while (mux.hasNext()) {
            specList.add(mux.getNextFrequency());
        }

        GetMuxSpectrumOutputBuilder ob = new GetMuxSpectrumOutputBuilder();
        ob.setSpectrum(specList);
        return ob.build();
    }

    public List<Scope> getScopes(Integer start, Integer end, int width, short ochIndex) {
        return mux.getScopes(start, end, width, ochIndex);
    }


    private class SortByName implements Comparator<Object> {

        MuxCardPortFormatting formatting;
        public SortByName(NeYangModel model) {
            formatting = new MuxCardPortFormatting(model);
        }

        @Override
        public int compare(Object arg0, Object arg1) {
            Link a = (Link) arg0;
            Link b = (Link) arg1;
            int aId = getIndex(a);
            int bId = getIndex(b);
            return aId - bId;
        }

        private int getIndex(Link link) {
            if (link.getSupportingLink() != null) {
                for (SupportingLink sLink : link.getSupportingLink()) {
                    Pattern pattern = Pattern.compile(formatting.getPortMatchingRegex());
                    Matcher matcher = pattern.matcher(sLink.getLinkRef().getValue());
                    if (matcher.find()) {
                        String name = matcher.group(0);
                        String[] ids = name.split(formatting.getKeyBeforChannelNo());
                        return Integer.parseInt(ids[1]);
                    }
                }
            }
            return 0;
        }
    }

//    public static void main(String[] args) {
//        String input = "OS-Site-1600851502338#Ne-1600851963969#MUXPANEL-1-50#PORT-1-50-M43D43-Site-1600851502338#Ne-1601189914228#LINECARD-1-1#PORT-1-1-L2";
//        String regex = "M\\d+D\\d+";
//        Pattern pattern = Pattern.compile(regex);
//        Matcher matcher = pattern.matcher(input);
//        List<String> set = new LinkedList<>();
//
//        while (matcher.find()) {
//            set.add(matcher.group());
//            System.out.println(matcher.group(0));
//            String name = matcher.group(0);
//            String[] ids = name.split("D");
//            System.out.println(Integer.parseInt(ids[1]));
//        }
//        System.out.println(set);
//    }
}
