/*
 *  Copyright (c) 2019 Network Flex Any Comp. and others and others.  All rights reserved.
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux;

import java.util.List;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.frequency.Constant;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.Spectrum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

public class Mux96 extends MUX {

    public Mux96(NeYangModel model, String equipmentId, List<Link> ochList) {
        super(model, equipmentId, ochList, GridType._50);
    }

    static final short total = 96;
    int centSpan = Integer.valueOf(Constant.FrequencyInterval.MUX96) / 2;
    long centFreq = getLower() - centSpan;

    @Override
    short totalOchNumbers() {
        return total;
    }

    @Override
    short getDot() {
        return 3;
    }

    @Override
    public Spectrum getNextFrequency() {
        long lowFreq = centFreq - centSpan;
        long upFreq = centFreq + centSpan;

        Spectrum port = port(centFreq, lowFreq, upFreq, index);

        centFreq = centFreq - Integer.valueOf(Constant.FrequencyInterval.MUX96);
        index++;
        return port;
    }

}
