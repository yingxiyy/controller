/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.component.resource.frequency;

import static net.flex.dci.otc.common.constants.Constants.POUND;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import net.flex.dci.otc.common.exception.CommonException;
import net.flex.dci.otc.common.exception.CommonExceptionType;
import net.flex.dci.otc.common.util.Constant;
import net.flex.dci.otc.common.util.TopoNameConstants;
import net.flex.dci.otn.controller.nms.constructs.MuxSpectrum;
import net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux.MUX.FrequencyRange;
import net.flex.dci.otn.controller.nms.nms.handler.LinkHandler;
import net.flex.dci.otn.controller.nms.utils.NetconfTopology;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.GetMuxSpectrumOutput;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.Scope;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.ScopeBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.ScopeKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.grouping.Map;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.grouping.MapBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.grouping.MapKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.links.Link;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.Spectrum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.otn.phy.topology.rev180514.phy.node.attributes.physical.Equipments;

/**
 * @date: 2021/4/9
 */
@Deprecated
@Slf4j
public class FrequencyMap {

    private final static String FREE = "free";
    private final static String BUSY = "busy";
    private final static TopologyId phyTopoId = new TopologyId(TopoNameConstants.Phy_Topo_Key);
    private final static TopologyId ochTopoId = new TopologyId(TopoNameConstants.Och_Topo_Key);
    private final NetconfTopology netconfTopology;
    private GridType grid;
    private List<Map> mapList;
    private int width = 75000;
    private int ochIndex = 222;
    private TerminationPoint ochTp;

    public FrequencyMap(NetconfTopology netconfTopology) {
        this.netconfTopology = netconfTopology;
    }

    public FrequencyMap getMap(NodeId nodeId, TpId tpId) throws CommonException {
        try {
            ochTp = netconfTopology.getTerminationPoint(phyTopoId, nodeId, tpId);
            if (ochTp == null) {
                String errInfo = String
                        .format("cannot find required TP %s, %s", nodeId.getValue(),
                                tpId.getValue());
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errInfo);
            }
            Equipments eq = netconfTopology
                    .getEquipment(phyTopoId, nodeId, getEquipId(tpId.getValue()));
            String equipClass =
                    eq.getEquipClass() != null ? eq.getEquipClass() : eq.getEquipTypeConfiged();
            if (equipClass.equals("T2X4C8")) {
                width = 75000;
            } else {
                width = 50000;
            }
            Link ntLink = getSiteLink(phyTopoId, nodeId, tpId);
            if (ntLink == null) {
                String errInfo = String
                        .format("find siteLink error based on required TP %s, %s",
                                nodeId.getValue(),
                                tpId.getValue());
                throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, errInfo);
            }
            NodeId opcNodeId = getNodeId(ntLink.getSource().getSourceTp().getValue());
            generateMap(opcNodeId);
            return this;
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }
    }

    private NodeId getNodeId(String tpId) {
        String[] ids = tpId.split(POUND);
        return new NodeId(ids[0] + POUND + ids[1]);
    }

    private String getEquipId(String tpId) {
        String[] ids = tpId.split(POUND);
        return ids[0] + POUND + ids[1] + POUND + ids[2];
    }

    private Link getSiteLink(TopologyId phyTopoId, NodeId nodeId, TpId tpId)
            throws CommonException {
        try {
            List<Link> ntLinks = new LinkHandler(netconfTopology)
                    .getSiteLink(phyTopoId, nodeId, null, null, tpId, null, null);
            if (ntLinks.size() != 1) {
                //the nodeId/tpId isn't OPC based.
                ntLinks = new LinkHandler(netconfTopology)
                        .getOchLinks(phyTopoId, nodeId, null, null, tpId, null, null);
                if (ntLinks.size() != 1) {
                    return null;
                } else {
                    ntLinks = new LinkHandler(netconfTopology)
                            .getSiteLink(ochTopoId, null, null, null, null,
                                    ntLinks.get(0).getLinkId(),
                                    null);
                    if (ntLinks.size() != 1) {
                        return null;
                    } else {
                        return ntLinks.get(0);
                    }
                }
            } else {
                return ntLinks.get(0);
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.INTERNAL_SERVICE_ERROR, ex.getMessage());
        }

    }


    private void generateMap(NodeId opcNodeId) throws CommonException {
        try {
            mapList = new LinkedList<>();

            MuxSpectrum mux = new MuxSpectrum(netconfTopology, opcNodeId);
            GetMuxSpectrumOutput spectrum = mux.getSpectrum();
            this.grid = mux.getGrid();
            FrequencyRange range = mux.getMuxSpectrumRange();

            List<MapBuilder> mapBList = new LinkedList<>();

            MapBuilder mapBuilder = new MapBuilder();
            mapBuilder.setStart(range.lower);
            mapBuilder.setEnd(range.upper);
            mapBuilder.setState(ImplementState.Plan);
            mapBList.add(mapBuilder);

            for (Spectrum spec : spectrum.getSpectrum()) {
                if (spec.getOchLink() != null) {
                    splitRange(mapBList, spec);
                    if (spec.getOchLink().getLinkId().getValue()
                            .contains(ochTp.getTpId().getValue())) {
                        ochIndex = spec.getIndex();
                    }
                }
            }

            Collections.sort(mapBList, new SortByStart());
            MapBuilder prev = null;
            Iterator<MapBuilder> iter = mapBList.iterator();
            while (iter.hasNext()) {
                //mereg busy blocks
                MapBuilder current = iter.next();
                if (current.getState().equals(BUSY)) {
                    if (prev == null) {
                        prev = current;
                    } else if (prev.getEnd().equals(current.getStart())) {
                        //previous end = current start, merge it
                        prev.getScope().addAll(current.getScope());
                        Collections.sort(prev.getScope(), new SortByUpper());
                        prev.setEnd(current.getEnd());
                        iter.remove();
                    }
                }
            }

            for (MapBuilder builder : mapBList) {
                //build scope list for free
                updateSpecMap(builder, mux);
                mapList.add(builder.build());
            }
        } catch (Exception ex) {
            throw new CommonException(CommonExceptionType.SERIALIZATION_ERROR, ex.getMessage());
        }
    }

    private void updateSpecMap(MapBuilder builder, MuxSpectrum mux) {
        builder.setKey(new MapKey(builder.getStart()));
        if (builder.getState().equals(FREE)) {
            builder.setScope(mux.getScopes(builder.getStart(), builder.getEnd(), width, (short)ochIndex));
        }
    }

    /**
     * initial  -------------+++++++---------+--------- och-spec           +++ end
     * ----------++++++++++---------+---------
     *
     * initial  -------------+++++++---------+--------- och-spec      +++ end
     * -----+++-----+++++++---------+--------- +
     *
     * @param mapBList
     * @param spec
     */
    private void splitRange(List<MapBuilder> mapBList, Spectrum spec) {
        int lower = convert2Int(spec.getLowerFrequency());
        int upper = convert2Int(spec.getUpperFrequency());
        int centre = convert2Int(spec.getCenterFrequency());

        List<MapBuilder> newList = new LinkedList<>();
        for (MapBuilder builder : mapBList) {
            int start = builder.getStart();
            int end = builder.getEnd();

            if (builder.getState().equals(BUSY)) {
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
                            newBuilder.setState(ImplementState.Plan);
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

    private int convert2Int(String floatString) {
        return new BigDecimal(Double.valueOf(floatString) * 1000000).intValue();
    }

    public GridType getGrid() {
        return grid;
    }

    public List<Map> getMap() {
        return mapList;
    }

    private class SortByStart implements Comparator<Object> {

        @Override
        public int compare(Object arg0, Object arg1) {
            MapBuilder a = (MapBuilder) arg0;
            MapBuilder b = (MapBuilder) arg1;
            return b.getStart() - a.getStart();
        }
    }


    private class SortByUpper implements Comparator<Object> {

        @Override
        public int compare(Object arg0, Object arg1) {
            Scope a = (Scope) arg0;
            Scope b = (Scope) arg1;
            return b.getUpper() - a.getUpper();
        }
    }
}

