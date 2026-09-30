/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux;

import static net.flex.dci.otn.controller.nms.utils.Constants.OCH_TOPO_KEY;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.flex.dci.otc.common.util.MuxCardPortFormatting;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.frequency.Constant;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.Scope;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.ScopeBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.frequency.map.ScopeKey;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.mux.spectrum.OchLinkBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.Spectrum;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.SpectrumBuilder;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.SpectrumKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.ImplementState;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.link.attributes.SupportingLink;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.och.topology.rev180515.Link1;

public abstract class MUX {

    protected final long speed = 299792458;
    protected short index = 1;
    protected FrequencyRange range;
    protected GridType grid;
    protected MuxCardPortFormatting formatting;

    protected NeYangModel neYangModel;

    protected List<Link> ochList = null;
    protected Map<Long, Integer> centreFreq = new HashMap<>();

    protected String equipmentId;

    protected Pattern pattern;

    public MUX(NeYangModel model, String equipmentId, List<Link> ochList, GridType grid) {
        this.formatting = new MuxCardPortFormatting(model);
        this.neYangModel = model;
        this.ochList = ochList;
        this.grid = grid;
        range = new FrequencyRange();
        switch (grid) {
            case _0:
            case _75:
                if (model.equals(NeYangModel.ChinaTelecom)) {
                    range.lower = Integer.valueOf(Constant.MaxUpperFrequency.MUX64_DX);
                    range.upper = Integer.valueOf(Constant.MaxLowerFrequency.MUX64_DX);
                } else {
                    range.lower = Integer.valueOf(Constant.MaxUpperFrequency.MUX64);
                    range.upper = Integer.valueOf(Constant.MaxLowerFrequency.MUX64);
                }
                break;
            case _50:
                if (model.equals(NeYangModel.ChinaTelecom)) {
                    range.lower = Integer.valueOf(Constant.MaxUpperFrequency.MUX96_DX);
                    range.upper = Integer.valueOf(Constant.MaxLowerFrequency.MUX96_DX);
                } else {
                    range.lower = Integer.valueOf(Constant.MaxUpperFrequency.MUX96);
                    range.upper = Integer.valueOf(Constant.MaxLowerFrequency.MUX96);
                }
                break;
            case _100:
                if (model.equals(NeYangModel.ChinaTelecom)) {
                    range.lower = Integer.valueOf(Constant.MaxUpperFrequency.MUX48_DX);
                    range.upper = Integer.valueOf(Constant.MaxLowerFrequency.MUX48_DX);
                } else {
                    range.lower = Integer.valueOf(Constant.MaxUpperFrequency.MUX48);
                    range.upper = Integer.valueOf(Constant.MaxLowerFrequency.MUX48);
                }
                break;
            case _150:
                if (model.equals(NeYangModel.ByteDance)) {
                    range.lower = Integer.valueOf(Constant.MaxUpperFrequency.MUX64_BD_C);
                    range.upper = Integer.valueOf(Constant.MaxLowerFrequency.MUX64_BD_L);
                }
                break;
        }
        this.equipmentId = equipmentId;
        this.pattern = Pattern.compile(formatting.getPortMatchingRegex());
    }

    protected int getLower() {
        return range.lower;
    }

    /*
            this method export all M?D? port related OCH frequency
             */
    public abstract Spectrum getNextFrequency();

    abstract short totalOchNumbers();

    abstract short getDot();

    public boolean hasNext() {
        if (index <= totalOchNumbers()) {
            return true;
        } else {
            return false;
        }
    }

    protected Spectrum port(long centFreq, long lowFreq, long upFreq, short index) {
        short total = totalOchNumbers();
        short dot = getDot();

        SpectrumBuilder sb = getSpectrum(centFreq, lowFreq, upFreq, dot);
        sb.setIndex((int) index);
        sb.setKey(new SpectrumKey((int) index));
        sb.setName(String.format(formatting.getPortOutputFormat(), index, index));
        for (Link link : ochList) {
            if (link.getSupportingLink() != null) {
                for (SupportingLink sLink : link.getSupportingLink()) {
                    if (sLink.getLinkRef().getValue().contains(sb.getName())) {
                        sb.setOchLink(new OchLinkBuilder()
                                .setTopologyId(new TopologyId(OCH_TOPO_KEY))
                                .setLinkId(link.getLinkId())
                                .setImplementState(link.getAugmentation(Link1.class).getOch()
                                        .getImplementState())
                                .build());
                        break;
                    }
                }
            }
        }

        centreFreq.put(centFreq, (int) index);
        return sb.build();
    }

    protected SpectrumBuilder getSpectrum(long centFreq, long lowFreq, long upFreq, short dot) {
        SpectrumBuilder sb = new SpectrumBuilder();
        sb.setCenterFrequency(new BigDecimal((double) centFreq / 1000000)
                .setScale(dot, BigDecimal.ROUND_HALF_UP).toString());
        sb.setLowerFrequency(
                new BigDecimal((double) lowFreq / 1000000).setScale(dot, BigDecimal.ROUND_HALF_UP)
                        .toString());
        sb.setUpperFrequency(
                new BigDecimal((double) upFreq / 1000000).setScale(dot, BigDecimal.ROUND_HALF_UP)
                        .toString());
        sb.setCenterLamda(new BigDecimal((double) speed / centFreq * 1000)
                .setScale(2, BigDecimal.ROUND_HALF_UP).toString());

        return sb;
    }

    public GridType getGrid() {
        return grid;
    }

    public FrequencyRange range() {
        return range;
    }

    public List<Scope> getScopes(Integer start, Integer end, int width, short ochIndex) {
        List<Scope> scopes = new LinkedList<>();
        Integer step = end;
        end = end + width;
        switch (this.grid) {
            case _50:
                step = Integer.valueOf(Constant.FrequencyInterval.MUX96);
                break;
            case _75:
                step = Integer.valueOf(Constant.FrequencyInterval.MUX64);
                break;
            case _100:
                step = Integer.valueOf(Constant.FrequencyInterval.MUX48);
                break;
            case _0:
                step = 6250;
                break;
        }
        for (int pos = start; pos >= end; pos -= step) {
            ScopeBuilder sb = new ScopeBuilder();
            sb.setLower(pos - width);
            sb.setUpper(pos);
            sb.setCentre(pos - width / 2);
            if (getGrid().equals(GridType._0)) {
                sb.setIndex((int) ochIndex);
            } else {
                sb.setIndex((int) getIndex(sb.getCentre()));
            }
            sb.setImplementState(ImplementState.Plan);
            sb.setKey(new ScopeKey(sb.getCentre()));

            scopes.add(sb.build());
        }

        return scopes;
    }

    private Short getIndex(Integer centre) {
        Long key = Long.valueOf(centre);
        if (centreFreq.containsKey(key)) {
            return centreFreq.get(key).shortValue();
        } else {
            return 222;
        }
    }


    public class FrequencyRange {

        public Integer lower;
        public Integer upper;
    }
}
