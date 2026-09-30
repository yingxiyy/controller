package net.flex.dci.otn.controller.nms.nms.component.resource.muxspectrum.mux;

import java.util.List;
import net.flex.dci.otc.common.util.NeYangModel;
import net.flex.dci.otc.common.util.frequency.Constant.FrequencyInterval;
import org.opendaylight.yang.gen.v1.http.flex.net.dci.otc.ns.nms.rev180816.spectrum.list.grouping.Spectrum;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.common.otn.types.rev180515.GridType;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Link;

/**
 * 2026/1/26
 *
 * @author musa
 * @version 1.0
 **/
public class PMUX64 extends MUX {

    public PMUX64(NeYangModel model,
            String equipmentId,
            List<Link> ochList) {
        super(model, equipmentId, ochList, GridType._75);
        range.upper = 196125000;
        range.lower = 191325000;
    }

    private static final short total = 64;
    int centSpan = Integer.valueOf(FrequencyInterval.MUX64) / 2;

    private long centFreq = getLower() - centSpan;

    @Override
    public Spectrum getNextFrequency() {
        long lowFreq = centFreq - centSpan;
        long upFreq = centFreq + centSpan;

        Spectrum port = port(centFreq, lowFreq, upFreq, index);

        centFreq = centFreq - Integer.valueOf(FrequencyInterval.MUX64);
        index++;
        return port;
    }

    @Override
    short totalOchNumbers() {
        return total;
    }

    @Override
    short getDot() {
        return 5;
    }
}
